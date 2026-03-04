package com.yuxuan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GroupDealDTO {

    private Long id;
    private String title;
    private String images;
    private String validTimeDesc;
    private Long soldCount;
    private BigDecimal dealPrice;       //团购价格
    private BigDecimal originalPrice;  //原价格
    private BigDecimal discount;  //折扣
}
