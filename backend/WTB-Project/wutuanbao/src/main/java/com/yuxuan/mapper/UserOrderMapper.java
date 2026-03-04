package com.yuxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxuan.entity.UserOrder;
import com.yuxuan.entity.vo.PendingPayOrderVO;
import org.apache.ibatis.annotations.Param;

public interface UserOrderMapper extends BaseMapper<UserOrder> {
    /**
     * 根据orderNo查询待支付页面所需数据（联表）
     */
    PendingPayOrderVO selectPendingPayOrder(@Param("orderNo") String orderNo,
                                            @Param("userId") Long userId);
}
