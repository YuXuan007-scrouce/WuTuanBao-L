<template>
  <div class="order-detail-page">
   

    <div class="header-status">
      <div class="status-title">
        <span class="back-icon" @click="onBack"><van-icon name="arrow-left" /></span>
        <span v-if="orderData?.status === 0">待支付，剩余 <van-count-down :time="countdownTime" format="mm:ss" @finish="onCountdownFinish" class="countdown-text"/></span>
        <span v-else-if="orderData?.status === 2">已完成</span>
        <span v-else-if="orderData?.status === 3">已取消</span>
        <span v-else-if="orderData?.status === 4">已退款</span>
      </div>
      <div v-if="orderData?.status === 0" class="status-sub">超时未支付，订单将自动取消</div>
      <div class="cs-icon">
        <van-icon name="service-o" size="20" />
        <span>客服</span>
      </div>
    </div>

    <div class="scroll-content" v-if="orderData">
      <div class="card deal-card">
        <div class="deal-header">
          <img :src="resolveAssetUrl(orderData.dealImage)" class="deal-img" alt="团购图" />
          <div class="deal-info">
            <div class="title">{{ orderData.dealTitle }} <van-icon name="arrow" color="#999" /></div>
            <div class="price-info">
              <div class="current-price">优惠后<span class="symbol">¥</span><span class="num">{{ orderData.payAmount }}</span></div>
              <div class="original-price">¥{{ orderData.originalPrice }}</div>
              <div class="qty">x{{ orderData.quantity }}</div>
            </div>
          </div>
        </div>
        <div class="tags-row">
          <span v-for="(tag, index) in tagList" :key="index" class="tag-item">{{ tag }}</span>
        </div>
        <div class="rules-row">
          <div class="rule-left">使用须知 <van-icon name="arrow" /></div>
        </div>
        <div class="price-breakdown">
          <div class="row"><span>参考价</span><span>¥{{ orderData.originalPrice }}</span></div>
          <div class="row"><span>团购优惠</span><span class="discount">-¥{{ (orderData.originalPrice - orderData.dealPrice).toFixed(2) }}</span></div>
        </div>
      </div>

      <div class="card merchant-card">
        <div class="card-title">适用门店</div>
        <div class="merchant-info">
          <img :src="resolveAssetUrl(orderData.coverImage)" class="merchant-img" alt="门店图" />
          <div class="info-main">
            <div class="m-name">{{ orderData.merchantName }}</div>
            <div class="m-hours">营业中 {{ orderData.businessHours }}</div>
            <div class="m-address">
              <span class="distance">13.5km</span> {{ orderData.merchantAddress }}
            </div>
          </div>
          <div class="actions">
            <div class="icon-circle" @click="openMap"><van-icon name="location-o" /></div>
            <div class="icon-circle" @click="callPhone"><van-icon name="phone-o" /></div>
          </div>
        </div>
      </div>

      <div class="card order-info-card">
        <div class="row">
          <span>订单详情</span>
          <span class="order-no-text">订单号:{{ orderData.orderNo }} <span class="copy-btn" @click="copyOrderNo">复制</span> <van-icon name="arrow" /></span>
        </div>
      </div>
    </div>

    <div class="bottom-bar" v-if="orderData">
      <template v-if="orderData.status === 0">
        <div class="left-price">
          <span class="text">实付</span>
          <span class="symbol">¥</span>
          <span class="num">{{ orderData.payAmount }}</span>
          <div class="detail-toggle" @click="showFeeDetail = true">
            共优惠¥{{ (orderData.originalPrice - orderData.dealPrice).toFixed(2) }} 明细<van-icon name="arrow-up" />
          </div>
        </div>
        <van-button type="danger" round class="action-btn pay-btn" @click="goPay">继续支付</van-button>
      </template>

      <template v-else-if="orderData.status === 2">
        <div class="btn-group right-align">
          <van-button plain round class="action-btn">申请售后</van-button>
          <van-button plain round class="action-btn">再来一单</van-button>
          <van-button type="danger" round class="action-btn">去评价</van-button>
        </div>
      </template>

      <template v-else-if="orderData.status === 3">
        <div class="btn-group right-align">
          <van-button type="danger" round class="action-btn">再来一单</van-button>
        </div>
      </template>
    </div>

    <van-popup v-model:show="showFeeDetail" position="bottom" round class="fee-detail-popup">
      <div class="popup-header">
        费用明细
        <van-icon name="cross" class="close-icon" @click="showFeeDetail = false" />
      </div>
      <div class="popup-content" v-if="orderData">
        <div class="fee-section">
          <div class="row title-row"><span>商品</span><span>¥{{ orderData.originalPrice }}</span></div>
          <div class="row sub-row"><span>参考价</span><span>{{ orderData.quantity }}件 x ¥{{ orderData.originalPrice }}</span></div>
        </div>
        <div class="fee-section">
          <div class="row title-row"><span>优惠</span><span class="discount">-¥{{ (orderData.originalPrice - orderData.dealPrice).toFixed(2) }}</span></div>
          <div class="row sub-row"><span>团购优惠</span><span class="discount">{{ orderData.quantity }}件 x -¥{{ (orderData.originalPrice - orderData.dealPrice).toFixed(2) }}</span></div>
        </div>
        <div class="fee-total">
          <span>实付</span>
          <span class="total-price">¥{{ orderData.payAmount }}</span>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast, showLoadingToast } from 'vant';
import useClipboard from 'vue-clipboard3'; // 需要安装：npm install vue-clipboard3
import { getOrderDetail,repay } from '@/api/shop/index'; // 引入上面写的接口
import { OrderDetailData } from '@/api/shop/type'
import { resolveAssetUrl } from '@/utils/asset'

const route = useRoute();
const router = useRouter();
const { toClipboard } = useClipboard();

const orderNo = ref(route.query.orderNo as string || '');
const orderData = ref<OrderDetailData | null>(null);

// 底部明细弹窗控制
const showFeeDetail = ref(false);

// 倒计时时间 (假设订单保留1小时)
const countdownTime = ref(0);

// 获取订单数据
const fetchOrderDetail = async () => {
  if (!orderNo.value) return;
  const toast = showLoadingToast({ message: '加载中...', forbidClick: true });
  try {
    const res = await getOrderDetail(orderNo.value);
    console.log('接口返回原始数据:', res); // <--- 添加这行调试
    if (res.success && res.data) {
      orderData.value = res.data;
      
      // 处理待支付倒计时 (创建时间 + 30分钟 - 当前时间)
      if (res.data.status === 0) {
        const createTimeMs = new Date(res.data.createTime).getTime();
        const expireTimeMs = createTimeMs + 60 * 60 * 1000; 
        const nowMs = new Date().getTime();
        const remainMs = expireTimeMs - nowMs;
        countdownTime.value = remainMs > 0 ? remainMs : 0;
      }
    } else {
      showToast(res.errorMsg || '获取详情失败');
    }
  } catch (error) {
    showToast('网络异常');
  } finally {
    toast.close();
  }
};

// 计算属性：处理逗号分隔的标签
const tagList = computed(() => {
  return orderData.value?.tags ? orderData.value.tags.split(',') : [];
});


// 事件处理函数
const onBack = () => {
  router.back();
};

const onCountdownFinish = () => {
  // 倒计时结束，可以重新刷新详情接口，或者前端直接把状态置为已取消
  if (orderData.value) {
    orderData.value.status = 3; 
  }
};

const copyOrderNo = async () => {
  try {
    await toClipboard(orderData.value?.orderNo || '');
    showToast('复制成功');
  } catch (e) {
    showToast('复制失败');
  }
};

const openMap = () => {
  showToast('跳转地图导航');
  // 实际开发：调用微信或H5的地图接口，传入 orderData.value.longitude / latitude
};

const callPhone = () => {
  if (orderData.value?.merchantPhone) {
    window.location.href = `tel:${orderData.value.merchantPhone}`;
  }
};

/**
 * 继续支付逻辑
 */
const goPay = async () => {
  if (!orderNo.value) return;

  const toast = showLoadingToast({
    message: '正在唤起支付...',
    forbidClick: true,
  });

  try {
    // 1. 发起请求，让后端更新订单状态为“支付中”
    const res = await repay(orderNo.value);

    if (res.success) {
      // 2. 接口成功后，跳转回模拟支付收银台
      // 注意：这里使用后端返回的 res.data (即订单号) 更加严谨
      router.push({
        path: '/payment/mock',
        query: { 
          orderNo: String(res.data), 
          amount: orderData.value?.payAmount 
        }
      });
    } else {
      showToast(res.errorMsg || '无法继续支付');
    }
  } catch (error) {
    console.error('继续支付请求失败', error);
    showToast('网络请求异常');
  } finally {
    toast.close();
  }
};

onMounted(() => {
  fetchOrderDetail();
});
</script>

<style scoped lang="scss">
/* 页面整体容器：实现上下固定，中间滚动 */
.order-detail-page {
  display: flex;
  flex-direction: column;
  height: 100%; 
  min-height: 100dvh;
  width: 100%;        /* 宽度撑满 */
  background-color: #f5f5f5;
  overflow: hidden;

  /* 重写顶部导航栏颜色 */
  :deep(.van-nav-bar) {
    background-color: #f5f5f5;
  }
  :deep(.van-nav-bar::after) {
    border-bottom: none;
  }
}

/* 顶部状态区 */
.header-status {
  padding: 10px 20px 20px;
  background-color: #f5f5f5;
  position: relative;

  .status-title {
    font-size: 22px;
    font-weight: bold;
    display: flex;
    align-items: center;
    .back-icon {
      margin-right: 10px;
      font-size: 20px;
    }
    .countdown-text {
      display: inline-block;
      color: #ee0a24;
      font-size: 22px;
      font-weight: bold;
    }
  }
  .status-sub {
    font-size: 13px;
    color: #999;
    margin-top: 5px;
    margin-left: 30px;
  }
  .cs-icon {
    position: absolute;
    right: 20px;
    top: 15px;
    display: flex;
    flex-direction: column;
    align-items: center;
    font-size: 12px;
    color: #666;
  }
}

/* 中间滚动区 */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 20px;
  /* 隐藏滚动条 */
  -webkit-overflow-scrolling: touch; /* 优化 iOS 滚动顺滑度 */
}

/* 通用卡片样式 */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

/* 团购商品卡片 */
.deal-card {
  .deal-header {
    display: flex;
    .deal-img {
      width: 60px;
      height: 60px;
      border-radius: 6px;
      object-fit: cover;
      margin-right: 12px;
    }
    .deal-info {
      flex: 1;
      .title {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 8px;
        display: flex;
        justify-content: space-between;
      }
      .price-info {
        display: flex;
        align-items: baseline;
        position: relative;
        .current-price {
          color: #333;
          font-size: 12px;
          margin-right: 6px;
          .symbol { font-size: 14px; font-weight: bold; }
          .num { font-size: 20px; font-weight: bold; }
        }
        .original-price {
          color: #999;
          font-size: 12px;
          text-decoration: line-through;
        }
        .qty {
          position: absolute;
          right: 0;
          color: #999;
          font-size: 14px;
        }
      }
    }
  }
  .tags-row {
    margin-top: 12px;
    font-size: 12px;
    color: #666;
    .tag-item {
      margin-right: 10px;
      position: relative;
      &::after {
        content: "·";
        position: absolute;
        right: -7px;
      }
      &:last-child::after { content: ""; }
    }
  }
  .rules-row {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    font-size: 12px;
    color: #999;
    padding-bottom: 12px;
    border-bottom: 1px dashed #eee;
  }
  .price-breakdown {
    margin-top: 12px;
    font-size: 14px;
    .row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      .discount { color: #ee0a24; }
    }
  }
}

/* 适用门店卡片 */
.merchant-card {
  .card-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 12px;
  }
  .merchant-info {
    display: flex;
    align-items: center;
    .merchant-img {
      width: 50px;
      height: 50px;
      border-radius: 6px;
      object-fit: cover;
      margin-right: 12px;
    }
    .info-main {
      flex: 1;
      overflow: hidden;
      .m-name {
        font-size: 15px;
        font-weight: bold;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .m-hours {
        font-size: 12px;
        color: #999;
        margin: 4px 0;
      }
      .m-address {
        font-size: 12px;
        color: #666;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        .distance { color: #ee0a24; margin-right: 4px; }
      }
    }
    .actions {
      display: flex;
      gap: 10px;
      margin-left: 10px;
      .icon-circle {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #f7f8fa;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 18px;
        color: #333;
      }
    }
  }
}

/* 订单信息卡片 */
.order-info-card {
  font-size: 14px;
  .row {
    display: flex;
    justify-content: space-between;
    color: #666;
    .order-no-text {
      display: flex;
      align-items: center;
      color: #999;
      font-size: 13px;
      .copy-btn {
        margin-left: 6px;
        margin-right: 4px;
        color: #666;
      }
    }
  }
}

/* 底部操作栏 */
.bottom-bar {
    margin-top: auto; /* 关键：即使内容不足，也将自己推向底部 */
  flex-shrink: 0;     /* 防止底部工具栏被内容挤压变形 */
  background: #fff;
  padding: 10px 16px;
 /* 确保在所有设备上都有基础间距 */
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 10;        /* 确保层级在滚动内容之上 */


  .left-price {
    .text { font-size: 14px; font-weight: bold; }
    .symbol { font-size: 14px; color: #ee0a24; font-weight: bold; margin-left: 4px;}
    .num { font-size: 24px; color: #ee0a24; font-weight: bold; }
    .detail-toggle {
      font-size: 12px;
      color: #999;
      display: flex;
      align-items: center;
      margin-top: 2px;
    }
  }

  .btn-group {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    &.right-align {
      justify-content: flex-end;
      width: 100%;
    }
  }

  .action-btn {
    height: 36px;
    padding: 0 20px;
  }
  .pay-btn {
    height: 40px;
    padding: 0 24px;
    font-size: 16px;
  }
}

/* 费用明细弹窗 */
.fee-detail-popup {
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  .popup-header {
    text-align: center;
    font-size: 16px;
    font-weight: bold;
    padding: 16px;
    position: relative;
    border-bottom: 1px solid #f5f5f5;
    .close-icon {
      position: absolute;
      right: 16px;
      top: 18px;
      font-size: 20px;
      color: #999;
    }
  }
  .popup-content {
    padding: 20px;
    .fee-section {
      margin-bottom: 24px;
      .row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
      }
      .title-row {
        font-size: 15px;
        font-weight: bold;
        .discount { color: #ee0a24; }
      }
      .sub-row {
        font-size: 13px;
        color: #999;
        .discount { color: #ee0a24; }
      }
    }
    .fee-total {
      display: flex;
      justify-content: flex-end;
      align-items: baseline;
      font-size: 15px;
      padding-top: 16px;
      border-top: 1px dashed #eee;
      .total-price {
        font-size: 22px;
        font-weight: bold;
        margin-left: 10px;
      }
    }
  }
}
</style>