package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    CartInfoMapper cartInfoMapper;
    @Reference
    ManageService manageService;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public CartInfo addCart(String userId, String skuId, Integer num) {
        // 加数据库
        // 尝试取出已有的数据    如果有  把数量更新 update   如果没有insert
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        CartInfo cartInfoExists=null;
        cartInfoExists = cartInfoMapper.selectOne(cartInfoQuery);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if (cartInfoExists != null) {
            cartInfoExists.setSkuName(skuInfo.getSkuName());
            cartInfoExists.setCartPrice(skuInfo.getPrice());
            cartInfoExists.setSkuNum(cartInfoExists.getSkuNum()+num);
            cartInfoExists.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExists);
        } else {
            CartInfo cartInfo =  new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(num);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            
            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExists=cartInfo;
        }
        loadCartCache(userId);
        return cartInfoExists;
    }

    /**
     *  缓存没有查数据库 ，同时加载到缓存中
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        // 读取数据库
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithSkuPrice(userId);
        //加载到缓存中
        //为了方便插入redis  把list --> map
        if (cartInfoList != null&&cartInfoList.size()>0) {
            Map<String,String> cartMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                cartMap.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
            }
            Jedis jedis = redisUtil.getJedis();
            String cartKey = "cart:" + userId +":info";
            jedis.del(cartKey);
            jedis.hmset(cartKey,cartMap);
            jedis.expire(cartKey,60*60*24);
            jedis.close();
        }
        return cartInfoList;
    }


}
