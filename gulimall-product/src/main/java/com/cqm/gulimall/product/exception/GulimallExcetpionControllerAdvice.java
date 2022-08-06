package com.cqm.gulimall.product.exception;


import com.cqm.common.exception.BizCodeEnume;
import com.cqm.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

/**
 * @ControllerAdvice 统一异常处理配置类
 * 可以标注接受哪个包下的类传来的异常，然后通过对应的方法进行异常的处理，而不是直接抛给 Java虚拟机
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.cqm.gulimall.product.com.cqm.gulimall.auth.controller")
@RestControllerAdvice(basePackages = "com.cqm.gulimall.product.com.cqm.gulimall.auth.controller")
public class GulimallExcetpionControllerAdvice {

    /**
     * value标识能处理什么异常
     * @param e
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R hnadleVaildException(MethodArgumentNotValidException e){
        log.error("数据校验出现问题{}，异常类型：{}",e.getMessage(),e.getClass());
        BindingResult bindingResult = e.getBindingResult();

        HashMap<String,String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item)->{
            errorMap.put(item.getField(),item.getDefaultMessage());
        });
        return  R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data",errorMap);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleEcxeption(Throwable throwable){
         return R.error();
    }

}
