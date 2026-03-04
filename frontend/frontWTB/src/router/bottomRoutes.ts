import type { RouteRecordRaw } from "vue-router";

const tabBarRoutes: Array<RouteRecordRaw> = [
  {
    path: "/home",
    name: "Home",
    component: () => import("@/views/home/home2.vue"),
    meta: {
      title: "首页",
      icon: "search"
    }
  },
  {
    path: "/group",
    name: "Group",
    component: () => import("../views/group/index.vue"),
    meta: {
      title: "圈子",
      icon: "star-o"
    }
  },
  {
    path: "/createblog",
    name: "CreateBlog",
    component: () => import("../views/createblog/index.vue"),
    meta: {
      title: "创建博客",
      icon: "plus"
    }
  },
  {
    path: "/message",
    name: "Message",
    component: () => import("../views/Message/index.vue"),
    meta: {
      title: "消息",
      icon: "comment-o"
    }
  },
  {
    path: "/userCenter",
    name: "UserCenter",
    component: () => import("../views/userCenter/index.vue"),
    meta: {
      title: "我的",
      icon: "home-o"
    }
  }
];

export default tabBarRoutes;
