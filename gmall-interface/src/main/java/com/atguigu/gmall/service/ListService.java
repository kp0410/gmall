package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    public void saveSkuInfo(SkuLsInfo skuLsInfo);

    public SkuLsResult search(SkuLsParams skuLsParams);

    //评分计数  Redis
    public void incrHotScore(String skuId);
}
