package com.yuxuan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MCommentDTO {
    private Long id;
    private Long userId;
    private String nickName;  //用户名
    private String userIcon;  // 用户头像
    private String content;
    private String contentUrl;
    private Long parentId;
    private Long replyToUserId;
    private BigDecimal rating;  //评分
    private Long likeCount;
    private LocalDateTime createTime;
}
