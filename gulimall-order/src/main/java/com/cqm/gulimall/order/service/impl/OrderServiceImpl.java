package com.cqm.gulimall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cqm.common.exception.NoStockException;
import com.cqm.common.to.mq.OrderTo;
import com.cqm.common.utils.R;
import com.cqm.common.vo.MemberRespVo;
import com.cqm.gulimall.order.constant.OrderConstant;
import com.cqm.gulimall.order.dao.OrderItemDao;
import com.cqm.gulimall.order.entity.OrderItemEntity;
import com.cqm.gulimall.order.enume.OrderStatusEnum;
import com.cqm.gulimall.order.feign.CartFeignService;
import com.cqm.gulimall.order.feign.MemberFeignService;
import com.cqm.gulimall.order.feign.ProductFeignService;
import com.cqm.gulimall.order.feign.WmsFeignService;
import com.cqm.gulimall.order.interceptor.LoginUserInterceptor;
import com.cqm.gulimall.order.service.OrderItemService;
import com.cqm.gulimall.order.to.OrderCreateTo;
import com.cqm.gulimall.order.vo.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.order.dao.OrderDao;
import com.cqm.gulimall.order.entity.OrderEntity;
import com.cqm.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.alibaba.fastjson.TypeReference;

import javax.swing.tree.TreeNode;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    public static ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * ???????????????????????????????????????
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //???????????????ThreadLocal???????????????????????????????????????????????????
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            //1.?????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            //???????????????RequestContextHolder
//            System.out.println("?????????adress??????id???"+ Thread.currentThread().getId());
            confirmVo.setAddress(address);
        }, executor);


        CompletableFuture<Void> cartFeature = CompletableFuture.runAsync(() -> {
            //2.?????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            //feign????????????????????????????????????????????????????????????????????????RequestInterceptor interceptor:interceptors
            // feign???????????????????????????????????????????????????????????????????????????????????????
//            System.out.println("?????????cart??????id???"+ Thread.currentThread().getId());
            confirmVo.setItems(currentUserCartItems);
        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R skuHasSock = wmsFeignService.getSkuHasSock(collect);
            List<SkuStockVo> data = skuHasSock.getData("data", new TypeReference<List<SkuStockVo>>() {
            });
            if(data!=null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        },executor);


        //3.??????????????????
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);
        //4.????????????????????????

        //todo 5.???????????????
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId(),token,30, TimeUnit.MINUTES);

        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddress,cartFeature).get();

        return confirmVo;
    }

    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        submitVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();

        responseVo.setCode(0);
        String orderToken = vo.getOrderToken();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
//        String redisToken = (String) redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        //0 ????????????  1 ????????????
        //lua?????????????????????????????????
        String lua = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long execute = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(lua, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if(execute == 0L){
            //??????????????????
            responseVo.setCode(1);
            return responseVo;
        }else {
            //??????????????????
            //xiadan :qu ????????????????????????????????????????????????
            //1.????????????
            OrderCreateTo order = createOrder();
            //2.??????
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
           if( Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //??????????????????
                //...
               //3.???????????????
               saveOrder(order);
               //4.????????????????????????????????????????????????
               //??????????????????????????????skuId???skuName???num???
               WareSkuLockVo lockVo = new WareSkuLockVo();
               lockVo.setOrderSn(order.getOrder().getOrderSn());
               List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                   OrderItemVo orderItemVo = new OrderItemVo();
                   orderItemVo.setSkuId(item.getSkuId());
                   orderItemVo.setCount(item.getSkuQuantity());
                   orderItemVo.setTitle(item.getSkuName());
                   return orderItemVo;
               }).collect(Collectors.toList());
               lockVo.setLocks(locks);

               //todo ???????????????
               R r = wmsFeignService.orderLockStock(lockVo);
               if(r.getCode()==0){
                   //????????????
//                   int i = 10/0;
                   responseVo.setOrder(createOrder().getOrder());
                   responseVo.setCode(0);
                   //todo ???????????????????????????mq????????????
                   rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder()) ;
                   return responseVo;

               }else {
                   //????????????

                   String msg = "???????????????????????????????????????";
                   throw new NoStockException(msg);

               }
           }else {
               responseVo.setCode(2);
               return responseVo;
           }

        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;



    }

    @Override
    public void closeOrder(OrderEntity entity) {
        OrderEntity orderEntity = this.getById(entity.getId());
        if(orderEntity.getStatus()== OrderStatusEnum.CREATE_NEW.getCode()){
             //guan???
            com.cqm.gulimall.order.entity.OrderEntity orderEntity1 = new OrderEntity();
            orderEntity1.setId(orderEntity.getId());
            orderEntity1.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderEntity1);
            //?????????????????????mq?????????????????????????????????????????????????????????????????????????????????????????????ware??????????????????????????????
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);

        }
    }

    /**
     * ??????????????????
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);


        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(){

        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1.??????????????? mybatis-plus????????? ??????+id ???????????????
        String orderSn = IdWorker.getTimeId();
        //c1.????????????
        OrderEntity orderEntity = BuildOrder(orderSn);

        //2.???????????????????????????
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);


        //3.?????????????????? / ?????????????????????
        computePrice(orderEntity,itemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        return orderCreateTo;

    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        BigDecimal giftIntegration = new BigDecimal("0.0");
        //????????????
        for (OrderItemEntity itemEntity : itemEntities) {
            total = total.add(itemEntity.getRealAmount());
            coupon = coupon.add( itemEntity.getCouponAmount());
            integration = integration.add( itemEntity.getIntegrationAmount());
            promotion = promotion.add(  itemEntity.getPromotionAmount());
            //???????????????????????????
            growth = growth.add(new BigDecimal(itemEntity.getGiftGrowth().toString()));
            giftIntegration = giftIntegration.add(new BigDecimal(itemEntity.getGiftIntegration().toString()));
        }
        //1.??????????????????
        orderEntity.setTotalAmount(total);
        //????????????
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //????????????
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        //?????????????????????
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0);//?????????

    }

    /**
     * ?????????????????????
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //????????????????????????????????????
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null&&currentUserCartItems.size()>0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * ????????????????????????
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1.????????????????????????
        //2.?????????spu??????
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoByskuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.?????????sku??????
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());

        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //4.????????????????????????
        //5.????????????
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //????????????????????????
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        //??????????????????????????????  ??????-???????????? ??????????????????
        itemEntity.setRealAmount(itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString())));
        return itemEntity;

    }


    /**
     * ?????????????????????????????????
     * @param timeId
     * @return
     */
    private OrderEntity BuildOrder(String timeId) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(timeId);
        orderEntity.setMemberId(memberRespVo.getId());
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();

        //??????????????????
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData("data", new TypeReference<FareVo>() {
        });

        //??????????????????
        orderEntity.setFreightAmount(fareResp.getFare());
        //?????????????????????
        orderEntity.setReceiverCity(fareResp.getAddressVo().getCity());
        orderEntity.setReceiverDetailAddress(fareResp.getAddressVo().getDetailAddress());
        orderEntity.setReceiverName(fareResp.getAddressVo().getName());
        orderEntity.setReceiverPhone(fareResp.getAddressVo().getPhone());
        orderEntity.setReceiverProvince(fareResp.getAddressVo().getProvince());
        orderEntity.setReceiverPostCode(fareResp.getAddressVo().getPostCode());
        orderEntity.setReceiverRegion(fareResp.getAddressVo().getRegion());

        //??????????????????
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

}