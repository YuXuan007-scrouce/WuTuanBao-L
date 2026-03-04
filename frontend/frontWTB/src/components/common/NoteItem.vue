<template>
  <div class="note-item" @click="goDetail">
    <!-- 封面 -->
   
    <img class="cover" :src="coverUrl" alt="封面" />

    <!-- 右侧内容 -->
    <div class="content">
      <div class="title">{{ note.title }}</div>
      <div class="desc">{{ shortContent }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { UserNote } from '@/api/user/type';

const FILE_BASE_URL = import.meta.env.VITE_FILE_BASE_URL;
const myNote = 'public/myNote.jpg';

const FILE_MINIO_BASE_URL = import.meta.env.VITE_MINIO_BASE_URL;


const props = defineProps<{
  note: UserNote;
}>();

const router = useRouter();

/** 封面图 */
const coverUrl = computed(() => {    
  if (!props.note.images) return myNote;

  const first = props.note.images.split(',')[0];
  //检查这个 URL 是否是完整的网络地址。传的是Minio的objectKey
  return first.startsWith('http')
    ? first
    : FILE_MINIO_BASE_URL + first;
})

/** 内容截断 */
const shortContent = computed(() => {
  const content = props.note.content || '';
  return content.length > 50
    ? content.slice(0, 50) + '...'
    : content;
});

/** "我的"跳转笔记详情(首页设置复用) */
const goDetail = () => {
  router.push({
  name: 'NoteDetail',
  params: {
    blogId: props.note.id   // 笔记作品 id
  }
});
}
</script>

<style scoped>
.note-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.cover {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.content {
  flex: 1;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.desc {
  font-size: 13px;
  color: #666;
}
</style>
