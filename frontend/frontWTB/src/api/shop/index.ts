import http from "../../utils/http";
import { MerchantDetailDTO,GroupDealDTO,CommentDTO,CouponDTO,GroupProduct,UserCoupon
  ,CreateOrderDTO,CreateOrderResult,ConfirmPayResult,CancelPayResponse,OrderDetailData
} from "./type";
// 引入你刚才定义的通用接口
import type { ResultData } from '@/utils/http/type'; // 假设你放在这里
/**
 * 获取商家详情页面，商家详情信息
 */
export function queryMDetail(id: number | string) {
  return http.get<MerchantDetailDTO>('/shopping/show/detail', {
     id 
  })
}
// 查询团购列表
export function queryGroupProducts(id: number | string) {
  return http.get<GroupDealDTO[]>('/shopping/show/groupProducts', {
    id 
  })
}

// 查询详情页面的精选"评论",后端限制3条
export function queryComments(merchantId: number | string) {
  return http.get<CommentDTO[]>('/shopping/show/comments', {
    merchantId 
  })
}

// 查询优惠卷(需要登录才能查看)
export function queryMerchantCoupon(id: number | string) {
  return http.get<CouponDTO[]>(`/shopping/merchant-coupon/${id}`)
}


/**
 * 优惠的秒杀请求，比如一小时内有效
 */

/**
 * 秒杀优惠券
 * @param id 优惠券ID
 * 返回值: ResultData<number> -> 表示 data 是订单号
 */
// export function seckillCoupon(id: number | string) {
//   // 注意：后端通常需要对象传参，所以我包裹了 { voucherId: id }
//   // 如果后端直接接收 param，请改为 params 方式，但秒杀推荐 POST body
//   return http.post<ResultData<number>>('/merchant/seckill-coupon/${id}');
// }
export function seckillCoupon(id: number | string) {
  return http.post<ResultData<number>>(
    `/merchant/seckill-coupon/${id}`
  );
}

/**
 * 支付界面的团购项查询
 */
// 支付界面的团购商品查询接口
export function queryGroupProduct(id: number | string) {
  return http.get<GroupProduct>(`/shopping/payment/groupProduct/${id}`);
}


// 可用优惠卷查询接口
export function queryUserCoupon(id: number | string) {
  return http.get<UserCoupon[]>(`/shopping/payment/userCoupon/${id}`);
}

/**
 * 提交订单/创建订单
 * @param data 订单表单数据
 */
export function createOrder(data: CreateOrderDTO) {
  return http.post<CreateOrderResult>('/payment/user/createOrder', data);
}

/**
 * 确认支付接口
 * @param orderNo 订单号
 */
export function confirmPay(orderNo: string) {
  // 注意：根据你的要求，参数是拼在 URL 上的 query 参数
  return http.post<ConfirmPayResult>(`/payment/user/confirmPay?orderNo=${orderNo}`);
}

// 如果后端要求是 /cancelPay?orderNo=xxx 这种格式，即便用 post 也要传 params
export const cancelPay = (orderNo: string) => {
  return http.post<CancelPayResponse>(
    '/payment/user/cancelPay',
    undefined,
    { params: { orderNo } } // 强制转为 Query 参数
  );
};

// 2. 订单详情查询 (GET 请求)
export const getOrderDetail = (orderNo: string) => {
  return http.get<OrderDetailData>(
    '/payment/user/orderDetail',
    { orderNo } 
  );
};

/**
 * 继续支付接口
 * @param orderNo 订单号
 */
export function repay(orderNo: string) {
  return http.post<ResultData<string>>(
    `/payment/user/repay?orderNo=${orderNo}`
  ); 
}