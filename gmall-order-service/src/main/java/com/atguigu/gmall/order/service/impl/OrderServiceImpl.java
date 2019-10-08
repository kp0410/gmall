package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ActiveMQUtil activeMQUtil;
//    @Reference
//    OrderService orderService;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String genToken(String userId) {
        //token  type   String  key   user:10201:trade_code  value token
        String token = UUID.randomUUID().toString();
        String tokenKey="user:"+userId+":trade_code";
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(tokenKey,10*60,token);
        jedis.close();

        return token;
    }


    @Override
    public boolean verifyToken(String userId, String token) {
        String tokenKey="user:"+userId+":trade_code";
        Jedis jedis = redisUtil.getJedis();
        String tokenExists = jedis.get(tokenKey);
        jedis.watch(tokenKey);
        Transaction transaction = jedis.multi();
        if(tokenExists!=null&&tokenExists.equals(token)){
            transaction.del(tokenKey);
        }
        List<Object> list = transaction.exec();
        if(list!=null&&list.size()>0&&(Long)list.get(0)==1L){
            return true;
        }else{
            return false;
        }

    }



    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        // 将orderDetai 放入orderInfo 中
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfos) {
        OrderInfo orderInfo = new OrderInfo();
        if(orderInfos!=null&& orderInfos.length>0 ){ //如果还需要附带更新其他订单信息则 使用可变参数中的订单信息
            orderInfo=orderInfos[0];
        }
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }


    /**
     * 通知减库存
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson =  initWareParamJson(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            producer.send(textMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化库存订单
     * 设置初始化仓库信息方法
     * @param orderId
     * @return
     */
    public String initWareParamJson(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrderJson(orderInfo);
        return JSON.toJSONString(map);
    }

    /**
     * 设置初始化仓库信息方法
     * @param orderInfo
     * @return
     */
    public Map initWareOrderJson(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.genSubject());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());
        // 组合json
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }


}
