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
     * @param searchParam ??????????????????
     * @return ??????????????????
     */
    @Override
    public SearchResult search(SearchParam searchParam) {
        //1.??????????????????????????????DSL??????

        SearchResult result = null;

        //1.zhun???????????????
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            //2.??????????????????
            SearchResponse response = restHighLevelClient.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

            //3.????????????????????????????????????????????????
            result = buildSearchResult(response,searchParam);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * ??????????????????, #???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * ???????????? ?????????????????????????????????????????????????????????????????????????????????
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
        //1.2.2 ????????????id????????????
        if(searchParam.getBrandId()!=null&&searchParam.getBrandId().size()>0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }
        //1.2.3 ????????????????????????
        //?????????????????????????????????????????????????????????nested??????query
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

        //1.2.4 ????????????????????????
        if(searchParam.getHasStock()!=null){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStack",searchParam.getHasStock()==1));
        }
        //1.2.5??????????????????
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParam.getSkuPrice().split("_");
            if(s.length==2){
                //??????
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

        //ba??????????????????????????????????????????
        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * ???????????????????????????
         */
         //2.1??????
        if(!StringUtils.isEmpty(searchParam.getSort())){
           // sort=hotScore_asc/desc  ???????????????
            String sort = searchParam.getSort();
            String[] s = sort.split("_");
            searchSourceBuilder.sort(s[0],s[1].equalsIgnoreCase("asc")? SortOrder.ASC:SortOrder.DESC);
        }
        //2.2??????
        // pageNum :1 form:0 size:5

        searchSourceBuilder.from((searchParam.getPageNum()-1)*5);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3gao???
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");

            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * ????????????
         */
        //todo ????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        //todo fen?????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        //todo ????????????
        NestedAggregationBuilder nested = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        nested.subAggregation(attr_id_agg);

        searchSourceBuilder.aggregation(nested);

        //==========================

        String s = searchSourceBuilder.toString();
        System.out.println("?????????DSL"+s);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * ???????????????
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam searchParam) {

        SearchResult result = new SearchResult();
        //1??????????????????????????????
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits()!=null&&hits.getHits().length>0){
            for(SearchHit hit:hits.getHits()){
                String sourceAsString = hit.getSourceAsString(); //????????????json?????????
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

//        //2.????????????????????????????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for(Terms.Bucket bucket:attr_id_agg.getBuckets()){
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //??????id
            String keyAsString = bucket.getKeyAsString();
            attrVo.setAttrId(Long.parseLong(keyAsString));
            //????????????
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //???????????????
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
//        //3???????????????????????????????????????????????????

        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for(Terms.Bucket bucket: brand_agg.getBuckets()){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //??????id
            brandVo.setBrandId(Long.parseLong(bucket.getKeyAsString()));
            //???????????????
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            //???????????????
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);
//        //4?????????????????????????????????????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for(Terms.Bucket bucket:buckets){
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String keyAsString = bucket.getKeyAsString();
            //
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //???????????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            //
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
//        ==========??????????????????????????????=========


//        //5.????????????-??????
//        //????????????
        long total = hits.getTotalHits().value;
        result.setTotal(total);
//        //?????????
        result.setPageNum(searchParam.getPageNum());
//        //?????????
         int totalPages = (int)total%EsConstant.PRODUCT_PAGESIZE==0?(int)total/EsConstant.PRODUCT_PAGESIZE:((int)total/EsConstant.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }

        result.setPageNavs(pageNavs);


        //6.???????????????????????????
       if(searchParam.getAttrs()!=null&&searchParam.getAttrs().size()>0) {
           List<SearchResult.NavVo> navVos = searchParam.getAttrs().stream().map(attr -> {
               SearchResult.NavVo navVo = new SearchResult.NavVo();
               String[] s = attr.split("_");
               navVo.setValue(s[1]);
               result.getAttrIds().add(Long.parseLong(s[0]));//?????????????????????id  ?????????????????????????????????
               //todo ????????????????????? product ?????? ?????????result?????????????????????id?????????name ?????????????????????
               R r = productFeignService.attrInfo(Long.parseLong(s[0]));
               if (r.getCode() == 0) {
                   AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                   });
                   navVo.setName(data.getAttrName());
               } else {
                   navVo.setName(s[0]);
               }

               //2.????????????????????????????????????????????????????????????????????????????????????????????????
               String encode = null;
               try {
                   encode = URLEncoder.encode(attr, "UTF-8");
                   //???utf-8?????????????????????????????????%20 ????????????java?????????+??? ?????????????????????
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

       //????????????????????????
//        if(searchParam.getBrandId()!=null&&searchParam.getBrandId().size()>0){
//            List<SearchResult.NavVo> navs = result.getNavs();
//            SearchResult.NavVo navVo = new SearchResult.NavVo();
//            navVo.setName("??????");
//
//
//        }





        return result;
    }


}
