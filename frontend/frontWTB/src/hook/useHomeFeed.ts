import { ref } from 'vue'
import { HomeFeedItem } from '@/api/home/type'


export type TabType = 'follow' | 'near' | 'recommend'

export const feedStateMap = {
  follow: {
    feedList: ref<HomeFeedItem[]>([]),
    lastId: ref(Date.now()),
    offset: ref(0),
    finished: ref(false)
  },
  near: {
    feedList: ref<HomeFeedItem[]>([]),
    lastId: ref(Date.now()),
    offset: ref(0),
    finished: ref(false)
  },
  recommend: {
    feedList: ref<HomeFeedItem[]>([]),
    lastId: ref(Date.now()),
    offset: ref(0),
    finished: ref(false)
  }
}

const loading = ref(false)
