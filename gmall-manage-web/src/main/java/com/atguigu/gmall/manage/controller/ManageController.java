package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    ManageService  manageService;



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


    @RequestMapping("saveAttrInfo")
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
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return   manageService.getBaseSaleAttrList();
    }
}
