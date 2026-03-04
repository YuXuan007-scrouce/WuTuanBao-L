import type { RouteRecordRaw } from "vue-router";

const otherRoutes: Array<RouteRecordRaw> = [
  {
    path: "/home",
    name: "home",
    component: () => import("../views/home/home2.vue"),
    meta: {
      title: "房间详情",
      noCache: true
    }
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/login/login.vue"),
    meta: {
      title: "登录",
      noCache: true
    }
  },
  {
    path: '/note/:blogId',
    name: 'NoteDetail',
    component: () => import('@/views/NoteDetail/index.vue'),
     meta: {
      title: "笔记详情",
      noCache: true
    }
  },
  {
    path: '/Search',
    name: 'Search',
    component: () => import('@/views/search/index.vue'),
     meta: {
      title: "搜索",
      noCache: true
    }
  },
  {
    path: 'publish',
    name: 'Publish',
    component: () => import('@/views/createblog/publish.vue'),
    meta:{
      title: "发布作品",
      noCache: true
    }
  },
  {
    path: '/MerchantDetail/:id',
    name: 'MerchantDetail',
    component: () => import('@/views/merchant/merchantDetail.vue'),
    props: true, // 开启 props 传参（可选，但推荐）
    meta:{
      title: "团购商家",
      noCache: true
    }
  },
  {
    path: '/payment/:id',
    name: 'Payment',
    component: () => import('@/views/payment/Payment.vue'),
    meta: { 
      title: '确认订单', 
      noCache: true
    }
  },
  {
    path: '/payment/mock',
    name: 'PaymentMock',
    component: () => import('@/views/payment/PaymentMock.vue'),
    meta: { 
      title: '收银台',
      noCache: true
     }
  },
  {
    path: '/payment/success',
    name: 'PaymentSuccess',
    component: () => import('@/views/payment/PaymentSuccess.vue'),
    meta: { 
      title: '支付成功',
      noCache: true
     }
  },
  {
    path: '/order/detail',
    name: 'OrderDetail.vue',
    component: () => import('@/views/order/OrderDetail.vue'),
    meta: { 
      title: '订单详情',
      noCache: true
     }
  }
];

export default otherRoutes;
