package com.atguigu.gmall.order.consumer;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {
    @Reference
    OrderService orderService;

    /**
     * 订单模块发送减库存通知
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_TO_ORDER",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("orderId="+orderId);
        System.out.println("result="+result);
        if("success".equals(result)){
            System.out.println("订单"+orderId +"支付完成");
            // 订单修改状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 通知减库存
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId,ProcessStatus.DELEVERED);

//            // 发送消息给库存系统
//            sendOrderToWare(orderId);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }




}
