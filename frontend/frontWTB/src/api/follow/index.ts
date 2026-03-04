
import http from "../../utils/http";
import { FollowFeedParams,FollowFeedResult } from "./type";

/**
 * 获取关注动态
 * 滚动分页查询
 */
export const getFollowFeed = (params: FollowFeedParams) => {
  return http.get<FollowFeedResult>('/blog2/me/follow', params)
}
