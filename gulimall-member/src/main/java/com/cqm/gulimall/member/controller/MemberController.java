package com.cqm.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;


import com.cqm.common.exception.BizCodeEnume;
import com.cqm.gulimall.member.exception.PhoneExistException;
import com.cqm.gulimall.member.exception.UsernameExistException;
import com.cqm.gulimall.member.feign.CouponFeignService;
import com.cqm.gulimall.member.vo.MemberLoginVo;
import com.cqm.gulimall.member.vo.MemberRegistVo;
import com.cqm.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cqm.gulimall.member.entity.MemberEntity;
import com.cqm.gulimall.member.service.MemberService;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.R;



/**
 * 会员
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:14:19
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;


    @PostMapping("/oauth2/login")
    public R oauthlogin(@RequestBody SocialUser vo) throws Exception {
        MemberEntity entity = memberService.login(vo);
        if(entity!=null){

            return R.ok().put("data",entity);
        }else {
            return R.error(BizCodeEnume.LOGINACCTT_PASSWORD_INVAILD__EXIST.getCode(),BizCodeEnume.LOGINACCTT_PASSWORD_INVAILD__EXIST.getMsg());
        }

    }


    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("zhangsan");
        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member",memberEntity).put("coupons",membercoupons.get("coupons"));

    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        System.out.println(vo);
        MemberEntity entity = memberService.login(vo);
        if(entity!=null){

            return R.ok().put("data",entity);
        }else {
           return R.error(BizCodeEnume.LOGINACCTT_PASSWORD_INVAILD__EXIST.getCode(),BizCodeEnume.LOGINACCTT_PASSWORD_INVAILD__EXIST.getMsg());
        }

    }

    @PostMapping("/regist")
    public R regist(@RequestBody  MemberRegistVo vo){

        try{
            memberService.regist(vo);
        }catch (PhoneExistException e){
            return R.error(BizCodeEnume.PURCHASEDETAIL_EXCEPTION.getCode(),BizCodeEnume.PURCHASEDETAIL_EXCEPTION.getMsg());
        }catch (UsernameExistException e){
            return R.error(BizCodeEnume.USER_EXIST.getCode(), BizCodeEnume.USER_EXIST.getMsg());
        }


        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
//   @RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//   @RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
  //  @RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
