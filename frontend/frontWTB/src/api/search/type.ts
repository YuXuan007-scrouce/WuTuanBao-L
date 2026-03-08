

// 商家数据接口定义 (对应后端 ES 结构)
export interface ShopDTO {
 id: string | number;
  name: string;             // 商家名
  coverImages: string;      // 封面图 (已拼接好完整URL)
  rating: number;           // 评分
  totalReviews: number;     // 评论数
  distance: string;         // 已格式化的距离 (如 "39m", "1.2km")
  shopType: string;         // 商家类型
  address: string;          // 地址
  avgPrice: number;         // 人均
  businessHours: string;    // 营业时间
  base_tags: string;        // 基础标签 (如 "超赞", "人气王")
  tagShop?: string[];      // 商家关联标签 (如 "24小时营业")
  tagCoupon?: string[];   // 优惠标签
  priceDes?: string;      // 价格描述 (如 "￥19.5")
 
}

// 搜索历史接口
export interface SearchHistoryItem {
  id: number;
  keyword: string;
}

// 1. 定义请求参数类型
export interface SearchParams {
  keyword: string;
  page: number;
  size: number;
  latitude?: number; // 经度 (可选，用于计算距离)
  longitude?: number; // 纬度 (可选)
  sortBy?: 'comments' | 'default' | 'rating'; // 排序方式 (预留): 评价(人气)|默认|星级
  near?: number;     //附近
}


// 接收后端响应的"词条补全"
export type SearchSuggestionResult = string[];
