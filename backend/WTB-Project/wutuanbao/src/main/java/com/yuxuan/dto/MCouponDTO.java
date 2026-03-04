package com.yuxuan.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MCouponDTO {

    private Long id;
    private String name;
    private Integer actualValue;
    private LocalDateTime beginTime;
    private String rules;
    private boolean userReceived;  // 当前用户是否已经领取
}
