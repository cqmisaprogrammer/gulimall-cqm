package com.cqm.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.product.entity.SpuInfoDescEntity;
import com.cqm.gulimall.product.entity.SpuInfoEntity;
import com.cqm.gulimall.product.vo.publicvo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:10
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

