package com.yuxuan.service;

import com.yuxuan.dto.Result;
import com.yuxuan.dto.ShopListParams;

public interface ShopListService {

    Result search(ShopListParams shopParams);

    Result queryMDetail(Long id);

    Result queryGroup(Long id);

    Result queryComment(Long merchantId);

    Result queryMerchantCoupon(Long id);

    Result queryGroupProduct(Long id);

    Result queryUserCoupon(Long merchantId);


    Result getSuggestions(String prefix);
}
