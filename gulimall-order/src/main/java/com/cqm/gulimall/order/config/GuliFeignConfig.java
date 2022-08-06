package com.cqm.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;


@Configuration
public class GuliFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(){

      return   new RequestInterceptor(){
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //xiang要用本次传递过来的cookie就必须获得此次线程的request请求中的请求头，可以在controller处，用threadLocal保存请求头信息
                //在此处就可以拿到，也可以用spring家自己的RequestContextHolder[底层也是用threadLocal]

                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//                System.out.println("当前线程id："+Thread.currentThread().getId());
                if(requestAttributes!=null){
                    //                System.out.println("feign远程之前先进性RequestInterceptor：apply");
                    //同步请求头数据
                    HttpServletRequest request = requestAttributes.getRequest();//老请求
                    requestTemplate.header("Cookie",request.getHeader("Cookie"));//同步cookie

                }
        }
        };
    }

}
