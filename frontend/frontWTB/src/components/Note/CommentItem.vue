<template>
  <div class="comment-item">
    <!-- 左：评论者头像 -->
    <van-image
      round
      width="36"
      height="36"
      :src="comment.userAvatar"
    />

    <!-- 中：内容 -->
    <div class="content">
      <div class="name">{{ comment.userName }}</div>

      <div class="text">
        <span
          v-if="comment.parentId"
          class="reply-user"
        >
          回复 {{ comment.replyToUserName }}:
        </span>
        {{ comment.content }}
      </div>

      <div class="meta">
        <span>{{ comment.createTime }}</span>
        <span v-if="comment.address">{{ comment.address }}</span>
        <span class="reply" @click="onReply">回复</span>
      </div>
    </div>

    <!-- 右：点赞 -->
    <div class="like" @click="onLike">
      <van-icon
        :name="comment.liked ? 'like' : 'like-o'"
        :color="comment.liked ? '#ee0a24' : ''"
      />
      <span>{{ comment.likeCount }}</span>
    </div>
  </div>

    <!-- ⭐⭐⭐ 关键：递归渲染 children ⭐⭐⭐ -->
  <div
    v-if="comment.children && comment.children.length"
    class="children"
  >
    <CommentItem
      v-for="child in comment.children"
      :key="child.id"
      :comment="child"
      isChild
      @like="$emit('like', $event)"
      @reply="$emit('reply', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { Comment } from '@/api/note/type'

const props = defineProps<{
  comment: Comment
   isChild?: boolean
}>()

const emit = defineEmits<{
  (e: 'like', commentId: number): void
  (e: 'reply', payload: {
    commentId: number
    userId: number
    userName: string
  }): void
}>()

const onLike = () => {
  emit('like', props.comment.id)
}

//告诉父组件：我点了谁
const onReply = () => {
  emit('reply', {
    commentId: props.comment.id,  // 回复的评论 ID 点击的那一刻就赋值了
    userId: props.comment.userId,
    userName: props.comment.userName
  })
}
</script>

<style scoped>
.comment-item {
  display: flex;
  padding: 12px;
}

.content {
  flex: 1;
  margin-left: 10px;
}

.name {
  font-size: 14px;
  font-weight: 500;
}

.text {
  margin: 6px 0;
  font-size: 14px;
}

.reply-user {
  color: #576b95;
}

.meta {
  font-size: 12px;
  color: #999;
}

.meta span {
  margin-right: 8px;
}

.reply {
  color: #576b95;
}

.like {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
}
.children {
  margin-left: 46px; /* 头像宽度 + 间距 */
}

.comment-item.child {
  padding-top: 6px;
  padding-bottom: 6px;
}

</style>
