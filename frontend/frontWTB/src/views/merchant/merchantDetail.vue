<template>
  <div class="merchant-detail-page">
    <div class="custom-nav" :class="{ 'scrolled': isScrolled }">
      <van-icon name="arrow-left" @click="router.back()" class="nav-icon"/>
      <div class="right-icons">
        <van-icon name="search" />
        <van-icon name="star-o" />
        <van-icon name="ellipsis" />
      </div>
    </div>

    <div class="scroll-container" @scroll="handleScroll">
      
      <div class="shop-gallery">
        <van-swipe :autoplay="3000" indicator-color="white">
          <van-swipe-item v-for="(img, index) in displayInfo.images" :key="index">
            <van-image :src="resolveAssetUrl(img)" fit="cover" width="100%" height="220px" />
          </van-swipe-item>
          <template #indicator="{ active, total }">
            <div class="custom-indicator">{{ active + 1 }}/{{ total }}</div>
          </template>
        </van-swipe>
      </div>

      <div class="info-card">
        <div class="header-row">
          <h1 class="shop-name">{{ displayInfo.name }}</h1>
          <div class="follow-btn">+ 关注</div>
        </div>
        
        <div class="rating-row">
          <van-rate v-model="displayInfo.rating" readonly allow-half size="12" color="#ff4d4f" />
          <span class="score">{{ displayInfo.rating }}</span>
          <span class="base-tag">{{ displayInfo.baseTags }}</span>
          <span class="reviews">{{ displayInfo.totalReviews }}条评价</span>
          <span class="price">¥{{ displayInfo.avgPrice }}/人</span>
          <span class="price">{{ displayInfo.category }}</span>
        </div>

        <div class="facilities-row">
          <span v-for="tag in displayInfo.customTags" :key="tag">{{ tag }}</span>
        </div>

        <div class="address-row van-hairline--top">
          <div class="addr-left">
            <div class="addr-text van-ellipsis">{{ displayInfo.address }}</div>
            <div class="distance-text">距你{{ displayInfo.distance }}</div>
          </div>
          <van-icon name="phone-o" class="phone-icon" />
        </div>
      </div>

     <div class="section-card coupon-entry" @click="handleOpenCouponPopup">
  <div class="coupon-label">
    <span class="tag-img">优惠券</span>
    <span class="desc">
       {{ couponList.length > 0 ? `有 ${couponList.length} 张优惠券待领取` : '点击查看店铺优惠' }}
    </span>
  </div>
  <div class="entry-right">全部 <van-icon name="arrow" /></div>
</div>

      <div class="section-card group-deals">
        <div class="section-title">优惠团购</div>
        <div class="deal-list">
          <GroupDealItem v-for="deal in groupDeals" :key="deal.id" :deal="deal" />
        </div>
      </div>

      <div class="section-card reviews-section">
        <div class="section-title row-between">
          <span>用户评价({{ displayInfo.totalReviews }})</span>
          <span class="view-all">查看全部 <van-icon name="arrow" /></span>
        </div>
        <div class="review-list">
          <ReviewItem v-for="comment in comments" :key="comment.id" :comment="comment" />
        </div>
      </div>
      
      <div style="height: 80px;"></div>
    </div>

    <div class="footer-bar">
      <div class="nav-item">
        <van-icon name="shop-o" size="20"/>
        <span>{{ displayInfo.category }}</span>
      </div>
      <van-button round color="linear-gradient(to right, #ffd01e, #ff8917)" class="action-btn">
        点亮门店
      </van-button>
    </div>

  <van-popup v-model:show="showCouponPopup" position="bottom" round closeable :style="{ height: '45%' }">
  <div class="coupon-popup-content">
    <div class="popup-title">优惠详情</div>
    <div class="coupon-item" v-for="c in couponList" :key="c.id">
      <div class="left">
        <span class="symbol">¥</span>
        <span class="num">{{ c.actualValue }}</span>
      </div>

      <div class="mid">
        <div class="name-row">
          <span class="tag">商品券</span>
          <span class="name">{{ c.name }}</span>
        </div>
        <div class="rule">{{ c.rules }}</div>
        
        <div v-if="!isStarted(c.beginTime)" class="time-notice">
          {{ formatBeginTime(c.beginTime) }} 开抢
        </div>
      </div>

      <div class="right-action">
        <van-button 
          v-if="c.userReceived" 
          size="small" 
          disabled
          class="status-btn"
        >
          已领取
        </van-button>
        <van-button 
           v-else-if="c.isSoldOut" 
           disabled 
           color="#999">
           已售罄
        </van-button>
        <van-button 
          v-else-if="!isStarted(c.beginTime)" 
          size="small" 
          type="warning" 
          plain
          @click="handleClaimCoupon(c)"
        >
          即将开抢
        </van-button>

        <van-button 
          v-else 
          size="small" 
          type="danger"
          @click="handleClaimCoupon(c)"
        >
          领取
        </van-button>
      </div>
    </div>
  </div>
</van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { resolveAssetUrl } from '@/utils/asset';
import GroupDealItem from '@/components/merchant/GroupDealItem.vue';
import ReviewItem from '@/components/merchant/ReviewItem.vue';
import { 
  queryMDetail, 
  queryGroupProducts, 
  queryComments, 
  queryMerchantCoupon 
} from '@/api/shop';
import type { MerchantDetailDTO, GroupDealDTO, CommentDTO, CouponDTO } from '@/api/shop/type';
import dayjs from 'dayjs';
import { showToast,showLoadingToast, closeToast } from 'vant';
import { seckillCoupon } from '@/api/shop/index'

// 1. 直接接收路由中的 id 参数
const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const route = useRoute();
const router = useRouter();

// --- 状态控制 ---
const isScrolled = ref(false);
const showCouponPopup = ref(false);

// --- 样本数据 ---
const MOCK_MERCHANT = {
  name: '加载中...',
  images: [], 
  rating: 5.0,
  totalReviews: 0,
  avgPrice: 0,
  category: '',
  address: '',
  baseTags: '',
  customTags: [],
  distance: ''
};

const displayInfo = reactive({
  id: '',
  name: '',
  images: [] as string[],
  rating: 0,
  totalReviews: 0,
  avgPrice: 0,
  category: '',
  address: '',
  baseTags: '',
  customTags: [] as string[],
  distance: '' // 距离字段
});

const groupDeals = ref<GroupDealDTO[]>([]);
const comments = ref<CommentDTO[]>([]);
const couponList = ref<CouponDTO[]>([]);

onMounted(async () => {
  // 关键调试：看 props.id 是否拿到值
  console.log('组件收到的 Props ID:', props.id);
  console.log('组件收到的 Query 距离:', route.query.distance);
  // 2. 正确提取参数
  // id 通常在 params 里 (对应 /shop/detail/:id)
  // 如果你的路由没配 :id，就尝试从 query 里取
  const rawId = route.params.id || route.query.id;
  const shopId = Array.isArray(rawId) ? rawId[0] : rawId; // 确保是字符串，不是数组

  // distance 在 query 里
  const rawDist = route.query.distance;
  const routeDistance = (Array.isArray(rawDist) ? rawDist[0] : rawDist) || '未知距离';

  // 3. 初始化基础信息
  Object.assign(displayInfo, {
    ...MOCK_MERCHANT,
    id: shopId,
    distance: routeDistance // 赋值距离
  });

  // 4. 【关键修复】如果没有 ID，绝对不要发请求
  if (!shopId) {
    console.error("严重错误：未获取到商家ID，无法请求接口");
    return;
  }

  console.log("正在请求商家ID:", shopId);

  try {
    // 5. 并行请求
    const [detailRes, groupRes, commentRes] = await Promise.all([
      queryMDetail(shopId),
      queryGroupProducts(shopId),
      queryComments(shopId),
     // queryMerchantCoupon(shopId)
    ]);

    // --- 处理详情 ---
    if (detailRes.data) {
      const data = detailRes.data;
      // 这里的判空处理很重要，防止后端返回 null 导致 split 报错
      const imgArray = data.images ? data.images.split(',') : [];
      // 注意：这里用正则 /[,，]/ 同时兼容中英文逗号
      const tagArray = data.customValue ? data.customValue.split(/[,，]/) : [];

      Object.assign(displayInfo, {
        ...data, // 展开后端数据
        images: imgArray,
        customTags: tagArray,
        distance: routeDistance // 再次强制覆盖距离，防止被后端数据(如果不含distance)冲掉
      });
    }

    // --- 处理团购 ---
    if (groupRes.data) {
      groupDeals.value = groupRes.data;
    }

    // --- 处理评论 ---
    if (commentRes.data) {
      comments.value = commentRes.data;
    }

  } catch (error) {
    console.error("请求失败详情:", error);
  }
});

// 点击查看优惠券
const handleOpenCouponPopup = async () => {
  // 1. 如果已经加载过数据，直接打开弹窗，不再重复请求（节省流量）
  if (couponList.value.length > 0) {
    showCouponPopup.value = true;
    return;
  }

  // 2. 显示加载中
  const toast = showLoadingToast({
    message: '加载优惠中...',
    forbidClick: true,
    duration: 0
  });

  try {
    // 3. 发起请求 (这里假设 shopId 已经存在 displayInfo 或 props 中)
    // 确保这里的 queryMerchantCoupon 引用正确
    const res = await queryMerchantCoupon(props.id); 
    
    toast.close();

    if (res.data && res.data.length > 0) {
      couponList.value = res.data;
      showCouponPopup.value = true; // 数据拿到后，打开弹窗
    } else {
      showToast('暂无可用优惠券');
      // 可选：此时 showCouponPopup 依然为 false
    }
    
  } catch (error) {
    toast.close();
    // 4. 这里的 catch 通常会捕获 401 未登录错误
    // 如果你的 http 拦截器没有自动跳登录，可以在这里手动处理
    console.error("获取优惠券失败", error);
    showToast('请先登录查看优惠券'); // 或者跳转到登录页
  }
};

// 格式化时间的辅助函数
const formatBeginTime = (timeStr: string) => {
  const target = dayjs(timeStr);
  const now = dayjs();
  
  // 如果是今年，显示 "M月D日 HH:mm"；否则显示年份
  return target.format('M月D日 HH:mm');
};

// 判断是否已经开始
const isStarted = (timeStr: string) => {
  return dayjs().isAfter(dayjs(timeStr));
};

// 领取优惠券的方法
const handleClaimCoupon = async (coupon: CouponDTO) => {
  // 1. 基础校验：未开始或已领取直接拦截（前端第一道防线）
  if (coupon.userReceived) return;
  if (!isStarted(coupon.beginTime)) {
    showToast('抢购还没开始呢');
    return;
  }

  // 2. 显示加载中（防止用户疯狂点击）
  const toast = showLoadingToast({
    message: '正在排队...',
    forbidClick: true,
    duration: 0 // 持续展示直到手动关闭
  });

  try {
    // 3. 发起秒杀请求
    const res = await seckillCoupon(coupon.id);
    
    // 关闭加载动画
    toast.close();

    if (res.success) {
      // 1. 抢购成功
      // res.data 在这里就是订单号 (number)
      showToast({ type: 'success', message: `抢到了！订单号: ${res.data}` });
      
      // 更新按钮状态
      coupon.userReceived = true; 

    } else {
      // 2. 抢购失败，根据 code 处理不同情况
      
      switch (res.code) {
        case 40001: 
          // 对应后端：return Result.fail(40001, "已售罄")
          showToast('手慢了，已被抢光');
          // 可以在前端标记该券已售罄，让按钮变灰
          // coupon.isSoldOut = true; 
          break;

        case 40002:
          // 对应后端：return Result.fail(40002, "重复领取")
          showToast('您已经领过啦，不要贪心哦');
          coupon.userReceived = true; // 修正显示状态
          break;

        default:
          // 其他未知错误 (500 等)
          showToast(res.errorMsg || '系统繁忙，请重试');
          break;
      }
    }

  } catch (error) {
    toast.close();
    console.error(error);
    showToast('网络请求失败，请检查网络');
  }
};

// 滚动监听保持不变...
const handleScroll = (e: Event) => {
  const scrollTop = (e.target as HTMLElement).scrollTop;
  isScrolled.value = scrollTop > 40;
};
</script>

<style scoped lang="scss">
.merchant-detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  position: relative;
}

.custom-nav {
  position: absolute; top: 0; left: 0; right: 0; z-index: 99;
  height: 44px; display: flex; justify-content: space-between; align-items: center; padding: 0 12px;
  color: #fff; transition: all 0.3s;
  
  &.scrolled { background: #fff; color: #333; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
  .right-icons { display: flex; gap: 16px; font-size: 20px; }
  .nav-icon { font-size: 20px; }
}

.scroll-container { flex: 1; overflow-y: auto; }

/* 轮播图指示器 */
.shop-gallery {
  position: relative;
  .custom-indicator {
    position: absolute; right: 12px; bottom: 12px;
    padding: 2px 8px; font-size: 12px; background: rgba(0,0,0,0.5); color: #fff; border-radius: 12px;
  }
}

/* 卡片通用样式 */
.section-card { margin: 10px 12px; padding: 12px; background: #fff; border-radius: 8px; }
.section-title { font-size: 15px; font-weight: bold; margin-bottom: 12px; display: flex; align-items: center; }
.row-between { justify-content: space-between; }
.view-all { font-size: 12px; color: #999; font-weight: normal; }

/* 商家信息卡片 (上移覆盖轮播图) */
.info-card {
  @extend .section-card;
  margin-top: -15px; position: relative; z-index: 1;
  
  .header-row { display: flex; justify-content: space-between; align-items: flex-start; 
    .shop-name { font-size: 18px; font-weight: bold; margin: 0; flex: 1; line-height: 1.4; }
    .follow-btn { background: #ff4d4f; color: #fff; font-size: 12px; padding: 3px 10px; border-radius: 12px; margin-left: 8px; flex-shrink: 0; }
  }
  
  .rating-row { display: flex; align-items: center; margin-top: 8px; font-size: 12px; color: #666;
    .score { font-weight: bold; color: #ff4d4f; margin: 0 6px; }
    .base-tag { font-weight: bold; color: #333; margin-right: 6px; }
  }
  
  .facilities-row { margin-top: 10px; display: flex; gap: 8px; font-size: 11px; color: #666; 
    span { background: #f5f6f7; padding: 2px 6px; border-radius: 2px; }
  }
  
  .address-row { margin-top: 12px; padding-top: 12px; display: flex; justify-content: space-between; align-items: center;
    .addr-text { font-size: 13px; font-weight: 500; color: #333; }
    .distance-text { font-size: 11px; color: #999; margin-top: 4px; }
    .phone-icon { font-size: 18px; color: #666; margin-left: 16px; padding-left: 16px; border-left: 1px solid #eee; }
  }
}

/* 优惠券入口 */
.coupon-entry { display: flex; justify-content: space-between; align-items: center;
  .coupon-label { display: flex; align-items: center; 
    .tag-img { background: #ff4d4f; color: #fff; font-size: 10px; padding: 1px 4px; border-radius: 2px; margin-right: 8px; }
    .desc { font-size: 13px; }
  }
  .entry-right { font-size: 12px; color: #999; }
}

/* 底部 Footer */
.footer-bar { position: fixed; bottom: 0; width: 100%; height: 50px; background: #fff; display: flex; align-items: center; padding: 0 12px; box-sizing: border-box; border-top: 1px solid #f0f0f0; z-index: 100;
  .nav-item { display: flex; flex-direction: column; align-items: center; color: #333; margin-right: 20px; font-size: 10px; }
  .action-btn { flex: 1; height: 36px; }
}

/* 优惠券弹窗内容 */
.coupon-popup-content {
  padding: 20px 16px;
  
  .popup-title {
    text-align: center;
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 20px;
  }

  .coupon-item {
    display: flex;
    align-items: center;
    background: #fff9f9; /* 浅淡红背景 */
    border: 1px solid #ffeded;
    border-radius: 8px;
    padding: 16px 12px;
    margin-bottom: 12px;
    position: relative;

    .left {
      color: #ff4500;
      width: 70px;
      .symbol { font-size: 14px; }
      .num { font-size: 28px; font-weight: bold; }
    }

    .mid {
      flex: 1;
      padding-left: 10px;
      .name-row {
        margin-bottom: 4px;
        .tag {
          background: #ff4500;
          color: #fff;
          font-size: 10px;
          padding: 1px 4px;
          border-radius: 4px;
          margin-right: 6px;
        }
        .name { font-size: 14px; font-weight: 500; }
      }
      .rule { font-size: 12px; color: #999; }
      .time-notice {
        font-size: 12px;
        color: #ff9900;
        margin-top: 4px;
      }
    }

    .right-action {
      .van-button {
        border-radius: 16px;
        padding: 0 15px;
      }
    }
  }
}
</style>