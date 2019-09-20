package com.atguigu.gmall.intecreptor;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.constants.WebConst;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import static com.atguigu.gmall.constants.WebConst.VERIFY_URL;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{

    @Override
    public boolean  preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws Exception{
        // 检查token     token可能存在 1 url参数  newToken   2 从cookie中获得 token
        String token = request.getParameter("newToken");
        //把token保存到cookie
        if (token != null) {
            //把token保存到cookie中
            CookieUtil.setCookie(request,response,"token",token, WebConst.cookieMaxAge,false);
        }else {
            //从cookie中取值  token
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        //如果token 从token中把用户信息取出来
        if (token != null) {
            //读取token
            Map userMapByToken = getUserMapByToken(token);
            String nickName = (String) userMapByToken.get("nickName");
            request.setAttribute("nickName",nickName);
        }



        //判断是否该请求需要用户登录
        //取到请求的方法上的注解  LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (loginRequire != null) {
            //需要认证
            if (token != null) {
                // 要把token 发给认证中心进行 认证
                String currentIp = request.getHeader("X-forwarded-for");
                String result = HttpClientUtil.doGet(VERIFY_URL + "?token=" + token + "&currentIp=" + currentIp);
                if ("success".equals(result)){  //认证成功
                    Map userMap = getUserMapByToken(token);
                    String userId = (String) userMap.get("userId");
                    request.setAttribute("userId",userId);
                    return true;
                }else if (!loginRequire.autoRedirect()){ //认证失败但是 运行不跳转
                    return true;
                }else {//认证失败 强行跳转
                    redirect(request,response);
                    return false;
                }
            }else { // 强行跳转
                //  进行重定向  passport 让用户登录
                redirect(request,response);
                return false;
            }
        }
        return true;
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURL = request.getRequestURL().toString();//取得用户的当前登录请求
        String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
        response.sendRedirect(WebConst.LONG_URL+"?originUrl="+encodeURL);

    }


    private Map getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);

        return map;
    }
}
