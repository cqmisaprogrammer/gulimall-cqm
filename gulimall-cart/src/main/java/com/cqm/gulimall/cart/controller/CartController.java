package com.cqm.gulimall.cart.controller;

import java.util.*;
import com.cqm.gulimall.cart.service.CartService;
import com.cqm.gulimall.cart.vo.Cart;
import com.cqm.gulimall.cart.vo.CartItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.concurrent.ExecutionException;

@Controller
public class CartController {


    @Autowired
    CartService cartService;

    @ResponseBody
    @GetMapping("/currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }


    @GetMapping("/deleteItem ")
    public String deleteItem(@RequestParam("skuId")Long skuId){

        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId")Long skuId,@RequestParam("num") Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId")Long skuId,@RequestParam("check")Integer check){
        cartService.checkItem(skuId,check);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 离线购物车，保持一个月，通过一个user-key 保存用户信息 过期时间一个月
     *
     * 登录session有用户信息
     *
     * 没登陆：按照user-key来做，第一次的临时用户，帮忙创建一个临时用户
     *
     * @param session
     * @return
     */


    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        Cart cart = cartService.getCart();
        model.addAttribute("cart",cart);

//        //快速得到用户信息，id ，user-key
//        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();



        return "cartList";
    }

    /**
     *添加商品到购物车
     * RedirectAttributes model.addFlashAttribute() 将数据放在session里面可以在页面去除，但只能取一次
     * model.addAttribute("skuId",skuId); 将数据放到请求路径后面
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num
    , RedirectAttributes model) throws ExecutionException, InterruptedException {

          cartService.addToCart(skuId,num);

         model.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId")Long skuId,Model model){
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item",cartItem);

        return "success";
    }

}
