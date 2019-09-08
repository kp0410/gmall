package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;

import java.util.List;

public interface ManageService {
    //查询一级分类
    public List<BaseCatalog1> getCatalog1();

    //根据一级分类ID查询二级分类
    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    //根据二级分类ID查询三级分类
    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类ID查询平台属性
    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    //根据平台ID查询平台属性的详情

    //保存平台属性

    //删除平台属性


}
