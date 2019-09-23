package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class OrderController {

    @Reference
    UserService userService;
    @Reference
    private CartService cartService;

    @GetMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        //  用户地址  列表
        List<UserAddress> userAddressList=userService.getUserAddressList(userId);
        request.setAttribute("userAddressList",userAddressList);
        //  用户需要结账的商品清单
        List<CartInfo> checkedCartList = cartService.getCheckedCartList(userId);

        return "userInfo";
    }
}
