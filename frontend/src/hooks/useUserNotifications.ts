import { useCallback, useEffect, useState } from 'react';
import {
  getCurrentUser,
  updateCurrentUserNotifications,
} from '../api/auth';
import type {
  CurrentUserResponse,
  UpdateUserNotificationsBody,
} from '../types/auth';

interface UseUserNotificationsReturn {
  profile: CurrentUserResponse | null;
  loading: boolean;
  saving: boolean;
  error: string;
  reload: () => Promise<void>;
  updateNotifications: (body: UpdateUserNotificationsBody) => Promise<void>;
}

const DEFAULT_ERROR_MESSAGE = '계정 알림 설정을 불러오지 못했어요.';
const UPDATE_ERROR_MESSAGE = '알림 설정 변경에 실패했어요. 다시 시도해주세요.';

const useUserNotifications = (): UseUserNotificationsReturn => {
  const [profile, setProfile] = useState<CurrentUserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const next = await getCurrentUser();
      setProfile(next);
      setError('');
    } catch {
      setError(DEFAULT_ERROR_MESSAGE);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const updateNotifications = useCallback(
    async (body: UpdateUserNotificationsBody) => {
      if (!profile) return;

      const prevProfile = profile;
      const optimisticProfile: CurrentUserResponse = {
        ...profile,
        emailNotificationYn: body.emailNotificationYn,
        teamsNotificationYn: body.teamsNotificationYn,
      };

      setProfile(optimisticProfile);
      setSaving(true);
      setError('');

      try {
        const next = await updateCurrentUserNotifications(body);
        setProfile(next);
      } catch {
        setProfile(prevProfile);
        setError(UPDATE_ERROR_MESSAGE);
      } finally {
        setSaving(false);
      }
    },
    [profile],
  );

  return {
    profile,
    loading,
    saving,
    error,
    reload: load,
    updateNotifications,
  };
};

export default useUserNotifications;
