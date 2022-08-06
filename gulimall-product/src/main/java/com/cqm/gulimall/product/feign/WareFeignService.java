package com.cqm.gulimall.product.feign;

import com.cqm.common.utils.R;
import com.cqm.gulimall.product.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {

    /**
     * 1.R设计的时候可以加上泛型，返回数据的时候可以直接返回想要的数据，不用自己转换
     * 2.直接返回想要的数据
     * 3.自己封装解析结果
     * @param skuIds
     * @return
     */
    @PostMapping("/ware/waresku/hasstock")
    R getSkuHasSock(@RequestBody List<Long> skuIds);
}
