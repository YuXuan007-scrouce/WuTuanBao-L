package com.yuxuan.controller.shop;

import com.yuxuan.dto.Result;
import com.yuxuan.entity.MCoupon;
import com.yuxuan.service.MCouponService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/coupon")
public class MCouponController {

    @Resource
    private MCouponService couponService;

    @PostMapping("/seckill/add")
    public Result addSeckillCoupon(@RequestBody MCoupon seckillCoupon) {
        return couponService.addSeckillCoupon(seckillCoupon);
    }
}
