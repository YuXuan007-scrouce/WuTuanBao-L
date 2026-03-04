<template>
  <div class="bottom-bar" :style="{ bottom: offset + 'px' }">
 
    <van-field
      ref="inputRef"
      v-model="content"
      :placeholder="placeholder"
      class="input"
      @focus="onFocus"
      @blur="onBlur"
      @keyup.enter="send"
    />

    <div class="action" @click="toggleLike">
  <van-icon :name="likeInfo.isliked ? 'like' : 'like-o'" />
  <span class="count">{{ likeInfo.liked }}</span>
</div>

<div class="action" @click="toggleCollect">
  <van-icon :name="likeInfo.iscollected ? 'star' : 'star-o'" />
  <span class="count">{{ likeInfo.collection }}</span>
</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { LikeCollectInfo, ReplyTo } from '@/api/note/type'

const props = defineProps<{
  likeInfo: LikeCollectInfo
  replyTo?: ReplyTo | null
}>()

const emit = defineEmits<{
  (e: 'sendComment', content: string): void
  (e: 'toggleLike'): void
  (e: 'toggleCollect'): void
}>()

const content = ref('')
const inputRef = ref()
const offset = ref(0)

const placeholder = computed(() =>
  props.replyTo ? `回复 @${props.replyTo.replyToUserName}` : '说点什么...'
)

watch(
  () => props.replyTo,
  (val) => {
    if (val) {
      inputRef.value?.focus()
      content.value = `@${val.replyToUserName} `
    }
  }
)

/** 键盘适配（核心） */
const handleResize = () => {
  if (!window.visualViewport) return
  const vh = window.innerHeight
  const vv = window.visualViewport.height
  offset.value = vh - vv
}

const onFocus = () => handleResize()
const onBlur = () => (offset.value = 0)

onMounted(() => {
  window.visualViewport?.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.visualViewport?.removeEventListener('resize', handleResize)
})

const send = () => {
  if (!content.value.trim()) return
  emit('sendComment', content.value)
  content.value = ''
}

defineOptions({
  inheritAttrs: false // 防止 Vue 把父组件 class 自动添加到 fragment 根节点
})

const toggleLike = () => emit('toggleLike')
const toggleCollect = () => emit('toggleCollect')
</script>
<style lang="css">
  .bottom-bar {
  position: fixed;          /* ⭐ 必须 */
  left: 0;
  right: 0;
  bottom: 0;                /* 默认贴底 */
  z-index: 1000;

  display: flex;
  align-items: center;
  padding: 8px 12px;

  background: #fff;
  border-top: 1px solid #eee;
}

</style>
