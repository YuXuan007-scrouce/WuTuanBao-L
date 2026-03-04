<template>
  <div class="tabbar">
    <span @click="go('/home')">首页</span>
    <span @click="go('/group')">圈子</span>
    <span @click="go('/createblog')">创建博客</span>
    <span @click="go('/message')">消息</span>   
    <span @click="go('/userCenter')">我的</span>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();
const go = (path: string) => router.push(path);
</script>

<style scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid #eee;
  background: #fff;
}
</style>
