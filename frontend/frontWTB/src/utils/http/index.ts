import axios from "axios";
import type {
  AxiosInstance,
  AxiosError,
  AxiosRequestConfig,
  AxiosResponse
} from "axios";
import { showToast, showFailToast } from "vant";
import { ResultEnum } from "../../enums/request";
import NProgress from "../progress";
import "vant/es/toast/style";
import { getToken, removeToken } from "../token";
import type { ResultData } from "./type";
import { useToLoginPage } from "../../hook/useToLoginPage";
import { isNoTokenUrl } from "./whiteList";

export const service: AxiosInstance = axios.create({
  // 判断环境设置不同的baseURL根，据环境设置请求的基础地址（开发时为 /，生产读取 env）。
  baseURL: import.meta.env.PROD ? import.meta.env.VITE_APP_BASE_URL : "/api",
  timeout: 25000
});
/**
 * @description: 请求拦截器
 * @returns {*}
 */
service.interceptors.request.use(
  config => {
    if ((config as HttpConfigProps).showNProgress) {
      NProgress.start();  // 开始进度条
    }

     console.log('请求URL:', config.url); // 调试
    console.log('请求头token:', getToken()); // 调试

    const token = getToken();
    if (token && !isNoTokenUrl(config.url)) {
      console.log('设置token到请求头:', token); // 调试
      config.headers["authorization"] = token;
      
    }
    return config;
  },
  (error: AxiosError) => {
    showFailToast(error.message);
    return Promise.reject(error);
  }
);
/**
 * @description: 响应拦截器
 * @returns {*}
 */
service.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done();        //结束进度条
    const { data } = response;
    // * 登陆失效
    if (ResultEnum.EXPIRE.includes(data.code)) {
      // 清除token
      removeToken();
      const useToLogin = useToLoginPage();
      useToLogin.showToLoginPageDialog();
      return Promise.reject(data);
    }

    if (data.code && data.code !== ResultEnum.SUCCESS) {
      showToast(data.message || ResultEnum.ERRMESSAGE);
      return Promise.reject(data);
    }
    return data;  //这会使外部使用 service.get()/service.post() 的 .then(...) 拿到的 是后端 body 而不是 AxiosResponse。
  },
  (error: AxiosError) => {
    NProgress.done();   
    // 处理 HTTP 网络错误
    let message = "";
    // HTTP 状态码
    const status = error.response?.status;
    switch (status) {
      case 400:
        message = "请求错误";
        break;
      case 401:
        message = "未授权，请登录";
        break;
      case 403:
        message = "拒绝访问";
        break;
      case 404:
        message = `请求地址出错: ${error.response?.config?.url}`;
        break;
      case 408:
        message = "请求超时";
        break;
      case 500:
        message = "服务器内部错误";
        break;
      case 501:
        message = "服务未实现";
        break;
      case 502:
        message = "网关错误";
        break;
      case 503:
        message = "服务不可用";
        break;
      case 504:
        message = "网关超时";
        break;
      case 505:
        message = "HTTP版本不受支持";
        break;
      default:
        message = "网络连接故障";
    }

    showFailToast(message);
    return Promise.reject(error);
  }
);

/**
 * @description: 导出封装的请求方法
 * @returns {*}
 */
interface HttpConfigProps extends AxiosRequestConfig {
  showNProgress?: boolean;
}

//导出的 http 对象（统一请求方法）
const http = {
  //每个方法都是泛型 <T>，返回 Promise<ResultData<T>>，方便在组件里写 http.get<User>('/user/1') 自动获得 data 的类型提示。
  get<T>(
    url: string,
    params?: object,
    config?: HttpConfigProps
  ): Promise<ResultData<T>> {
    return service.get(url, { params, ...config });
  },

  post<T>(
    url: string,
    data?: object,
    config?: HttpConfigProps
  ): Promise<ResultData<T>> {
    return service.post(url, data, config);
  },

  put<T>(
    url: string,
    data?: object,
    config?: HttpConfigProps
  ): Promise<ResultData<T>> {
    return service.put(url, data, config);
  },

  delete<T>(
    url: string,
    data?: object,
    config?: AxiosRequestConfig
  ): Promise<ResultData<T>> {
    return service.delete(url, { data, ...config });
  }
};

export default http;
