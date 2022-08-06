package com.cqm.gulimall.search.service;

import com.cqm.common.to.es.SkuEsModel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


public interface ProductSaveService {


    public Boolean productStatusUp(List<SkuEsModel> skuEsModel) throws IOException;
}
