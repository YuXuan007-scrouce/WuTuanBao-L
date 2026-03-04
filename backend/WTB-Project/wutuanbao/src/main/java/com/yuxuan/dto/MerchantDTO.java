package com.yuxuan.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantDTO {

    private Long id;
    private String name;
    //private String coverImage; // 详情页面不需要头像
    private String images;    // 轮播图
    private BigDecimal rating;

    private Integer totalReviews;  //总评价数

    private BigDecimal avgPrice;
    private String category;   //娱乐类型
    private String address;

    //private String businessHours;

    private String baseTags;   // 基础标签: 可以一试

    private String customValue;// 关联merchant_tag表

}
