<template>
  <van-tabbar
    v-show="isShowTabBar"
    route
   style="position: fixed; bottom: 0; left: 0; width: 100%; z-index: 999;"
  >
    <van-tabbar-item
      v-for="(item, index) in tabBarData"
      :key="index"
      :icon="item.icon"
      :to="item.to"
    >
      {{ item.title }}
    </van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import tabBarRoutes from "../../router/bottomRoutes";

const route = useRoute();
const active = ref(0);

const tabBarData = computed(() => {
  return tabBarRoutes.map(item => ({
    icon: item.meta?.icon as string,
    title: item.meta?.title as string,
    to: { path: item.path }
  }));
});

// ✅ 核心判断：当前路由是否属于 tabBarRoutes[] 中的某一个路由
const isShowTabBar = computed(() => {
  return tabBarRoutes.some(item =>
    route.path.startsWith(item.path)
  );
});
</script>
