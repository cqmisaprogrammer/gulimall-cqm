package com.cqm.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.cqm.common.constant.ProductConst;
import com.cqm.common.to.SkuReductionTo;
import com.cqm.common.to.SpuBoundTo;
import com.cqm.common.to.es.SkuEsModel;
import com.cqm.common.utils.R;
import com.cqm.gulimall.product.entity.*;
import com.cqm.gulimall.product.feign.CouponFeignService;
import com.cqm.gulimall.product.feign.SearchFeignService;
import com.cqm.gulimall.product.feign.WareFeignService;
import com.cqm.gulimall.product.service.*;
import com.cqm.gulimall.product.vo.SkuHasStockVo;
import com.cqm.gulimall.product.vo.publicvo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {


    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private AttrService attrService;


    @Autowired
    private ProductAttrValueService attrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *  //todo ???????????????????????????????????????
     * @param vo
     */

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //1??????spu????????????`pms_spu_info`
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //2??????spu????????????`pms_spu_info_desc`
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);

        //3??????spu????????????`pms_spu_images`
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(),images);

        //4??????spu??????????????? product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setSpuId(spuInfoEntity.getId());
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());

            valueEntity.setAttrName(byId.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            return valueEntity;
        }).collect(Collectors.toList());

        attrValueService.saveProductAttr(collect);

        //5????????????spu???????????????sku??????
        //5.??????spu???????????????gulimall_sms`-???`sms_spu_bounds`
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();

        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r1 = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r1.getCode()!=0){
            log.error("????????????spuBounds ????????????");
        }

        //??????spu?????????sku??????
        List<Skus> skus = vo.getSkus();

        if(skus!=null&&skus.size()>0){
            skus.forEach(sku->{
                //5.1sku????????????`pms_sku_info`
                String defaultImg = "";
                for(Images image : sku.getImages()){
                    if(image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }


                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);//????????????????????????images??????????????????

                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();
                //5.2sku???????????????
                List<SkuImagesEntity> imagesEntities = sku.getImages().stream().filter(item->{

                    return item.getImgUrl()!=null&&item.getImgUrl().length()!=0;
                }).map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();

                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);

                //5.3sku?????????????????????`pms_sku_sale_attr_value``
                List<Attr> attr = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntitys = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntitys);

                //5.4sku????????????????????????gulimall_sms`-???`sms_sku_ladder`/`sms_sku_full_reduction`/`sms_member_price`
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku,skuReductionTo);
                List<com.cqm.common.to.MemberPrice> mp = new ArrayList<>();
                BeanUtils.copyProperties(sku.getMemberPrice(),mp);
                skuReductionTo.setMemberPrice(mp);
                skuReductionTo.setSkuId(skuId);

                if(skuReductionTo.getFullCount()>0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0))==1
                ||(skuReductionTo.getMemberPrice()!=null&&skuReductionTo.getMemberPrice().size()!=0)){
                    R r = couponFeignService.saveSkuReduction(skuReductionTo);
                    if(r.getCode()!=0){
                        log.error("????????????sku?????? ????????????");
                    }

                }


            });
        }

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key =(String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
/**
 *
 * catelogId: 6,//????????????id
 *                 brandId: 1,//??????id
 *                 status: 0,//????????????
 */
        String status =(String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }
        String catelogId =(String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&&!"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }
        String brandId =(String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&!"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }



        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);


    }

    @Override
    public void up(Long spuId) {



        //1.????????????spuid???????????????sku????????????????????????
        List<SkuInfoEntity> skus = skuInfoService.getSkuBySpuId(spuId);
        //todo 4.????????????spu?????????????????????????????????????????????
        List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrListForspu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        //????????????????????????ids
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        //????????????????????????????????????????????????????????????Attrs
        List<SkuEsModel.Attrs> attrsList= baseAttrs.stream().filter(item -> {
            return idSet.contains(item.getAttrId());
        }).map(item->{
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item,attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        //todo 1.??????????????????????????????????????????????????????
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //todo ?????????????????????????????????R???????????????????????????map ???????????????????????????????????????
        Map<Long, Boolean> stockMap =null;
        try{
            R skuHasSock = wareFeignService.getSkuHasSock(skuIdList);
            List<SkuHasStockVo> data = skuHasSock.getData(new TypeReference<List<SkuHasStockVo>>(){});
//            List<SkuHasStockVo> data =(List<SkuHasStockVo>) skuHasSock.get("data");
            stockMap = data.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));

        }catch (Exception e){
            log.error("?????????????????????????????????",e);
        }
          //2.????????????sku?????????
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts=skus.stream().map(sku->{
            //??????????????????
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);
            //skuPrice???skuImg
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
//            hasStock???hotScore
            //todo 1.??????????????????????????????????????????????????????
            //??????????????????
            if(finalStockMap==null){
                esModel.setHasStack(true);
            }else {
                esModel.setHasStack(finalStockMap.get(sku.getSkuId()));
            }


            //todo 2.???????????????0
            esModel.setHotScore(0L);
            //todo 3.????????????????????????????????????
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity categoryEntity = categoryService.getById(esModel.getCatalogId());

            esModel.setCatalogName(categoryEntity.getName());



            //skuPrice???skuImg???hasStock???hotScore???brandName???BrandImg???catalogName
            /**
             * attrs attrId attrName attrValue
             */
            esModel.setAttrs(attrsList);


            return esModel;
        }).collect(Collectors.toList());

        //todo 5.??????????????????es ?????? ????????? ???gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){
            //??????????????????
            //todo 6.????????????spu?????????
            baseMapper.updateSpuStatus(spuId, ProductConst.StatusEnum.SPU_UP.getCode());
        }else {
            //??????????????????
            //todo 7.??????????????? ?????????????????????????????????xxx
            /**
             * feign????????????
             * 1.????????????????????????????????????JSON   RequestTemplate template = this.buildTemplateFromArgs.create(argv);
             * 2.???????????????????????????????????????????????????
             *  return this.executeAndDecode(template, options);
             *  3.??????????????????????????????
             *   while???true???{
             *   executeAndDecode(template, options);
             *   }catch??????{
             *         try {
             *              retryer.continueOrPropagate(e); //?????????????????????????????????never??????retry ??????????????????????????????default ??????
             *                 } catch (RetryableException var8) {
             *   }
             */
        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity byId = skuInfoService.getById(skuId);
        SpuInfoEntity spuInfoEntity = getById(byId.getSpuId());
        return spuInfoEntity;
    }


}