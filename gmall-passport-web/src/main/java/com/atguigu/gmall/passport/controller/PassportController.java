package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
//@RestController
public class PassportController {

    @Reference
    UserService userService;


    @GetMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        //保存上
        request.setAttribute("originUrl",originUrl);

        return "index";
    }

    @Value("${token.key}")
    String singKey;

    @PostMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        //取得IP地址
        String remoteAddr = request.getHeader("X-forwarded-for");
//        String remoteAddr = request.getRemoteAddr();
//        System.out.println(remoteAddr); //192.168.17.1   192.168.17.128
//        System.out.println(request.getRemoteAddr());// 192.168.17.128   192.168.17.128
        if (userInfo != null) {
            UserInfo loginUser = userService.login(userInfo);
            if (loginUser == null) {
                return "fail";
            } else {
                //生成token
                Map map = new HashMap();
                map.put("userId",loginUser.getId());
                map.put("nickName",loginUser.getNickName());
                String token = JwtUtil.encode(singKey, map, remoteAddr);
                return token;
            }
        }
        return "fail";
    }


    @GetMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");
        //检查token
        Map<String, Object> map = JwtUtil.decode(token, singKey, currentIp);
        if (map != null) {
            //检查redis信息
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }

    @Test
    public void test01(){
        String key = "atguigu";
        String ip="192.168.17.128";
        Map map = new HashMap();
        map.put("userId","1111");
        map.put("nickName","kunpeng");
        String token = JwtUtil.encode(key, map, ip);
        System.out.println("token = "+token);
        Map<String, Object> decode = JwtUtil.decode(token, key, "192.168.17.232");
        System.out.println("decode = "+decode);
    }




}
