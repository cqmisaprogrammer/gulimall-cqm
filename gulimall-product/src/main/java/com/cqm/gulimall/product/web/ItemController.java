package com.cqm.gulimall.product.web;

import com.cqm.gulimall.product.service.SkuInfoService;
import com.cqm.gulimall.product.vo.SkuItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;


    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) throws ExecutionException, InterruptedException {

        System.out.println("详情"+skuId);
        SkuItemVO result = skuInfoService.item(skuId);

        model.addAttribute("item",result);

        return "item";
    }

}
