package com.yuxuan.entity;

import lombok.Data;

import java.util.List;

@Data
public class PageResult {
    // 用于返回给前端结果的
    private List<ShopDoc> shopDocs;

    public PageResult() {
    }

    public PageResult(List<ShopDoc> shopDocs) {

        this.shopDocs = shopDocs;
    }
}