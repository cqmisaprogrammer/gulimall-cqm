package com.cqm.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.member.entity.MemberEntity;
import com.cqm.gulimall.member.exception.PhoneExistException;
import com.cqm.gulimall.member.exception.UsernameExistException;
import com.cqm.gulimall.member.vo.MemberLoginVo;
import com.cqm.gulimall.member.vo.MemberRegistVo;
import com.cqm.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:14:19
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;
    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

