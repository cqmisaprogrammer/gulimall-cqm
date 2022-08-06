package com.cqm.gulimall.coupon.dao;

import com.cqm.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 20:44:19
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
