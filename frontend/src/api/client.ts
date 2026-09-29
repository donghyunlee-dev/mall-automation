import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: true,
  timeout: 10_000,
});

// 401 수신 시 로그인으로 자동 이동 (인증 API 자체는 제외)
client.interceptors.response.use(
  (res) => res,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !error.config?.url?.includes('/api/auth/')
    ) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export default client;
