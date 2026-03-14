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
@TableName("merchant_coupon")
public class MCoupon implements Serializable {

    /**
     * 主键自增类型
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long merchantId;
    private String name;
    private BigDecimal actualValue;      //实际扣减金额
    private Integer stock;
    private String rules;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer status;   //该卷状态，1：上架中，2：下架了，3：过期了
    private LocalDateTime removalTime;    // 活动下架时间
}
