import http from "../../utils/http";
import type { ShopDTO,SearchParams,SearchSuggestionResult } from "./type"
import { resolveAssetUrl } from '@/utils/asset'; // 图片url拼接

// 2. 定义响应结构 (根据你后端的统一返回格式，这里假设是 data.list)
export interface SearchResponse {
  shopDocs: ShopDTO[];
  total: number;
}

// 3. 导出接口请求函数
// 这是一个 GET 请求示例
export function getSearchResult(params: SearchParams) {
  return http.get<SearchResponse>('/shopping/show/search', params);
}

/**
 * 获取搜索联想词建议
 * @param key 搜索关键词
 */
export function getSearchSuggestion(key: string) {
  return http.get<SearchSuggestionResult>(`/shopping/show/search/suggestion?key=${key}`);
}

/**
 * 距离格式化工具
 * 0.039 -> "39m"
 * 0.0872 -> "87m"
 * 1.2 -> "1.2km"
 */
function formatDistance(val: number): string {
  if (!val && val !== 0) return '';
  if (val < 1) {
    // 小于1公里，显示米，取整
    return Math.round(val * 1000) + 'm';
  } else {
    // 大于1公里，保留1位小数
    return val.toFixed(1) + 'km';
  }
}

/**
 * 数据适配器：将后端原始数据转换为前端 ShopDTO
 * @param backendData 后端 shopDocs 数组中的一项
 */
export function adaptShopData(backendData: any): ShopDTO {
  return {
    id: backendData.id,
    name: backendData.name,
    // 1. 处理图片路径拼接
    coverImages: resolveAssetUrl(backendData.cover_image),
    rating: Number(backendData.rating),
    totalReviews: backendData.total_reviews,
    // 2. 处理距离格式化 (核心逻辑)
    distance: formatDistance(backendData.distance),
    shopType: backendData.category,
    address: backendData.address,
    avgPrice: Number(backendData.avg_price),
    businessHours: backendData.business_hours,
    base_tags: backendData.base_tags,
    
    // 3. 补充可选字段默认值
    tagShop: [], 
    priceDes: ''
  };
}

// 如果你的搜索条件特别复杂（比如包含多选标签、价格区间），也可以用 POST
// export function getSearchResult(data: SearchParams) {
//   return request.post<SearchResponse>('/shop/search', data);
// }