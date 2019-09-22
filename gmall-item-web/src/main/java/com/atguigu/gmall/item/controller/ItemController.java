package com.atguigu.gmall.item.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    ManageService manageService;
    @Reference
    ListService listService;

    @GetMapping("{skuId}.html")
    @LoginRequire(autoRedirect = false)
    public String skuInfoPage(@PathVariable("skuId") String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("saleAttrList",saleAttrList);

        //得到属性组合与skuid的映射关系 ，用于页面根据属性组合进行跳转
        Map skuValueIdsMap = manageService.getSkuValueIdsMap(skuInfo.getSpuId());
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
        request.setAttribute("valuesSkuJson",valuesSkuJson);
//        request.setAttribute("gname","<span style=\"color:red\">鲲鹏</span>");
//        System.out.println("skuInfo="+skuInfo);
//        System.out.println("skuDefaultImg="+skuInfo.getSkuDefaultImg());
//        System.out.println("saleAttrList="+saleAttrList);
//        System.out.println(valuesSkuJson);
//        System.out.println(skuId);
        listService.incrHotScore(skuId);
        request.getAttribute("userId");
        return "item";
    }


}
