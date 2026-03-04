<template>
  <div class="pay-success-page">
    <van-nav-bar
      right-text="完成"
      @click-right="onFinished"
      :border="false"
      fixed
      placeholder
    />

    <div class="success-content">
      <div class="status-header">
        <van-icon name="checked" color="#07c160" size="60" />
        <div class="status-text">购买成功</div>
        <div class="savings-text">
          团购频道·超值团提示你已支付 ¥{{ payAmount }}
        </div>
      </div>

      <div class="action-btn-row">
        <van-button 
          plain 
          round 
          size="small" 
          class="view-order-btn" 
          @click="toOrderDetail"
        >
          查看订单
        </van-button>
      </div>

      <div class="coupon-card">
        <div class="deal-name">{{ title }}</div>
        <div class="exp-date">请在 2026.05.07(含) 前到店消费</div>
        
        <div class="qrcode-box">
          <div class="qr-wrapper">
            <qrcode-vue 
              :value="orderNo" 
              :size="160" 
              level="H" 
              render-as="svg"
              background="#ffffff"
              foreground="#000000"
            />
          </div>
          <div class="coupon-no">
            券号 {{ formatOrderNo(orderNo) }}
            <span class="copy-tag" @click="onCopy">复制</span>
          </div>
        </div>
      </div>

      <div class="bottom-tips">
        温馨提示：为了您的权益，请在到店消费时出示此券码。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showToast } from 'vant';
// 确保你已经运行了 npm install qrcode.vue
import QrcodeVue from 'qrcode.vue';

const route = useRoute();
const router = useRouter();

// --- 响应式数据 ---
// 从路由 query 获取支付确认后传过来的参数
const orderNo = computed(() => (route.query.orderNo as string) || '202600000000000000');
const title = computed(() => (route.query.title as string) || '团购商品');
const payAmount = computed(() => (route.query.amount as string) || '0.00');


// --- 方法 ---

// 格式化订单号：每 4 位加一个空格，方便用户阅读
const formatOrderNo = (no: string) => {
  return no.replace(/(.{4})/g, '$1 ').trim();
};

// 点击完成：回退到商家详情页
const onFinished = () => {
  // 假设你的商家详情页路由 name 为 'MerchantDetail'
 router.go(-3)
}

// 查看订单：跳转到空白占位页
const toOrderDetail = () => {
  router.push('/order/detail/blank');
};

// 复制券号
const onCopy = () => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(orderNo.value).then(() => {
      showToast('券号已复制');
    });
  } else {
    // 兼容性处理
    const input = document.createElement('input');
    input.setAttribute('value', orderNo.value);
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    showToast('券号已复制');
  }
};
</script>

<style lang="less" scoped>
.pay-success-page {
  min-height: 100vh;
  background-color: #ffffff;

  .success-content {
    padding: 30px 20px;
    text-align: center;

    .status-header {
      margin-bottom: 24px;
      .status-text {
        font-size: 22px;
        font-weight: 600;
        color: #333;
        margin-top: 12px;
      }
      .savings-text {
        font-size: 14px;
        color: #ff6034;
        margin-top: 8px;
        background: #fff5f2;
        display: inline-block;
        padding: 4px 12px;
        border-radius: 12px;
      }
    }

    .action-btn-row {
      margin-bottom: 40px;
      .view-order-btn {
        padding: 0 24px;
        height: 32px;
        color: #666;
        border: 1px solid #dcdfe6;
      }
    }

    .coupon-card {
      background: #ffffff;
      border-top: 1px solid #f2f3f5;
      padding-top: 30px;

      .deal-name {
        font-size: 17px;
        font-weight: bold;
        color: #333;
        padding: 0 10px;
      }

      .exp-date {
        font-size: 13px;
        color: #999;
        margin-top: 8px;
      }

      .qrcode-box {
        margin-top: 30px;
        
        .qr-wrapper {
          width: 180px;
          height: 180px;
          margin: 0 auto;
          padding: 10px;
          background: #fff;
          border: 1px solid #f0f0f0;
          box-shadow: 0 4px 12px rgba(0,0,0,0.05);
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .coupon-no {
          margin-top: 20px;
          font-size: 16px;
          color: #333;
          letter-spacing: 0.5px;
          
          .copy-tag {
            color: #1989fa;
            font-size: 14px;
            margin-left: 10px;
            cursor: pointer;
            &:active {
              opacity: 0.7;
            }
          }
        }
      }
    }

    .bottom-tips {
      margin-top: 50px;
      font-size: 12px;
      color: #bbb;
    }
  }
}
</style>