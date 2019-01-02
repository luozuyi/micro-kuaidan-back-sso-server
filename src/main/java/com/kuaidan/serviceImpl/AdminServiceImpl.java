package com.kuaidan.serviceImpl;

import com.kuaidan.entity.Admin;
import com.kuaidan.entity.SysRes;
import com.kuaidan.entity.SysRole;
import com.kuaidan.mapper.AdminMapper;
import com.kuaidan.mapper.SysResMapper;
import com.kuaidan.mapper.SysRoleMapper;
import com.kuaidan.service.AdminService;
import com.kuaidan.utils.Constants;
import com.kuaidan.utils.JwtToken;
import com.kuaidan.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Transactional
@Service
public class AdminServiceImpl implements AdminService{
    @Autowired
    private AdminMapper adminMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private HttpServletResponse response;
    @Autowired
    private SysResMapper sysResMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private HttpServletRequest request;
    @Override
    public Result adminLogin(String adminName, String password) {
        Result result = new Result();
        String code = "-1";
        String msg = "初始化值";
        try {
            if(StringUtils.isBlank(adminName)){
                code = "-2";
                msg = "登陆名不能为空";
            }else if(StringUtils.isBlank(password)){
                code = "-3";
                msg = "密码不能为空";
            }else{
                Admin admin = adminMapper.selectByAdminName(adminName);
                if(admin == null){
                    code = "-4";
                    msg = "用户名或密码不正确";
                }else if(!password.equals(admin.getPassword())){
                    code = "-4";
                    msg = "用户名或密码不正确";
                }else if("1".equals(admin.getDelFlag())){
                    code = "-5";
                    msg = "管理员已被禁用";
                }else{
                    String token = JwtToken.createAdminToken(admin);
                    token = token +"_"+ admin.getId();
                    redisTemplate.opsForValue().set(token, admin, 30, TimeUnit.DAYS);
                    /**登陆次数加1*/
                    /*Long loginCount = admin.getLoginCount()+1L;*/
                    /*admin.setLoginCount(loginCount);*/
                    /**获取管理员的最近登陆ip放到上一次IP去*/
                    /*String currentLoginIp = admin.getCurrentLoginIp();*/
                    /*admin.setLastLoginIp(currentLoginIp);*/
                    /*admin.setCurrentLoginIp(CommonUtil.getIpAddr(request));*/
                    /*admin.setLastLoginTime(new Date());*/
                    adminMapper.updateByPrimaryKeySelective(admin);
                    code = "0";
                    msg = "成功";
                    Map<String,Object> map = new HashMap<String,Object>();
                    map.put("token",token);
                    map.put("admin", admin);
                    /**查询角色*/
                    String sysRoleId = admin.getRoleId();
                    SysRole sysRole = sysRoleMapper.selectByPrimaryKey(sysRoleId);
                    map.put("sysRole", sysRole);
                    /**查询权限列表*/
                    List<SysRes> sysResList = sysResMapper.selectByRoleId(sysRoleId);
                    map.put("sysResList", sysResList);
                    result.setData(map);
                    Cookie adminCookie=new Cookie("kuaidanAdminToken",token);
                    adminCookie.setMaxAge(30*24*60*60);   //存活期为一个月 30*24*60*60
                    adminCookie.setPath("/");
                    response.addCookie(adminCookie);
                }
            }
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            code = "-5";
            msg = "系统异常";
            e.printStackTrace();
        }
        result.setMsg(msg);
        result.setCode(code);
        return result;
    }

    @Override
    public Result cleanRedis(String token) {
        Result result = new Result();
        String code = Constants.FAIL;
        String msg = "初始化";
        if (StringUtils.isBlank(token)) {
            code = "-3";
            msg = "非法请求";
        } else {
            if(redisTemplate.hasKey(token)){
                redisTemplate.delete(token);
                code = Constants.SUCCESS;
                msg = "清除redis成功";
                Cookie adminCookie=new Cookie("aidianmaoAdminToken",token);
                adminCookie.setMaxAge(-1);   //将cookie时间清除
                adminCookie.setPath("/");
                response.addCookie(adminCookie);
            }else{
                code = "-4";
                msg = "已经退出";
            }
        }
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    @Override
    public Result adminLoginCheck(String token, String method, String url) {
        Result result = new Result();
        String code = Constants.FAIL;
        String msg = "初始化";
        if (StringUtils.isBlank(token) || StringUtils.isBlank(method) || StringUtils.isBlank(url)) {
            code = "-3";
            msg = "非法请求";
        } else {
            Boolean flag = redisTemplate.hasKey(token);
            if (!flag) {
                code = "-4";
                msg = "已经过期";
            } else {
                String adminId = token.substring(token.lastIndexOf("_")+1);
                Admin admin = adminMapper.selectByPrimaryKey(adminId);
                String roleId = admin.getRoleId();
                //查询当前用户的权限
                List<SysRes> sysResList = sysResMapper.selectByRoleId(roleId);
                //封装查询条件
                SysRes sysResParam = new SysRes();
                sysResParam.setMethod(method);
                sysResParam.setUrl(url);
                //当前请求res
                SysRes sysRes_db = sysResMapper.selectByUrlAndMethod(sysResParam);
                if(sysRes_db == null){
                    code = "-6";
                    msg = "无当前请求的res";
                }else if(sysResList.size() == 0){
                    code = "-7";
                    msg = "该角色无任何权限";
                }else{
                    Boolean f = false;
                    for (SysRes sysRes:sysResList) {
                        if(sysRes.getId().equals(sysRes_db.getId())){
                            f = true;
                            break;
                        }

                    }
                    if(f){
                        code = Constants.SUCCESS;
                        msg = "成功";
                    }else{
                        code = "-5";
                        msg = "无权限";
                    }
                    /*if(sysResList.contains(sysRes_db)) {
                        code = Constants.SUCCESS;
                        msg = "成功";
                    }else {

                        code = "-5";
                        msg = "无权限";
                    }*/
                }
            }
        }
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    @Override
    public Result tokenCheck(String token) {
        Result result = new Result();
        String code = Constants.FAIL;
        String msg = "初始化";
        if (StringUtils.isBlank(token)) {
            code = "-3";
            msg = "非法请求";
        } else {
            Boolean flag = redisTemplate.hasKey(token);
            if (!flag) {
                code = "-4";
                msg = "已经过期";
            } else {
                code = Constants.SUCCESS;
                msg = "成功";
            }
        }
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
