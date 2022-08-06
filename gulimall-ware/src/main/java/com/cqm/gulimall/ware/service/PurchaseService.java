package com.cqm.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cqm.common.utils.PageUtils;
import com.cqm.gulimall.ware.entity.PurchaseEntity;
import com.cqm.gulimall.ware.exception.PurchaseDetailException;
import com.cqm.gulimall.ware.vo.MergeVo;
import com.cqm.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;

import java.util.Map;

/**
 * 采购信息
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:32:09
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceive(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo) throws PurchaseDetailException;

    void received(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

