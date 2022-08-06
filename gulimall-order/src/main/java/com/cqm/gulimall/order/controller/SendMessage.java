package com.cqm.gulimall.order.controller;

import com.cqm.common.utils.R;
import com.cqm.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
public class SendMessage {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @ResponseBody
    @GetMapping("/sendMessage")
    public String sendMsg(@RequestParam("num")Integer num){
        for (int i = 0; i < num; i++) {
            OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
            orderReturnReasonEntity.setId(1L);
            orderReturnReasonEntity.setCreateTime(new Date());
            orderReturnReasonEntity.setName("haha"+i);
            rabbitTemplate.convertAndSend("hello_exchange","hello",orderReturnReasonEntity);
        }
        return "Ok";
    }
}
