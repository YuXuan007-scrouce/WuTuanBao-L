package com.yuxuan.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_blog")
public class BlogDTO {

    private Long id;
    private Long userId;
    private String title;
    /**
     * 图片集
     * 存储 objectKey，用逗号分隔
     */
    private String images;

    private String content;

    private Integer liked;
    private Integer collection;   //收藏数
    private Integer comments;

    private LocalDateTime createTime;
    private String address;

}

