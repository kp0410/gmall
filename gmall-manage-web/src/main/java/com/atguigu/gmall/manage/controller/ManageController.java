package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    ManageService  manageService;
    @Reference
    ListService listService;



    @PostMapping("getCatalog1")
    public List<BaseCatalog1> getBaseCatalog1(){
        List<BaseCatalog1> baseCatalog1List = manageService.getCatalog1();
        return baseCatalog1List;

    }

    @PostMapping("getCatalog2")
    public List<BaseCatalog2> getBaseCatalog2(String catalog1Id){
        List<BaseCatalog2> baseCatalog2List = manageService.getCatalog2(catalog1Id);
        return baseCatalog2List;
    }

    @PostMapping("getCatalog3")
    public List<BaseCatalog3> getBaseCatalog3(String catalog2Id){
        List<BaseCatalog3> baseCatalog3List = manageService.getCatalog3(catalog2Id);
        return baseCatalog3List;
    }

    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> getBaseAttrInfo(String catalog3Id){
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(catalog3Id);

        return baseAttrInfoList;
    }

//    @GetMapping("attrInfoList")
//    public List<BaseAttrInfo> getBaseAttrInfoList(String catalog3Id){
//        List<BaseAttrInfo> attrList = manageService.getAttrList(catalog3Id);
//
//        return attrList;
//
//    }


    @PostMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);

    }

//    @RequestMapping(value = "getAttrValueList",method = RequestMethod.POST)
//    @ResponseBody
    @PostMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo baseAttrInfo = manageService.getBaseAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return attrValueList;
    }


    //查询基本销售属性表
    @PostMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> getBaseSaleAttrList(){

        return   manageService.getBaseSaleAttrList();
    }

    @PostMapping("onSale")
    @ResponseBody
    public String onSale(String skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 属性拷贝
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        listService.saveSkuInfo(skuLsInfo);

        return "success";

    }
    

}
