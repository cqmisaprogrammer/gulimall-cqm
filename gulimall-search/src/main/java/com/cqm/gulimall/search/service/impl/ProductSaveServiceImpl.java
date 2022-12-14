package com.cqm.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.cqm.common.to.es.SkuEsModel;
import com.cqm.gulimall.search.config.ElasticSearchConfig;
import com.cqm.gulimall.search.constant.EsConstant;
import com.cqm.gulimall.search.service.ProductSaveService;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service(value = "ProductSaveService")
public class  ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public Boolean productStatusUp(List<SkuEsModel> skuEsModel) throws IOException {
        //baocun到es
        //1.给es中简历索引。product，建立好映射关系

        //给es中保存数据
        BulkRequest bulkRequest = new BulkRequest();
        for(SkuEsModel model:skuEsModel){
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);

        //todo 如果批量错误，就处理
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.error("商品上架完成：{}",collect);

        return b;
    }
}
