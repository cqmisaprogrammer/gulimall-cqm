package com.cqm.gulimall.ware.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.*;


public class WareSkuLockVo implements Serializable {
    private String orderSn;//订单号
    private List<OrderItemVo> locks;

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public List<OrderItemVo> getLocks() {
        return locks;
    }

    public void setLocks(List<OrderItemVo> locks) {
        this.locks = locks;
    }
}
