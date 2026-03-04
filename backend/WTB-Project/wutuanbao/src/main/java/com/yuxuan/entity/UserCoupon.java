package com.yuxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_coupon")
public class UserCoupon implements Serializable {
    // 用户领取到优惠卷
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long couponId;
    private Long merchantId;    //用于限制使用范围，只能在当前商家使用
    private BigDecimal couponAmount;    // 可用的扣减订单的金额
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime expireTime;
    private LocalDateTime useTime;    // 使用时间
    private Long orderId;
    private Integer version;
}
