package com.cqm.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

/**
 * δΈι’εε
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 20:44:19
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

