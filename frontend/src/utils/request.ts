import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';

/**
 * 统一请求封装。
 *
 * 后端所有接口都返回 { code, message, data }，code = 0 表示成功。
 * 这里直接把 data 返回给页面，页面就不用每次写 res.data.data。
 */
const service = axios.create({
    baseURL: '/api',
    timeout: 15000
});

service.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

service.interceptors.response.use(
    response => {
        const body = response.data;

        // 文件下载之类不是 JSON，原样返回
        if (!body || typeof body.code !== 'number') {
            return body;
        }
        if (body.code === 0) {
            return body.data;
        }
        if (body.code === 2001) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.hash = '#/login';
            return Promise.reject(new Error(body.message));
        }
        ElMessage.error(body.message || '请求失败');
        return Promise.reject(new Error(body.message));
    },
    (error: AxiosError) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.hash = '#/login';
        } else {
            ElMessage.error('网络异常，请确认后端已启动');
        }
        return Promise.reject(error);
    }
);

/** 转一层类型，避免每个页面都写 AxiosResponse 断言 */
export const http = {
    get: (url: string, params?: object) =>
        service.get(url, { params }) as unknown as Promise<any>,
    post: (url: string, data?: object, config?: AxiosRequestConfig) =>
        service.post(url, data, config) as unknown as Promise<any>,
    put: (url: string, data?: object) =>
        service.put(url, data) as unknown as Promise<any>,
    delete: (url: string) => service.delete(url) as unknown as Promise<any>
};

/**
 * 下载文件。
 * 不能直接把带 token 的地址丢给 window.open —— 网关只认 Authorization 请求头，
 * 所以必须用 axios 带着头去拿 blob，再在前端触发保存。
 */
export const downloadFile = async (url: string, fileName: string) => {
    const blob = (await service.get(url, { responseType: 'blob' })) as unknown as Blob;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(link.href);
};

/**
 * 预览：拉成 blob 后在新标签页打开。
 * 同样不能直接 window.open 带 token 的 URL，网关只认请求头。
 */
export const openBlob = async (url: string) => {
    const blob = (await service.get(url, { responseType: 'blob' })) as unknown as Blob;
    const objectUrl = URL.createObjectURL(blob);
    window.open(objectUrl, '_blank');
    // 一分钟后释放，太早释放会把还在加载的页面掐断
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
};

export default service;
