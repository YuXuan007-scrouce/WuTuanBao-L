package com.yuxuan.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PendingPayOrderVO {
    // 订单信息
    private String orderNo;
    private BigDecimal payAmount;     // 实付金额
    private BigDecimal totalAmount;   // 总价
    private Integer quantity;         // 购买数量
    private LocalDateTime createTime; // 订单创建时间（用于前端计算倒计时）
    private int status;               // 订单状态

    // 团购商品信息
    private String dealTitle;         // 团购标题
    private String dealImage;         // 团购图片
    private BigDecimal originalPrice; // 原价
    private BigDecimal dealPrice;     // 团购价
    private String tags;              // 团购商品标签
    private String validTimeDesc;     // 有效期描述

    // 商家信息
    private String coverImage;
    private String merchantName;      // 商家名称
    private String merchantAddress;   // 商家地址
    private String businessHours;     // 营业时间
    private String merchantPhone;     // 商家电话
    private BigDecimal longitude;     // 经度（用于导航）
    private BigDecimal latitude;      // 纬度
}