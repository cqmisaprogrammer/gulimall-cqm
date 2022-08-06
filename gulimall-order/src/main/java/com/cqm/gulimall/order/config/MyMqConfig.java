package com.cqm.gulimall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMqConfig {

    /**
     * kun存服务的总控机
     * @return
     */
    @Bean
    public Exchange orderEventExchange(){
        return new TopicExchange("order-event-exchange",true,false);
    }

    /**
     * 释放队列
     */


//    @RabbitListener(queues = "stock.release.stock.queue")
    public void handle(Message message){

    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue",true,false,false,null);
    }

    @Bean
    public Queue orderDelayQueue(){
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange","order-event-exchange");
        args.put("x-dead-letter-routing-key","order.release.order") ; //写明消息去死信交换机的路由键
        args.put("x-message-ttl",1000);
        return new Queue("order.delay.queue",true,false,false,args);
    }

    @Bean
    public Binding orderReleaseBinding(){

        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange","order.release.order",null);
    }

    @Bean
    public Binding orderCreateOrderBinding(){

        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange","order.create.order",null);
    }

    @Bean
    public Binding orderReleaseOtherBinding(){

        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange","order.release.other.#",null);
    }

}
