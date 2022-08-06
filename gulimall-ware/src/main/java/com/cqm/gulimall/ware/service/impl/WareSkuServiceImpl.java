package com.cqm.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.exception.NoStockException;
import com.cqm.common.to.mq.OrderTo;
import com.cqm.common.to.mq.StockLockedTo;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;
import com.cqm.common.utils.R;
import com.cqm.gulimall.ware.dao.WareSkuDao;
import com.cqm.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.cqm.gulimall.ware.entity.WareOrderTaskEntity;
import com.cqm.gulimall.ware.entity.WareSkuEntity;
import com.cqm.gulimall.ware.feign.OrderFeignService;
import com.cqm.gulimall.ware.feign.ProductFeignService;
import com.cqm.gulimall.ware.service.WareOrderTaskDetailService;
import com.cqm.gulimall.ware.service.WareOrderTaskService;
import com.cqm.gulimall.ware.service.WareSkuService;
import com.cqm.gulimall.ware.vo.OrderItemVo;
import com.cqm.gulimall.ware.vo.OrderVo;
import com.cqm.gulimall.ware.vo.SkuHasStockVo;
import com.cqm.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    /**
     * *  wareId: 123,//仓库id
     * *    skuId: 123//商品id
     *
     * @param params
     * @return
     */

    @Autowired
    private WareSkuDao skuDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProductFeignService productFeignService;


    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;


    public void unLockedStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        skuDao.unlockStock(skuId, wareId, num);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {


        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {


        //1.判断是否是第一次更改
        List<WareSkuEntity> wareSkuEntities = skuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        WareSkuEntity wareSkuEntity = new WareSkuEntity();
        if (wareSkuEntities != null && wareSkuEntities.size() != 0) {
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum + wareSkuEntities.get(0).getStock());
            skuDao.update(wareSkuEntity, new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        } else {
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);
            //todo 远程查询sku的名字，如果失败，整个事务无需回滚
            //1.自己catch异常
            //todo 还可以用什么方法让异常以后不回滚

            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {

                    Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
            }

            skuDao.insert(wareSkuEntity);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            //查询当前sku的总库存量
            //select sum(stock-stock_locked) from `wms_ware_sku` where sku_id=1
            Long count = baseMapper.getSkuStock(skuId);

            skuHasStockVo.setSkuId(skuId);
            if (count != null) {
                skuHasStockVo.setHasStock(count > 0);
            } else {
                skuHasStockVo.setHasStock(false);
            }

            return skuHasStockVo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * throw new NoStockException(skuId);
     * 默认只要是运行时异常都会回滚
     * 为某个订单锁定库存
     *
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {

        /**
         * 保存库存工作单详情
         * 追溯后期补偿
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);


        //1.按照下单的收货地址，找到一个就近仓库，锁定库存

        //1.找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stockVo = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stockVo.setSkuId(skuId);
            stockVo.setNum(item.getCount());
            List<Long> wareIds = skuDao.listWareIdHasSkuStock(skuId);
            stockVo.setWareId(wareIds);
            return stockVo;
        }).collect(Collectors.toList());

        Boolean allLock = true;
        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功就返回1，否则就是0
                Long count = skuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    //todo 告诉mq库存锁定成功
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, null, hasStock.getNum(), wareOrderTaskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    stockLockedTo.setDetailId(wareOrderTaskDetailEntity.getId());
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);


                    break;//有一个成功就break
                } else {
                    //当前仓库锁失败，换下一个库
                }
            }
            if (skuStocked == false) {
                //当前商品的所有仓库都没有锁成功
                throw new NoStockException(skuId);
            }
        }

        //3.全部锁定成功


        return true;
    }

    /**
     * 解锁库存
     *
     * @param to
     */
    @Override
    public void unlockStock(StockLockedTo to) throws IOException {

        System.out.println("收到解锁库存的消息");
        Long id = to.getId();
        Long detailId = to.getDetailId();
        //1.解锁
        //1.查询数据库关于这个订单的锁定库存信息
        //有：
        //   解锁：订单情况
        //1.没有这个订单。必须解锁
        //2.有这个订单，不是解锁库存
        //订单状态：已取消 解锁 ，没取消，不能解锁
        //m没有：库存锁定失败了，已经自动回滚了不需要解锁

        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            //解锁
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                //返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });

                if (data == null || data.getStatus() == 4) {
                    //订单不存在
                    //订单已经被取消了
                    unLockedStock(byId.getSkuId(), byId.getWareId(), byId.getSkuNum(), detailId);

                }

            } else {
                //消息被拒绝从新放入队列
                throw new RuntimeException("远程服务失败");
            }


        } else {
            //无需解锁
        }

    }

//防止网络抖动导致库存解锁逻辑结束了
    @Transactional
    @Override
    public void unlockStock(OrderTo to) {
        String orderSn = to.getOrderSn();

        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn (orderSn);
        Long id = task.getId();

        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
        .eq("task_id",id).eq("lock_status",1));

        for (WareOrderTaskDetailEntity entity : entities) {
            unLockedStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
            WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
            wareOrderTaskDetailEntity.setId(entity.getId());
            wareOrderTaskDetailEntity.setLockStatus(2);
            wareOrderTaskDetailService.updateById(wareOrderTaskDetailEntity);
        }
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}