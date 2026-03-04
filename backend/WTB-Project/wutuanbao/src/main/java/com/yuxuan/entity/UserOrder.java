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
@TableName("user_order")
public class UserOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo; //订单号
    private Long userId;
    private Long deal_id;     //团购id
    private Long merchantId;  //商家id
    private Long couponId;   //使用的优惠卷id
    private String dealTitle;
    private BigDecimal dealPrice;  //团购价
    private int quantity;   //购买数量
    private BigDecimal totalAmount;   //总价
    private BigDecimal payAmount;   //实际支付的金额
    private int status;  // 0:待支付、1：支付中、2：支付完成、3、取消支付、4：退款
    private int payType;   //支付方式
    private LocalDateTime payTime;   //支付时间
    private BigDecimal refundAmount;  //退款金额
    private LocalDateTime refundTime;  //退款时间
    private String remark;   //退款备注
    private LocalDateTime createdTime;
}
