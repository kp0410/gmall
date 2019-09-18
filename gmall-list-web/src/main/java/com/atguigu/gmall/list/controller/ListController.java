package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    ListService listService;
    @Reference
    ManageService manageService;

    @GetMapping("list.html")
//    @ResponseBody
    public String getList(SkuLsParams skuLsParams,Model model){
        //根据参数返回sku列表
        SkuLsResult skuLsResult  = listService.search(skuLsParams);
        model.addAttribute("skuLsResult",skuLsResult);

        //从结果中取出平台属性列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList",attrList);

        // 已选的属性值列表\
        String urlParam = makeUrlParam(skuLsParams);
        //inco
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator();iterator.hasNext();) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0) {
                    for (String valueId : skuLsParams.getValueId()) {
                        //选中的属性值 和 查询结果的属性值
                        if (valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                        }
                    }
                }
            }
        }

        model.addAttribute("urlParam",urlParam);

        //获取sku属性值列表
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        model.addAttribute("skuLsInfoList",skuLsInfoList);
        //model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
        //return JSON.toJSONString(search);
        return "list";
    }


    /**
     * 把页面传入的参数对象 转换成为参数url
     * @param skuLsParam
     * @return
     */
    public String makeUrlParam(SkuLsParams skuLsParam){
        String urlParam="";
        if(skuLsParam.getKeyword()!=null){
            urlParam+="keyword="+skuLsParam.getKeyword();
        }
        if (skuLsParam.getCatalog3Id()!=null){
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParam.getCatalog3Id();
        }
        // 构造属性参数
        if (skuLsParam.getValueId()!=null && skuLsParam.getValueId().length>0){
            for (int i=0;i<skuLsParam.getValueId().length;i++){
                String valueId = skuLsParam.getValueId()[i];
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return  urlParam;
    }
}
