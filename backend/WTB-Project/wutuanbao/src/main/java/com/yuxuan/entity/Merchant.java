package com.yuxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    //映射merchant表
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String coverImage;
    private String images;
    private BigDecimal rating;  //星级/评分
    private Integer totalReviews;  //总评价数
    private String category;   //娱乐类型
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal avgPrice;
    private Integer salesVolume;
    private String businessHours;
    private String baseTags;
    private String phone;
    private Boolean status;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
