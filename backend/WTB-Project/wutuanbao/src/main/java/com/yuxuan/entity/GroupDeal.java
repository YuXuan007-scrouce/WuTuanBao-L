package com.yuxuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("group_deal")
public class GroupDeal implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键自增类型
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String title;

    private String images;
    private BigDecimal originalPrice;  //原件
    private BigDecimal dealPrice;  //团购价
    private BigDecimal discount;   //折扣
    private String briefDesc;    //简短描述
    private Integer soldCount;    //已销售数量
    private Integer stock;      //库存
    private Integer dealType;   // 团购类型
    private String validTimeDesc;  //有效时间

    private Date startDate;
    /**
     * 结束日期
     */
    private Date endDate;
    /**
     * 状态(0下架1上架)
     */
    private Integer status;
    /**
     * 排序权重
     */
    private Integer sortOrder;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;

}
