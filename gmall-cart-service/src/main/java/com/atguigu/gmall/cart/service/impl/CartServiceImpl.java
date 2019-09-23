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

import java.util.*;

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

    @Override
    public List<CartInfo> getCartList(String userId) {
        //先查缓存
        Jedis jedis = redisUtil.getJedis();
        String cartKey = "cart:"+userId +":info";
        List<String> cartJsonList = jedis.hvals(cartKey);
        List<CartInfo> cartList = new ArrayList<>();
        if(cartJsonList!=null&&cartJsonList.size()>0){//缓存命中
            for (String cartJson : cartJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartList.add(cartInfo);
            }

            cartList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o2.getId().compareTo(o1.getId());
                }
            });
            return cartList;
        } else {
            //缓存未命中  //缓存没有查数据库 ，同时加载到缓存中
            return loadCartCache(userId);
        }
    }

    /**
     * 合并购物车
     * @param userIdDest
     * @param userIdOrig
     * @return
     */
    @Override
    public List<CartInfo> mergeCartList(String userIdDest, String userIdOrig) {
        //1、先做合并
        cartInfoMapper.mergeCartList(userIdDest,userIdOrig);
        // 2、 合并后 把临时购物车删除
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userIdOrig);
        cartInfoMapper.delete(cartInfo);
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userIdOrig+":info");
        jedis.close();
        //3、重新读写数据  加载缓存
        List<CartInfo> cartInfoList = loadCartCache(userIdDest);
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        loadCartCache(userId);// 检查一下缓存是否存在 避免因为缓存失效造成 缓存和数据库不一致
        //  isCheck数据 值保存在缓存中
        //保存标志
        String cartKey ="cart:" + userId + ":info";
        Jedis jedis = redisUtil.getJedis();
        String cartInfoJson = jedis.hget(cartKey,skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson,CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartInfoJsonNew = JSON.toJSONString(cartInfo);
        jedis.hset(cartKey,skuId,cartInfoJsonNew);
        // 为了订单结账 把所有勾中的商品单独 在存放到一个checked购物车中
        String cartCheckeKey = "cart:" + userId +":checked";
        if(isChecked.equals("1")) {//勾中加入到待结账购物车中， 取消勾中从待结账购物车中删除
            jedis.hset(cartCheckeKey,skuId,cartInfoJsonNew);
            jedis.expire(cartCheckeKey,60*60);
        } else {
            jedis.hdel(cartCheckeKey,skuId);
        }
        jedis.close();
    }

    /**
     * 得到选中购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCheckedCartList(String userId) {
        // 获得redis中的key
//        CartConst.
        return null;
    }


    /**
     *  缓存没有 查数据库 ，同时加载到缓存中
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
