package com.yuxuan.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yuxuan.dto.UserDTO;
import com.yuxuan.utils.RedisConstants;
import com.yuxuan.utils.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//拦截一切(第一层拦截器)
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 添加日志
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenInterceptor.class);

    //使用登录状态
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO 1、获取请求头中的 token
        String token = request.getHeader("authorization");//authorization 前端设置了
        if (StrUtil.isBlank(token)) {
            return true;
        }

        log.info("收到请求: {}, token: {}", request.getRequestURI(), token); // 添加日志
        // TODO 2、基于token获取redis中的用户
        //entries 是获取整个hash实体值
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

        // 3、判断用户是否存在
        if (userMap.isEmpty()) {
            //不存在，属于不需要授权操作，直接放行跳出
            return true;
        }
        //TODO 5、将查询到的Hash数据转为UserDIO 对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6、存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        // TODO 7、刷新token有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8、放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();

    }
}
