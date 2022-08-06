package com.cqm.gulimall.search.vo;

import lombok.Data;
import java.util.*;

/**
 * Fengz装页面所有可能传递过来的查询条件
 *
 * catalog3Id=225&keyword=小米&sort=saleCount_desc/asc&hasStock=0/1&skuPrice=1_500&brandId=1&brandId=2
 *  & attrs=arrtId_value1：value2..... &attrs=arrtId_value1：value2..... 可以选多个属性 每个属性又可以选多个值
 */
@Data
public class SearchParam {
    private String keyword;//页面穿过来的关键字，进行全文匹配
    private Long catalog3Id; //三级分类id

    /**
     * sort=saleCount_desc/asc
     * sort=skuPrice_desc/asc
     * sort=hotScore_asc/desc  只能选一个
     */
    private String sort;

    /**
     * 过滤条件
     * hasStock（是否有货） skuPrice区间 brandId catalogId attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     *brandId =1
     * attrs
     *
     */
    private Integer hasStock ;//shifou 只显示有货 0 .1  0无1有
    private String skuPrice;//价格区间

    private List<Long> brandId; //按照品牌id 可以多选

    private List<String> attrs;//按照属性进行筛选可以选多个属性 每个属性又可以选多个值

    private Integer pageNum=1;//页码

    private String _queryString;//原生的所有查询条件


}
