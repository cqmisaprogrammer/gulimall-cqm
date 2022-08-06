package com.cqm.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {
    private Long addrId;//收货地址id
    private  Integer payType;//支付方式

    //无需提交需要购买的商品，去购物车在获取一遍
    //优惠发票

    private String orderToken;//防重令牌

    private BigDecimal payPrice;//应付价格，验价，后台在查一遍对比两个价格，，不一样可以给客户友好提示

    //用户相关信息，直接去session取出信息

    private String note;//订单备注信息

}
