package com.yuxuan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GroupDetailDTO {

    private Long id;
    private Long merchantId;
    private String title;
    private String images;
    private BigDecimal originalPrice;  //原价
    private BigDecimal dealPrice;   //团购价
    private BigDecimal discount;  //折扣
    private String briefDesc;    //简短描述
    private Long soldCount;
    private Long stock;   //存库
    private int dealType;   //团购类型
    private String validTimeDesc;   //有效时间描述
    private String tags;    //支付标签
    private LocalDateTime startDate;  //开始日期
    private LocalDateTime endDate;    //结束日期
    private int status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
