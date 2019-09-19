package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {
    List<UserInfo> getUserInfoListAll();
    void addUser(UserInfo userInfo);
    void updateUser(UserInfo userInfo);
    void updateUserByName(String name, UserInfo userInfo);
    void delUser(String id);

    UserInfo getUserInfoById(String id);

    Boolean login(UserInfo userInfo);
}
