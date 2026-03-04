<template>
  <div class="layout">
    <router-view class="page-view" ref="pageViewRef" />
    <Tabbar />
  </div>
</template>


<script setup>
import Tabbar from '@/components/Tabbar/index.vue'

import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

watch(() => route.fullPath, () => {
  const pageView = document.querySelector('.page-view')
  if (pageView) pageView.scrollTop = 0
})
</script>

<style scoped>
.layout {
   display: flex;              /* 🔥 必须 */
  flex-direction: column;     /* 🔥 必须 */
    position: relative;
  height: 100vh;
  background: #f7f8fa;
}

.page-view {
   flex: 1; 
  padding-bottom: 50px;           /* 给 Tabbar 留出空间 */
  /* overflow-y: auto; */
  -webkit-overflow-scrolling: touch; /* H5 滑动优化 */
}
</style>

