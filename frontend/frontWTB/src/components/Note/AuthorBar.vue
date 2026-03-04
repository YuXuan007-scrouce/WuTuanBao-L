<template>
  <div class="author-bar">
    <div class="left">
      <!-- 返回按钮 -->
      <span class="back-btn" @click="handleBack">&lt;</span>
      <van-image
        round
        width="40"
        height="40"
        :src="props.author.icon"
      />
      <span class="name">{{ props.author.nickName }}</span>
    </div>

    <van-button
      size="small"
      :type="props.author.followed ? 'default' : 'primary'"
      @click="toggleFollow"
    >
      {{ props.author.followed ? '已关注' : '关注' }}
    </van-button>
  </div>
</template>

<script setup lang="ts">
import type { Author } from '@/api/note/type'
import { resolveAssetUrl } from '@/utils/asset';

const props = defineProps<{
  author: Author
}>()


const emit = defineEmits(['toggleFollow', 'back'])

const toggleFollow = () => {
  emit('toggleFollow')
}

const handleBack = () => {
  emit('back')
}

</script>

<style scoped>
.author-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 10vh;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #fff;
  z-index: 10;
  border-bottom: 1px solid #eee;
}

.left {
  display: flex;
  align-items: center;
}

.name {
  margin-left: 8px;
  font-size: 14px;
  font-weight: 500;
}
.back-btn {
  font-size: 22px;
  margin-right: 8px;
  cursor: pointer;
  line-height: 1;
}
</style>
