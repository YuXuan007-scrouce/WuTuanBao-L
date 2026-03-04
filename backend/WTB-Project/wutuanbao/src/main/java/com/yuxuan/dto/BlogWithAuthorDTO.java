package com.yuxuan.dto;

import lombok.Data;

import java.time.LocalDateTime;

// 接收数据查询结果
@Data
public class BlogWithAuthorDTO {

    //tb_blog
    private Long blogId;
    private String title;
    private String images;
    private LocalDateTime createTime;
    private Integer liked;

    //tb_user
    private String authorAvatar;
    private String authorName;

    private Boolean isLike;

}
