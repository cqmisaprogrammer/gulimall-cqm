package com.cqm.gulimall.product.vo;

import com.cqm.gulimall.product.entity.SkuImagesEntity;
import com.cqm.gulimall.product.entity.SkuInfoEntity;
import com.cqm.gulimall.product.entity.SpuInfoDescEntity;
import com.cqm.gulimall.product.entity.SpuInfoEntity;
import lombok.Data;
import lombok.ToString;

import java.util.*;

@Data
public class SkuItemVO {

    //1.sku基本信息 获取 pms_sku_info
    SkuInfoEntity info;

    boolean hasStock =true;

    //2.sku图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    //3.spu销售属性组合

    List<SkuItemSaleAttrVo> saleAttr;

    //4.spu介绍
    SpuInfoDescEntity descp;

    //5.规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    @ToString
    @Data
    public static class SkuItemSaleAttrVo{
        private Long attrId;
        private String attrName;
        private List<attrValueWithSkuIdVo> attrValues;
    }

    @ToString
    @Data
    public static class attrValueWithSkuIdVo{
        private String attrValue;
        private String skuIds;
    }

    @ToString
    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @ToString
    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }

}
