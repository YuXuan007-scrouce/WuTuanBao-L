package com.yuxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxuan.dto.*;
import com.yuxuan.entity.Merchant;

import java.util.List;

public interface ShopListMapper extends BaseMapper<Merchant> {
    MerchantDTO queryMDetail(Long id);

    List<GroupDealDTO> queryGroup(Long id);


    List<MCommentDTO> queryComment(Long merchantId);

    List<MCouponDTO> queryCoupon(Long userId, Long merchantId,long status);

    GroupPayDTO queryGroupProduct(Long id);

    List<UserCouponDTO> queryUserCoupon(Long merchantId, Long userId);

    List<Long> selectAllMerchantIds();
}
