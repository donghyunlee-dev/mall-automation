import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import useUserNotifications from './useUserNotifications';
import {
  getCurrentUser,
  updateCurrentUserNotifications,
} from '../api/auth';
import type { CurrentUserResponse } from '../types/auth';

vi.mock('../api/auth', () => ({
  getCurrentUser: vi.fn(),
  updateCurrentUserNotifications: vi.fn(),
}));

const CURRENT_USER: CurrentUserResponse = {
  email: 'donghyun.lee@s-food.com',
  emailNotificationYn: 'N',
  teamsNotificationYn: 'N',
  lastLoginAt: '2026-04-07T00:00:00Z',
  createdAt: '2026-04-07T00:00:00Z',
  updatedAt: '2026-04-07T00:00:00Z',
};

describe('useUserNotifications', () => {
  it('loads the current user notification preferences', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue(CURRENT_USER);

    const { result } = renderHook(() => useUserNotifications());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.profile).toEqual(CURRENT_USER);
    expect(result.current.error).toBe('');
  });

  it('updates notifications and syncs with the response payload', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue(CURRENT_USER);
    vi.mocked(updateCurrentUserNotifications).mockResolvedValue({
      ...CURRENT_USER,
      emailNotificationYn: 'Y',
      teamsNotificationYn: 'N',
      updatedAt: '2026-04-07T00:05:00Z',
    });

    const { result } = renderHook(() => useUserNotifications());

    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.updateNotifications({
        emailNotificationYn: 'Y',
        teamsNotificationYn: 'N',
      });
    });

    expect(updateCurrentUserNotifications).toHaveBeenCalledWith({
      emailNotificationYn: 'Y',
      teamsNotificationYn: 'N',
    });
    expect(result.current.profile?.emailNotificationYn).toBe('Y');
    expect(result.current.error).toBe('');
  });

  it('restores the previous state and shows an error when update fails', async () => {
    vi.mocked(getCurrentUser).mockResolvedValue(CURRENT_USER);
    vi.mocked(updateCurrentUserNotifications).mockRejectedValue(new Error('boom'));

    const { result } = renderHook(() => useUserNotifications());

    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.updateNotifications({
        emailNotificationYn: 'Y',
        teamsNotificationYn: 'N',
      });
    });

    expect(result.current.profile).toEqual(CURRENT_USER);
    expect(result.current.error).toBe('알림 설정 변경에 실패했어요. 다시 시도해주세요.');
  });
});
