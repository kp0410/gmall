package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

public interface CartService {
    public CartInfo addCart(String userId,String skuId,Integer num);
}
