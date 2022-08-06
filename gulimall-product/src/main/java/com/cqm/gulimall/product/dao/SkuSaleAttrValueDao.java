package com.cqm.gulimall.product.dao;

import com.cqm.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqm.gulimall.product.vo.SkuItemVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:10
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemVO.SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);


    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}
