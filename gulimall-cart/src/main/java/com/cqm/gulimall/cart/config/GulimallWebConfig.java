package com.cqm.gulimall.cart.config;

import com.cqm.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class GulimallWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        CartInterceptor cartInterceptor = new CartInterceptor();
        registry.addInterceptor(cartInterceptor).addPathPatterns("/**");
    }
}
