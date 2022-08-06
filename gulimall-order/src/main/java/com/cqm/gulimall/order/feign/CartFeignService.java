package com.cqm.gulimall.order.feign;

import com.cqm.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("gulimall-cart-server")
public interface CartFeignService {

    @GetMapping("/currentUserCartItems")
     List<OrderItemVo> getCurrentUserCartItems();

}
