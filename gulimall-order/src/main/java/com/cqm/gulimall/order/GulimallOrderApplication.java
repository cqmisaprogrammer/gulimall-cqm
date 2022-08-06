package com.cqm.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 引入rabbitmq
 * 1.引入rabbitmq starter:RabbitAutoConfiguration
 * 2.给容器中导入了RabbitTemplate AmqpAdmin、CachingConnectionFactoryConfigurer RabbitMessagingTemplate
 *
 *
 * 3.给配置文件中配置 spring.rabbitmq信息
 * 4.@EnableRabbit：@Enablexxx 开启功能
 * 5.监听消息：使用@RabbitListener; 必须有@EnableRabbit
 *  @RabbitListener 可以标注在方法上也可以在类上
 *  @RabbitHandler 标注在方法上 （用来区分同一队列中的，不同消息，由不同的方法处理）
 *
 * 本地事务失效问题
 * 同一个对象内事务方法互调默认失败，绕过了代理对象
 *
 *
 *
 *
 * seata控制分布式事务
 * 1）每一个微服务线必须创建uddo_log
 * 2.安装事务协调器：seata-server :http//github.com/seata/seata/release
 * 3).整合
 *      1.导入依赖 spring-cloud-starter-alibaba-seata eata-all-0.7.1
 *      2.解压并启动seata-server  bingqie在需要开启事务的微服务注册uodo_log表
 *        registry.conf :注册中心配置：修改registry type=nacos
 *        file.conf
 *      3.配置DataSourceProxy  所有想要使用分布式事务的微服务使用DataSourceProxy代理自己的数据源偷看个 
 */


@EnableRedisHttpSession
@EnableRabbit
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.cqm.gulimall.order.feign"})
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);

    }

}
