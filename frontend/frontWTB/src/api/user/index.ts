import http from "../../utils/http";
import type {
  loginQueryInterface,
  UserNote,
  UserInfoInterface,
  UserDetailInterface ,
  UserNotesQuery
} from "./type";


/**
 * @description 登录
 * @param params
 */
export function login(params: loginQueryInterface) {
  return http.post<string>(`/app/login`, params);
}

/**
 * @description 获取短信验证码
 * @param params
 */

export function getSmsCode(phone: string) {
  return http.get("/app/login/getCode", {
    phone
  });
}

/**
 * @description 获取用户基本信息即对应后端ThreadLocal中的用户信息
 */
export function getUserInfo() {
  return http.get<UserInfoInterface>(`/app/me`);
}

/**
 * 查询用户具体详情 user_info表
 */

export function getUserDetail(uid: number) {
  return http.get<UserDetailInterface>(`/app/me/detail`, { uid });
}

/**
 * 笔记：分页查询(滚动触发)
 */
export function getUserNotes(params: UserNotesQuery) {
  return http.get<UserNote[]>('/blog2/of/me', params);
}