<template>
  <div class="review-item">
    <van-image :src="resolveAssetUrl(comment.userIcon)" round class="avatar" />
    
    <div class="review-content">
      <div class="review-header">
        <span class="name">{{ comment.nickName }}</span>
        <span class="time">{{ formatTime(comment.createTime) }}</span>
      </div>
      
      <div class="rating-row">
        <van-rate v-model="comment.rating" readonly size="10" color="#ff4d4f" allow-half />
        <span class="tag-text">{{ getRatingTag(comment.rating) }}</span>
      </div>
      
      <div class="text-content van-multi-ellipsis--l3">
        {{ comment.content }}
      </div>
      
      </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue';
import type { CommentDTO } from '@/api/shop/type';
import { resolveAssetUrl } from '@/utils/asset';

defineProps({
  comment: {
    type: Object as PropType<CommentDTO>,
    required: true
  }
});

// 简单的评分文案转换
const getRatingTag = (score: number) => {
  if (score >= 4.5) return '超赞';
  if (score >= 4.0) return '满意';
  return '一般';
};

// 简单的时间格式化
const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
};
</script>

<style scoped lang="scss">
/* 保持原样式 */
.review-item { display: flex; padding: 16px 0; border-bottom: 1px solid #f9f9f9; 
  .avatar { width: 36px; 
    height: 36px; 
    margin-right: 10px; 
    flex-shrink: 0; 
  }
  .review-content { flex: 1; 
    .review-header { 
      display: flex; 
      justify-content: space-between; 
      margin-bottom: 4px; font-size: 13px; 
      color: #333; 
      .time { color: #999; font-size: 11px; } 
    }
    .rating-row { 
      display: flex; 
      align-items: center; 
      margin-bottom: 8px; 
      font-size: 11px;
       .tag-text {
         margin-left: 8px; 
         font-weight: bold; 
        } 
      }
    .text-content { font-size: 13px; color: #333; line-height: 1.5; }
  }
}
</style>