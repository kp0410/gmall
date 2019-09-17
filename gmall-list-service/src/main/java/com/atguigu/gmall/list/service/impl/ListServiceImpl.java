package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;


@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    public static final String ES_INDEX="gmall_sku_info";

    public static final String ES_TYPE="doc";

    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        // 保存数据
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            DocumentResult documentResult = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
