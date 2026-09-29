import { useCallback, useState } from 'react';
import axios from 'axios';
import { logout } from '../api/auth';

const useLogout = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const clearError = useCallback(() => {
    setError('');
  }, []);

  const runLogout = useCallback(async () => {
    if (loading) return;

    setLoading(true);
    setError('');

    try {
      await logout();
      window.location.href = '/login';
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        window.location.href = '/login';
        return;
      }

      setError('로그아웃에 실패했어요. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  }, [loading]);

  return {
    loading,
    error,
    clearError,
    logout: runLogout,
  };
};

export default useLogout;
