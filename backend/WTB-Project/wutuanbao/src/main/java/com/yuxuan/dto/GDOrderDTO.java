package com.yuxuan.dto;

import java.math.BigDecimal;
import javax.validation.constraints.*;
import lombok.Data;


@Data
public class GDOrderDTO {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "商品标题不能为空")
    private String productTitle;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价不能低于0.01")
    private BigDecimal originalPrice;

    @NotNull(message = "团购价不能为空")
    @DecimalMin(value = "0.01", message = "团购价不能低于0.01")
    private BigDecimal dealPrice;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    @NotNull(message = "实付金额不能为空")
    private BigDecimal realPayAmount;

    // 优惠券ID，非必填
    private Long couponId;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /**
     * 支付方式: 1-抖音支付, 2-微信支付, 3-支付宝
     */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    /**
     * 订单状态: 1-支付中
     */
    private Integer status;
}
