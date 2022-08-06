package com.cqm.gulimall.auth.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cqm.common.utils.HttpUtils;
import com.cqm.common.utils.R;
import com.cqm.gulimall.auth.feign.MemberFeignService;
import com.cqm.gulimall.auth.vo.MemberRespVo;
import com.cqm.gulimall.auth.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 处理社交登录请求
 */
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam String code, HttpSession session) throws Exception {

        //根据code换取accessTocken;
        Map<String,String> map = new HashMap<>();
//        client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET
//        &grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        map.put("client_id","YOUR_CLIENT_ID");
        map.put("client_secret","YOUR_CLIENT_SECRET");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http:/auth.gulimall.com/oauth2.0/weibo/success"); //完成所有
        map.put("code",code);
        HttpResponse response = HttpUtils.doPost("api.weibo.com", "/oauth2/access_token", "post", null, null, map);

        //根据access token 换取信息
        if(response.getStatusLine().getStatusCode()==200){
            //获取到了token
            String json = EntityUtils.toString(response.getEntity());//微博认证返回的token实体 需要根据这个返回体编写相应的vo 先暂时按照视频的来

            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //知道当前是哪个社交用户
            //1.当前用户如果是第一次进网站，就自动注册进来（为当前社交用户生成一个慧园信息账号，以后这个社交账号就对应这个指定的会员）
            //通过远程服务判断是登录还是注册这个社交用户
            R oauthlogin = memberFeignService.oauthlogin(socialUser);
            if(oauthlogin.getCode()==0){
                MemberRespVo data = oauthlogin.getData("data", new TypeReference<MemberRespVo>() {
                });

                session.setAttribute("loginUser",data);

                //2.登录成功就跳回
                return "redirct:http//gulimall.com";

            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }


        }else {

            return "redirect:http://auth.gulimall.com/login.html";
        }


    }

}
