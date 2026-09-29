import { useCallback, useEffect, useState } from 'react';
import { getMemberSyncSchedule, runMemberSyncNow } from '../api/memberSync';
import type { BatchSchedule } from '../types/batch';

interface MemberSyncState {
  schedule: BatchSchedule | null;
  loading: boolean;
  running: boolean;
  error: string;
}

const useMemberSync = () => {
  const [state, setState] = useState<MemberSyncState>({
    schedule: null,
    loading: true,
    running: false,
    error: '',
  });

  const load = useCallback(async () => {
    try {
      const schedule = await getMemberSyncSchedule();
      setState((prev) => ({ ...prev, schedule, loading: false, error: '' }));
    } catch {
      setState((prev) => ({ ...prev, loading: false, error: '동기화 상태를 불러오지 못했어요.' }));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const runNow = useCallback(async () => {
    setState((prev) => ({ ...prev, running: true, error: '' }));
    try {
      await runMemberSyncNow();
      await load();
    } catch {
      setState((prev) => ({ ...prev, error: '회원 수집 실행에 실패했어요.' }));
    } finally {
      setState((prev) => ({ ...prev, running: false }));
    }
  }, [load]);

  return { ...state, load, runNow };
};

export default useMemberSync;
