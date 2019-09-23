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

import java.util.ArrayList;
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
        skuLsParams.setPageSize(8);
        //根据参数返回sku列表
        SkuLsResult skuLsResult  = listService.search(skuLsParams);
        model.addAttribute("skuLsResult",skuLsResult);

        //从结果中取出平台属性列表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList",attrList);

        // 已选的属性值列表
        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();
        String urlParam = makeUrlParam(skuLsParams);
        //itco
        // 把所有已经选择的数值  从属性+属性值清单中删除属性
        // 清单--》  attrList      已选择的属性值   skuLsParams.getAttrValueIds
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator();iterator.hasNext();) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0) {
                    for (String valueId : skuLsParams.getValueId()) {
                        //选中的属性值 和 查询结果的属性值
                        if (valueId.equals(baseAttrValue.getId())){
                            iterator.remove();

                            //构造面包屑列表
                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            //去除重复数据
                            String makeUrlParam = makeUrlParam(skuLsParams,valueId);
                            baseAttrValueSelected.setUrlParam(makeUrlParam);
                            baseAttrValuesList.add(baseAttrValueSelected);
                        }
                    }
                }
            }
        }

        // 保存面包屑清单
        model.addAttribute("baseAttrValuesList",baseAttrValuesList);
        model.addAttribute("keyword",skuLsParams.getKeyword());
        model.addAttribute("urlParam",urlParam);

        model.addAttribute("totalPages",skuLsResult.getTotalPages());
        model.addAttribute("pageNo",skuLsParams.getPageNo());

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
    public String makeUrlParam(SkuLsParams skuLsParam,String... excludeValueIds){
        String urlParam="";
        List<String> paramList = new ArrayList<>();
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

                if(excludeValueIds!=null&&excludeValueIds.length>0){ //需要排除的valueid
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        // 跳出代码，后面的参数则不会继续追加【后续代码不会执行】
                        // 不能写break；如果写了break；其他条件则无法拼接！
                        continue;
                    }
                }

                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }
        return  urlParam;
    }
}
