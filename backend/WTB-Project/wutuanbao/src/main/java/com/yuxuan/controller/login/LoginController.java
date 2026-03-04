package com.yuxuan.controller.login;

import com.yuxuan.dto.LoginFormDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.dto.UserDTO;
import com.yuxuan.entity.UserInfo;
import com.yuxuan.service.IUserInfoService;
import com.yuxuan.service.IUserService;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping ("/app")
public class LoginController {

    @Resource
    private IUserService userService;


    @Resource
    private IUserInfoService userInfoService;

    //发送验证码
    @GetMapping("/login/getCode")
    public Result sendCode(@RequestParam("phone") String phone,HttpSession session) {
        return userService.sendCode(phone,session);
    }

    //登录
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginFormDTO, HttpSession session){
        return userService.login(loginFormDTO,session);
    }
    //登录后的请求：获取用户基本信息
    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        System.out.println(user);
        return Result.ok(user);
    }

    //“我的”页面初始加载请求
    // 根据用户id查询个人主页所需数据
    @GetMapping("/me/detail")
    public Result me1(@RequestParam(name = "uid") Long userId){
     // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    //“我的”页面点击“笔记”

}
