// src/api/merchant/type.ts

/**
 * 商家详情 DTO (对应 MySQL merchant + merchant_tag 表)
 */
export interface MerchantDetailDTO {
  id: number | string;
  name: string;             // 商家名称
  images: string;         // 轮播图 (对应 merchant.images，前端转数组)
  rating: number;           // 评分
  totalReviews: number;     // 评论总数
  avgPrice: number;         // 人均价格
  category: string;         // 分类 (台球厅)
  address: string;          // 地址
  baseTags: string;         // 基础标签 (如 "可以一试")
  customValue: string;     // 对应 merchant_tag.custom_value (如 ["24小时营业", "免费停车"])
  distance?: string;        // 从列表页传过来的距离 (如 "1.2km")
  visitorCount?: string;    // 回头客 (静态 "200+")
}

/**
 * 团购商品 DTO (对应 group_deal 表)
 */
export interface GroupDealDTO {
  id: number | string;
  title: string;            // 标题 "【谈小娱...】"
  images: string;         // 商品图
  validTimeDesc: string;    // "周一至周日可用"
  soldCount: number;        // "已售10万+"
  dealPrice: number;        // 团购价 23.5
  originalPrice: number;    // 原价 116
  discount: number;         // 折扣 2.1
}

/**
 * 用户评论 DTO (对应 tb_user + merchant_comment 表)
 */
export interface CommentDTO {
  id: number | string;
  userId: number | string;   //用户ID
  nickName: string;         // 用户名
  userIcon: string;         // 头像
  rating: number;           // 评分 5.0
  content: string;          // 评论内容
  likeCount: number;        //点赞数
  createTime: string;

  conIcons?: string[];      // 评论配图
  avgPrice?: number;        // 评论时的人均
}

/**
 * 优惠券 DTO (用于弹窗)
 */
export interface CouponDTO {
  id: number;
  name: string;         // "超值券"
  actualValue: number;       // 减免金额 (如 50)
  beginTime: string;          // 开抢时间
  rules: string;           //使用规则说明 
  userReceived: boolean; // 用户是否已领取
  isSoldOut: boolean;    //前端UI使用
}


// 定义团购商品项数据类型
export interface GroupProduct {
  id: number;
  merchantId: number;
  title: string;
  images: string;
  originalPrice: number;
  dealPrice: number;
  validTimeDesc: string;
  tags: string;
}

// 定义优惠卷数据类型
export interface UserCoupon {
  id: number;
  couponId: number;
  merchantId: number;
  couponAmount: number;
  name: string;
  rules: string;
}

// 现有的 UserCoupon, GroupProduct 等保持不变...

// 新增：创建订单的请求参数 (DTO)
export interface CreateOrderDTO {
  merchantId: number;       // 商家ID
  productId: number;        // 团购商品ID
  productTitle: string;     // 商品标题 (有些后端设计需要冗余存标题)
  originalPrice: number;    // 商品原价 (单价)
  dealPrice: number;        // 商品团购价 (单价)
  quantity: number;         // 购买数量
  totalAmount: number;      // 订单总原价 (originalPrice * quantity)
  realPayAmount: number;    // 用户实际支付金额 (减去优惠后的最终金额)
  couponId?: number;        // 优惠券ID (可选，没选就是 null/undefined)
  mobile: string;           // 用户填写的手机号
  payType: 1 | 2 | 3;       // 支付方式: 1抖音, 2微信, 3支付宝
  status: number;           // 支付状态: 1 (支付中)
}

// 新增：创建订单的响应结果 (通常后端会返回生成的订单号)
export interface CreateOrderResult {
  orderNo: string;          // 订单编号
  payAmount: number;          //实际支付金额
  payUrl?: string;          // (可选) 如果真接支付，后端可能返回支付链接
}


// 确认支付的响应数据
export interface ConfirmPayResult {
  orderNo: string;
  payAmount: number;
  payTime: string;    // 后端返回的 ISO 时间字符串
  dealTitle: string;  // 团购标题
}

/**
 * 取消支付的响应数据
 */
export interface CancelPayResponse {
  orderNo: string;
  message: string;
  status: number;
}
/**
 * 订单详情页面
 */
export interface OrderDetailData {
  orderNo: string;
  payAmount: number;
  totalAmount: number;
  quantity: number;
  createTime: string;
  status: number; // 0:待支付, 2:已完成, 3:已取消, 4:已退款
  dealTitle: string;
  dealImage: string;
  originalPrice: number;
  dealPrice: number;
  tags: string;
  validTimeDesc: string;
  coverImage: string;
  merchantName: string;
  merchantAddress: string;
  businessHours: string;
  merchantPhone: string;
  longitude: number;
  latitude: number;
}