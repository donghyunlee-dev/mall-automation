import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import useOrderSync from './useOrderSync';
import { getOrderSyncSchedule, runOrderSyncNow } from '../api/orderSync';
import type { BatchSchedule } from '../types/batch';

vi.mock('../api/orderSync', () => ({
  getOrderSyncSchedule: vi.fn(),
  runOrderSyncNow: vi.fn(),
}));

const SCHEDULE: BatchSchedule = {
  jobName: 'order-sync',
  enabled: true,
  fixedDelaySeconds: 3600,
  running: false,
  lastStartedAt: '2026-09-06T00:00:00Z',
  lastEndedAt: '2026-09-06T00:01:00Z',
  lastStatus: 'SUCCESS',
  lastMessage: null,
};

describe('useOrderSync', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads the order-sync schedule on mount', async () => {
    vi.mocked(getOrderSyncSchedule).mockResolvedValue(SCHEDULE);

    const { result } = renderHook(() => useOrderSync());

    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));
  });

  it('runNow triggers the batch with the given targetDate and reloads the schedule', async () => {
    vi.mocked(getOrderSyncSchedule).mockResolvedValue(SCHEDULE);
    vi.mocked(runOrderSyncNow).mockResolvedValue({ triggered: true, status: 'STARTED' });

    const { result } = renderHook(() => useOrderSync());
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow('2026-08-15');

    expect(runOrderSyncNow).toHaveBeenCalledWith('2026-08-15');
    await waitFor(() => expect(getOrderSyncSchedule).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.running).toBe(false));
    expect(result.current.error).toBe('');
  });

  it('sets an error message when the run request fails', async () => {
    vi.mocked(getOrderSyncSchedule).mockResolvedValue(SCHEDULE);
    vi.mocked(runOrderSyncNow).mockRejectedValue(new Error('network error'));

    const { result } = renderHook(() => useOrderSync());
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow('2026-08-15');

    await waitFor(() => expect(result.current.error).toBe('주문 동기화 실행에 실패했어요.'));
    expect(result.current.running).toBe(false);
  });

  it('shows a distinct "already running" message when the run is skipped because a previous run is still in progress', async () => {
    vi.mocked(getOrderSyncSchedule).mockResolvedValue(SCHEDULE);
    vi.mocked(runOrderSyncNow).mockResolvedValue({ triggered: false, status: 'SKIPPED' });

    const { result } = renderHook(() => useOrderSync());
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow('2026-08-15');

    await waitFor(() => expect(result.current.error).not.toBe(''));
    expect(result.current.error).not.toBe('주문 동기화 실행에 실패했어요.');
    expect(result.current.error).toContain('실행 중');
    expect(result.current.running).toBe(false);
  });

  it('keeps the button locked and polls until the backend actually finishes after a client timeout', async () => {
    const RUNNING_SCHEDULE: BatchSchedule = { ...SCHEDULE, running: true, lastStatus: null };
    const FINISHED_SCHEDULE: BatchSchedule = { ...SCHEDULE, running: false, lastStatus: 'SUCCESS' };

    let resolveFirstPoll!: (schedule: BatchSchedule) => void;
    const firstPoll = new Promise<BatchSchedule>((resolve) => {
      resolveFirstPoll = resolve;
    });
    let resolveSecondPoll!: (schedule: BatchSchedule) => void;
    const secondPoll = new Promise<BatchSchedule>((resolve) => {
      resolveSecondPoll = resolve;
    });

    vi.mocked(getOrderSyncSchedule)
      .mockResolvedValueOnce(SCHEDULE) // mount load
      .mockImplementationOnce(() => firstPoll) // 1st poll after timeout: still running (resolved manually below)
      .mockImplementationOnce(() => secondPoll); // 2nd poll: finished (resolved manually below)

    const timeoutError = Object.assign(new Error('timeout of 300000ms exceeded'), {
      isAxiosError: true,
      code: 'ECONNABORTED',
    });
    vi.mocked(runOrderSyncNow).mockRejectedValue(timeoutError);

    const { result } = renderHook(() => useOrderSync(5));
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    const runPromise = result.current.runNow('2026-08-15');

    // 1st poll resolves with "still running" — the button must stay locked and the
    // message must say "still running", not a failure.
    await waitFor(() => expect(getOrderSyncSchedule).toHaveBeenCalledTimes(2));
    resolveFirstPoll(RUNNING_SCHEDULE);
    await waitFor(() => expect(result.current.schedule).toEqual(RUNNING_SCHEDULE));
    expect(result.current.running).toBe(true);
    expect(result.current.error).not.toBe('');
    expect(result.current.error).not.toBe('주문 동기화 실행에 실패했어요.');

    // 2nd poll resolves with "finished" — the button unlocks and the message clears.
    await waitFor(() => expect(getOrderSyncSchedule).toHaveBeenCalledTimes(3));
    resolveSecondPoll(FINISHED_SCHEDULE);
    await runPromise;

    await waitFor(() => expect(result.current.schedule).toEqual(FINISHED_SCHEDULE));
    await waitFor(() => expect(result.current.running).toBe(false));
    expect(result.current.error).toBe('');
  });

  it('treats a gateway timeout (504 from a proxy/load balancer) the same as a client timeout, not a failure', async () => {
    const FINISHED_SCHEDULE: BatchSchedule = { ...SCHEDULE, running: false, lastStatus: 'SUCCESS' };
    vi.mocked(getOrderSyncSchedule).mockResolvedValueOnce(SCHEDULE).mockResolvedValueOnce(FINISHED_SCHEDULE);

    const gatewayTimeoutError = Object.assign(new Error('Request failed with status code 504'), {
      isAxiosError: true,
      response: { status: 504 },
    });
    vi.mocked(runOrderSyncNow).mockRejectedValue(gatewayTimeoutError);

    const { result } = renderHook(() => useOrderSync(5));
    await waitFor(() => expect(result.current.schedule).toEqual(SCHEDULE));

    await result.current.runNow('2026-08-15');

    await waitFor(() => expect(result.current.schedule).toEqual(FINISHED_SCHEDULE));
    expect(result.current.running).toBe(false);
    expect(result.current.error).toBe('');
  });
});
