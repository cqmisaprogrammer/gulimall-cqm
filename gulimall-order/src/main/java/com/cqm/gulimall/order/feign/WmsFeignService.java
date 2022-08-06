package com.cqm.gulimall.order.feign;

import com.cqm.common.utils.R;
import com.cqm.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WmsFeignService {

    @PostMapping("/ware/waresku/hasstock")
     R getSkuHasSock(@RequestBody List<Long> skuIds);

    @RequestMapping("/ware/wareinfo/fare")
//   @RequiresPermissions("wave:wareinfo:list")
     R getFare(@RequestParam("addrId")Long addrId);

    @PostMapping("/ware/waresku/lock/order")
    R orderLockStock(@RequestBody WareSkuLockVo vo);

}
