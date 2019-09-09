package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

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



    //根据平台ID查询平台属性的详情,顺便把该属性的属性值列表也取到
    BaseAttrInfo getBaseAttrInfo(String attrId);

    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);



    //修改平台属性
//    void getAttrValueList(String attrId);

    //删除平台属性


    //spu列表查询
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //spu保存属性
    public void saveSpuInfo(SpuInfo spuInfo);


}