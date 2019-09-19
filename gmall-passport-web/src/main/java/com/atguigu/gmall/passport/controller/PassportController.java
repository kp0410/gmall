package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

//@Controller
@RestController
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


    @PostMapping("login")
    public String login(UserInfo userInfo){

        boolean isLogin = userService.login(userInfo);

        if (isLogin == true){
            return "success";
        } else {
            return "fail";
        }
//        return "login";
    }
}
