
// 滚动分页请求参数
export interface FollowFeedParams {
  lastId: number
  offset: number
}
// 接收滚动分页响应数据
export interface FollowFeedItem{
    blogId: number
    title: string
    images: string
    createTime: string
    liked: number
    authorAvatar: string
    authorName: string
    isLike: boolean
}
export interface FollowFeedResult {
  list: FollowFeedItem[]
  minTime: number   // 本次返回数据中的最小 score
  offset: number    // 相同 score 下的偏移量
}
