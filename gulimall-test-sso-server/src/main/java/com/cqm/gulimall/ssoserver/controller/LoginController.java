package com.cqm.gulimall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @ResponseBody
    @GetMapping("/userInfo")
    public String userInfo(@RequestParam("token")String token){
        String o = redisTemplate.opsForValue().get(token);
        return o;
    }



    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url")String url, Model model,
                            @CookieValue(value = "sso_token",required = false)String sso_token){
        if(!StringUtils.isEmpty(sso_token)){
            return "redirect:"+url+"?token="+sso_token;
        }

        model.addAttribute("url",url);
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          @RequestParam("url") String url,
                          HttpServletResponse response){


        System.out.println(url);
        if(!StringUtils.isEmpty(username)&&!StringUtils.isEmpty(password)){
            String s = UUID.randomUUID().toString();
            s=s.replace("-","");
            System.out.println(s);
            redisTemplate.opsForValue().set(s,username);
            Cookie sso_token = new Cookie("sso_token",s);
            response.addCookie(sso_token);

//登录cehgngong，跳回到之前的页面
            return "redirect:"+url+"?token="+s;
        }else {
            //登录失败
            return "login";
        }

    }



}
