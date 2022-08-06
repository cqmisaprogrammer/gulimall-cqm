package com.cqm.gulimall.product.dao;

import com.cqm.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:11
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
