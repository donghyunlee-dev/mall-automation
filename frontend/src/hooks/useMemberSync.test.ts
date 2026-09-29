import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import useMemberSync from './useMemberSync';
import { getMemberSyncSchedule, runMemberSyncNow } from '../api/memberSync';
import type { BatchSchedule } from '../types/batch';

vi.mock('../api/memberSync', () => ({
  getMemberSyncSchedule: vi.fn(),
  runMemberSyncNow: vi.fn(),
}));

const SCHEDULE: BatchSchedule = {
  jobName: 'member-sync',
  enabled: true,
  fixedDelaySeconds: 3600,
  running: false,
  lastStartedAt: '2026-09-06T00:00:00Z',
  lastEndedAt: '2026-09-06T00:01:00Z',
  lastStatus: 'SUCCESS',
  lastMessage: null,
};

describe('useMemberSync', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads the member-sync schedule on mount', async () => {
    vi.mocked(getMemberSyncSchedule).mockResolvedValue(SCHEDULE);

    const { result } = renderHook(() => useMemberSync());

    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));
  });

  it('runNow triggers the batch and reloads the schedule', async () => {
    vi.mocked(getMemberSyncSchedule).mockResolvedValue(SCHEDULE);
    vi.mocked(runMemberSyncNow).mockResolvedValue({ triggered: true, status: 'STARTED' });

    const { result } = renderHook(() => useMemberSync());
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow();

    expect(runMemberSyncNow).toHaveBeenCalled();
    await waitFor(() => expect(getMemberSyncSchedule).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.running).toBe(false));
    expect(result.current.error).toBe('');
  });

  it('sets an error message when the run request fails', async () => {
    vi.mocked(getMemberSyncSchedule).mockResolvedValue(SCHEDULE);
    vi.mocked(runMemberSyncNow).mockRejectedValue(new Error('network error'));

    const { result } = renderHook(() => useMemberSync());
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow();

    await waitFor(() => expect(result.current.error).not.toBe(''));
    expect(result.current.running).toBe(false);
  });
});
