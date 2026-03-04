<template>
  <div class="shop-item" @click="onClick">
    <div class="shop-cover">
      <van-image 
        :src="item.coverImages" 
        fit="cover" 
        radius="6px" 
        class="img"
      >
        <template v-slot:error>
          <van-icon name="photo-fail" color="#dcdee0" size="20" />
        </template>
      </van-image>
      
      <div v-if="item.base_tags" class="cover-badge">
        {{ item.base_tags }}
      </div>
    </div>

    <div class="shop-info">
      <h3 class="shop-name van-ellipsis">{{ item.name }}</h3>

      <div class="row-meta">
        <div class="meta-left">
          <span class="score-num">{{ item.rating }}分</span>
          <span class="score-star">
            <van-icon name="star" color="#ff9900" size="10" />
          </span>
          <span class="divider">|</span>
          <span class="reviews">{{ item.totalReviews }}条评价</span>
        </div>
        <span class="distance">{{ item.distance }}</span>
      </div>

      <div class="row-location van-ellipsis">
        <span>{{ item.shopType }}</span>
        <span class="address-text">{{ item.address }}</span>
        <span class="avg-price">¥{{ item.avgPrice }}/人</span>
      </div>

      <div class="row-tags">
        <span v-if="item.businessHours" class="info-tag time-tag">
          {{ item.businessHours }}
        </span>
        
        <span 
          v-for="(tag, index) in item.tagShop" 
          :key="index" 
          class="info-tag"
        >
          {{ tag }}
        </span>
      </div>
      
      <div class="row-bottom" v-if="item.priceDes">
        <span class="price-highlight">{{ item.priceDes }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue';
import type { ShopDTO } from '@/api/search/type';

// 接收父组件传来的数据
const props = defineProps({
  item: {
    type: Object as PropType<ShopDTO>,
    required: true
  }
});

const emit = defineEmits(['click']);
const onClick = () => emit('click', props.item.id);
</script>

<style scoped lang="scss">
.shop-item {
  display: flex;
  padding: 12px;
  background: #fff;
  /* 增加一点按压效果 */
  &:active {
    background-color: #f9f9f9;
  }
  
  .shop-cover {
    position: relative;
    width: 90px;
    height: 90px;
    margin-right: 12px;
    flex-shrink: 0;
    
    .img {
      width: 100%;
      height: 100%;
      display: block;
      border: 1px solid #f2f3f5; /* 淡淡的边框防止白图撞色 */
    }
    
    .cover-badge {
      position: absolute;
      top: 0;
      left: 0;
      background: linear-gradient(135deg, #ff6034, #ee0a24);
      color: #fff;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 6px 0 6px 0;
      z-index: 1;
    }
  }

  .shop-info {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    justify-content: space-around; /* 内容垂直均分 */

    .shop-name {
      margin: 0;
      font-size: 16px;
      color: #222;
      font-weight: 600;
      line-height: 1.2;
    }

    .row-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      
      .meta-left {
        display: flex;
        align-items: center;
      }
      
      .score-num {
        color: #ff9900;
        font-weight: bold;
        font-size: 13px;
        margin-right: 2px;
      }
      
      .divider {
        margin: 0 6px;
        color: #ddd;
        transform: scale(0.8);
      }
      
      .reviews {
        color: #666;
      }
      
      .distance {
        color: #999;
        font-family: Arial, sans-serif; /* 数字用Arial更好看 */
      }
    }

    .row-location {
      font-size: 12px;
      color: #666;
      display: flex;
      align-items: center;
      
      .address-text {
        margin: 0 6px;
        padding: 0 6px;
        border-left: 1px solid #eee;
        border-right: 1px solid #eee;
        max-width: 100px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      
      .avg-price {
        color: #333;
      }
    }

    .row-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      
      .info-tag {
        font-size: 10px;
        color: #666;
        background: #f5f6f7;
        padding: 2px 5px;
        border-radius: 3px;
      }
      
      .time-tag {
        color: #1989fa;
        background: rgba(25, 137, 250, 0.1);
      }
    }
    
    .price-highlight {
      font-size: 14px;
      color: #ff4d4f;
      font-weight: bold;
      
      &::before {
        content: '¥';
        font-size: 10px;
        margin-right: 1px;
      }
    }
  }
}
</style>