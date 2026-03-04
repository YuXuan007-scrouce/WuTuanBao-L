// 通用API响应类型
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}