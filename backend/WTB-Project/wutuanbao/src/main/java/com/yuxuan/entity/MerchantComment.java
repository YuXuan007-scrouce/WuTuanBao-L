package com.yuxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("merchantComment")
public class MerchantComment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键自增类型
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long userId;
    private String content;
    private String contentUrl;
    private Long parentId;
    private Long replyToUserId;
    private BigDecimal rating;  //评分
    private Long likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
