package com.cqm.gulimall.order.vo;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.util.*;


public class OrderConfirmVo {

    //收货地址
    @Setter@Getter
    List<MemberAddressVo> address;

    //所有选中的购物项
    @Setter@Getter
    List<OrderItemVo> items;

    //发票记录信息。。。


    //优惠卷信息【以积分为例】
    @Setter@Getter
    private Integer integration;

    @Setter@Getter
    String orderToken;//订单的防重复令牌
    @Setter@Getter
    Map<Long, Boolean> stocks;

    public Integer getCount(){
        Integer count = 0;
        if(items!=null&&items.size()>0){
            for (OrderItemVo item : items) {
                count+= item.getCount(); ;
            }
        }
        return count;
    }

//    BigDecimal total;//订单总额

    public BigDecimal getTotal(){
        BigDecimal total = new BigDecimal("0");
        if(items!=null&&items.size()>0){
            for (OrderItemVo item : items) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }
        return total;
    }

//    BigDecimal payPrice;//应付价格

    public BigDecimal getPayPrice(){
        return getTotal();
    }

}
