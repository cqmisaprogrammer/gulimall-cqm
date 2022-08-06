package com.cqm.gulimall.product.service.impl;

import com.cqm.gulimall.product.entity.SkuImagesEntity;
import com.cqm.gulimall.product.entity.SpuInfoDescEntity;
import com.cqm.gulimall.product.service.*;
import com.cqm.gulimall.product.vo.SkuItemVO;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.product.dao.SkuInfoDao;
import com.cqm.gulimall.product.entity.SkuInfoEntity;



@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    ThreadPoolExecutor threadPool;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        /**
         * key: '华为',//检索关键字
         * catelogId: 0,
         * brandId: 0,
         * min: 0,
         * max: 0
         */

        String key = (String) params.get("key");
        if(!StringUtils.isNullOrEmpty(key)){
            wrapper.and(w->{
                w.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isNullOrEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isNullOrEmpty(brandId)&&!"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String min = (String) params.get("min");
        if(!StringUtils.isNullOrEmpty(min)){
            wrapper.ge("price",min);
        }
        String max = (String) params.get("max");
        if(!StringUtils.isNullOrEmpty(max)){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal(0))==1){
                    wrapper.le("price",max);
                }
            }catch (Exception e){

            }

        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper

        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkuBySpuId(Long spuId) {
        List<SkuInfoEntity> skus = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));


        return skus;
    }

    @Override
    public SkuItemVO item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVO skuItemVO = new SkuItemVO();

        //使用异步编排，异步处理逻辑

        CompletableFuture<SkuInfoEntity> infoFeature= CompletableFuture.supplyAsync(() -> {
            //1.sku基本信息 获取 pms_sku_info
            SkuInfoEntity info = getById(skuId);
            skuItemVO.setInfo(info);
            return info;
        }, threadPool);


        CompletableFuture<Void> saleAttrFuture = infoFeature.thenAcceptAsync((info) -> {
            //3.spu销售属性组合
            Long spuId = info.getSpuId();
            List<SkuItemVO.SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(spuId);
            skuItemVO.setSaleAttr(saleAttrVos);
        }, threadPool);

        CompletableFuture<Void> descFuture = infoFeature.thenAcceptAsync((info) -> {
            //4.spu介绍
            Long spuId = info.getSpuId();

            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(spuId);
            skuItemVO.setDescp(spuInfoDescEntity);
        }, threadPool);

        CompletableFuture<Void> baseAttrFuture = infoFeature.thenAcceptAsync((info) -> {

            //5.规格参数信息
            Long spuId = info.getSpuId();
            Long catalogId = info.getCatalogId();
            List<SkuItemVO.SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(catalogId, spuId);
            skuItemVO.setGroupAttrs(attrGroupVos);
        }, threadPool);


        //2.sku图片信息 pms_sku_images
        CompletableFuture<Void> imagesFeature = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVO.setImages(images);
        }, threadPool);

        CompletableFuture.allOf(saleAttrFuture,baseAttrFuture,descFuture,imagesFeature).get();








        return skuItemVO;
    }

}