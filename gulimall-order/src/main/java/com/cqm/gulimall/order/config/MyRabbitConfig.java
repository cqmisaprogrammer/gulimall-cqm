package com.cqm.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 使用json序列化机制，进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
    /**
     * 定制rabbitTemplate
     *1.开启确认回调 confirmPublish
     * 2.设置确认回调
     *    publisher-confirm-type: correlated #开启发送端确认 ，消息到达broker触发
     *    publisher-returns: true #开启发送端消息抵达队列的确认
     *     template:
     *       mandatory: true #zhiyao抵达队列，以异步方式优先回调我们这个returnconfirm
     *
     * 3.消费端确认 （保证每个消息被正确消费，此时才可以broker删除这个消息
     *      1.默认是自动确认的，只要消息接收到，客户端会自动确认，服务器端就会移除这个消息
     *      2.手动应答 可以用来处理一些自己的逻辑
     *          channel.basicAck(messageProperties.getDeliveryTag(),false);
     *      3.拒绝接受
     *          channel.basicNack(long deliveryTag, boolean multiple, boolean requeue);
     *          channel.basicReject(long deliveryTag, boolean requeue);
     */
    @PostConstruct //MyRabbitConfig对象创建完成以后，执行这个方法
    public void initRabitTemplate(){
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 当前消息的唯一关联数据（消息的唯一id
             * @param correlationData
             * @param b 消息是否成功收到
             * @param s 失败原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("confirm...correlationData["+correlationData+"]==>ack["+b+"]==>cause["+s+"]");
            }
        });

        //消息抵达队列的回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            /**
             * 只要哪条消息没有抵达队列，则触发 该方法
             * @param returnedMessage
             */
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                Message message = returnedMessage.getMessage(); //投递失败的详细信息
                int replyCode = returnedMessage.getReplyCode();//回复状态码
                String replyText = returnedMessage.getReplyText();//hui回复的文本内容
                String exchange = returnedMessage.getExchange();//当时这个消息由哪个交换机传过来
                String routingKey = returnedMessage.getRoutingKey();//消息投递的路由键
            }
        });

    }
}
