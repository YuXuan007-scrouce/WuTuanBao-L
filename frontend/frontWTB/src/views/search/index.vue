<template>
  <div class="search-page">
    <van-sticky>
      <div class="search-header">
        <van-icon name="arrow-left" class="back-icon" @click="goBack" />
        <van-search
          v-model="keyword"
          shape="round"
          placeholder="台球团购附近"
          @search="triggerNewSearch"
          class="flex-1"
        />
        <div class="search-btn" @click="triggerNewSearch">搜索</div>
      </div>
      
      <div v-if="hasSearched" class="result-filters">
        <van-tabs v-model:active="activeTab" shrink line-width="20px">
          <van-tab title="团购" />
          <van-tab title="直播" />
          <van-tab title="视频" />
        </van-tabs>
        
        <div class="filter-bar">
          <span 
            class="filter-tag" 
            :class="{ active: currentSort === 'default' && !currentNear }"
            @click="handleFilter('default')"
          >
            默认
          </span>
          <span 
            class="filter-tag" 
            :class="{ active: currentNear === 3 }"
            @click="handleFilter('near', 3)"
          >
            附近3km
          </span>
          <span 
            class="filter-tag" 
            :class="{ active: currentSort === 'comments' }"
            @click="handleFilter('comments')"
          >
            人气榜
          </span>
          <span 
            class="filter-tag" 
            :class="{ active: currentSort === 'rating' }"
            @click="handleFilter('rating')"
          >
            星级
          </span>
        </div>
      </div>
    </van-sticky>

    <div v-if="!hasSearched" class="history-container">
      <div class="section-header">
        <span class="title">历史记录</span>
        <van-icon name="delete-o" @click="clearHistory" />
      </div>
      <div class="tags-wrapper">
        <div 
          v-for="item in historyList" 
          :key="item.id" 
          class="history-tag"
          @click="onTagClick(item.keyword)"
        >
          {{ item.keyword }}
        </div>
      </div>
      <div class="section-header mt-20">
        <span class="title">猜你想搜</span>
        <van-icon name="replay" />
      </div>
      <div class="tags-wrapper guess-wrapper">
        <div class="history-tag">台球团购附近 <span class="hot">热</span></div>
        <div class="history-tag">茶百道</div>
      </div>
    </div>

    <div v-else class="result-container">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoadMore"
      >
        <ShopItem 
          v-for="shop in resultList" 
          :key="shop.id" 
          :item="shop"
          @click="goToDetail(shop)"
        />
      </van-list>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import ShopItem from '@/components/common/ShopItem.vue';

import { getSearchResult, adaptShopData } from '@/api/search/index';
import type { ShopDTO, SearchHistoryItem, SearchParams } from '@/api/search/type';


const router = useRouter();

// --- 核心状态 ---
const keyword = ref('');
const hasSearched = ref(false); 
const activeTab = ref(0);

// --- 筛选状态 ---
const currentSort = ref<'default' | 'comments' | 'rating'>('default');
const currentNear = ref<number | undefined>(undefined);

// --- 列表分页状态 ---
const loading = ref(false);
const finished = ref(false);
const resultList = ref<ShopDTO[]>([]);
const currentPage = ref(1);
const pageSize = 10; 

// 模拟用户坐标 (实际开发请替换为 uni.getLocation 或 navigator)
const userLocation = { lat: 23.1291, lng: 113.2644 };

// --- 历史记录状态 ---
const historyList = ref<SearchHistoryItem[]>([]);

// 初始化加载历史记录
onMounted(() => {
  // 这里暂时用静态数据，后续换成你的 loadHistory API
  historyList.value = [
    { id: 1, keyword: '台球团购附近' },
    { id: 2, keyword: '团购烤肉' },
  ];
});

// ---------------------------------------------------------
// 交互逻辑：点击标签/历史记录
// ---------------------------------------------------------
const onTagClick = (tag: string) => {
  keyword.value = tag;
  triggerNewSearch(); 
};

const clearHistory = () => {
  historyList.value = [];
  showToast('历史记录已清空');
};

const goBack = () => {
  if (hasSearched.value) {
    hasSearched.value = false;
    keyword.value = '';
    // 重置筛选
    currentSort.value = 'default';
    currentNear.value = undefined;
    resultList.value = [];
  } else {
    router.back();
  }
};

// ---------------------------------------------------------
// 核心逻辑：处理筛选点击
// ---------------------------------------------------------
const handleFilter = (type: string, value?: number) => {
  // 重置页码和列表
  loading.value = true; // 防止重复触发
  
  if (type === 'near') {
    // 切换“附近”模式
    // 如果当前已经是选中状态，可以考虑取消选中(变成 default)，或者保持高亮
    // 这里逻辑是：点击附近3km -> 开启附近筛选，重置排序
    currentNear.value = value;
    currentSort.value = 'default'; 
  } else {
    // 切换排序模式 (默认、人气、星级)
    currentSort.value = type as any;
    currentNear.value = undefined; // 互斥逻辑：选排序时取消“附近3km”限制(视业务需求而定)
  }

  triggerNewSearch();
};

// ---------------------------------------------------------
// 核心逻辑：触发新搜索 (重置列表)
// ---------------------------------------------------------
const triggerNewSearch = () => {
  if (!keyword.value.trim()) return showToast('请输入内容');
  
  hasSearched.value = true;
  
  // 重置列表状态
  resultList.value = [];
  currentPage.value = 1;
  finished.value = false;
  loading.value = true; 
  
  // 立即发起第一次请求
  onLoadMore();
};

// ---------------------------------------------------------
// 核心逻辑：加载数据 (调用真实 API)
// ---------------------------------------------------------
const onLoadMore = async () => {
  try {
    const params: SearchParams = {
      keyword: keyword.value,
      page: currentPage.value,
      size: pageSize, // 注意：你的接口定义是 size 还是 pageSize，请保持一致，这里按你最新的 SearchParams 用的 size
      latitude: userLocation.lat,
      longitude: userLocation.lng,
      sortBy: currentSort.value,
      near: currentNear.value
    };

    const res = await getSearchResult(params);
   
    // 1. 获取后端原始列表
    // 根据你提供的响应结构：res.data.shopDocs
    // 注意 axios 响应拦截器可能已经脱了一层 data，请根据实际情况调整
    // 这里假设 getSearchResult 返回的是完整的 AxiosResponse，所以数据在 res.data.data.shopDocs
    // 如果你的拦截器直接返回 body，那就是 res.data.shopDocs
    const rawList = res.data?.shopDocs || [];
    console.log('后端响应的数据',rawList);

    // 2. 数据适配 (清洗数据)
    const formattedList = rawList.map(item => adaptShopData(item));

    // 3. 追加数据
    resultList.value.push(...formattedList);
    
    // 4. 页码增加
    currentPage.value++;
    loading.value = false;

    // 5. 判断结束 (如果没有数据了，或者返回的数据量小于分页大小)
    if (rawList.length < pageSize) {
      finished.value = true;
    }

  } catch (error) {
    loading.value = false;
    finished.value = true;
    console.error('搜索出错:', error);
    showToast('网络请求失败');
  }
};
// 在 ShopList 或 ShopItem 组件中
const goToDetail = (item: any) => {
  console.log("准备跳转，参数：", item.id, item.distance); // 调试打印

  router.push({
    name: 'MerchantDetail', // 必须用 name，不能用 path
    params: { 
      id: item.id  // 对应路由中的 :id
    },
    query: { 
      distance: item.distance // 距离通过 query 传递 (?distance=1.2km)
    }
  });
};
</script>

<style scoped lang="scss">
.search-page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.search-header {
  display: flex;
  align-items: center;
  background: #fff;
  padding-right: 12px;
  
  .back-icon {
    padding: 0 12px;
    font-size: 20px;
    color: #333;
  }
  
  .flex-1 { flex: 1; }
  
  .search-btn {
    font-size: 14px;
    color: #333;
    font-weight: 500;
    margin-left: 8px;
  }
}

.result-filters {
  background: #fff;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  
  .filter-bar {
    display: flex;
    overflow-x: auto;
    padding: 8px 12px 0;
    
    .filter-tag {
      flex-shrink: 0;
      background: #f5f6f7;
      color: #666;
      font-size: 12px;
      padding: 6px 14px;
      border-radius: 4px;
      margin-right: 8px;
      transition: all 0.2s;
      
      /* 选中态样式 */
      &.active {
        background: rgba(255, 77, 79, 0.1); /* 浅红色背景 */
        color: #ff4d4f; /* 红色文字 */
        font-weight: bold;
      }
    }
  }
}

.history-container {
  padding: 16px;
  background: #fff;
  min-height: calc(100vh - 54px);
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    color: #999;
    
    .title {
      font-size: 14px;
      font-weight: bold;
      color: #333;
    }
  }
  
  .mt-20 { margin-top: 24px; }
  
  .tags-wrapper {
    display: flex;
    flex-wrap: wrap;
    
    .history-tag {
      background: #f5f6f7;
      padding: 6px 12px;
      border-radius: 4px;
      font-size: 13px;
      color: #333;
      margin-right: 10px;
      margin-bottom: 10px;
      display: inline-flex; 
      align-items: center;

      .hot {
        font-size: 10px;
        color: #ff4d4f;
        background: rgba(255, 77, 79, 0.1);
        padding: 0 2px;
        margin-left: 4px;
      }
    }
  }
}
</style>