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
     * 给订单确认页返回需要的数据
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //异步调用，ThreadLocal不在共享，必须要需要的数据在放一边
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货地址
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            //本线程内的RequestContextHolder
//            System.out.println("党全面adress线程id："+ Thread.currentThread().getId());
            confirmVo.setAddress(address);
        }, executor);


        CompletableFuture<Void> cartFeature = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            //feign在进行远程调用之前，要构造请求，调用很多的拦截器RequestInterceptor interceptor:interceptors
            // feign在远程调用的时候是创建新的请求，这个请求会把请求头全部丢失
//            System.out.println("党全面cart线程id："+ Thread.currentThread().getId());
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


        //3.查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);
        //4.其他数据自动计算

        //todo 5.防重复令牌
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
        //0 代表失败  1 删除成功
        //lua原子验证令牌和删除令牌
        String lua = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long execute = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(lua, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if(execute == 0L){
            //令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        }else {
            //令牌验证成功
            //xiadan :qu 创建订单，验令牌，验价格，锁库存
            //1.创建订单
            OrderCreateTo order = createOrder();
            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
           if( Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //金额对比成功
                //...
               //3.保存订单项
               saveOrder(order);
               //4.库存锁定，只要有异常回滚订单数据
               //订单号，所有订单项（skuId，skuName，num）
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

               //todo 远程锁库存
               R r = wmsFeignService.orderLockStock(lockVo);
               if(r.getCode()==0){
                   //锁成功了
//                   int i = 10/0;
                   responseVo.setOrder(createOrder().getOrder());
                   responseVo.setCode(0);
                   //todo 订单创建成功，发送mq确认消息
                   rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder()) ;
                   return responseVo;

               }else {
                   //锁失败了

                   String msg = "库存锁定失败，商品库存不足";
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
             //guan单
            com.cqm.gulimall.order.entity.OrderEntity orderEntity1 = new OrderEntity();
            orderEntity1.setId(orderEntity.getId());
            orderEntity1.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderEntity1);
            //关单结束，也给mq发一个消息，让其主动解锁，防止因网络抖动使得订单状态还未修改，ware库存解锁逻辑已经走完
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);

        }
    }

    /**
     * 保存订单数据
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
        //1.生成订单号 mybatis-plus自带的 时间+id 唯一订单号
        String orderSn = IdWorker.getTimeId();
        //c1.创建订单
        OrderEntity orderEntity = BuildOrder(orderSn);

        //2.获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);


        //3.计算价格相关 / 积分等相关信息
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
        //订单总额
        for (OrderItemEntity itemEntity : itemEntities) {
            total = total.add(itemEntity.getRealAmount());
            coupon = coupon.add( itemEntity.getCouponAmount());
            integration = integration.add( itemEntity.getIntegrationAmount());
            promotion = promotion.add(  itemEntity.getPromotionAmount());
            //当前订单获得的积分
            growth = growth.add(new BigDecimal(itemEntity.getGiftGrowth().toString()));
            giftIntegration = giftIntegration.add(new BigDecimal(itemEntity.getGiftIntegration().toString()));
        }
        //1.订单价格相关
        orderEntity.setTotalAmount(total);
        //应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //优惠总额
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        //总积分和成长值
        orderEntity.setIntegration(giftIntegration.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0);//未删除

    }

    /**
     * 构建所有订单项
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
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
     * 构建每一个订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //1.订单信息：订单号
        //2.商品的spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoByskuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());

        //3.商品的sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());

        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //4.优惠信息【不做】
        //5.积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        //当前订单项的实际金额  总额-各种优惠 这里略过没减
        itemEntity.setRealAmount(itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString())));
        return itemEntity;

    }


    /**
     * 构建订单，保存基本信息
     * @param timeId
     * @return
     */
    private OrderEntity BuildOrder(String timeId) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(timeId);
        orderEntity.setMemberId(memberRespVo.getId());
        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();

        //获取收货地址
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData("data", new TypeReference<FareVo>() {
        });

        //设置运费信息
        orderEntity.setFreightAmount(fareResp.getFare());
        //设置收货人信息
        orderEntity.setReceiverCity(fareResp.getAddressVo().getCity());
        orderEntity.setReceiverDetailAddress(fareResp.getAddressVo().getDetailAddress());
        orderEntity.setReceiverName(fareResp.getAddressVo().getName());
        orderEntity.setReceiverPhone(fareResp.getAddressVo().getPhone());
        orderEntity.setReceiverProvince(fareResp.getAddressVo().getProvince());
        orderEntity.setReceiverPostCode(fareResp.getAddressVo().getPostCode());
        orderEntity.setReceiverRegion(fareResp.getAddressVo().getRegion());

        //设置订单状态
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

}