package com.yuxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)  //自动为类生成 equals() 和 hashCode() 方法
@Accessors(chain = true)
@TableName("tb_blog_comment")
public class BComment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long blogId;     //前端传入
    private Long userId;
    private String content;     //前端传入
    private Long parentId;       //二级评论，指向一级评论
    private Long replyToUserId;  //回复的是哪一个用户？
    private Integer likeCount;
    private LocalDateTime createTime;
    private String address;
}
