package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    @Autowired
    PaymentService paymentService;

    @GetMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request, Model model){
        // 获取订单的id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        model.addAttribute("orderId",orderId);
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @PostMapping("/alipay/submit")
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        //获取订单明细
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);


        //保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("-----");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        //保存信息
        paymentService.savePaymentInfo(paymentInfo);

        // 支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
//        long currentTimeMillis = System.currentTimeMillis();
//        String outTradeNo = "ATGUIGU-" + orderId + "-" +currentTimeMillis;
//        String productNo = "FAST_INSTANT_TRADE_PAY";
//        BigDecimal totalAmount = orderInfo.getTotalAmount();
//        String subject =  orderInfo.genSubject();


        return null;

    }
}
