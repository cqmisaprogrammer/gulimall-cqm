package com.cqm.gulimall.ware.service.impl;

import com.cqm.common.constant.WareConstant;
import com.cqm.gulimall.ware.entity.PurchaseDetailEntity;
import com.cqm.gulimall.ware.exception.PurchaseDetailException;
import com.cqm.gulimall.ware.service.PurchaseDetailService;
import com.cqm.gulimall.ware.service.WareSkuService;
import com.cqm.gulimall.ware.vo.MergeVo;
import com.cqm.gulimall.ware.vo.PurchaseDoneVo;
import com.cqm.gulimall.ware.vo.PurchaseItemDoneVo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.ware.dao.PurchaseDao;
import com.cqm.gulimall.ware.entity.PurchaseEntity;
import com.cqm.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {


    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) throws PurchaseDetailException {
        List<Long> items = mergeVo.getItems();
        //??????????????????????????????????????????
        List<PurchaseDetailEntity> entities = purchaseDetailService.listByIds(items);
        for(PurchaseDetailEntity item:entities){
            if (item.getStatus()!=WareConstant.PurchaseDetailEnum.Create.getCode()&&
                    item.getStatus()!=WareConstant.PurchaseDetailEnum.ASSIGNED.getCode()){
                    throw new PurchaseDetailException("??????????????????????????????id??????"+item.getId());
            }
        }
        Long purchaseId = mergeVo.getPurchaseId();
        if(purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.Create.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(id -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(id);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        purchaseDetailService.updateBatchById(collect);

        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(finalPurchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }

    @Transactional
    @Override
    public void received(List<Long> ids) {
        //1.???????????????????????????????????????????????????
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.Create.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item->{
            if(item.getStatus() == WareConstant.PurchaseStatusEnum.Create.getCode() ){
                //todo ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            }
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        //2.????????????????????????
        this.updateBatchById(collect);

        //3.???????????????????????????
        //**  ???????????????
        List<Long> ids1 = collect.stream().map(item -> {
            Long id = item.getId();
            return id;
        }).collect(Collectors.toList());

        PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
        purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.BUYING.getCode());
        purchaseDetailService.update(purchaseDetailEntity,new QueryWrapper<PurchaseDetailEntity>().in("purchase_id",ids1));


//        collect.forEach(item->{
//            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(item.getId());
//            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entitie -> {
//                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
//                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.BUYING.getCode());
//                return entitie;
//            }).collect(Collectors.toList());
//            purchaseDetailService.updateBatchById(detailEntities);
//        });


    }

    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {

        Long id = doneVo.getId();

        //2.?????????????????????
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();

        for(PurchaseItemDoneVo item:items){
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if(item.getStatus() == WareConstant.PurchaseDetailEnum.FAILURE.getCode()){
                flag = false;
                purchaseDetailEntity.setStatus(item.getStatus());
            } else {

                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.FINISH.getCode());
                //3.??????????????????????????????
                PurchaseDetailEntity byId = purchaseDetailService.getById(item.getItemId());
                log.info("zaici");
                wareSkuService.addStock(byId.getSkuId(),byId.getWareId(),byId.getSkuNum());
            }
            purchaseDetailEntity.setId(item.getItemId());
            updates.add(purchaseDetailEntity);
        }
        purchaseDetailService.updateBatchById(updates);

        //1.?????????????????????
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        if(flag){
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISH.getCode());
        }else {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASEERROR.getCode());
        }
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);



    }

}