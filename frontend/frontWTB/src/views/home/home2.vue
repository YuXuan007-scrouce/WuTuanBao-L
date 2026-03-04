<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <van-nav-bar
       safe-area-inset-top
       class="sticky-nav"
    >
      <template #left>
        <van-icon name="bars" size="20" />
      </template>

      <template #title>
        <van-tabs
          v-model:active="activeTab"
          shrink
          color="#ff2c55"
        >
          <van-tab title="关注" name="follow" />
          <van-tab title="附近" name="near" />
          <van-tab title="发现2" name="recommend" />
        </van-tabs>
      </template>

      <template #right>
        <van-icon name="search" size="20" @click="goSearch" />
      </template>
    </van-nav-bar>

    <!-- 内容区 -->
    
        <van-list
          v-model:loading="loading"
             ref="listScrollRef"
             :finished="finished"
              finished-text="没有更多了"
              offset="100"
             class="list-scroll"
             @load="loadFeed"
        >
          <div class="waterfall">
            <FeedCard
              v-for="item in feedList"
              :key="item.blogId"
              :feed="item"
              @like="handleFeedLike"
            />
          </div>
        </van-list>
  </div>
</template>
<script setup lang="ts">
import { ref, watch,nextTick,computed } from 'vue'
import FeedCard from '@/components/common/FeedCard.vue'
import { getHomeFeed } from '@/api/home'
import { useRouter } from 'vue-router';
import { likeOrCollectNote } from '@/api/note/index'
import { onMounted } from 'vue'
import { feedStateMap,TabType } from '@/hook/useHomeFeed'

const listScrollRef = ref<HTMLElement | null>(null)
const router = useRouter();
onMounted(() => {
  loadFeed()
   const el = listScrollRef.value
   console.log('listScrollRef:', listScrollRef.value)
   console.log(
    'mounted:',
    el?.scrollHeight,
    el?.clientHeight
  )
  console.log(el?.scrollTop)
})

const PAGE_SIZE = 6
const activeTab = ref<TabType>('recommend')

const currentState = computed(() => feedStateMap[activeTab.value])

const feedList = computed(() => currentState.value.feedList.value)
const lastId = computed(() => currentState.value.lastId.value)
const offset = computed(() => currentState.value.offset.value)
const finished = computed(() => currentState.value.finished.value)

const loading = ref(false)



/** 加载列表 */
const loadFeed = async () => {
  const state = currentState.value
   if ( state.finished.value) return
  loading.value = true
  console.log('🔥 loadFeed triggered',activeTab.value)

  try {
    const res = await getHomeFeed({
      lastId: state.lastId.value,
      offset: state.offset.value,
      type: activeTab.value
    })

    const data = res?.data
    if (!data) return

    const { list, minTime, offset: newOffset } = data
    // ⭐ 核心：小于 pageSize，说明已经到最后一页
    if (list.length < PAGE_SIZE) {
      state.finished.value = true
    }

  
    state.feedList.value.push(...list)
    state.lastId.value = minTime
    state.offset.value = newOffset

  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
//作品点赞
const handleFeedLike = async (blogId: number) => {
  const res = await likeOrCollectNote({
    blogId,
    action: 'LIKE'
  })

  if (!res?.data) return

  const target = feedList.value.find(item => item.blogId === blogId)
  if (!target) return

  // 直接用后端返回的权威数据
  target.isLike = res.data.isliked
  target.liked = res.data.liked
}



/** Tab 切换 */
watch(activeTab, async () => {
 await nextTick()

  const state = currentState.value
  // ⭐ 只有第一次进入这个 Tab 才请求
  if (state.feedList.value.length === 0) {
    loadFeed()
  }
})

const goSearch = () =>{
  router.push({ name: 'Search' })
}

</script>
<style scoped>
.home-page {
  height: 100vh; 
  background: #f7f8fa;
}

.home-nav {
  z-index: 1000;
}
/* 瀑布流 */
.waterfall {
    column-count: 2; 
  column-gap: 8px;
  padding: 8px;
}

.waterfall > * {
  break-inside: avoid;
  margin-bottom: 8px;
}
.sticky-nav {
  position: sticky;
  top: 0;
  z-index: 1000;
}

.list-scroll {
  height: calc(100vh - 46px - 50px);
  overflow-y: auto;
}

</style>
