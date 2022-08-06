package com.cqm.gulimall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main方法开始");

//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结束：" + i);
//
//        }, executor);

//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 /2;
//            System.out.println("运行结束：" + i);
//            return i;
//        }).whenComplete((res,e)->{
//            //虽然能得到异常信息，但是没法修改返回数据
//            System.out.println("异步任务完成了。。。结果是："+res+";异常是："+e);
//        }).exceptionally(throwable -> {
//            //感知异常，并返回默认值
//            return 10;
//        });
        /**
         * 方法完成执行后的处理
         */
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 /0;
//            System.out.println("运行结束：" + i);
//            return i;
//        }).handle((res,thr)->{
//            if(res!=null){
//                return res*2;
//            }else {
//                return 0;
//            }
//
//        });

//        CompletableFuture<Void> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 /0;
//            System.out.println("运行结束：" + i);
//            return i;
//        }).thenRunAsync(()->{
//            System.out.println("任务2 启动");
//        });
//        System.out.println("获取结果"+integerCompletableFuture.get());

//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 /2;
////            try {
////                Thread.sleep(20000);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//            System.out.println("运行结束：" + i);
//            return i;
//        }).thenAcceptAsync(res->{
//            System.out.println("任务2 启动"+res);
//        });

        new Thread(()->{
            System.out.println("线程开始运行");
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程结束运行");
        }).start();
        System.out.println("main方法结束");
    }

}
