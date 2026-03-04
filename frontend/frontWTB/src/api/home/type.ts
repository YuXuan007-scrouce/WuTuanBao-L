

export interface HomeFeedParams {
  lastId: number
  offset: number
  type?: 'recommend' | 'follow' | 'near'
}

// 接收滚动分页响应数据
export interface HomeFeedItem{
    blogId: number
    title: string
    images: string
    createTime: string
    liked: number
    authorAvatar: string
    authorName: string
    isLike: boolean
}
export interface HomeFeedResult {
  list: HomeFeedItem[]
  minTime: number   // 本次返回数据中的最小 score
  offset: number    // 相同 score 下的偏移量
  type?: 'recommend' | 'follow' | 'near'
}