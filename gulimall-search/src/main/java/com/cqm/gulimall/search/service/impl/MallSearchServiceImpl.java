package com.cqm.gulimall.search.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cqm.common.to.es.SkuEsModel;
import com.cqm.common.utils.R;
import com.cqm.gulimall.search.config.ElasticSearchConfig;
import com.cqm.gulimall.search.constant.EsConstant;
import com.cqm.gulimall.search.feign.ProductFeignService;
import com.cqm.gulimall.search.service.MallSearchService;
import com.cqm.gulimall.search.vo.AttrResponseVo;
import com.cqm.gulimall.search.vo.SearchParam;
import com.cqm.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("MallSearchService")
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    ProductFeignService productFeignService;

    /**
     * @param searchParam 检索所有参数
     * @return 返回检索结果
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        //1.动态构建出查询需要的DSL语句

        SearchResult result = null;

        //1.zhun备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            //2.执行检索请求
            SearchResponse response = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

            //3.分析响应数据封装成我们需要的格式
            result = buildSearchResult(response,searchParam);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * 准备检索请求, #模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存），排序，分页，高亮，聚合分析
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * 查询条件 模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）
         */
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
           boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle",searchParam.getKeyword()));
        }
        //1.2 bool-filter
        if(searchParam.getCatalog3Id()!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId",searchParam.getCatalog3Id()));
        }
        //1.2.2 按照品牌id进行查询
        if(searchParam.getBrandId()!=null&&searchParam.getBrandId().size()>0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }
        //1.2.3 按照属性进行查询
        //每一个属性对应的多个属性值都要生成一个nested查询query
         if(searchParam.getAttrs()!=null&&searchParam.getAttrs().size()>0){

             for (String attr: searchParam.getAttrs()) {
                 BoolQueryBuilder  nestedBoolQuery= QueryBuilders.boolQuery();
                 //attrs=1_5cun:6cun&attrs=2_16G:8G
                 String[] s = attr.split("_");
                 String attrId = s[0];
                 String[] attrValues = s[1].split(":");
                 nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                 nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                 NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs",nestedBoolQuery , ScoreMode.None);

                 boolQueryBuilder.filter(nestedQuery);
             }


         }

        //1.2.4 按照是否拥有库存
        if(searchParam.getHasStock()!=null){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStack",searchParam.getHasStock()==1));
        }
        //1.2.5按照价格区间
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParam.getSkuPrice().split("_");
            if(s.length==2){
                //区间
                rangeQuery.gte(s[0]).lte(s[1]);
            }else if(s.length==1){
                if(searchParam.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }else {
                    rangeQuery.gte(s[0]);
                }
            }

            boolQueryBuilder.filter(rangeQuery);
        }

        //ba以前的所有条件都拿来进行封装
        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * 排序，分页，高亮，
         */
         //2.1排序
        if(!StringUtils.isEmpty(searchParam.getSort())){
           // sort=hotScore_asc/desc  只能选一个
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            searchSourceBuilder.sort(s[0],s[1].equalsIgnoreCase("asc")? SortOrder.ASC:SortOrder.DESC);
        }
        //2.2分页
        // pageNum :1 form:0 size:5

        searchSourceBuilder.from((searchParam.getPageNum()-1)*5);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3gao亮
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 聚合分析
         */
        //todo 品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        //todo fen类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        //todo 属性聚合
        NestedAggregationBuilder nested = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        nested.subAggregation(attr_id_agg);

        searchSourceBuilder.aggregation(nested);

        //==========================

        String s = searchSourceBuilder.toString();
        System.out.println("构建的DSL"+s);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 解析返回值
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam searchParam) {

        SearchResult result = new SearchResult();
        //1返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits()!=null&&hits.getHits().length>0){
            for(SearchHit hit:hits.getHits()){
                String sourceAsString = hit.getSourceAsString(); //返回的是json字符串
                SkuEsModel esModel = JSON.parseObject(sourceAsString, new TypeReference<SkuEsModel>(){});
                if(!StringUtils.isEmpty(searchParam.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

//        //2.当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for(Terms.Bucket bucket:attr_id_agg.getBuckets()){
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //属性id
            String keyAsString = bucket.getKeyAsString();
            attrVo.setAttrId(Long.parseLong(keyAsString));
            //属性名字
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //属性所有值
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = new ArrayList<>();
            for(Terms.Bucket bucket1 :attr_value_agg.getBuckets()){
                String attrValue = bucket1.getKeyAsString();
                attrValues.add(attrValue);
            }
            attrVo.setAttrValue(attrValues);




            attrVos.add(attrVo);
        }




        result.setAttrs(attrVos);
//        //3当前所有商品所涉及到的所有品牌信息

        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for(Terms.Bucket bucket: brand_agg.getBuckets()){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //品牌id
            brandVo.setBrandId(Long.parseLong(bucket.getKeyAsString()));
            //品牌的名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //品牌的图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);
//        //4当前商品所设计到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for(Terms.Bucket bucket:buckets){
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            //
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //拿到子聚合
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            //
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
//        ==========以上从聚合信息中获取=========


//        //5.分页信息-页码
//        //总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
//        //当前页
        result.setPageNum(searchParam.getPageNum());
//        //总页码
         int totalPages = (int)total%EsConstant.PRODUCT_PAGESIZE==0?(int)total/EsConstant.PRODUCT_PAGESIZE:((int)total/EsConstant.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }

        result.setPageNavs(pageNavs);


        //6.构建面包屑导航功能
       if(searchParam.getAttrs()!=null&&searchParam.getAttrs().size()>0) {
           List<SearchResult.NavVo> navVos = searchParam.getAttrs().stream().map(attr -> {
               SearchResult.NavVo navVo = new SearchResult.NavVo();
               String[] s = attr.split("_");
               navVo.setValue(s[1]);
               result.getAttrIds().add(Long.parseLong(s[0]));//添加已经选中的id  前端页面属性栏不在展示
               //todo 调远程查询功能 product 服务 可以用result内的数据去查出id对应的name 跨服务太迟网络
               R r = productFeignService.attrInfo(Long.parseLong(s[0]));
               if (r.getCode() == 0) {
                   AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                   });
                   navVo.setName(data.getAttrName());
               } else {
                   navVo.setName(s[0]);
               }

               //2.取消了这个面包屑以后，我们要跳转的地方，拿到所有查询方法去掉当前
               String encode = null;
               try {
                   encode = URLEncoder.encode(attr, "UTF-8");
                   //在utf-8下，前端会将空格翻译成%20 ，在后端java会变成+号 ，需要替换回来
                   encode = encode.replace("+","%20");
               } catch (UnsupportedEncodingException e) {
                   e.printStackTrace();
               }

               String replace = searchParam.get_queryString().replace("&attrs=" + encode, "");

               navVo.setLink("http://search.gulimall.com/list.html?" + replace);

               return navVo;
           }).collect(Collectors.toList());

           result.setNavs(navVos);
       }

       //品牌的面包屑导航
//        if(searchParam.getBrandId()!=null&&searchParam.getBrandId().size()>0){
//            List<SearchResult.NavVo> navs = result.getNavs();
//            SearchResult.NavVo navVo = new SearchResult.NavVo();
//            navVo.setName("品牌");
//
//
//        }





        return result;
    }


}
