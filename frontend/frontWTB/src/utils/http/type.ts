
// 接收后端相应的Result类型
export interface ResultData<T = any> {
    success: boolean;
  errorMsg: string; // 有时候后端成功时这个是 null
  data: T;
  total: number;
  code?: number;    // 加个问号，表示可选，或者是 number
}


