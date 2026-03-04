import Layout from "../layout/index.vue";
import type { RouteRecordRaw } from "vue-router";
import bottomRoutes from "./bottomRoutes";
import otherRoutes from "./otherRoutes";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    component: Layout,
    redirect: "/home",
    children: [
      ...bottomRoutes,
      ...otherRoutes
    ]
  }
];

export default routes;
