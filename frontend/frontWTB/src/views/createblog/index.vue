<template>
  <div class="create-page">
    <van-button
      type="primary"
      size="large"
      round
      @click="goPublish"
    >
     去发布作品
    </van-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goPublish = () => {
  router.push('/publish')
}
</script>

<style scoped>
.create-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
</style>
