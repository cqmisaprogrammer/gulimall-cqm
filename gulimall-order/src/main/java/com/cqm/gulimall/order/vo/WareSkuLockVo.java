package com.cqm.gulimall.order.vo;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
public class WareSkuLockVo implements Serializable {
    private String orderSn;//订单号

    private List<OrderItemVo> locks;

}
