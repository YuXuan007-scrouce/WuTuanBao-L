<template>
  <div class="feed-card" @click="goDetail">
    <!-- 封面区域（4/5） -->
    <div class="cover-box">
      <img class="cover" :src="cover" />
    </div>

    <!-- 信息区域（1/5） -->
    <div class="info-box">
      <!-- 标题 -->
      <div class="title van-ellipsis">
        {{ feed.title }}
      </div>

      <!-- 作者 + 点赞 -->
      <div class="row">
        <div class="author">
          <img class="avatar" :src="avatar" />
          <span class="name">{{ feed.authorName }}</span>
        </div>

        <div
          class="like"
          :class="{ active: feed.isLike }"
          @click.stop="onLike"
        >
          <van-icon
            :name="feed.isLike ? 'like' : 'like-o'"
            size="18"
          />
          <span class="count">{{ feed.liked }}</span>
        </div>
      </div>

      <!-- 时间 -->
      <div class="time">
        {{ feed.createTime }}
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { resolveAssetUrl } from '@/utils/asset'
import type { FollowFeedItem } from '@/api/follow/type'

const props = defineProps<{
  feed: FollowFeedItem
}>()

const emit = defineEmits<{
  (e: 'like', blogId: number): void
}>()

const router = useRouter()

const cover = computed(() =>
  props.feed.images
    ? resolveAssetUrl(props.feed.images.split(',')[0])
    : ''
)

const avatar = computed(() =>
  resolveAssetUrl(props.feed.authorAvatar || '/userIcon.png')
)

const onLike = () => {
  emit('like', props.feed.blogId)
}

const goDetail = () => {
  router.push({
    name: 'NoteDetail',
    params: { blogId: props.feed.blogId }
  })
}
</script>


<style>
.feed-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* ================= 封面（4/5） ================= */
.cover-box {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 5; /* 核心：4/5 */
  background: #f7f8fa;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ================= 信息区（1/5） ================= */
.info-box {
  padding: 10px 12px;
}

/* 标题 */
.title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

/* 作者 + 点赞 行 */
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 作者 */
.author {
  display: flex;
  align-items: center;
}

.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 6px;
}

.name {
  font-size: 13px;
  color: #555;
}

/* 点赞 */
.like {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #999;
}

.like .count {
  margin-left: 4px;
}

.like.active {
  color: #ff2442;
}

/* 时间 */
.time {
  margin-top: 4px;
  font-size: 12px;
  color: #aaa;
}

</style>