package com.cqm.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.to.mq.OrderTo;
import com.cqm.common.to.mq.StockLockedTo;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.ware.entity.WareSkuEntity;

import com.cqm.gulimall.ware.vo.SkuHasStockVo;
import com.cqm.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;


import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:32:08
 */
public interface WareSkuService extends IService<WareSkuEntity> {


    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    /**
     * 解锁库存
     * @param to
     * @param message
     * @param channel
     */
    void unlockStock(StockLockedTo to) throws IOException;

    void unlockStock(OrderTo to);
}

