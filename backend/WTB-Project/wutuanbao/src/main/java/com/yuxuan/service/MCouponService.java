package com.yuxuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuxuan.entity.MCoupon;

public interface MCouponService extends IService<MCoupon> {

    void addSeckillCoupon(MCoupon seckillCoupon);
}
