package com.yuxuan.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {

    @Bean
    public RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        //创建RedissonCilent对象并返回
        return Redisson.create(config);
    }
}
