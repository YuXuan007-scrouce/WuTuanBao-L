package com.yuxuan.controller.shop;

import com.yuxuan.dto.Result;
import com.yuxuan.dto.ShopListParams;
import com.yuxuan.service.ShopListService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shopping/")
public class ShopListController {

    @Resource
    private ShopListService shopListService;

    @GetMapping("/show/search")
    public Result search(ShopListParams shopParams) {
        return shopListService.search(shopParams);
    }

    // 根据id查询商家详情
    @GetMapping("/show/detail")
    public Result queryMDetail(Long id) {
        return shopListService.queryMDetail(id);
    }
    /**
     * 根据商家id查询团购信息
     */
    @GetMapping("/show/groupProducts")
    public Result queryGroup(Long id) {
        return shopListService.queryGroup(id);
    }

    /**
     * 根据商家id查询优惠卷信息
     */
    @GetMapping("/merchant-coupon/{id}")
    public Result queryMerchantCoupon(@PathVariable("id") Long id) {
        return shopListService.queryMerchantCoupon(id);
    }

    /**
     * 根据商家id查询查询评论信息初始页面展示3条
     */
    @GetMapping("/show/comments")
    public Result queryComment(Long merchantId) {
        return shopListService.queryComment(merchantId);
    }

    /**
     * 订单支付界面的团购商品查询信息
     */
    @GetMapping("/payment/groupProduct/{id}")
    public Result queryGruopProduct(@PathVariable("id") Long id){
        return shopListService.queryGroupProduct(id);
    }
    /**
     * 订单支付界面的优惠卷选项查询,即查询该商家下可用的优惠卷
     */
    @GetMapping("/payment/userCoupon/{merchantId}")
    public Result queryUserCoupon(@PathVariable("merchantId") Long merchantId){
        return shopListService.queryUserCoupon(merchantId);
    }

}
