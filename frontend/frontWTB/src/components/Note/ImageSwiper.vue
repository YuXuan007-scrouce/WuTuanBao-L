<template>
  <van-swipe
    class="swipe"
    indicator-color="white"
  >
    <van-swipe-item
      v-for="(img, index) in images"
      :key="index"
    >
      <van-image
        :src="img"
        width="100%"
        height="100%"
        fit="cover"
      />
    </van-swipe-item>
  </van-swipe>
</template>

<script setup lang="ts">
defineProps<{
  images: string[]
}>()
</script>

<style scoped>
.swipe {
  width: 100%;
  height: 300px;
}
</style>
