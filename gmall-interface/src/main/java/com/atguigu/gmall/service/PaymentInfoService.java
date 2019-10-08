package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentInfoService {
    public void savePaymentInfo(PaymentInfo paymentInfo);

    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);


    public void updatePaymentInfoByOutTradeNo(String outTradeNo, PaymentInfo paymentInfo);


    public void sendPaymentToOrder(String orderId, String result);
}
