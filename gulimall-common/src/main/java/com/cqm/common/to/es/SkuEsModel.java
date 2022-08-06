package com.cqm.common.to.es;



import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
public class SkuEsModel {
    /**

     *       "catalogName":{
     *         "type": "keyword",
     *         "index": false,
     *         "doc_values": false
     *       },
     *       "attrs":{
     *         "type": "nested",
     *         "properties": {
     *           "attrId":{
     *             "type": "long"
     *           },
     *           "attrName":{
     *             "type": "keyword",
     *             "index": false,
     *             "doc_values": false
     *           },
     *           "attrValue":{
     *             "type": "keyword"
     *           }
     *         }
     *       }
     *     }
     *   }
     */
    private Long skuId;
    private Long spuId;

    private String skuTitle;
    private BigDecimal skuPrice;
    private String skuImg;


    private Long saleCount;
    private Boolean hasStack;
    private Long hotScore;
    private Long brandId;
    private Long catalogId;
    private String brandName;
    private String brandImg;

//
//            *       "attrs":{
//     *         "type": "nested",
//     *         "properties": {
//     *           "attrId":{
//     *             "type": "long"
//                        *           },
//     *           "attrName":{
//     *             "type": "keyword",
//     *             "index": false,
//     *             "doc_values": false
//                        *           },
//     *           "attrValue":{
//     *             "type": "keyword";
     private String catalogName;

     private List<Attrs> attrs;

     @Data
     public static class Attrs{
          private Long attrId;
          private String attrName;
          private String attrValue;
     }



}
