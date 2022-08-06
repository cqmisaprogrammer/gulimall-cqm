package com.cqm.gulimall.search.vo;


import com.cqm.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.*;

@Data
public class SearchResult {

    //查询到的所有商品信息
    private List<SkuEsModel> products;

    private Integer pageNum;//当前ye面
    private Long total;//总记录数
    private Integer totalPages;//总记页码
    private List<Integer> pageNavs;

    private List<BrandVo> brands; //当前查询到的结果，所有涉及到的品牌
    private List<CatalogVo> catalogs;//当前查询所有机到的所有分类
    private List<AttrVo> attrs;  //当前查询到的结果，所有涉及到的所有属性

    //================= 以上是返回给页面的所有信息 ================

    //面包屑导航数据
    private List<NavVo> navs =  new ArrayList<>();
    private List<Long> attrIds = new ArrayList<>();//判断哪些属性已经被帅选了，不显示


    @Data
    public static class NavVo{
        private String name;
        private String value;
        private String link;
    }

    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }
    @Data
    public static class CatalogVo{
        private Long catalogId;
        private String catalogName;

    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue  ;
    }


}
