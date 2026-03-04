package com.yuxuan.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate redisTemplate;
    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {  //传入锁的名称
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;    //静态的 初始化就加载好lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        //使用spring提供的ClassPathResource加在类路径下的resources目录下的资源
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    //这里的ID_PREFIX同一线程时，值是一样的
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示   当作vaule
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁   setIfAbsent()封装好了对ok和nil的判断
        Boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId , timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);   //这里success返回的nil和null最后返回结果都是false,避免空指针的风险
    }

    @Override
    public void unlock() {
        //调用lua脚本
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),    //单个集合，将一个字符串存入KYS[]
                ID_PREFIX + Thread.currentThread().getId());
    }
//    @Override
//    public void unlock() {
//        //获取线程标识
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        //获取锁中的标识
//        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
//        //判断标识是否一致
//        if(threadId.equals(id)){
//            redisTemplate.delete(KEY_PREFIX + name);//key均是lock:userId
//        }
//
//    }
}
