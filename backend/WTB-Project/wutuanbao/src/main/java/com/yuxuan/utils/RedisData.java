package com.yuxuan.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;    //逻辑过期时间
    private Object data;                //转换成可以存入Redis的对象
}
