import { useState, useEffect, useCallback } from 'react';
import { getBatchSchedule, runBatchNow, updateBatchSchedule } from '../api/batch';
import type { BatchSchedule, BatchScheduleUpdateBody } from '../types/batch';

interface BatchState {
  schedule: BatchSchedule | null;
  loading: boolean;
  running: boolean;
  error: string;
  lastRefreshedAt: Date | null;
}

const useBatch = () => {
  const [state, setState] = useState<BatchState>({
    schedule: null,
    loading: true,
    running: false,
    error: '',
    lastRefreshedAt: null,
  });

  const load = useCallback(async () => {
    try {
      const schedule = await getBatchSchedule();
      setState((prev) => ({
        ...prev,
        schedule,
        loading: false,
        error: '',
        lastRefreshedAt: new Date(),
      }));
    } catch {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: '배치 상태를 불러오지 못했어요.',
      }));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const runNow = useCallback(
    async (onComplete?: () => void) => {
      if (state.running) return;
      setState((prev) => ({ ...prev, running: true, error: '' }));
      try {
        await runBatchNow();
        await load();
        onComplete?.();
      } catch {
        setState((prev) => ({
          ...prev,
          running: false,
          error: '동기화 실행에 실패했어요.',
        }));
      } finally {
        setState((prev) => ({ ...prev, running: false }));
      }
    },
    [state.running, load],
  );

  const applyInterval = useCallback(
    async (minutes: number, email: string) => {
      const fixedDelaySeconds = minutes * 60;
      if (fixedDelaySeconds < 60) {
        throw new Error('최소 1분 이상 설정해야 해요.');
      }
      const body: BatchScheduleUpdateBody = {
        enabled: state.schedule?.enabled ?? true,
        fixedDelaySeconds,
        modifiedBy: email,
      };
      const updated = await updateBatchSchedule(body);
      setState((prev) => ({
        ...prev,
        schedule: updated,
        lastRefreshedAt: new Date(),
      }));
    },
    [state.schedule],
  );

  return {
    ...state,
    load,
    runNow,
    applyInterval,
  };
};

export default useBatch;
