package com.cqm.gulimall.product.web;

import com.cqm.gulimall.product.entity.CategoryEntity;
import com.cqm.gulimall.product.service.CategoryService;

import com.cqm.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    RedissonClient redisson;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        Long l = System.currentTimeMillis();
        //todo 1.查出所有的1级分类
        List<CategoryEntity> categoryEntityList = categoryService.getLevel1Categorys();
        model.addAttribute("categorys",categoryEntityList);
        System.out.println("消耗时间："+(System.currentTimeMillis()-l));
        //"classpath:/templates/"
        return "index";
    }

    //index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        Map<String, List<Catelog2Vo>> map =  categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){

        RLock my_lock = redisson.getLock("my_lock");

        //加锁、
        my_lock.lock();
        /**
         * 阻塞式等待，默认加锁的时间都是30s
         * 1）锁自动续期，如果业务超长，运行期间自动给锁续上新的30s，不用担心业务时间长，锁自动过期被删掉
         * 2）加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，所默认30s以后自动删除
         *
         * 如果设置了leasttime ，会自动过期，看门狗不在生效
         * lock.lock(10, TimeUnit.SECONDS)
         * 1.如果传递了超时时间，就发送给redis执行脚本，默认超时时间就是我们指定的时间
         *2.如果未指定超时时间，就会使用30000【看门狗默认时间】
         * 只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】，3/1 看门够时间自动续期
         *
         */

        try{
            System.out.println("加锁成功,执行业务"+Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch (Exception e){

        }finally {
            System.out.println("释放锁"+Thread.currentThread().getId());
            my_lock.unlock();
        }
        return "hello";

    }

    @ResponseBody
    @GetMapping("/write")
    public String writeValue(){

        RReadWriteLock readWriteLock = redisson.getReadWriteLock("readwritelock");
        RLock rLock = readWriteLock.writeLock();
        String s = "";
        try{

            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue",s);

        }catch (Exception e){}
        finally {
            rLock.unlock();
        }
        return s;
    }
    @ResponseBody
    @GetMapping("/read")
    public String readValue(){

        RReadWriteLock readWriteLock = redisson.getReadWriteLock("readwritelock");
        RLock rLock = readWriteLock.readLock();
        String s = "";
        try{

            rLock.lock();


            s = (String) redisTemplate.opsForValue().get("writeValue");
            System.out.println("我准备读了");
            Thread.sleep(30000);
        }catch (Exception e){}
        finally {
            rLock.unlock();
        }
        return s;
    }


}
