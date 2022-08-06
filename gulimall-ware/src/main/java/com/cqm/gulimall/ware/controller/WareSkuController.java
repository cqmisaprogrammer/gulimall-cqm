package com.cqm.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.*;

import com.cqm.common.exception.BizCodeEnume;
import com.cqm.common.exception.NoStockException;
import com.cqm.gulimall.ware.vo.LockStockResult;
import com.cqm.gulimall.ware.vo.SkuHasStockVo;
import com.cqm.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cqm.gulimall.ware.entity.WareSkuEntity;
import com.cqm.gulimall.ware.service.WareSkuService;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.R;



/**
 * 商品库存
 *
 * @author chenquanman
 * @email sunlightcs@gmail.com
 * @date 2022-06-06 21:32:08
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


      @PostMapping("/lock/order")
      public R waveLockStock(@RequestBody WareSkuLockVo vo){
          try {
              Boolean stockResults =  wareSkuService.orderLockStock(vo);
              return R.ok().put("data",stockResults);
          }catch (NoStockException e){

              return R.error(BizCodeEnume.NO_STOCK_EXCEPTION.getCode(),BizCodeEnume.NO_STOCK_EXCEPTION.getMsg());
          }

      }

    //查询是否有库存
    @PostMapping("/hasstock")
    public R getSkuHasSock(@RequestBody List<Long> skuIds){

        List<SkuHasStockVo> vos =  wareSkuService.getSkuHasStock(skuIds);

        return R.ok().put("data",vos);

    }


    /**
     * 列表
     */
    @RequestMapping("/list")
//   @RequiresPermissions("wave:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//   @RequiresPermissions("wave:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
  //  @RequiresPermissions("wave:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("wave:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("wave:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
