package com.cqm.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cqm.gulimall.product.service.CategoryBrandRelationService;
import com.cqm.gulimall.product.vo.Catelog2Vo;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cqm.common.utils.PageUtils;
import com.cqm.common.utils.Query;

import com.cqm.gulimall.product.dao.CategoryDao;
import com.cqm.gulimall.product.entity.CategoryEntity;
import com.cqm.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
//本地缓存
//    private Map<String,Object> cache = new HashMap<>();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);//baseMapper 是mybatis-plus自动为我们生成的代理dao

        //2.组装成父子的树形结构
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren((getChildrens(menu, categoryEntities)));
            return menu;
        }).sorted((menu1, menu2) -> {
            return menu1.getSort() - menu2.getSort();
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //todo 1.检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds( asList);
    }

    //[2,25,225] 完整路径
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        findParentPath(catelogId,paths);
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @CacheEvict：失效模式
     *  @Caching 组合多个缓存失效
     *  批量清除  @CacheEvict(value = "category",allEntries = true) 存储统一类型的数据，都可以指定成同一个分区
     *
     *  @CachePut 双写模式，当返回的结果是最新结果，可以将redis中的数据进行更新
     *
     * @param category
     */

    @Caching(evict = {@CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatalogJson'")})
//    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

    }

    /**每一个需要缓存的数据我们都来指定要放到哪个名字的缓存【缓存的分区（按照业务类型分区）】
     *
     * 2.@Cacheable({"category"}) 代表当前的方法的结果需要缓存，如果缓存中有，方法不用调用，如果没有回调用方法，并将方法的结果放入缓存
     * 3.默认行为
     *  1）.如果缓存中有，方法不用调用
     *  2.）key默认自动生成；缓存的名字：：SimpelKey [] （自动生成的key值）
     *  3.）缓存的value，默认使用jdk序列化机制。将序列化后的数据存到redis
     *  4.）默认时间-1 永不过期
     *
     *  自定义
     *      1.）指定生成的缓存使用的key： @Cacheable key属性 指定搞一个SpELhttps://docs.spring.io/spring-framework/docs/6.0.0-SNAPSHOT/reference/html/integration.html#cache-spel-context
     *      2.） 指定缓存的数据的存活时间 :在配置文件中修改TTL
     *      3.将数据保存为JSON格式
     *
     * @return
     */
    //
    @Cacheable(value = {"category"},key = "#root.method.name")  //
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys.....");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }


    @Cacheable(value = {"category"},key = "#root.methodName")  //
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("正在查询数据库" + Thread.currentThread().getName());


        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //查出所有分类。按照前端要求的格式回传，显示数据
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    //封装成2vo格式
                    List<Catelog2Vo> catelog2Vogs = null;
                    if (categoryEntities != null) {
                        catelog2Vogs = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //找出2级分类下的3 级分类，并封装进2级分类中
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                            if (level3Catelog != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }


                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vogs;
                }
        ));

        return parent_cid;
    }


    public Map<String, List<Catelog2Vo>> getCatalogJson2() {

        /**
         * 1.空结果缓存：解决缓存穿透
         * 2.设置过期时间（加随机值）：解决缓存雪崩
         * 3.加锁：解决缓存击穿
         */

        //1.加入缓存逻辑
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if(StringUtil.isNullOrEmpty(catalogJson)){
            System.out.println("缓存中没有，查询数据库");
            //2.缓存中没有，查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithLocalLock();

            return catalogJsonFromDb;
        }
        System.out.println("缓存命中直接返回");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});

        return result;

    }


    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        //todo 本地锁：synchronize，双检索模式 ，JUC（lock），在分布式情况下，想要锁住所有，必须使用分布式锁



        //1.占分布式锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111");
        if(lock){
            Map<String, List<Catelog2Vo>> dataFromDb =    getDataFromDb();
            redisTemplate.delete("lock");
            return dataFromDb;
        }else {
            return getCatalogJsonFromDbWithRedisLock();
        }

    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        /**
         * 优化：将反复查询数据库变为1次查询数据库，用java逻辑去构造vo封装
         */
        //1.加入缓存逻辑
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtil.isNullOrEmpty(catalogJson)) {
            System.out.println("双端检索起作用");
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }
        System.out.println("正在查询数据库" + Thread.currentThread().getName());


        List<CategoryEntity> selectList = baseMapper.selectList(null);


        //查出所有分类。按照前端要求的格式回传，显示数据
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    //封装成2vo格式
                    List<Catelog2Vo> catelog2Vogs = null;
                    if (categoryEntities != null) {
                        catelog2Vogs = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            //找出2级分类下的3 级分类，并封装进2级分类中
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                            if (level3Catelog != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(collect);
                            }


                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vogs;
                }
        ));

        //3.查到的数据放入缓存,将对象转为json放在缓存中
        catalogJson = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson", catalogJson, 1, TimeUnit.DAYS);
        return parent_cid;
    }


    //重数据库查询并封装数据
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        //todo 本地锁：synchronize，双检索模式 ，JUC（lock），在分布式情况下，想要锁住所有，必须使用分布式锁

        synchronized (this){
            /**
             * 优化：将反复查询数据库变为1次查询数据库，用java逻辑去构造vo封装
             */
            //1.加入缓存逻辑
            return getDataFromDb();
        }

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        return selectList.stream().filter(item->{
            return item.getParentCid().equals(parent_cid);
        }).collect(Collectors.toList());
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    private void findParentPath(Long catelogId,List<Long> paths){
        CategoryEntity byId = this.getById((catelogId));
        paths.add(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(), paths);
        }
    }

    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == root.getCatId()
        ).map((menu) -> {
            menu.setChildren((getChildrens(menu, all)));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0: menu2.getSort());
        }).collect(Collectors.toList());//map用来进行映射函数，其实就是自定义规则，然后讲数据返回，sorted流是将数据排序，collect将每个数据重新收集为一个集合
        return children;
    }

}