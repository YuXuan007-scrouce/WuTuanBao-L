package com.yuxuan.dto;

import lombok.Data;

import java.time.LocalDateTime;
// 下面是创建个作品使用到的参数

@Data
public class NoteDTO {
    private Long id;
    private Long userId;
    private String images;
    private String title;
    private String content;
    private String address;
    private LocalDateTime createTime;
}
