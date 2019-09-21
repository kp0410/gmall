package com.atguigu.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    CartService cartService;

    @PostMapping("addToCart")
//    @LoginRequire
    @LoginRequire(autoRedirect = false)
    public String addCart(@RequestParam("skuId") String skuId, @RequestParam("num") int num, HttpServletRequest request, HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        // 判断用户是否登录
        if (userId == null) {
            //如果用户未登录  检查cookie用户是否有token 如果有token  用token 作为id 加购物车 如果没有生成一个新的token放入cookie
            userId=CookieUtil.getCookieValue(request,"user_tmp_id",false);
            if (userId == null) {
                userId= UUID.randomUUID().toString();
                CookieUtil.setCookie(request,response,"user_tmp_id",userId,60*60*24,false);
            }
        }
        CartInfo cartInfo = cartService.addCart(userId, skuId, num);
        request.setAttribute("cartInfo",cartInfo);
        request.setAttribute("num",num);
        return "success";
    }
}
