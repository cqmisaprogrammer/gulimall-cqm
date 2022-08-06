package com.cqm.gulimall.search.controller;

import java.io.IOException;
import java.util.*;

import com.cqm.common.exception.BizCodeEnume;
import com.cqm.common.to.es.SkuEsModel;
import com.cqm.common.utils.R;
import com.cqm.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    private ProductSaveService productSaveService;

    //商家商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModel)  {
        boolean b = false;
        try{
            b =  productSaveService.productStatusUp(skuEsModel);
        }catch (Exception e){
            log.error("ElasticSaveController商品上架错误{}，",e);
            return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(),BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());
        }

        if(b){
            return R.error(BizCodeEnume.PRODUCT_UP_EXCEPTION.getCode(),BizCodeEnume.PRODUCT_UP_EXCEPTION.getMsg());

        }else {
            return R.ok();
        }


    }

}
