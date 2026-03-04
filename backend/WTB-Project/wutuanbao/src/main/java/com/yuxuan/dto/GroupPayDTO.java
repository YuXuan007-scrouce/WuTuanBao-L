package com.yuxuan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroupPayDTO {

    private Long id;
    private Long merchantId;
    private String title;
    private String images;
    private BigDecimal originalPrice;
    private BigDecimal dealPrice;
    private String validTimeDesc;
    private String tags;
}
