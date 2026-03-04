<template>
  <div class="payment-page">
    <van-nav-bar
      title="确认订单"
      left-arrow
      fixed
      placeholder
      @click-left="router.back()"
    />

    <div class="scroll-content" v-if="product">
      
      <div class="section-card product-card">
        <div class="card-flex">
          <van-image
            :src="resolveAssetUrl(product.images)"
            width="80"
            height="80"
            radius="4"
            fit="cover"
          />
          <div class="info-right">
            <div class="title van-multi-ellipsis--l2">{{ product.title }}</div>
            <div class="tags-row">
              <van-tag plain type="primary" v-for="(tag, index) in tagList" :key="index">{{ tag }}</van-tag>
            </div>
            <div class="valid-time">{{ product.validTimeDesc }}</div>
            <div class="price-row">
              <span class="unit">¥</span>
              <span class="price">{{ product.dealPrice }}</span>
              <span class="orig">¥{{ product.originalPrice }}</span>
            </div>
          </div>
        </div>

        <van-divider />

        <div class="cell-row">
          <span class="label">购买份数</span>
          <van-stepper v-model="quantity" min="1" max="10" integer />
        </div>
      </div>

      <div class="section-card price-detail">
        <div class="section-title">本单可享</div>
        <div class="cell-row">
          <span class="label">参考价</span>
          <span class="value">¥{{ (product.originalPrice * quantity).toFixed(2) }}</span>
        </div>
        <div class="cell-row">
          <span class="label">团购优惠</span>
          <span class="value red">-¥{{ totalDiscountAmount }}</span>
        </div>
        <div class="cell-row clickable" @click="showCouponPopup = true">
          <span class="label">优惠券</span>
          <div class="value-right">
            <span v-if="selectedCoupon" class="coupon-text">-¥{{ selectedCoupon.couponAmount }}</span>
            <span v-else-if="coupons.length > 0" class="coupon-hint">{{ coupons.length }}张可用</span>
            <span v-else class="gray">暂无可用</span>
            <van-icon name="arrow" class="arrow-icon" />
          </div>
        </div>
      </div>

      <div class="section-card form-card">
        <van-field
          v-model="mobile"
          label="手机号码"
          type="tel"
          placeholder="用于接收验证码或服务通知"
          maxlength="11"
          input-align="right"
          clearable
    :border="false"
        />
      </div>

      <div class="section-card payment-method">
        <van-radio-group v-model="paymentMethod">
          <van-cell-group :border="false">
            <van-cell title="抖音支付" clickable @click="paymentMethod = 'douyin'">
              <template #icon><van-icon name="fire" color="#ff0000" size="20" class="pay-icon" /></template>
              <template #right-icon><van-radio name="douyin" checked-color="#ee0a24" /></template>
            </van-cell>
            <van-cell title="微信支付" clickable @click="paymentMethod = 'wechat'">
              <template #icon><van-icon name="wechat" color="#07c160" size="20" class="pay-icon" /></template>
              <template #right-icon><van-radio name="wechat" checked-color="#ee0a24" /></template>
            </van-cell>
            <van-cell title="支付宝" clickable @click="paymentMethod = 'alipay'">
              <template #icon><van-icon name="alipay" color="#1989fa" size="20" class="pay-icon" /></template>
              <template #right-icon><van-radio name="alipay" checked-color="#ee0a24" /></template>
            </van-cell>
          </van-cell-group>
        </van-radio-group>
      </div>
    </div>

    <van-submit-bar
      :price="finalPriceFen"
      button-text="提交订单"
      @submit="onSubmit"
      safe-area-inset-bottom
    >
      <template #default>
        <span class="promo-text" v-if="Number(totalSaved) > 0">共优惠 ¥{{ totalSaved }}</span>
      </template>
    </van-submit-bar>

    <van-popup
      v-model:show="showCouponPopup"
      position="bottom"
      round
      :style="{ height: '50%' }"
    >
      <div class="coupon-popup-container">
        <div class="popup-header">可用优惠券 ({{ coupons.length }})</div>
        <div class="coupon-list">
          <div 
            class="coupon-item" 
            v-for="c in coupons" 
            :key="c.id"
            @click="selectCoupon(c)"
            :class="{ active: selectedCoupon?.id === c.id }"
          >
            <div class="left">
              <span class="symbol">¥</span>
              <span class="amount">{{ c.couponAmount }}</span>
            </div>
            <div class="mid">
              <div class="name">{{ c.name }}</div>
              <div class="rule">{{ c.rules }}</div>
            </div>
            <div class="right">
              <van-icon name="success" v-if="selectedCoupon?.id === c.id" color="#ee0a24" />
              <div class="circle" v-else></div>
            </div>
          </div>
          <div class="no-use-btn" @click="selectCoupon(null)">不使用优惠券</div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast, showLoadingToast,showDialog  } from 'vant';
import { queryGroupProduct, queryUserCoupon,createOrder} from '@/api/shop/index'; // 你的API路径
import  { GroupProduct,UserCoupon,CreateOrderDTO } from '@/api/shop/type';
import { resolveAssetUrl } from '@/utils/asset'

const route = useRoute();
const router = useRouter();

// --- 状态数据 ---
const product = ref<GroupProduct | null>(null);
const coupons = ref<UserCoupon[]>([]);
const selectedCoupon = ref<UserCoupon | null>(null);

const quantity = ref(1);
const mobile = ref(''); // 这里可以从用户Store里预填
const paymentMethod = ref('douyin');
const showCouponPopup = ref(false);
const submitting = ref(false); // 提交时的 loading 状态

// --- 计算属性 ---

// 处理 Tags 字符串转数组
const tagList = computed(() => {
  if (!product.value?.tags) return [];
  return product.value.tags.split(/[,，]/);
});

// 计算总优惠金额（仅展示用）
const totalDiscountAmount = computed(() => {
  if (!product.value) return '0.00';
  const originalTotal = product.value.originalPrice * quantity.value;
  const saved = originalTotal - finalPrice.value;
  return saved.toFixed(2);
});

// 计算总原价
const totalOriginalPrice = computed(() => {
  if (!product.value) return 0;
  return product.value.originalPrice * quantity.value;
});

// 计算团购立减金额 (不含优惠券)
const groupDiscount = computed(() => {
  if (!product.value) return 0;
  const diff = product.value.originalPrice - product.value.dealPrice;
  return diff * quantity.value;
});

// 计算最终实付金额 (元)
const finalPrice = computed(() => {
  if (!product.value) return 0;
  // 基础团购价 * 数量
  let price = (product.value.dealPrice * quantity.value);
  //如果使用优惠卷
  if (selectedCoupon.value) {
    price -= selectedCoupon.value.couponAmount;
  }
  return price > 0 ? price : 0.01; // 防止负数，最少付1分钱
});

// Vant SubmitBar 需要的是“分”
const finalPriceFen = computed(() => {
  return Math.round(finalPrice.value * 100);
});

// 计算总共优惠了多少 (展示在底部)
const totalSaved = computed(() => {
  if (!product.value) return '0.00';
  const originalTotal = product.value.originalPrice * quantity.value;
  return (originalTotal - finalPrice.value).toFixed(2);
});

// 顶部展示的“团购优惠”金额 (包含优惠券吗？通常这里指纯团购优惠，或者总优惠)
// 根据截图逻辑，这里展示的是总优惠金额比较合适，或者分项展示
// const totalDiscountAmount = computed(() => {
//   return (totalOriginalPrice.value - finalPrice.value).toFixed(2);
// });

// --- 方法 ---

// 1. 获取支付方式对应的数字
const getPayType = (method: string): 1 | 2 | 3 => {
  switch (method) {
    case 'douyin': return 1;
    case 'wechat': return 2;
    case 'alipay': return 3;
    default: return 1; // 默认抖音
  }
}

const loadData = async () => {
  const id = route.params.id as string; // 这是团购商品 ID
  if (!id) return;

  const toast = showLoadingToast({ forbidClick: true, message: '加载中...' });

  try {
    // 1. 第一步：先查询团购商品详情
    const prodRes = await queryGroupProduct(id);

    // 校验成功且 data 存在
    if (prodRes.success && prodRes.data) {
      product.value = prodRes.data; // 赋值不再爆红

      // 2. 第二步：拿到 merchantId 后，再去查优惠券
      const mId = prodRes.data.merchantId; 
      
      // 发起优惠券请求
      const couponRes = await queryUserCoupon(mId);

      if (couponRes.success && couponRes.data) {
        coupons.value = couponRes.data; // 赋值不再爆红

        // 默认选中金额最大的逻辑
        if (coupons.value.length > 0) {
          coupons.value.sort((a, b) => b.couponAmount - a.couponAmount);
          selectedCoupon.value = coupons.value[0];
        }
      }
    } else {
      showToast(prodRes.errorMsg || '商品信息获取失败');
    }

  } catch (e) {
    console.error(e);
    showToast('网络请求失败');
  } finally {
    toast.close();
  }
};

const selectCoupon = (coupon: UserCoupon | null) => {
  selectedCoupon.value = coupon;
  showCouponPopup.value = false;
};

// 2. 提交订单核心逻辑
const onSubmit = async () => {
  if (!product.value) return;
  // 1. 校验手机号
  if (!/^1[3-9]\d{9}$/.test(mobile.value)) {
    showToast('请输入正确的手机号码');
    return;
  }
  // (2) 构造提交给后端的 DTO
  const orderData: CreateOrderDTO = {
    merchantId: product.value?.merchantId,       //商家id
    productId: product.value?.id,                //团购商品id
    productTitle: product.value?.title,  
    
    // 价格相关
    originalPrice: product.value?.originalPrice,     // 商品原价单价
    dealPrice: product.value?.dealPrice,             // 商品团购单价
    quantity: quantity.value,                       // 数量
    totalAmount: +(product.value?.originalPrice * quantity.value).toFixed(2), // 订单总原价
    realPayAmount: +finalPrice.value.toFixed(2),    // 实付总金额
    
    // 选填项
    couponId: selectedCoupon.value?.couponId,            // 优惠券ID (可能为空)
    
    // 用户信息
    mobile: mobile.value,
    payType: getPayType(paymentMethod.value),      // 转换 'douyin' -> 1
    status: 1                                      // 支付中
  };

  submitting.value = true; // 开启按钮 Loading
  try {
    // (3) 调用接口
    const res = await createOrder(orderData);

    if (res.success && res.data) {
      // 创建成功，跳转到 Mock 支付页
      // 这里可以把后端返回的 orderId 带过去
      const newOrderNo = res.data.orderNo; 
      
      router.push({
        path: '/payment/mock',
        query: {
          amount: finalPrice.value.toFixed(2), // 展示金额
          orderNo: newOrderNo                  // 真实订单号
        }
      });
    } else {
      showDialog({ message: res.errorMsg || '订单创建失败' });
    }
  } catch (error) {
    console.error('提交订单失败', error);
    showToast('网络异常，请稍后重试');
  } finally {
    submitting.value = false; // 关闭 Loading
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="less">
.payment-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px; // 为底部栏留空
}

.scroll-content {
  padding: 12px;
}

.section-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

// 商品卡片样式
.product-card {
  .card-flex {
    display: flex;
    gap: 10px;
  }
  .info-right {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    
    .title {
      font-size: 15px;
      font-weight: bold;
      color: #333;
    }
    .tags-row {
      margin: 4px 0;
      .van-tag { margin-right: 4px; }
    }
    .valid-time {
      font-size: 12px;
      color: #999;
    }
    .price-row {
      .unit { font-size: 12px; color: #ee0a24; }
      .price { font-size: 18px; font-weight: bold; color: #ee0a24; margin-right: 6px;}
      .orig { font-size: 12px; color: #999; text-decoration: line-through; }
    }
  }
  .cell-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    font-size: 14px;
  }
}

// 价格明细样式
.price-detail {
  .section-title {
    font-size: 15px;
    font-weight: bold;
    margin-bottom: 12px;
  }
  .cell-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-size: 14px;
    color: #333;
    
    &.clickable:active { opacity: 0.7; }

    .value.red { color: #ee0a24; }
    .value-right {
      display: flex;
      align-items: center;
      .coupon-text { color: #ee0a24; font-weight: 500; }
      .coupon-hint { color: #ff976a; background: #fff3e9; padding: 2px 6px; font-size: 12px; border-radius: 4px;}
      .gray { color: #999; }
      .arrow-icon { color: #ccc; margin-left: 4px; }
    }
  }
}

// 支付方式样式
.pay-icon {
  margin-right: 8px;
  margin-top: 2px;
}

// 底部栏文字
.promo-text {
  font-size: 12px;
  color: #ee0a24;
  margin-right: 10px;
}

// 优惠券弹窗样式
.coupon-popup-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  
  .popup-header {
    text-align: center;
    padding: 16px;
    font-weight: bold;
    font-size: 16px;
    background: #fff;
  }
  
  .coupon-list {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    
    .coupon-item {
      display: flex;
      align-items: center;
      background: #fff;
      margin-bottom: 10px;
      padding: 16px;
      border-radius: 8px;
      position: relative;
      overflow: hidden;
      
      &.active { border: 1px solid #ee0a24; background: #fffbfb; }
      
      .left {
        width: 80px;
        color: #ee0a24;
        text-align: center;
        .symbol { font-size: 14px; }
        .amount { font-size: 24px; font-weight: bold; }
      }
      .mid {
        flex: 1;
        .name { font-size: 15px; font-weight: bold; margin-bottom: 4px; }
        .rule { font-size: 12px; color: #999; }
      }
      .right {
        width: 30px;
        display: flex;
        justify-content: flex-end;
        .circle {
          width: 16px; height: 16px; 
          border: 1px solid #ccc; 
          border-radius: 50%;
        }
      }
    }
    
    .no-use-btn {
      text-align: center;
      padding: 12px;
      background: #fff;
      border-radius: 8px;
      color: #666;
      font-size: 14px;
      margin-top: 20px;
    }
  }
}
</style>