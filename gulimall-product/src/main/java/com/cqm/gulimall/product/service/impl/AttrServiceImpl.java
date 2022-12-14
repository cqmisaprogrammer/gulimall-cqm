package com.cqm.gulimall.product.service.impl;

import com.cqm.common.constant.ProductConst;
import com.cqm.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.cqm.gulimall.product.dao.AttrGroupDao;
import com.cqm.gulimall.product.dao.CategoryDao;
import com.cqm.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.cqm.gulimall.product.entity.AttrGroupEntity;
import com.cqm.gulimall.product.entity.CategoryEntity;
import com.cqm.gulimall.product.service.CategoryService;
import com.cqm.gulimall.product.vo.AttrGroupRelationVo;
import com.cqm.gulimall.product.vo.AttrRespVo;
import com.cqm.gulimall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.product.dao.AttrDao;
import com.cqm.gulimall.product.entity.AttrEntity;
import com.cqm.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;


    @Autowired
    private AttrAttrgroupRelationDao attrgroupRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //????????????
        BeanUtils.copyProperties(attr,attrEntity);
        //??????????????????
        this.save(attrEntity);//?????????????????????????????????????????????id

        if(attr.getAttrType()== ProductConst.AttrEnum.ATTR_TYPE_BASE.getCode()&&attr.getAttrGroupId()!=null){
            //??????????????????
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrgroupRelationDao.insert(relationEntity);
        }



    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type","base".equalsIgnoreCase(type)?ProductConst.AttrEnum.ATTR_TYPE_BASE.getCode():ProductConst.AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId!= 0 ){
            queryWrapper.eq("catelog_id",catelogId);
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            queryWrapper.and((wrapper)->{
                wrapper.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );
//        System.out.println("page.totalcount: " +page.getTotal());
        PageUtils pageUtils = new PageUtils(page);

        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> collect = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);



            if("base".equalsIgnoreCase(type)){
                //??????????????????????????????
                AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attrEntity.getAttrId()));

                if(attrAttrgroupRelationEntity!=null && attrAttrgroupRelationEntity.getAttrGroupId()!=null){
                    Long attrGroupId = attrAttrgroupRelationEntity.getAttrGroupId();
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }




            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());

            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }


            return attrRespVo;
        }).collect(Collectors.toList());
        pageUtils.setList(collect);

        return pageUtils;
    }

    @Cacheable(value = "attr",key = "'attrinfo:'+#root.args[0]")
    //??????????????????????????????
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,attrRespVo);

        //????????????????????????????????????????????????????????????
        if(attrEntity.getAttrType()==ProductConst.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //??????????????????
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attrId) );
            if(attrAttrgroupRelationEntity!=null){
                attrRespVo.setAttrGroupId(attrAttrgroupRelationEntity.getAttrGroupId());
//            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrAttrgroupRelationEntity.getAttrGroupId());
//            if(attrGroupEntity!=null){
//                attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
//            }
            }
        }



        //??????????????????
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        attrRespVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity!=null){
            attrRespVo.setCatelogName(categoryEntity.getName());
        }



        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        System.out.println(attr);
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.updateById(attrEntity);


        //????????????????????????????????????????????????????????????
        if(attrEntity.getAttrType()==ProductConst.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //??????????????????
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId( attr.getAttrGroupId());

            //??????????????????????????????????????????????????????????????????????????????
            Integer count = attrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attrEntity.getAttrId()));

            if(count>0){
                attrgroupRelationDao.update(attrAttrgroupRelationEntity,new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attr.getAttrId()));
            }else {
                attrgroupRelationDao.insert(attrAttrgroupRelationEntity);
            }
        }


    }

    /**
     * ????????????id???????????????????????????
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = attrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));
        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds==null||attrIds.size()==0){
            return null;
        }
        List<AttrEntity> attrEntities = this.listByIds(attrIds);

        return attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
//        attrgroupRelationDao.delete(new QueryWrapper<>().eq("attr_id",).eq("attr_group_id",))
        attrgroupRelationDao.deleteBatchRelation(vos);
    }

    /**
     * ??????????????????????????????????????????
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1???????????????????????????????????????????????????????????????????????????????????????
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        //2?????????????????????????????????????????????????????????
        //2.1?????????????????????????????????
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //2.2???????????????????????????
        List<Long> groupIds = attrGroupEntities.stream().map((item) -> {
            Long groupId = item.getAttrGroupId();
            return groupId;
        }).collect(Collectors.toList());
        List<AttrAttrgroupRelationEntity> entities = attrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", groupIds));
        //???????????????????????????????????????
        List<Long> attrIds = entities.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3???????????????????????????????????????????????????
       QueryWrapper<AttrEntity>  queryWrapper=new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type",ProductConst.AttrEnum.ATTR_TYPE_BASE.getCode());
       if(attrIds!=null&&attrIds.size()>0){
           System.out.println("attrIds: "+attrIds);
           queryWrapper.notIn("attr_id", attrIds);
       }

       String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            queryWrapper.and((wapper)->{
                wapper.eq("attr_id",key).or().like("attr_name",key);
            });
        }

       IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);


        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
       return baseMapper.selectSearchAttrIds(attrIds);

    }

}