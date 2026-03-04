package com.yuxuan.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Data
public class ShopDoc {
    private Long id;
    private String name;
    @JsonProperty("cover_image")
    private String coverImage;
    private BigDecimal rating;
    @JsonProperty("total_reviews")
    private Integer totalReviews;  //总评价数
    @JsonProperty("avg_price")
    private BigDecimal avgPrice;
    private String category;   //娱乐类型
    private String address;
    private String location;    //位置
    @JsonProperty("business_hours")
    private String businessHours;
    @JsonProperty("base_tags")
    private String baseTags;
    private Double distance;  //距离值
    private List<String> suggestion;   //存储用户搜索，词条自动补全的词

    public ShopDoc() {
    }

   public ShopDoc(Merchant merchant){
       this.id = merchant.getId();
       this.name = merchant.getName();
       this.coverImage = merchant.getCoverImage();
       this.rating = merchant.getRating();
       this.totalReviews = merchant.getTotalReviews();
       this.avgPrice = merchant.getAvgPrice();
       this.category = merchant.getCategory();
       this.address = merchant.getAddress();
       this.location = merchant.getLatitude()+", "+merchant.getLongitude();
       this.businessHours = merchant.getBusinessHours();
       this.baseTags = merchant.getBaseTags();
       this.suggestion = Arrays.asList(this.name, this.category);
   }
}
