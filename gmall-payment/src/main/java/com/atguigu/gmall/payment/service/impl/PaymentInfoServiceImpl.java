package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentInfoService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQuery);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfoByOutTradeNo(String outTradeNo, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public void sendPaymentToOrder(String orderId, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentResultQeue = session.createQueue("PAYMENT_TO_ORDER");
            MessageProducer producer = session.createProducer(paymentResultQeue);
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId", orderId);
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }


}
