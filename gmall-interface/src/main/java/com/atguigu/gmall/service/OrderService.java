package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {

    public String saveOrder(OrderInfo orderInfo);

    public String genToken(String userId);

    public boolean verifyToken(String userId, String token);


    public OrderInfo checkStock(String orderId);
}
