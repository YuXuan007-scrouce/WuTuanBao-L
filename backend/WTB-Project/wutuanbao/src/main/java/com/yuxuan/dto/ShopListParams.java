package com.yuxuan.dto;

import lombok.Data;

@Data
public class ShopListParams {
    private String keyword;
    private int page;
    private int size;
    private String sortBy;  // 排序规则
    private Double near;  // 附近距离
    private Double latitude;
    private Double longitude;


}
