package com.yuxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxuan.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    int lockCoupon(@Param("userId") Long userId,
                   @Param("couponId") Long couponId);

    BigDecimal queryCoupon(Long userId, Long couponId);
}
