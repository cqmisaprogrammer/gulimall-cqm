package com.cqm.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.cqm.gulimall.product.entity.AttrEntity;
import com.cqm.gulimall.product.service.AttrAttrgroupRelationService;
import com.cqm.gulimall.product.service.AttrService;
import com.cqm.gulimall.product.service.CategoryService;
import com.cqm.gulimall.product.vo.AttrGroupRelationVo;
import com.cqm.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cqm.gulimall.product.entity.AttrGroupEntity;
import com.cqm.gulimall.product.service.AttrGroupService;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.R;



/**
 * 属性分组
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:11
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService relationService;

    //    product/attr/relation
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){
        relationService.saveBatch(vos);


        return R.ok();
    }
    
//    /product/attrgroup/{catelogId}/withattr
    @GetMapping("/{catelogId}/withattr")
    public R getAttrgroupWithAttrs(@PathVariable("catelogId")Long catelogId){
        //1.查出当前分类下的所有属性分组

        //2.查出每个分组下的所有属性
        List<AttrGroupWithAttrsVo> vos =  attrGroupService.getAttrGroupWithAttrsByCatlogId(catelogId);
        return R.ok().put("data",vos);
    }

//    product/attrgroup/1/attr/relation
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId){

        List<AttrEntity> datas = attrService.getRelationAttr(attrgroupId);

        return R.ok().put("data", datas);
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param attrgroupId
     * @param params
     * @return
     */
    //    product/attrgroup/1/attr/relation
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params){

        PageUtils page = attrService.getNoRelationAttr(params,attrgroupId);

        return R.ok().put("page", page);
    }



    /**
     * 列表
     * {
     *    page: 1,//当前页码
     *    limit: 10,//每页记录数
     *    sidx: 'id',//排序字段
     *    order: 'asc/desc',//排序方式
     *    key: '华为'//检索关键字
     * }
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params
            ,@PathVariable("catelogId") Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params,catelogId);


        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

		Long catelogId = attrGroup.getCatelogId();

		Long[] path = categoryService.findCatelogPath(catelogId);

		attrGroup.setCatelogPath(path);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }


/**
 * 删除关联关系
 */
//    product/attrgroup/attr/relation/delete

    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos){
        attrService.deleteRelation(vos);
        return R.ok();

    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
