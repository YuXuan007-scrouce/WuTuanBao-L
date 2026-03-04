import { createRouter, createWebHistory } from "vue-router";
import routes from "./routes";
import type { RouteLocationNormalized } from "vue-router";
import NProgress from "../utils/progress";

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 扩展路由 meta 类型（可选，但推荐）
export interface ToRouteType extends RouteLocationNormalized {
  meta: {
    title?: string;
  };
}

// 前置守卫
router.beforeEach((to: ToRouteType, from, next) => {
  NProgress.start();

  // 设置页面标题
  if (to.meta?.title) {
    document.title = to.meta.title;
  }

  next();
});

// 后置守卫
router.afterEach(() => {
  NProgress.done();
});

export default router;
