package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    public CartInfo addCart(String userId,String skuId,Integer num);

    public List<CartInfo> getCartList(String userId);


    public List<CartInfo> mergeCartList(String userIdDest, String userIdOrig);

    public void checkCart(String skuId, String isChecked, String userId);

}
