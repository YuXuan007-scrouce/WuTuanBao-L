const TOKEN_KEY = 'authorization';

/** 获取 token */
export const getToken = () => {
  console.log('从localStorage获取token:', localStorage.getItem(TOKEN_KEY)); // 添加调试日志
  return localStorage.getItem(TOKEN_KEY);
};

/** 设置 token */
export const setToken = (token: string) => {
  localStorage.setItem(TOKEN_KEY, token);
};

/** 移除 token */
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};
