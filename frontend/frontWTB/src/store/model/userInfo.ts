import type { UserInfoInterface } from '@/api/user/type';

export const getUser = (): UserInfoInterface | null => {
  const infoStr = localStorage.getItem('userInfo');
  if (!infoStr) return null;
  try {
    return JSON.parse(infoStr);
  } catch {
    return null;
  }
};

export const setUserInfoStorage = (userInfo: UserInfoInterface) => {
  localStorage.setItem('userInfo', JSON.stringify(userInfo));
};

export const removeUserInfoStorage = () => {
  localStorage.removeItem('userInfo');
};
