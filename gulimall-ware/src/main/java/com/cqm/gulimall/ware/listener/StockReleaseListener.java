package com.cqm.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.cqm.common.to.mq.OrderTo;
import com.cqm.common.to.mq.StockLockedTo;
import com.cqm.common.utils.R;
import com.cqm.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.cqm.gulimall.ware.entity.WareOrderTaskEntity;
import com.cqm.gulimall.ware.service.WareSkuService;
import com.cqm.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@RabbitListener(queues = "stock.release.stock.queue")
@Configuration
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;





    /**
     * 库存自动解锁
     * 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     *
     *
     * 只要解锁库存的消息失败，一定要告诉服务器解锁失败，消息不能丢手动ACK
     * @param to
     * @param message
     */
    @RabbitHandler
    public void handleStockLockRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }


    }


    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }


    }
}
