package com.cqm.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;


import com.cqm.gulimall.product.vo.publicvo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cqm.gulimall.product.entity.SpuInfoEntity;
import com.cqm.gulimall.product.service.SpuInfoService;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.R;



/**
 * spu信息
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 16:01:09
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;


    //localhost:88/api/product/spuinfo/{spuId}/up
    @PostMapping("/{spuId}/up")
    public R spuUp(@PathVariable("spuId")Long spuId){

        spuInfoService.up(spuId);
        return R.ok();

    }

    @GetMapping("/skuId/{id}")
    public R  getSpuInfoByskuId(@PathVariable("id")Long skuId){

        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuInfoBySkuId(skuId);
        return R.ok().put("data",spuInfoEntity);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
//   @RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//   @RequiresPermissions("product:spuinfo:info")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
  //  @RequiresPermissions("product:spuinfo:save")
    public R save(@RequestBody SpuSaveVo vo){
		spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
