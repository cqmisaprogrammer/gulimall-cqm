package com.cqm.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.coupon.entity.SeckillPromotionEntity;

import java.util.Map;

/**
 * 秒杀活动
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 20:44:19
 */
public interface SeckillPromotionService extends IService<SeckillPromotionEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

