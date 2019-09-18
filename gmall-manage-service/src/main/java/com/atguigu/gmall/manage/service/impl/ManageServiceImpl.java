package com.atguigu.gmall.manage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService{
    @Autowired
    RedisUtil  redisUtil;

    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    SpuInfoMapper spuInfoMapper;
    @Autowired
    SpuImageMapper spuImageMapper;
    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    public static final String SKUKEY_PREFIX="sku:";

    public static final String SKUKEY_INFO_SUFFIX=":info";
    public static final String SKUKEY_LOCK_SUFFIX=":lock";
//    public static final int SKUKEY_TIMEOUT=24*60*60;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return baseCatalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);
//
//        //查询平台属性值
//        for (BaseAttrInfo baseAttrInfoValue : baseAttrInfoList) {
//            BaseAttrValue baseAttrValue = new BaseAttrValue();
//            baseAttrValue.setAttrId(baseAttrInfoValue.getId());
//            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
//            baseAttrInfo.setAttrValueList(baseAttrValueList);
//        }

        List<BaseAttrInfo> list = baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);

        return list;
    }


    @Override
    public BaseAttrInfo getBaseAttrInfo(String attid) {

        // 创建属性对象
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attid);

        // 创建属性对象
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        // 根据attrId字段查询对象
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        // 给属性对象中的属性值集合赋值
        baseAttrInfo.setAttrValueList(baseAttrValueList);

        return baseAttrInfo;
    }


    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //如果有主键就进行更新，如果没有就插入
        if(baseAttrInfo.getId()!=null&&baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        }else{
            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //把原属性值全部清空
        BaseAttrValue baseAttrValue4Del = new BaseAttrValue();
        baseAttrValue4Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue4Del);

        //重新插入属性值
        if(baseAttrInfo.getAttrValueList()!=null&&baseAttrInfo.getAttrValueList().size()>0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {

                //防止主键被赋上一个空字符串
                attrValue.setId(null);
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }


    //spu列表查询
    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    //销售属性查询
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    //spu列表属性保存 （ 销售属性  图片列表  销售属性值 ）
    @Override
//    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        // 什么情况下是保存，什么情况下是更新 spuInfo
        if (spuInfo.getId()==null || spuInfo.getId().length() == 0){
            //保存数据
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        } else {
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }

        //  spuImage 图片列表 先删除，在新增
        //  delete from spuImage where spuId =?
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        // 保存数据，先获取数据
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!=null && spuImageList.size()>0){
            for (SpuImage image : spuImageList) {
                image.setId(null);
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(image);
            }
        }

        // 销售属性 删除，插入
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        // 销售属性值 删除，插入
        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        //获取数据
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size()>0) {
            // 循环遍历
            for (SpuSaleAttr saleAttr:spuSaleAttrList ) {
                saleAttr.setId(null);
                saleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(saleAttr);

                // 添加销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    //循环遍历
                    for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                        saleAttrValue.setId(null);
                        saleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(saleAttrValue);

                    }

                }
            }
        }

    }

    /**
     *  根据spuId获取spuImage中的所有图片列表
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存sku属性值
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info
        if(skuInfo.getId() == null ||skuInfo.getId().length() == 0){
            // 设置id 为自增
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        } else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }
        // sku_img
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        // insert
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList != null && skuImageList.size() > 0){
            for (SkuImage image : skuImageList) {
                /* "" 区别 null*/
                if (image.getId() != null && image.getId().length()==0) {
                    image.setId(null);
                }
                // skuId 必须赋值
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }

        // sku_attr_value
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        //插入数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size()>0) {
            for (SkuAttrValue attrValue : skuAttrValueList) {
                if (attrValue.getId()!=null && attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                // skuId
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }

        // sku_sale_attr_value,
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        //插入数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size()>0) {
            for (SkuSaleAttrValue  saleAttrValue : skuSaleAttrValueList) {
                if (saleAttrValue.getId() != null && saleAttrValue.getId().length()==0) {
                    saleAttrValue.setId(null);
                }
                // skuId
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        SkuInfo skuInfoResult=null;
        //1  先查redis  没有再查数据库
        Jedis jedis = redisUtil.getJedis();
        int SKU_EXPIRE_SEC=100;
        // redis结构 ： 1 type  string  2 key   sku:101:info  3 value  skuInfoJson
        String skuKey=SKUKEY_PREFIX+skuId+SKUKEY_INFO_SUFFIX;
        String skuInfoJson = jedis.get(skuKey);
        if(skuInfoJson!=null){
            if(!"EMPTY".equals(skuInfoJson)){
                System.out.println(Thread.currentThread()+"命中缓存！！");
                skuInfoResult = JSON.parseObject(skuInfoJson, SkuInfo.class);
            }
        }else{
            Config config = new Config();
            config.useSingleServer().setAddress("redis://redis.gmall.com:6379");
            RedissonClient redissonClient = Redisson.create(config);
            String lockKey=SKUKEY_PREFIX+skuId+SKUKEY_LOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            // lock.lock(10,TimeUnit.SECONDS);
            boolean locked=false ;
            try {
                locked = lock.tryLock(10, 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(locked) {
                System.out.println(Thread.currentThread() + "得到锁！！");
                // 如果得到锁后能够在缓存中查询 ，那么直接使用缓存数据 不用在查询数据库
                System.out.println(Thread.currentThread()+"再次查询缓存！！");
                String skuInfoJsonResult = jedis.get(skuKey);
                if (skuInfoJsonResult != null) {
                    if (!"EMPTY".equals(skuInfoJsonResult)) {
                        System.out.println(Thread.currentThread() + "命中缓存！！");
                        skuInfoResult = JSON.parseObject(skuInfoJsonResult, SkuInfo.class);
                    }

                } else {
                    skuInfoResult = getSkuInfoDB(skuId);

                    System.out.println(Thread.currentThread() + "写入缓存！！");

                    if (skuInfoResult != null) {
                        skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                    } else {
                        skuInfoJsonResult = "EMPTY";
                    }
                    jedis.setex(skuKey, SKU_EXPIRE_SEC, skuInfoJsonResult);
                }
                lock.unlock();
            }

        }
        return skuInfoResult;
    }


    public SkuInfo getSkuInfo_redis(String skuId) {

        SkuInfo skuInfoResult=null;
        //1  先查redis  没有再查数据库
        Jedis jedis = redisUtil.getJedis();
        int SKU_EXPIRE_SEC=100;
        // redis结构 ： 1 type  string  2 key   sku:101:info  3 value  skuInfoJson
        String skuKey=SKUKEY_PREFIX+skuId+SKUKEY_INFO_SUFFIX;
        String skuInfoJson = jedis.get(skuKey);
        if(skuInfoJson!=null){
            if(!"EMPTY".equals(skuInfoJson)){
                System.out.println(Thread.currentThread()+"命中缓存！！");
                skuInfoResult = JSON.parseObject(skuInfoJson, SkuInfo.class);
            }

        }else{
            System.out.println(Thread.currentThread()+"未命中！！");
            //setnx     1  查锁   exists 2 抢锁  set
            //定义一下 锁的结构   type  string     key  sku:101:lock      value  locked
            String lockKey=SKUKEY_PREFIX+skuId+SKUKEY_LOCK_SUFFIX;
//            Long locked = jedis.setnx(lockKey, "locked");
//            jedis.expire(lockKey,10);
            String token=UUID.randomUUID().toString();
            String locked = jedis.set(lockKey, token, "NX", "EX", 100);

            if("OK".equals(locked)){
                System.out.println(Thread.currentThread()+"得到锁！！");

                skuInfoResult = getSkuInfoDB(skuId);

                System.out.println(Thread.currentThread()+"写入缓存！！");
                String skuInfoJsonResult=null;
                if(skuInfoResult!=null){
                    skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                }else{
                    skuInfoJsonResult="EMPTY";
                }
                jedis.setex(skuKey,SKU_EXPIRE_SEC,skuInfoJsonResult);
                System.out.println(Thread.currentThread()+"释放锁！！"+lockKey);
                if(jedis.exists(lockKey)&&token.equals(jedis.get(lockKey))){   // 不完美 ，可以用lua解决
                    jedis.del(lockKey);
                }

            }else{
                System.out.println(Thread.currentThread()+"为得到锁，开始自旋等待！！");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getSkuInfo(  skuId);
            }

        }

        jedis.close();
        return   skuInfoResult;

    }

    private SkuInfo getSkuInfoDB(String skuId) {
        System.err.println(Thread.currentThread()+"读取数据库！！");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //单纯的信息
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        if(skuInfo==null){
            return null;
        }

        //查询图片
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        //将查询出来所有图片赋予对象
        skuInfo.setSkuImageList(skuImageList);

        //查询销售属性值
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        //将查询出来所有销售属性值赋给对象
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);

        //查询平台属性值
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        //将查询出来的所有平台属性值赋给对象
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;
    }


    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String skuId, String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
        return spuSaleAttrList;
    }

    @Override
    public Map getSkuValueIdsMap(String spuId) {
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValueBySpu(spuId);
        Map skuValueIdsMap = new HashMap();
        for (Map map: mapList) {
          String skuId = map.get("sku_id")+"";
          String valueIds = (String) map.get("value_ids");
          skuValueIdsMap.put(valueIds,skuId);
        }
        return skuValueIdsMap;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
        return baseAttrInfoList;
    }


}
