package com.cqm.gulimall.auth.feign;

import com.cqm.common.utils.R;
import com.cqm.gulimall.auth.vo.SocialUser;
import com.cqm.gulimall.auth.vo.UserLoginVo;
import com.cqm.gulimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
     R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
     R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    public R oauthlogin(@RequestBody SocialUser vo) throws Exception;

}
