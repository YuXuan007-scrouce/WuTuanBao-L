package com.yuxuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.entity.MCoupon;

import com.yuxuan.mapper.MCouponMapper;
import com.yuxuan.service.MCouponService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class MCouponServiceImpl extends ServiceImpl<MCouponMapper, MCoupon> implements MCouponService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void addSeckillCoupon(MCoupon seckillCoupon) {
        // 写入数据库
        save(seckillCoupon);
        // 保存秒杀信息
        stringRedisTemplate.opsForValue().set("coupon:stock:" + seckillCoupon.getId(),seckillCoupon.getStock().toString());
    }
}
