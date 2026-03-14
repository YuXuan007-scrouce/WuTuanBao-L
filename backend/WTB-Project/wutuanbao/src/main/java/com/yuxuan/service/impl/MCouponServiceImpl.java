package com.yuxuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.MCoupon;

import com.yuxuan.mapper.MCouponMapper;
import com.yuxuan.mq.CouponMqProducer;
import com.yuxuan.service.MCouponService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class MCouponServiceImpl extends ServiceImpl<MCouponMapper, MCoupon> implements MCouponService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CouponMqProducer couponMqProducer;

    @Override
    @Transactional
    public Result addSeckillCoupon(MCoupon seckillCoupon) {
        // 1、校验下架时间是否晚于当前时间
        if (seckillCoupon.getRemovalTime() == null){
            return Result.fail("必须设置当前时间");
        }
        if (!seckillCoupon.getRemovalTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("下架时间必须晚于当前时间");
        }
        // 2. 校验下架时间不能晚于结束时间
        if (seckillCoupon.getRemovalTime().isAfter(seckillCoupon.getEndTime())) {
            throw new RuntimeException("下架时间不能晚于活动结束时间");
        }
        // 3. 写入数据库
        save(seckillCoupon);

        // 4. 保存秒杀库存到 Redis
        stringRedisTemplate.opsForValue().set(
                "coupon:stock:" + seckillCoupon.getId(),
                seckillCoupon.getStock().toString()
        );
        // 5、向MQ发送消息
        couponMqProducer.sendCouponRemovalMessage(seckillCoupon.getId(),seckillCoupon.getRemovalTime());
        return Result.ok("添加成功");
    }
}
