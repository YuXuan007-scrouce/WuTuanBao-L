
// 作者信息 查询
export interface Author {
  id: number              //作者ID传给后端用来关注/取关
  nickName: string
  icon: string
  followed: boolean    // 是否已关注
}


// 中间部分展示  查询笔记详情
export interface NoteDetail {
  id: number
  title: string
  content: string
  address: string
  createTime: string
  images: string[]         // 图片数组
  authorId: number   // ⭐ 关键作者
  liked: number
  collection: number
  comments: number    // ⭐ 评论数
}
//存储后端接收再转换成上面的形式
export interface NoteDetailRaw {
  id: number
  title: string
  content: string
  address: string
  createTime: string
  images: string           // ⚠️ 后端返回的字符串
  userId: number
  liked: number
  collection: number
  comments: number    // ⭐ 评论数
}


// 中间部分(评论区内容)  查询
export interface Comment {
  id: number           
  blogId: number             
  userId: number             // 评论者ID
  userAvatar: string
  userName: string
  content: string
  likeCount: number          //该条评论的点赞数
  liked?: boolean           

  parentId: number | null    // ⭐ 父评论ID（null = 一级评论）
  replyToUserId?: number     // ⭐ 被回复的人
  replyToUserName?: string   // ⭐ 被回复的人昵称
  replyToUserAvatar?: string // ⭐ 被回复的人头像

  createTime: string           
  address: string    
  children?: Comment[] // ⭐ 可选，用于本地渲染新增评论      
}

// 回复对象 信息 传回复某个人时用到
export interface ReplyTo {
  parentId: number
  replyToUserId?: number
  replyToUserName?: string
}

// 发送评论 传给后端的参数
export interface SendCommentDTO {
  blogId: number
  content: string
  parentId: number | null    // null = 一级评论 有值就是二级评论
  replyToUserId?: number
}

// 点赞和收藏信息  查询
export interface LikeCollectInfo {
  isliked: boolean
  liked: number
  iscollected: boolean
  collection: number
}

// 点赞或收藏 传给后端的参数
export interface NoteActionDTO {
  blogId: number
  action: 'LIKE' | 'COLLECT'
}

