package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

public interface OrderService {

    public String saveOrder(OrderInfo orderInfo);

    public String genToken(String userId);

    public boolean verifyToken(String userId, String token);

    public OrderInfo getOrderInfo(String orderId);

    public void updateOrderStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfos);

    public void sendOrderStatus(String orderId);
}
