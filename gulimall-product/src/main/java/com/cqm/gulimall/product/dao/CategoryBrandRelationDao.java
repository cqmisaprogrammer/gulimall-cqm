package com.cqm.gulimall.product.dao;

import com.cqm.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌分类关联
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:10
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {

    //@param 为mapper.xml 的sql语句绑定变量
    void updateCategory(@Param("catId") Long catId,@Param("name") String name);
}
