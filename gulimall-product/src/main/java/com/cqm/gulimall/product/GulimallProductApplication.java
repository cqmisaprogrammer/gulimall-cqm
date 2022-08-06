package com.cqm.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
/**
 * 1.导入mybatis-plus 依赖
 *
 * 2.配置
 *  1）配置数据源：
 *      导入数据库驱动
 *      配置数据源相关信息
 *  2.配置mybatis-plus
 *       使用@MapperScan
 *       配置sql映射文件路径
 *
 *
 *  3.逻辑删除
 *      1）配置全局删除规则：
 *      mybatis-plus:
 *          global-config:
 *              db-config:
 *                  id-type: auto #设置所有主键生成规则 和entity 上的注解@TableId【标识主键】 搭配使用
 *                  logic-delete-value: 1
 *                  logic-not-delete-value: 0
 *      2）低于3.1版本还需配置逻辑删除的组件bean
 *      3）在实体类字段上+@TableLogic注解 @TableLogic(value = "1",delval = "0") 当规则和默认的不同 value表示不删除的值。devval表示删除的值
 *
 * 5.模板引擎
 *  1）关闭缓存
 *  2）静态资源都放在static文件夹下就可以按照路径直接访问
 *  3）页面放在templates下直接进行访问
 *  4.）页面修改实时更新
 *      1.引入devtools
 *      2.修改完页面使用 com.cqm.gulimall.auth.controller shift f9☺
 *      重新编译下页面即可
 *6.整合redis
 *  1）引入data-redis-starter
 *  2)简单配置redis的host信息
 *  3）使用spirngboot自动配置好的StringRedistemplate
 *7.整合Redisson作为分布式锁等功能框架
 *  1.)引入依赖
 *  2.）配置redissonconfig参照官网，当前是单节点模式
 *
 *8.整合springCache简化缓存开发
 *  1.引入依赖
 *   spring-boot-starter-cache、+redis
 *  2.写配置
 *      1.）自动配置
 *          CacheAutoConfiguration 根据cache.type 导入RedisCacheConfiguration
 *          ->自动配好了缓存管理器redisCacheManger ,已经names-> RedisCacheConfiguration（配置信息和上面不是同一个类，是不同包下的）
 *     2.）配置使用redis作为缓存
 *     spring.cache.type = redis
 *     3.）测试使用缓存
 *     @Cacheable: Triggers cache population. 触发将数据保存到缓存的操作
 *      @CacheEvict: Triggers cache eviction.   触发将数据从缓存删除的操作
 *
 *      @CachePut: Updates the cache without interfering with the method execution.不影响方法执行更新缓存
 *
 *      @Caching: Regroups multiple cache operations to be applied on a method.组个多个缓存操作
 *
 *      @CacheConfig: Shares some common cache-related settings at class-level. 在类级别共享缓存的相同配置
 *      1.）开启缓存功能
 *      2.)只需要使用注解就能完成缓存操作
 *
 *      4.)原理
 *          CacheAutoConfiguration -> RedisCacheConfiguration ->
 *          自动配置了RedisCacheManger->初始化所有的缓存-》每个缓存决定用什么配置-》如果redisCacheConfiguration有就用已有的，没有就用默认的
 *          逻辑：调用 determineConfiguration() -> redisCacheConfiguration.getIfAvailable(() -> createConfiguration(cacheProperties, classLoader));方法
 *          在ObjectProvider<redisCacheConfiguration> 内去判断逻辑，如果能拿到redisCacheConfiguration 就用已有的，没有就调用createConfiguration方法创建默认的
 *
 *
 */


@EnableRedisHttpSession
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cqm.gulimall.product.feign")
@MapperScan("com.cqm.gulimall.product.dao")
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
