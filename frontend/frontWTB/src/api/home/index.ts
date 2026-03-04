
import http from '@/utils/http'
import { HomeFeedParams, HomeFeedResult } from './type'

export const getHomeFeed = (params: HomeFeedParams) => {
  return http.get<HomeFeedResult>('/home/topbar', params)
}
