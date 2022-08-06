package com.cqm.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.cqm.gulimall.product.vo.SkuItemVO;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:10
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuItemVO.SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);



    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}

