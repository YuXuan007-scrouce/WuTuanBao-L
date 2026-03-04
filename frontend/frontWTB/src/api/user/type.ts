// 登录
export interface loginQueryInterface {
  // 手机号码
  phone: string;
  // 	短信验证码
  code: string;
}
// 获取短信验证码
export interface SmsCodeQueryInterface {
  // 手机号码
  phone: string;
}

// 用户信息
export interface UserInfoInterface {
  // 用户id
  id: number;
   // 用户名
  nickName: string;
  // 头像
  icon: string;
}
// 用户state
export interface UserStateInterface {
  // 用户信息
  userInfo: UserInfoInterface | null;
  // token
  token: string | null;
}

// 用户详情
export interface UserDetailInterface {
  introduce: string;
  city: string;
  gender: 0 | 1;
  fans: number;
  followee: number;
}

//获取用户笔记列表参数(请求)
export interface UserNotesQuery {
  page: number;
  size: number;
}


// 用户笔记列表返回(实体)
export interface UserNote {
  id: number;
  shopId: number;
  userId: number;
  title: string;
  images: string;      // 逗号分隔
  content: string;
  liked: number;
  createTime: string;
  updateTime: string;
}