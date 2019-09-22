package com.atguigu.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
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


    /**
     * 购物车
     * @param request
     * @return
     */
    @LoginRequire(autoRedirect = false)
    @GetMapping("cartList")
    public String cartList(HttpServletRequest request){
        // 判断用户是否登录，登录了从redis中，redis中没有，从数据库中取
        // 没有登录，从cookie中取得
        String userId = (String) request.getAttribute("userId");//查看用户登录id

        if (userId != null) {//有登录
            List<CartInfo> cartList =null;//如果登录前（未登录）时，存在临时购物车 ，要考虑合并
            String userTmpId=CookieUtil.getCookieValue(request, "user_tmp_id", false); //取临时id
            if(userTmpId!=null){
                List<CartInfo> cartTmpList =  cartService.getCartList(  userTmpId);  //如果有临时id ，查是否有临时购物车
                if( cartTmpList!=null&&cartTmpList.size()>0){
                    cartList=  cartService.mergeCartList(userId,userTmpId); // 如果有临时购物车 ，那么进行合并 ，并且获得合并后的购物车列表
                }
            }
            if(cartList==null||cartList.size()==0){
                cartList =  cartService.getCartList(  userId);  //如果不需要合并 ，再取登录后的购物车
            }
            request.setAttribute("cartList",cartList);
        } else { //未登录 直接取临时购物车
            String userTmpId=CookieUtil.getCookieValue(request, "user_tmp_id", false);
            if(userTmpId!=null) {
                List<CartInfo> cartTmpList = cartService.getCartList(userTmpId);
                request.setAttribute("cartList",cartTmpList);
            }
        }
        return "cartList";
    }


    /**
     * 选中状态的变更
     * @param isChecked
     * @param skuId
     * @param request
     */
    @PostMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void chectCart(@RequestParam("isChecked") String isChecked,@RequestParam("skuId") String skuId, HttpServletRequest request){
//        String skuId = request.getParameter("skuId");
//        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");
        if (userId == null) { //登录，修改缓存中的数据
            userId = CookieUtil.getCookieValue(request, "user_tmp_id", false);
        }
        cartService.checkCart(skuId,isChecked,userId);
    }





}
