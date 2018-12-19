package com.kuaidan.service;

import com.kuaidan.utils.Result;

public interface AdminService {
    /**
     * 管理员登陆接口
     * @param adminName 管理员登陆名
     * @param password 登陆密码
     * @return
     */
    Result adminLogin(String adminName, String password);

    /**
     * 退出管理员接口
     * @param token 管理员id
     * @return
     */
    Result cleanRedis(String token);

    /**
     * 接口校验
     * @param token 凭据
     * @return
     */
    Result adminLoginCheck(String token, String method, String url);

    /**
     * 校验token是否过期
     * @param token
     * @return
     */
    Result tokenCheck(String token);
}
