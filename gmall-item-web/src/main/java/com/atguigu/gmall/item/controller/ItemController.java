package com.atguigu.gmall.item.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    ManageService manageService;

    @GetMapping("{skuId}.html")
    public String skuInfoPage(@PathVariable String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("saleAttrList",saleAttrList);
//        request.setAttribute("gname","<span style=\"color:red\">鲲鹏</span>");
        System.out.println("skuInfo="+skuInfo);
        System.out.println("skuDefaultImg="+skuInfo.getSkuDefaultImg());
        System.out.println("saleAttrList="+saleAttrList);
        return "item";
    }

}
