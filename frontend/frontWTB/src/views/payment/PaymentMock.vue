<template>
  <div class="mock-pay-page">
    <van-nav-bar title="收银台" left-arrow @click-left="onCancel" />
    
    <div class="pay-info">
      <div class="shop-name">小铁台球商户平台</div>
      <div class="amount">¥ {{ amount }}</div>
    </div>

    <div class="action-area">
      <van-button 
        type="primary" 
        block 
        size="large" 
        color="#07c160" 
        @click="onConfirm"
      >
        确认支付
      </van-button>
      
      <div class="spacer"></div>
      
      <van-button 
        plain 
        block 
        size="large" 
        type="default" 
        @click="onCancel"
      >
        取消支付
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showSuccessToast, showFailToast,showLoadingToast } from 'vant';
import { confirmPay,cancelPay } from '@/api/shop/index'


const route = useRoute();
const router = useRouter();


// 从路由 query 中获取金额和订单号
const amount = ref(route.query.amount || '0.00');
const orderNo = ref(route.query.orderNo as string || '');

const onConfirm = async () => {
  if (!orderNo.value) {
    showFailToast('订单号缺失');
    return;
  }

  const toast = showLoadingToast({ message: '支付中...', forbidClick: true });

  try {
    const res = await confirmPay(orderNo.value);
    if (res.success && res.data) {
      toast.close();
      showSuccessToast('支付成功');
      
      // 跳转到支付成功页，并把结果传过去
      // 使用 query 传参比较简单直接
      router.push({
        path: '/payment/success',
        query: {
          orderNo: res.data.orderNo,
          amount: res.data.payAmount,
          title: res.data.dealTitle
        }
      });
    } else {
      showFailToast(res.errorMsg || '支付确认失败');
    }
  } catch (error) {
    showFailToast('网络异常');
  }
};


const onCancel = async () => {
  if (!orderNo.value) {
    router.back();
    return;
  }

  const toast = showLoadingToast({ message: '取消中...', forbidClick: true });

  try {
    const res = await cancelPay(orderNo.value);
    if (res.success) {
      toast.close();
      showSuccessToast('支付已取消');
      // 跳转到订单详情页
      router.replace({
        path: '/order/detail', // 假设你的详情页路由是这个
        query: { orderNo: orderNo.value }
      });
    } else {
      showFailToast(res.errorMsg || '取消失败');
    }
  } catch (error) {
    showFailToast('网络异常');
  }
};
</script>

<style scoped lang="less">
.mock-pay-page {
  height: 100vh;
  background: #f2f2f2;
  display: flex;
  flex-direction: column;
}

.pay-info {
  background: #fff;
  padding: 40px 0;
  text-align: center;
  margin-bottom: 20px;
  
  .shop-name {
    font-size: 14px;
    color: #666;
    margin-bottom: 10px;
  }
  .amount {
    font-size: 36px;
    font-weight: bold;
    color: #333;
  }
}

.action-area {
  padding: 20px;
  .spacer { height: 16px; }
}
</style>