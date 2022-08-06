package com.cqm.gulimall.order.vo;

import com.cqm.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {
    private OrderEntity order;
    private Integer code;//0成功 错误状态码
}
