package com.atguigu.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    ManageService manageService;

    /**
     *  根据spuId获取spuImage中的所有图片列表
     * @param spuId
     * @return
     */
    @GetMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){
        return manageService.getSpuImageList(spuId);
    }

    @GetMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }
}
