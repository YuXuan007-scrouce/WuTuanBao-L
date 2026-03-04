<template>
  <!-- 顶部作者栏 -->
  <AuthorBar
    v-if="author"
    :author="author"
    @toggleFollow="handleFollow"
    @back="handleBack "
  />

  <!-- 中间滚动区域 -->
  <div class="content">
    <!-- 图片轮播 -->
    <ImageSwiper :images="noteDetail?.images || []" />

    <!-- 标题 & 内容 -->
    <div class="note-body">
      <h2 class="title">{{ noteDetail?.title }}</h2>
      <p class="content-text">{{ noteDetail?.content }}</p>
      <div class="meta">
        <span>{{ noteDetail?.createTime }}</span>
        <span>{{ noteDetail?.address }}</span>
      </div>
    </div>

    <van-divider />

    <!-- 评论区 -->
    <div class="comment-header">
      共 {{ noteDetail?.comments }} 条评论
    </div>

 <CommentItem
  v-for="item in comments"
  :key="item.id"
  :comment="item"
  @like="handleCommentLike"
  @reply="handleReply"
/>


  </div>

  <!-- 底部操作栏 -->
<BottomBar
  :likeInfo="likeInfo"
  :replyTo="replyTo"
  @sendComment="handleSendComment"
  @toggleLike="handleLike"
  @toggleCollect="handleCollect"
/>

</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import AuthorBar from '@/components/Note/AuthorBar.vue'
import ImageSwiper from '@/components/Note/ImageSwiper.vue'
import CommentItem from '@/components/Note/CommentItem.vue'
import BottomBar from '@/components/Note/BottomBar.vue'
import type { Author, NoteDetail, Comment, LikeCollectInfo, 
  ReplyTo, SendCommentDTO } from '@/api/note/type'
import { followAuthor, getAuthorInfo,getNoteDetail
  ,getComments,likeComment,sendComment,likeOrCollectNote } from '@/api/note/index' 
import { useRoute,useRouter } from 'vue-router'
import { resolveAssetUrl } from '@/utils/asset'


const router = useRouter();
const route = useRoute()
const blogId = Number(route.params.blogId); // 获取路由参数
const commentMap = new Map<number, Comment>()
const replyTo = ref<ReplyTo | null>(null)


// 顶部作者
const author = ref<Author>({
  id: 2,
  nickName: '妮可博主',
  icon: '/avatar.png',
  followed: false
})

const noteDetail = ref<NoteDetail>()
const comments = ref<Comment[]>([])  // 中间评论列表
const loading = ref(false)
const errorMsg = ref('')

const likeInfo = ref<LikeCollectInfo>({
  isliked: false,
  liked: 0,
  iscollected: false,
  collection: 0
})



/** 页面加载 */
onMounted(() => {
  fetchNoteDetail()
  fetchComments()
})

/** 查询笔记详情*/
const fetchNoteDetail = async () => {
  const res = await getNoteDetail(blogId)
  const raw = res.data    //后端放回的数据，images是字符串
  // 转换 images 字符串为数组
 noteDetail.value = {
    id: raw.id,
    title: raw.title,
    content: raw.content,
    address: raw.address,
    createTime: raw.createTime,
    images: raw.images
      ? raw.images.split(',').map(i => resolveAssetUrl(i))
      : [],
    authorId: raw.userId,
    liked: raw.liked,
    collection: raw.collection,
    comments: raw.comments   // ⭐ 评论数
  }
  likeInfo.value = {
    isliked: false, // 当前方案：登录默认 false
    liked: raw.liked,
    iscollected: false,
    collection: raw.collection
  }
  // 获取作者信息
  fetchAuthor(noteDetail.value.authorId)
}


// 查询评论列表
const fetchComments = async () => {
    loading.value = true
  errorMsg.value = ''

  try {
    const res = await getComments(blogId)
    // 正常赋值
    const rawList = Array.isArray(res.data) ? res.data : []

   rawList.forEach(c => {
      c.userAvatar = resolveAssetUrl(c.userAvatar)
  })

   comments.value = buildCommentTree(rawList)
   console.log('评论原始数据', res.data)
   } catch (err) {
     console.error('fetchComments error:', err)
     errorMsg.value = '网络异常，请稍后重试'
     comments.value = []
   } finally {
    loading.value = false
  }
}
function buildCommentTree(rawList: Comment[]) {
  const map = new Map<number, Comment>()
  const roots: Comment[] = []

  // 初始化 map 并添加 children 属性
  rawList.forEach(c => {
    map.set(c.id, { ...c, children: [] })
    commentMap.set(c.id, { ...c, children: [] }) // 保持 map
  })

  map.forEach(c => {
    if (c.parentId) {
      const parent = map.get(c.parentId)
      if (parent) {
        parent.children!.push(c)
      }
    } else {
      roots.push(c)
    }
  })
  return roots
}


const handleBack = () => {
  router.back()
}

/** 上部分:在查询作品笔记详情后，得到作者id，再查询作者基本信息 */
const fetchAuthor = async (authorId: number) => {
  const res = await getAuthorInfo(authorId)
  const userAvatar = resolveAssetUrl(res.data.icon || '/userIcon.png');
  console.log('作者头像地址：',userAvatar);
  author.value = {
    id: res.data.id,
    nickName: res.data.nickName,
    icon: userAvatar,
    followed: res.data.followed
  }
}
/** 关注|取关 */
const handleFollow = async () => {
  if (!author.value) return;

  try {
    const res = await followAuthor(author.value.id);
    // 用后端返回的状态来更新前端
    author.value.followed = res.data.followed;
  } catch (e) {
    // toast 提示失败
    console.error('关注操作失败', e);
  }
}


/** 点赞 */
const handleLike = async () => {
  const res = await likeOrCollectNote({
    blogId,
    action: 'LIKE'
  })
  console.log('res =', res)

  if (!res?.data) return

  likeInfo.value.isliked = res.data.isliked
  likeInfo.value.liked = res.data.liked
}

/** 收藏 */
const handleCollect = async () => {
  const res = await likeOrCollectNote({
    blogId,
    action: 'COLLECT'
  })

  if (!res?.data) return

  likeInfo.value.iscollected = res.data.iscollected
  likeInfo.value.collection = res.data.collection
}

/** 发送评论: 一级评论 | 二级评论回复 */
const handleSendComment = async (content: string) => {
  const payload: SendCommentDTO = {
    blogId,
    content,
    parentId: replyTo.value?.parentId ?? null,
    replyToUserId: replyTo.value?.replyToUserId ?? undefined
  }

  await sendComment(payload)

  // 清空回复状态
  replyTo.value = null

  // TODO: 本地插入评论（下一步我们可以做）
  insertCommentLocal(payload, content)
}
function insertCommentLocal(payload: SendCommentDTO, content: string) {
  const newComment: Comment = {
    id: Date.now(),
    blogId: payload.blogId,
    content,
    parentId: payload.parentId ?? null,
    userId: 123,
    userName: '我',
    createTime: new Date().toLocaleString(),
    likeCount: 0,
    userAvatar: '',
    address: '',
    children: []
  }
  noteDetail.value!.comments += 1  // 评论数加1

  if (payload.parentId) {
    const parent = commentMap.get(payload.parentId)
    if (parent) {
       // 如果 parent.children 不是 reactive，可以先用 reactive 包裹
      if (!('children' in parent)) parent.children = reactive([])
      parent.children!.push(newComment)  // 把这条二级评论放到一级评论的 children 数组里
    }
  } else {
    comments.value.unshift(newComment)
  }

  commentMap.set(newComment.id, newComment)
}


// 点赞评论(已完成)
const handleCommentLike = async (id: number) => {
  const c = comments.value.find(i => i.id === id)
  if (!c) return

  // 记录旧状态（用于回滚）
  const oldLiked = c.liked
  const oldCount = c.likeCount

  //  前端立即更新（提升体验）
  c.liked = !oldLiked
  c.likeCount += c.liked ? 1 : -1

  try {
    //调接口
    await likeComment(id, c.liked)
  } catch (err) {
    //接口失败 → 回滚
    c.liked = oldLiked
    c.likeCount = oldCount
    console.error('评论点赞失败', err)
  }
}

// 回复评论（来自 CommentItem）
const handleReply = (payload: {
  commentId: number
  userId: number
  userName: string
}) => {
  const rootId = findRootCommentId(payload.commentId)

  replyTo.value = {
    parentId: rootId,                // 永远是一级评论 ID
    replyToUserId: payload.userId,   // 被回复的人
    replyToUserName: payload.userName
  }
}
function findRootCommentId(commentId: number): number {
  let current = commentMap.get(commentId)

  while (current?.parentId) {
    current = commentMap.get(current.parentId)
  }

  return current?.id ?? commentId
}

</script>

<style scoped>
.content {
  padding: 10vh 0;
  overflow-y: auto;
}

.note-body {
  padding: 12px;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 10px;
}
</style>
