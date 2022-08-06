package com.cqm.gulimall.order.service.impl;

import com.cqm.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.order.dao.OrderItemDao;
import com.cqm.gulimall.order.entity.OrderItemEntity;
import com.cqm.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello_queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);


    }

    /**
     * queues:声明需要监听的所有队列
     *
     *
     * org.springframework.amqp.core.Message
     *
     * 参数类型可以写以下类型
     * 1.Message ： 原生消息详细信息。 头 体
     * 2.T<发送消息的类型>
     * 3.Channel channel
     *
     */

    @RabbitHandler
    public void listen(Message message, OrderReturnReasonEntity content, Channel channel){

        //消息体 ，消息的内容
        byte[] body = message.getBody();
        //消息头
        MessageProperties messageProperties = message.getMessageProperties();
//        System.out.println("开始处理消息。。。");
//
//        channel.basicNack();
//        channel.basicReject();
        System.out.println(messageProperties.getDeliveryTag()+"处理了");

        System.out.println("接受到的消息。。===>内容："+content);

    }

}