package com.yuxuan.controller.shop;

import com.yuxuan.dto.GDOrderDTO;
import com.yuxuan.dto.Result;
import com.yuxuan.service.UserOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/payment")
public class UserOderController {

    //专门用来管理用户订单：支付、退款、待支付倒计数、订单完成后的查询
    @Resource
    private UserOrderService userOrderService;

    /**
     * 订单支付界面：订单创建并支付(支付中状态)
     */
    @PostMapping("/user/createOrder")
    public Result createOrder(@Validated @RequestBody GDOrderDTO orderDTO){
        if(orderDTO.getMerchantId() == null){
            return Result.fail("订单提交不能为空");
        }
        return userOrderService.createOrder(orderDTO);
    }

    /**
     * 确认支付：订单状态 支付中 → 已完成
     */
    @PostMapping("/user/confirmPay")
    public Result confirmPay(@RequestParam String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return Result.fail("订单号不能为空");
        }
        return userOrderService.confirmPay(orderNo);
    }

    /**
     *  更具订单号查询订单详情信息
     */
    @GetMapping("/user/orderDetail")
    public Result oderDetail(@RequestParam String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return Result.fail("订单号不能为空");
        }
        return userOrderService.orderDetail(orderNo);
    }

    /**
     * 用户主动取消支付：支付中(1) → 待支付(0)
     */
    @PostMapping("/user/cancelPay")
    public Result cancelPay(@RequestParam String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return Result.fail("订单号不能为空");
        }
        return userOrderService.cancelPay(orderNo);
    }
    /**
     * 用户再次确认支付：待支付(0) -> 支付中(1)
     */
    @PostMapping("/user/repay")
    public Result repay(@RequestParam String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return Result.fail("订单号不能为空");
        }
        return userOrderService.repay(orderNo);
    }
}
