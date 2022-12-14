package com.cqm.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.order.entity.PaymentInfoEntity;

import java.util.Map;

/**
 * 支付信息表
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:23:47
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

