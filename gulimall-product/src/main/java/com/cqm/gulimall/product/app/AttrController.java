package com.cqm.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import com.cqm.gulimall.product.entity.ProductAttrValueEntity;
import com.cqm.gulimall.product.service.ProductAttrValueService;
import com.cqm.gulimall.product.vo.AttrRespVo;
import com.cqm.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cqm.gulimall.product.service.AttrService;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.R;



/**
 * 商品属性
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 15:21:11
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @PostMapping("/update/{spuId}")
    public R updateSpuAttr(@PathVariable("spuId")Long spuId,@RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttr(spuId,entities);
        return R.ok();
    }


//    /product/attr/base/listforspu/{spuId} 查出商品的规格属性
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrList(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entitys = productAttrValueService.baseAttrListForspu(spuId);

        return R.ok().put("data",entitys);
    }

    /**
     * 分页查询具体分类的规格属性 /product/attr/base/list/{catelogId}
     * 分页查询具体分类的销售属性 /product/attr/sale/list/{catelogId}
     *
     */
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId")Long catelogId,
                          @PathVariable("attrType") String type){

        PageUtils page  = attrService.queryBaseAttrPage(params,catelogId,type);

        return R.ok().put("page", page);
    }



    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
//		AttrEntity attr = attrService.getById(attrId);
        AttrRespVo respVo = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
//		attrService.updateById(attr);
        attrService.updateAttr(attr);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
