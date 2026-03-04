package com.yuxuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuxuan.dto.GDOrderDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.entity.UserOrder;

public interface UserOrderService extends IService<UserOrder> {
    Result createOrder(GDOrderDTO orderDTO);

    Result confirmPay(String orderNo);

    Result orderDetail(String orderNo);

    Result cancelPay(String orderNo);

    void cancelOrderBySystem(UserOrder order);

    Result repay(String orderNo);
}
