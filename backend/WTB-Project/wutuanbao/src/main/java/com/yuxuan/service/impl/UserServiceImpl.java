package com.yuxuan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.AuthorDTO;
import com.yuxuan.dto.LoginFormDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.dto.UserDTO;
import com.yuxuan.entity.User;
import com.yuxuan.mapper.FollowMapper;
import com.yuxuan.mapper.UserMapper;
import com.yuxuan.service.IUserService;
import com.yuxuan.utils.RegexUtils;
import com.yuxuan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yuxuan.utils.RedisConstants.*;
import static com.yuxuan.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private FollowMapper followMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1、校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2、如果不符合，返回错误信息
            return Result.fail("手机号格式错误!");
        }
        //3、符号，生成一个验证码
        String vercode = RandomUtil.randomNumbers(6);

        //4、保存验证码到Redis    //set key value ex:120
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, vercode,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5、发送验证码
        log.debug("发送验证码短信成功，验证码：{}",vercode);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //1、校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误!");
        }
        //TOOD 2、校验验证码from Redis
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String vercode01 = loginForm.getCode();
        if (code == null || ! code.equals(vercode01)){
            //不一致报错
            return Result.fail("验证码错误");
        }

        //4、一致，根据手机号查询用户   tb_user 查询的具体的表
        User user = query().eq("phone", phone).one();

        //5、判断用户存在
        if (user == null){
            //6、不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        //7、保存用户信息到session中
        // 7.1 随机生成token,作为令牌
        String token = UUID.randomUUID().toString(true);
        // 7.2  将UserDTO对象转为 Hash 存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fielName,fieldValue) -> fieldValue.toString())); //所有字段都转字符串
        //7。3 存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //7.4 设置token 有效期    每次用户登录都会更新Redis中的token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL, TimeUnit.MINUTES);

        //8.返回token
        return Result.ok(token);
    }

    //签到功能
    @Override
    public Result sign() {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2、获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy-MM"));
        //3、拼接key
           String key = USER_SIGN_KEY + userId + keySuffix;
        //4、获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        //5、写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);

        return Result.ok();
    }

    @Override
    public Result signCount() {
        //1、获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2、获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy-MM"));
        //3、拼接key
        String key = USER_SIGN_KEY + userId + keySuffix;
        //4、获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        //5、获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字
        // bitfield sign:1010:2025-11 get u8 0 返回结果 64、    //为啥是一个集合，一位bitfield包含查询、修改、自增，可以同时拿到多个结果
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );

        if (result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        int count = 0;
        //6、循环遍历
        while (true){
            //6.1 让这个数字与 1 做与运算，得到数字的最后一个bit位
            if((num & 1) == 0){
                //6.2 判断这个bit位是否为0 // 如果为0，说明未签到，结束
                break;
            } else {
                //不为 0 说明已签到，签到数+1
                count++;
            }
            //把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>=1;
        }

        return Result.ok(count);
    }

    //笔记详情页面查询用户基本详情
    @Override
    public Result queryAuthorById(Long authorId) {
        User user = userMapper.selectById(authorId);
        // 如果用户注销直接返回
        if (user == null){
            return Result.ok("该用户不存在！");
        }
        Boolean fllowed = isFllowed(user.getId());
        AuthorDTO authorDTO = new AuthorDTO(user);
        authorDTO.setFollowed(fllowed);
        return Result.ok(authorDTO);
    }

    private User createUserWithPhone(String phone) {
        // 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomNumbers(10));
        // 保存用户
        save(user);
        return user;
    }
    private Boolean isFllowed(Long authorId){
            // 检测当前用户是否关注过作者
            Long userId = UserHolder.getUser().getId();
            String key = FOLLOWS_KEY + userId;

            // 使用 Boolean.TRUE.equals() 正确处理 null 值
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForSet().isMember(key, authorId.toString())
            );

    }
}
