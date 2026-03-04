package com.yuxuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.UserCoupon;

import java.math.BigDecimal;

public interface UserCouponService extends IService<UserCoupon> {
    Result deskillCoupon(Long id);

    void createCoupon(UserCoupon userCoupon);

    Boolean lockCoupon(Long userId, Long couponId);

    BigDecimal queryCoupon(Long userId, Long couponId);
}
