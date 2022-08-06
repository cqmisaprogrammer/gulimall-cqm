package com.cqm.gulimall.order.dao;

import com.cqm.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:23:47
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
