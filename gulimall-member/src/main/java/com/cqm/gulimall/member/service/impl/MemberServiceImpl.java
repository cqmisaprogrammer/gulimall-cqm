package com.cqm.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cqm.common.utils.HttpUtils;
import com.cqm.gulimall.member.dao.MemberLevelDao;
import com.cqm.gulimall.member.entity.MemberLevelEntity;
import com.cqm.gulimall.member.exception.PhoneExistException;
import com.cqm.gulimall.member.exception.UsernameExistException;
import com.cqm.gulimall.member.vo.MemberLoginVo;
import com.cqm.gulimall.member.vo.MemberRegistVo;
import com.cqm.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.member.dao.MemberDao;
import com.cqm.gulimall.member.entity.MemberEntity;
import com.cqm.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        //设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        //检查用户名和手机号的唯一性 ,使用异常来让controller感知
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());
        //设置
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());

        //密码要进行加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);
        //其他默认信息

        //保存
        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException
    {
        Integer mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile>0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username)  throws UsernameExistException{
        Integer mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(mobile>0){
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
         String loginacct = vo.getLoginacct();
         //1.去数据库查询
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or()
                .eq("mobile", loginacct));
        if(memberEntity==null){
            return null;
        }else {
            //数据库ps
            String passwordDb = memberEntity.getPassword();
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            //密码匹配
            boolean matches = bCryptPasswordEncoder.matches(vo.getPassword(), passwordDb);
            if(matches){
                return memberEntity;
            }else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //登录和注册合并逻辑
        String uid = socialUser.getUid();
        //1.判断当前社交用户是否绑定过已有的账号
        MemberEntity memberEntity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntity!=null){
            //说明这个用户注册过
            //更新    令牌和时间
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            baseMapper.updateById(update);
            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }else {
            //用户没有注册过，需要注册
            MemberEntity regist = new MemberEntity();

            try {
                //3.查询当前社交用户的账号信息
                HashMap<String, String> query = new HashMap<>();
                query.put("access_token",socialUser.getAccess_token());
                query.put("uid",socialUser.getUid());

                //根据微博api文档调用
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/users/show", "get", new HashMap<String, String>(),
                        query);

                if(response.getStatusLine().getStatusCode() == 200){
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name =(String) jsonObject.get("name");//微博昵称
                    String gender =(String)  jsonObject.get("gender");
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender)?1:0);
                    //...
                }
            }catch (Exception e){}

            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());

            baseMapper.insert(regist);

            return regist;
        }
    }

}