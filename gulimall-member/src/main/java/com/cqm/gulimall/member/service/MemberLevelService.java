package com.cqm.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.member.entity.MemberLevelEntity;

import java.util.Map;

/**
 * 会员等级
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:14:19
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

