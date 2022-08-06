package com.cqm.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.cqm.common.constant.AuthServerConstant;
import com.cqm.common.exception.BizCodeEnume;
import com.cqm.common.utils.R;
import com.cqm.common.vo.MemberRespVo;
import com.cqm.gulimall.auth.feign.MemberFeignService;
import com.cqm.gulimall.auth.feign.ThirdPartFeignService;

import com.cqm.gulimall.auth.vo.UserLoginVo;
import com.cqm.gulimall.auth.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {


    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    /**
     * 为了不放空方法映射一些页面
     * 可以用springMVC viewcontroller：将请求和页面映射过来
     *
     *
     */

//    @GetMapping("/login.html")
//    public String loginPage(){
//        return "login";
//    }
//
//    @GetMapping("/reg.html")
//    public String regPage(){
//        return "reg";
//    }

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        //todo 1.接口防刷


        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(redisCode!=null){
            long l = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-l<60000){
                //上次验证码时间没过60s 不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2.验证码的再次校验,redis 存key-phone value-code
        String code = UUID.randomUUID().toString().substring(0,5);
        String redisKey = code+"_"+System.currentTimeMillis();

        //redis缓存验证码，防止同一个phone在60秒内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,redisKey,10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }

    /**
     * 
     *重定向跨域内容保存  RedirectAttributes.利用session技术。只要跳到下一个页面取出这个数据以后，session里面的数据就会删掉
     * @param vo
     * @param result
     * @param model
     * @return
     */

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes model){
        if(result.hasErrors()){

            Map<String, String> error = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            System.out.println(error);
            model.addFlashAttribute("errors",error);
            //校验出错

            //request method 'post' not supported
            //用户注册-》/regist[post] -?转发/reg.html [默认都是get]
            return "redirect:http://auth.gulimall.com/reg.html";
//            return "reg";
        }

        //真正注册
        //1.校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){

                //删除验证码;令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过*//真正注册
                R r = memberFeignService.regist(vo);
                if(r.getCode() == 0){
                    //成功
                    //注册成功回到登录页面
                    return "redirect:http://auth.gulimall.com/login.html";
                }else {

                    Map<String, String> errors = new HashMap<>();

                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    model.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else {
                Map<String,String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                model.addFlashAttribute("errors",errors);
                //校验出错，转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }

        }else {
            Map<String,String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            model.addFlashAttribute("errors",errors);
            //校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }


    }

    //登录页处理请求
    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute == null){
            //没登陆
            return "login";
        }else {
          return   "redirect:http://gulimall.com";
        }
    }

    @PostMapping ("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){

        //远程登录 ,
        R r = memberFeignService.login(vo);
        if(r.getCode()==0){
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            // 第一次使用session： 命令浏览器保存卡号，发送一个JSESSION，这个cookie，是根据请求域名来设定域值范围的，只有该域名下的所有请求
            //才会携带该cookie，为了解决跨域问题，就要扩大域名范围  gulimall.com 是父域  auth.gulimall.com order.gulimall.com 是子域
            // 发卡的时候就需要扩大到父域名，这样所有子域 及父域都能共享
            System.out.println(data);
            //todo 1.默认发的令牌session 是当前域，解决不了子域session共享问题
            //todo 2.使用json的序列化方式来序列化对象数据到redis中
            session.setAttribute(AuthServerConstant.LOGIN_USER,data);
            return "redirect:http://gulimall.com";
        }else {
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }


    }

}
