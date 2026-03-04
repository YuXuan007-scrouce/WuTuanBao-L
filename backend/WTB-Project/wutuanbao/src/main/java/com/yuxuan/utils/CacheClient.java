package com.yuxuan.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.yuxuan.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //1、将任意Java对象序列化为json并存储在string类型的key中,并且可以设置TTL过期时间
    public void set(String key, Object value,Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    //2、将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
    public void setWithLogicalExpire(String key, Object value,Long time, TimeUnit timeUnit) {
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //3、根据指定的key查询缓存,并反序列化为指定类型,利用缓存空值的方式解决缓存穿透问题
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit timeUnit){
        String key = keyPrefix + id;
        //1、从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2、判断是否存命中
        if (StrUtil.isNotBlank(json)) {
            //3、存在，直接返回
            return JSONUtil.toBean(json, type); //将字符串对象反序列化成实体对象
        }
        // TODO: 3、判断命中的的是否是空值(数据库缓存null对象到Redis)
        if( json != null){
            return null;
        }
        //4、不存在，根据id查询数据库
        R r = dbFallback.apply(id);   //传入函数逻辑
        //TODO 5、数据库不存在，返回错误,"空值存入Redis"
        if (r == null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6、数据库存在，写入Redis 并设置过期时间
        this.set(key,r,time,timeUnit);
        //7、返回
        return r;
    }

    //缓存重建执行器的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //4、根据指定的key查询缓存,并反序列化为指定类型,需要利用逻辑过期解决缓存击穿问题
    public <R,ID> R queryWithLogicalExire(String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time, TimeUnit timeUnit){
        String key = keyPrefix + id;
        //1、从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2、判断是否存命中
        if (StrUtil.isBlank(json)) {
            //3、未命中直接返回null
            return null;
        }
        //4、命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5、判断是否过期           过期时间是在当前时间之前，还是之后
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1 未过期，直接返回店铺信息
            return r;
        }
        //5.2 已过期，需要重键缓存
        //TODO: 6、缓存重建
        // 6.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 获取是否成功
        if (isLock) {
            // 6.3 成功，开启独立线程，实现缓存重建，最后释放锁
            CACHE_REBUILD_EXECUTOR.submit(() ->{
                try {
                  //查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入Redis
                    this.setWithLogicalExpire(key,r1,time,timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    releaseLock(lockKey);
                }
            });
        }

        // 6.4 失败，返回过期店铺信息
        return r;
    }

    private boolean tryLock(String key){
        // setnx Locak 1 10s
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        // TODO: 拆箱
        return BooleanUtil.isTrue(flag);
    }

    private void releaseLock(String key){
        stringRedisTemplate.delete(key);
    }
}
