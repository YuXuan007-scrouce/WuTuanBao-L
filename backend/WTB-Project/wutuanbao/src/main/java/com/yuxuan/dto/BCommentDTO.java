package com.yuxuan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BCommentDTO {
    private Long id;              //该评论的id
    private Long blogId;
    private Long userId;
    private String content;
    private Long parentId;       //二级评论，回复的是哪一条pl
    private Long replyToUserId;  //回复的是哪一个用户？
    private Integer likeCount;
    private LocalDateTime createTime;
    private String address;

    // 下面是连接tb_user表进行查询用户信息
    private String userName;
    private String userAvatar;   //用户头像

}
