package com.kuaidan.controller;


import com.kuaidan.service.AdminService;
import com.kuaidan.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;

    /**
     * 管理员登陆接口
     * @param adminName 管理员名称
     * @param password 管理员密码
     * @return
     */
    @PostMapping(value = "v1/admins/login")
    public Result adminLogin(String adminName, String password) {
        return adminService.adminLogin(adminName, password);
    }

    /**
     * 管理员退出接口
     * @param token
     * @return
     */
    @GetMapping(value = "v1/admins/logout")
    public Result cleanRedis(String token) {
        return adminService.cleanRedis(token);
    }

    /**
     * 身份校验+权限校验
     * @param token 凭据
     * @param method 请求方式
     * @param url 请求路径
     * @return
     */
    @GetMapping(value = "v1/admins/admin-login-check")
    public Result adminLoginCheck(String token,String method,String url) {
        return adminService.adminLoginCheck(token, method, url);
    }

    /**
     * token 校验
     * @param token 凭据
     * @return
     */
    @GetMapping(value = "v1/admins/token-check")
    public Result tokenCheck(String token) {
        return adminService.tokenCheck(token);
    }
}
