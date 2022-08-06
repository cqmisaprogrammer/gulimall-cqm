package com.cqm.gulimall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.ArrayList;

@Controller
public class HelloController {

    @Value("${sso.server.url}")
    String  ssoServerUrl;

    /**
     *无需登录就可访问
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

    /**
     * 需要感知这次实在ssoserver登录成功跳回来的。,就会有个token带回来
     * @param model
     * @param session
     * @return
     */
    @GetMapping("employees")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token",required = false)String token){

        if(!StringUtils.isEmpty(token)){
            //登陆成功
            //todo 1.qussoserver获取当前token真正对应的用户信息
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> forEntity = restTemplate.getForEntity("http://ssoserver.com:8080/userInfo?token=" + token, String.class);
            String body = forEntity.getBody();
            session.setAttribute("loginUser",body);
        }

        Object loginUser = session.getAttribute("loginUser");
        if(loginUser==null){

            //没登陆，跳转到登录服务器登录
         return "redirect:"+ssoServerUrl+"?redirect_url=http://client1.com:8081/employees";
        }else{

        }


        List<String> emps = new ArrayList<>();
        emps.add("张三");
        emps.add("李四");

        model.addAttribute("emps",emps);
        return "list";
    }

}
