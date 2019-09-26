package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    @Reference
    PaymentInfoService paymentInfoService;

    @Autowired
    AlipayClient alipayClient;

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
    public String submitPayment(String orderId, HttpServletResponse response){
        //获取订单明细  //1 准备参数 给支付宝提交
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        long currentTimeMillis = System.currentTimeMillis();//系统日期
        String outTradeNo="ATGUIGU-"+orderId+"-"+currentTimeMillis;

        String productNo="FAST_INSTANT_TRADE_PAY";
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        String subject=orderInfo.genSubject();
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("out_trade_no",outTradeNo);
        jsonObject.put("product_code",productNo);
        jsonObject.put("total_amount",totalAmount);
        jsonObject.put("subject",subject);
        alipayRequest.setBizContent(jsonObject.toJSONString());

        //组织参数
        String submitHtml="";
        try {
            submitHtml = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        //保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfo.setSubject(subject);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        //保存信息
        paymentInfoService.savePaymentInfo(paymentInfo);

        return submitHtml;
    }



    /**
     * 支付信息回调
     * @param paramMap
     * @param request
     * @return
     * @throws AlipayApiException
     */
    @PostMapping("/alipay/callback/notify")
    public String notify(@RequestParam Map<String,String> paramMap,HttpServletRequest request) throws AlipayApiException {
        String sign = paramMap.get("sign");
        // 1    验签  //  支付宝公钥  数据
        boolean ifPass = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        if (ifPass) {
            String tradeStatus = paramMap.get("trade_status");
            String total_amount = paramMap.get("total_amount");
            String out_trade_no = paramMap.get("out_trade_no");
            // 2    判断成功失败标志
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo =  paymentInfoService.getPaymentInfo(paymentInfoQuery);
                // 3    判断一下 当前支付状态的状态    未支付  更改支付状态
                if(paymentInfo.getTotalAmount().compareTo(new BigDecimal(total_amount)) == 0) {
                    if (paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID)) {
                        // 4     用户订单状态    仓储 发货     异步方式处理
                        //更新 状态   时间戳  回调信息集合
                        PaymentInfo paymentInfoForUpdate =  new PaymentInfo();
                        paymentInfoForUpdate.setPaymentStatus(PaymentStatus.PAID);
                        paymentInfoForUpdate.setCallbackTime(new Date());
                        paymentInfoForUpdate.setCallbackContent(JSON.toJSONString(paramMap));
                        paymentInfoForUpdate.setAlipayTradeNo(paramMap.get("trade_no"));
                        paymentInfoService.updatePaymentInfoByOutTradeNo(out_trade_no,paymentInfoForUpdate);
                        // TODO 发送异步 消息  给订单

                        // 5     返回 success 标志
                        return "success";
                    }else if(paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED)){
                        //手工发送关单操作
                        return "fail";
                    } else if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID)){
                        return "success";
                    }
                }
            }
        }
        return "fail";
    }


    @GetMapping("/alipay/callback/return")
    @ResponseBody
    public String alipayReturn(){
        return "交易成功";
    }

    /**
     * 退款
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    @GetMapping("refund")
    @ResponseBody
    public String refund(String orderId) throws AlipayApiException {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(paymentInfoQuery);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",paymentInfo.getOutTradeNo());
        jsonObject.put("refund_amount",paymentInfo.getTotalAmount());
        request.setBizContent(jsonObject.toJSONString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
            System.out.println("业务退款成功");
            PaymentInfo paymentInfoForUpdate =  new PaymentInfo();
            paymentInfoForUpdate.setPaymentStatus(PaymentStatus.PAY_REFUND);
            paymentInfoService.updatePaymentInfoByOutTradeNo(paymentInfo.getOutTradeNo(),paymentInfoForUpdate);
            //处理订单状态
            //异步处理
            return "success";
        } else {
            return response.getSubCode()+":"+response.getSubMsg();
        }
    }


}
