package com.cqm.gulimall.cart.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cqm.common.utils.R;
import com.cqm.gulimall.cart.feign.ProductFeignService;
import com.cqm.gulimall.cart.interceptor.CartInterceptor;
import com.cqm.gulimall.cart.service.CartService;
import com.cqm.gulimall.cart.vo.Cart;
import com.cqm.gulimall.cart.vo.CartItem;
import com.cqm.gulimall.cart.vo.SkuInfoVo;
import com.cqm.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service("CartServer")
public class CartServiceImpl implements CartService {

    private final  String CART_PREFIX = "gulimall:cart:";

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor threadPool;


    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res =(String) cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(res)){
            //购物车中无此商品
            CartItem cartItem = new CartItem();
            //2.商品添加到购物车


            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //1.远程差选当前要添加的商品的信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setSkuId(skuId);
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setPrice(data.getPrice());
            }, threadPool);

            //2.远程查询sku的组合信息
            CompletableFuture<Void> getSkuAttrs = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, threadPool);

            CompletableFuture<Void> future = CompletableFuture.allOf(getSkuInfoTask, getSkuAttrs);
            future.get();

            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);
            return cartItem;
        }else {

            //购物车有此商品，修改数量
            CartItem  cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));

            return cartItem;
        }




    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o =(String) cartOps.get(skuId.toString());
        CartItem item = JSON.parseObject(o, CartItem.class);

        return item;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId()!=null){
            //1.登录状态
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
            //2.如果临时购物车还没进行合并
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(tempCartKey);
            if(cartItems!=null&&cartItems.size()>0){
                //临时购物车需要合并
                for(CartItem item:cartItems){
                    addToCart(item.getSkuId(),item.getCount());
                }
                //清空临时购物车
                clearCart(tempCartKey);
            }



            //3.获取登录后的购物车的数据【包含合并过来的临时数据】
            List<CartItem> cartItems1 = getCartItems(cartKey);
            cart.setItems(cartItems1);

        }else {
            //2.没登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey); //获取临时购物车的所有数据
            cart.setItems(cartItems);
        }

        return cart;
    }

    /**
     * 清空购物车数据
     *
     * @param cartkey
     */
    @Override
    public void clearCart(String cartkey) {
        redisTemplate.delete(cartkey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    /**
     * 修改购物项数量
     *
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物项
     *
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId()==null){
            return null;
        }else {
            String cartKey=CART_PREFIX+userInfoTo.getUserId();
           List<CartItem> cartItems =  getCartItems(cartKey);
           //获取所有被选中的购物项
            List<CartItem> collect = cartItems.stream().filter(item -> item.getCheck()).map(item->{

                R price = productFeignService.getPrice(item.getSkuId());
                String price1 = (String) price.get("price");
                item.setPrice(new BigDecimal(price1));

                return item;    }
            ).collect(Collectors.toList());
            return  collect;
        }


    }

    /**
     * 获取到要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //1.pan断用户是否登录
        String cartKey = "";
        if(userInfoTo.getUserId()!=null){
            cartKey=CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartKey = CART_PREFIX+userInfoTo.getUserKey();
        }

        //2.
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values();

        if(values!=null&&values.size()>0){
            List<CartItem> collect = values.stream().map((value) -> {
                String str = (String) value;
                CartItem item = JSON.parseObject(str, CartItem.class);
                return item;
            }).collect(Collectors.toList());
            return collect;
        }
       return null;
    }


}
