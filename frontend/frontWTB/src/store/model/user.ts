import { defineStore } from 'pinia'
import { getUserInfo,login } from '../../api/user/index'
import { setToken,removeToken,getToken } from '../../utils/token'
import { getUser,setUserInfoStorage,removeUserInfoStorage } from '../model/userInfo';

import type {
  loginQueryInterface,
  UserInfoInterface,
  UserStateInterface
} from '../../api/user/type';

export const useUserStore = defineStore('app-user', {
  state: (): UserStateInterface => ({
    token: getToken(),         //初始化时从 localStorage 取
    userInfo: getUser()
  }),

  actions: {
    setToken(token: string) {
      this.token = token;
      setToken(token);
    },

    async LoginAction(params: loginQueryInterface) {
      const { data } = await login(params);
      console.log("登录获取的token:",data);

      // 去掉 Bearer 前缀
      const token = data.startsWith('Bearer ') ? data.slice(7) : data;

      this.setToken(token);
     
      await this.GetInfoAction();
    },

    setUserInfo(userInfo: UserInfoInterface) {
      this.userInfo = userInfo;
      setUserInfoStorage(userInfo);
    },

    async GetInfoAction() {
      const { data } = await getUserInfo();
      this.setUserInfo(data);
    },

    async Logout() {
      this.resetUserStore();
      removeToken();
      removeUserInfoStorage();
    },

    resetUserStore() {
      this.token = null;
      this.userInfo = null;
    }
  },

  persist: true
});

