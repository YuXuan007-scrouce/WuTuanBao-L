package com.yuxuan.controller.shop;

import com.yuxuan.dto.Result;
import com.yuxuan.service.UserCouponService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/merchant")
public class UserCouponController {


    @Resource
    private UserCouponService userCouponService;

    /**
     *根据优惠卷id,抢构到，则向用户用户优惠卷表添加一张id
     */
    @PostMapping("/seckill-coupon/{id}")
    public Result deskillCoupon(@PathVariable("id") Long id) {
        return userCouponService.deskillCoupon(id);
    }
}
