package com.cqm.common.exception;

/**
 * 统一定义异常类型，异常码，异常信息
 * 10通用
 *  001：参数格式校验
 *  002：短信验证码频率太高
 *
 *  11:商品
 *  12：订单
 *  13购物车
 *  14：物流
 *  15 ：用户
 *
 * 21库存
 */

public enum BizCodeEnume {
    UNKOW_EXCEPTION(10000,"系统未知异常"),
    VAILD_EXCEPTION(10001,"系统未知异常"),
    SMS_CODE_EXCEPTION(10002,"验证码获取频率太高请稍后再试"),
    PURCHASEDETAIL_EXCEPTION(10003,"采购需求已分配"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),

    USER_EXIST(15001,"用户已经存在"),
    PHONE_EXIST(15001,"手机号已经存在"),
    LOGINACCTT_PASSWORD_INVAILD__EXIST(15001,"账号或密码错误"),

    NO_STOCK_EXCEPTION(21000,"商品库存不足");

    private int code;
    private String msg;
    BizCodeEnume(int code,String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
