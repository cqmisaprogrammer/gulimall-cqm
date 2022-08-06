package com.cqm.gulimall.member.dao;

import com.cqm.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:14:19
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
