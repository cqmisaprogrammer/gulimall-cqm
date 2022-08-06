package com.cqm.gulimall.ware.exception;


import com.cqm.common.exception.BizCodeEnume;
import com.cqm.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @ControllerAdvice 统一异常处理配置类
 * 可以标注接受哪个包下的类传来的异常，然后通过对应的方法进行异常的处理，而不是直接抛给 Java虚拟机
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.cqm.gulimall.product.com.cqm.gulimall.auth.controller")
@RestControllerAdvice(basePackages = "com.cqm.gulimall.wave.com.cqm.gulimall.auth.controller")
public class GulimallExcetpionControllerAdvice {

    /**
     * value标识能处理什么异常
     * @param e
     */
    @ExceptionHandler(value = PurchaseDetailException.class)
    public R hnadleVaildException(PurchaseDetailException e){
        log.error("采购需求异常：{}，异常类型：{}",e.getMessage(),e.getClass());

        return  R.error(BizCodeEnume.PURCHASEDETAIL_EXCEPTION.getCode(),BizCodeEnume.PURCHASEDETAIL_EXCEPTION.getMsg());
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleEcxeption(Throwable throwable){
         return R.error();
    }

}
