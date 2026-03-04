<template>
  <div class="my-page">
    <!-- ========== 顶部用户信息 ========== -->
    <div class="user-card">
      <!-- 已登录 -->
      <template v-if="isLogin">
        <img class="avatar" :src="userAvatar" />
           

        <div class="info">
          <div class="nickname">{{ nickName }}</div>
          <div class="action-row">
          <van-button size="mini" plain @click="goEdit">
            编辑资料
          </van-button>
          <van-button size="mini" plain @click="handleLogout">
            退出当前账号
          </van-button>
        </div>
        </div>
      </template>

      <!-- 未登录 -->
      <template v-else>
        <van-button type="primary" @click="goLogin">
          去登录
        </van-button>
      </template>
    </div>

    <!-- ========== 个人介绍 & 地址性别 ========== -->
    <div v-if="isLogin" class="profile">
      <div class="introduce">
        {{ userDetail?.introduce || '暂无个人介绍' }}
      </div>

      <div class="extra">
        <span>{{ userDetail?.city || '未知城市' }}</span>
        <span>{{ genderText}}</span>
      </div>
    </div>

    <!-- ========== Tab 导航 ========== -->
    <van-tabs v-model:active="activeTab" sticky  offset-top="0">
      <van-tab title="笔记" name="note" />
      <van-tab title="订单" name="comment" />
      <van-tab :title="`粉丝(${userDetail?.fans ?? 0})`" name="fans" />
      <van-tab :title="`关注(${userDetail?.followee ?? 0})`" name="follow" />
    </van-tabs>

    <!-- ========== 内容区域 ========== -->
    <div class="tab-content">
      <!-- 笔记 / 评价：分页 -->
      <van-list
          v-if="activeTab === 'note'"
          v-model:loading="noteLoading"
          :finished="noteFinished"
          finished-text="没有更多了"
          @load="loadMyNotes">
      <NoteItem
      v-for="item in list"
      :key="item.id"
      :note="item"
      />
    </van-list>
     <van-list
          v-if="activeTab === 'follow'"
          v-model:loading="followLoading"
          :finished="followFinished"
          finished-text="没有更多了"
          @load="loadFollowFeed">
       <FollowItem
        v-for="item in feedList"
        :key="item.blogId"
        :feed="item"
        @like="handleFeedLike"
        />
</van-list>


    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/model/user';
import type { UserDetailInterface } from '@/api/user/type';
import { showConfirmDialog } from 'vant';
import { resolveAssetUrl } from '@/utils/asset';
import {
  getUserDetail,getUserNotes 
} from '@/api/user';
import NoteItem from '@/components/common/NoteItem.vue';
import type { FollowFeedItem } from '@/api/follow/type';
import { getFollowFeed } from '@/api/follow/index';
import FollowItem from '@/components/common/FollowFeed.vue';
import { likeOrCollectNote } from '@/api/note/index';

/* ================== 基础 ================== */
const router = useRouter();
const userStore = useUserStore();

// 严格判断token有效性
const isLogin = computed(() => {
  const token = userStore.token;
  return !!token && token !== 'undefined' && token !== 'null';
});
/* ================== Feed流关注参数 ================== */
const feedList = ref<FollowFeedItem[]>([])

const lastId = ref<number>(Date.now()) // 第一次用当前时间
const offset = ref<number>(0)          // 偏移量初始0



const userAvatar = resolveAssetUrl(userStore.userInfo?.icon || '/userIcon.png');
/* ================== 用户信息如果没有设置就用默认的 ================== */
const nickName = userStore.userInfo?.nickName || '匿名用户';
const uid = userStore.userInfo?.id || 0;


const userDetail = ref<UserDetailInterface | null>(null);

const genderText = computed(() => {
  if (!userDetail.value) return '';
  return userDetail.value.gender === 0 ? '女' : '男';
});

/* ================== Tabs ================== */
type TabType = 'note' | 'comment' | 'fans' | 'follow';
const activeTab = ref<TabType>('note');

/* ================== 列表状态 ================== */
const list = ref<any[]>([]);
const noteLoading = ref(false)
const noteFinished = ref(false)
const followLoading = ref(false)
const followFinished = ref(false)
const commentLoading = ref(false)
const commentFinished = ref(false)
// 粉丝（一次性）
const fansLoading = ref(false)
const fansFinished = ref(true)

const page = ref(1);
const pageSize = 5;




/* ================== 生命周期 初始加载就请求用户详情 ================== */
onMounted(async () => {
  if (!isLogin.value) return;

  const res = await getUserDetail(uid);
  userDetail.value = res.data;   // 存储用户详情

  loadCurrentTab();
});

/* tab 切换 */
watch(activeTab, () => {
  resetList();
  loadCurrentTab();
});

/* ================== 统一调度入口(多出口) ================== */
const loadCurrentTab = () => {
  switch (activeTab.value) {
    case 'note':
      loadMyNotes();
      break;
    case 'comment':
      loadMyComments();
      break;
    case 'fans':
      loadMyFans();
      break;
    case 'follow':
      loadFollowFeed();
      break;
  }
};

/* ================== 重置 ================== */
const resetList = () => {
  list.value = [];
  page.value = 1;
  noteFinished.value = false;
};

/* ================== Mock API（真实接口） ================== */

/** 我的笔记（分页） */
const loadMyNotes = async () => {
  if (noteLoading.value || noteFinished.value) return;

  noteLoading.value = true;
  try {
    const res = await getUserNotes({
      page: page.value,
      size: pageSize
    });

    const records = res.data;

    list.value.push(...records);

    if (records.length < pageSize) {
      noteFinished.value = true;
    } else {
      page.value++;
    }
  } finally {
    noteLoading.value = false;
  }
};


/** 我的评价（分页） */
const loadMyComments = async () => {
  noteLoading.value = true;

  setTimeout(() => {
    const data = mockPagingData('评价');

    list.value.push(...data);
    noteLoading.value = false;

    handlePagingEnd(data.length);
  }, 500);
};

/** 我的粉丝（一次性） */
const loadMyFans = async () => {
  noteLoading.value = true;

  setTimeout(() => {
    list.value = mockOnceData('粉丝');
    noteLoading.value = false;
    noteFinished.value = true;
  }, 400);
};

/** 我的关注(滚动分页查询) */
const loadFollowFeed = async () => {
  if (noteLoading.value || noteFinished.value) return

  noteLoading.value = true
  try {
    const res = await getFollowFeed({
      lastId: lastId.value!,
      offset: offset.value
    })

    const data = res?.data
    if (!data) return

    const { list, minTime, offset: newOffset } = data

    if (!list || list.length === 0) {
      noteFinished.value = true
      return
    }

    feedList.value.push(...list)
    lastId.value = minTime
    offset.value = newOffset
  } finally {
    noteLoading.value = false
  }
}

//作品点赞
const handleFeedLike = async (blogId: number) => {
  const res = await likeOrCollectNote({
    blogId,
    action: 'LIKE'
  })

  if (!res?.data) return

  const target = feedList.value.find(item => item.blogId === blogId)
  if (!target) return

  // 直接用后端返回的权威数据
  target.isLike = res.data.isliked
  target.liked = res.data.liked
}


/* ================== 工具方法 ================== */

/** 分页 mock */
const mockPagingData = (prefix: string) => {
  return Array.from({ length: pageSize }).map((_, i) => ({
    id: `${prefix}-${page.value}-${i}`,
    title: `${prefix}内容 ${page.value}-${i + 1}`
  }));
};

/** 一次性 mock */
const mockOnceData = (prefix: string) => {
  return Array.from({ length: 8 }).map((_, i) => ({
    id: `${prefix}-${i}`,
    name: `${prefix}用户 ${i + 1}`
  }));
};

/** 分页结束判断 */
const handlePagingEnd = (len: number) => {
  if (len < pageSize) {
    noteFinished.value = true;
  } else {
    page.value++;
  }
};
// 获取简短内容
const getShortContent = (content: string) => {
  if (!content) return '';
  return content.length > 50
    ? content.slice(0, 50) + '...'
    : content;
};
// 跳转笔记详情
const goNoteDetail = (id: number) => {
  router.push(`/blog/${id}`);
};

/* ================== 路由 ================== */
const goLogin = () => router.push('/login');
const goEdit = () => router.push('/edit-profile');
const handleLogout = () => {
  showConfirmDialog({
    title: '确认退出',
    message: '确定要退出当前账号吗？',
  }).then(() => {
    userStore.Logout();
    router.push('/login');
  });
};

</script>
<style lang="scss" scoped>
.my-page {
  display: flex;
  flex-direction: column;
  flex: 1; /* 关键：撑满 router-view */
  background: #f7f8fa;
  min-height: 100%; /* 避免 flex 子元素超出滚动容器 */
}

.user-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;

  .avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    margin-right: 12px;
  }
  .info {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .nickname {
    font-size: 16px;
    font-weight: bold;
  }
}
.profile {
  background: #fff;
  padding: 12px 16px;
  margin-bottom: 8px;

  .introduce {
    color: #333;
    margin-bottom: 6px;
  }

  .extra {
    color: #999;
    font-size: 13px;
    display: flex;
    gap: 12px;
  }
}

.tab-content {
   flex: 1;
   overflow-y: auto;
  padding: 12px;

  .list-item {
    background: #fff;
    padding: 12px;
    border-radius: 8px;
    margin-bottom: 8px;
  }
}
.info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between; /* 左右分布 */
  gap: 8px;
}
.note-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
}
.cover {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.desc {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}
</style>