package com.cqm.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.product.entity.AttrGroupEntity;
import com.cqm.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.cqm.gulimall.product.vo.SkuItemVO;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:11
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatlogId(Long catelogId);

    List<SkuItemVO.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long catalogId, Long spuId);
}

