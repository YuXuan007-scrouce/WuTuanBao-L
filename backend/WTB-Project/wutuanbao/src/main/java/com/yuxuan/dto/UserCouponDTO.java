package com.yuxuan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserCouponDTO {

    private Long id;
    private Long couponId;
    private Long merchantId;
    private BigDecimal couponAmount;
    private String name;
    private String rules;
}
