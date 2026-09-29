import axios from 'axios';
import { useCallback, useEffect, useRef, useState } from 'react';
import { getOrderSyncSchedule, runOrderSyncNow } from '../api/orderSync';
import type { BatchSchedule } from '../types/batch';

interface OrderSyncState {
  schedule: BatchSchedule | null;
  loading: boolean;
  running: boolean;
  error: string;
}

const POLL_INTERVAL_MS = 5_000;
const WAITING_MESSAGE = '서버에서 계속 실행 중이에요. 완료될 때까지 버튼을 잠가두고 기다릴게요.';
// ALB 등 중간 게이트웨이의 idle timeout이 클라이언트 타임아웃보다 먼저 연결을 끊으면
// 502/503/504로 응답한다 — 이 경우도 axios 자체 타임아웃(ECONNABORTED)과 동일하게
// "서버가 계속 실행 중일 수 있다"로 취급한다.
const GATEWAY_TIMEOUT_STATUSES = [502, 503, 504];

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const shouldWaitForBackend = (error: unknown): boolean => {
  if (!axios.isAxiosError(error)) {
    return false;
  }
  if (error.code === 'ECONNABORTED') {
    return true;
  }
  const status = error.response?.status;
  return status !== undefined && GATEWAY_TIMEOUT_STATUSES.includes(status);
};

const useOrderSync = (pollIntervalMs: number = POLL_INTERVAL_MS) => {
  const [state, setState] = useState<OrderSyncState>({
    schedule: null,
    loading: true,
    running: false,
    error: '',
  });
  const unmountedRef = useRef(false);

  useEffect(() => {
    // StrictMode의 개발 모드 이중 마운트(mount→cleanup→mount)에도 실제 마운트 상태를
    // 정확히 반영하도록 마운트 시점에 명시적으로 false로 되돌린다.
    unmountedRef.current = false;
    return () => {
      unmountedRef.current = true;
    };
  }, []);

  const load = useCallback(async () => {
    try {
      const schedule = await getOrderSyncSchedule();
      setState((prev) => ({ ...prev, schedule, loading: false, error: '' }));
    } catch {
      setState((prev) => ({ ...prev, loading: false, error: '동기화 상태를 불러오지 못했어요.' }));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  /** 클라이언트/게이트웨이 타임아웃 후에도 실제로 서버 작업이 끝날 때까지 버튼을 잠가둔 채 상태를 폴링한다. */
  const waitUntilBackendFinishes = useCallback(async () => {
    while (!unmountedRef.current) {
      let schedule: BatchSchedule;
      try {
        schedule = await getOrderSyncSchedule();
      } catch {
        return;
      }
      if (unmountedRef.current) {
        return;
      }
      if (!schedule.running) {
        setState((prev) => ({ ...prev, schedule, error: '' }));
        return;
      }
      setState((prev) => ({ ...prev, schedule, error: WAITING_MESSAGE }));
      await wait(pollIntervalMs);
    }
  }, [pollIntervalMs]);

  const runNow = useCallback(
    async (targetDate: string) => {
      setState((prev) => ({ ...prev, running: true, error: '' }));
      try {
        const result = await runOrderSyncNow(targetDate);
        await load();
        if (!result.triggered) {
          // 실패가 아니라 이전 실행이 아직 끝나지 않아 건너뛴 것이다 — 실패와 다르게 안내한다.
          setState((prev) => ({ ...prev, error: '이미 실행 중이에요. 완료될 때까지 잠시 기다려주세요.' }));
        }
      } catch (error) {
        // 클라이언트 타임아웃이나 중간 게이트웨이의 타임아웃(502/503/504)은 서버가 계속
        // 실행 중일 뿐 실패가 아닐 수 있다 — 실제로 끝날 때까지 버튼을 잠가두고 폴링한다.
        if (shouldWaitForBackend(error)) {
          await waitUntilBackendFinishes();
        } else {
          setState((prev) => ({ ...prev, error: '주문 동기화 실행에 실패했어요.' }));
        }
      } finally {
        if (!unmountedRef.current) {
          setState((prev) => ({ ...prev, running: false }));
        }
      }
    },
    [load, waitUntilBackendFinishes],
  );

  return { ...state, load, runNow };
};

export default useOrderSync;
