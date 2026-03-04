<template>
  <div class="group-deal-item">
    <van-image 
      :src="resolveAssetUrl(deal.images)" 
      radius="4" 
      fit="cover" 
      class="deal-img" 
    />
    
    <div class="deal-content">
      <div class="deal-title van-multi-ellipsis--l2">{{ deal.title }}</div>
      <div class="deal-tags">
        <span class="time-tag">{{ deal.validTimeDesc }}</span>
        <span class="sold-count">已售{{ deal.soldCount }}</span>
      </div>
      
      <div class="deal-bottom">
        <div class="price-box">
          <span class="symbol">¥</span>
          <span class="price">{{ deal.dealPrice }}</span>
          <span class="original">¥{{ deal.originalPrice }}</span>
          <span class="discount-tag">{{ deal.discount }}折</span>
        </div>
        <van-button size="small" type="danger" round class="buy-btn" @click.stop="handleBuy">抢购</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue';
import type { GroupDealDTO } from '@/api/shop/type';
import { resolveAssetUrl } from '@/utils/asset'; // 确保路径正确
import { useRouter } from 'vue-router';

const router = useRouter();

const props = defineProps({
  deal: {
    type: Object as PropType<GroupDealDTO>,
    required: true
  }
});

// 处理抢购点击
const handleBuy = () => {
  // 路由跳转，携带团购商品 ID
  router.push({
    name: 'Payment', // 确保你的路由配置里 name 叫 'Payment'
    params: {
      id: props.deal.id
    }
  });
};

</script>

<style scoped lang="scss">
/* 保持原样式不变，省略以节省篇幅 */
.group-deal-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f5f6f7;
  background: #fff;
  /* ... */
  .deal-img { width: 80px; height: 80px; margin-right: 10px; flex-shrink: 0; }
  .deal-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
  .deal-bottom { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 6px; }
  .price-box { color: #ff4d4f; .price { font-size: 18px; font-weight: bold; } .original { color: #999; text-decoration: line-through; font-size: 12px; margin: 0 4px; } }
}
</style>