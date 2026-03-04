package com.yuxuan.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResult {
    private List<?> list;       //每次分页查询的数据量
    private Long minTime;      //上次查询的最小时间戳
    private Integer offset;   //偏移量，每次要跳过多少个元素开始查

    // 顶部切换类型
    private String type;
}
