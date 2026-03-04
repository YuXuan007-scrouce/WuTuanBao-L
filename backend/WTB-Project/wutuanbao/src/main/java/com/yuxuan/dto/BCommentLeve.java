package com.yuxuan.dto;

import lombok.Data;

@Data
public class BCommentLeve {
    private Long blogId;
    private String content;
    private Long parentId;
    private Long replyToUserId;
}
