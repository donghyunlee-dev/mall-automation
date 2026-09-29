import client from './client';
import type { BatchSchedule, BatchRunResult } from '../types/batch';

const JOB_NAME = 'order-sync';

export const getOrderSyncSchedule = (): Promise<BatchSchedule> =>
  client
    .get<BatchSchedule>(`/api/admin/batch-schedules/${JOB_NAME}`)
    .then((r) => r.data);

// 실제 Godo API를 호출해 21일치 주문을 건별로 저장하며 동기화하는 동안 서버가 응답을 블로킹한다
// (운영 주문량 기준 수 분 소요될 수 있음) — 기본 타임아웃(10s)보다 훨씬 길게 잡는다.
const RUN_TIMEOUT_MS = 300_000;

export const runOrderSyncNow = (targetDate: string): Promise<BatchRunResult> =>
  client
    .post<BatchRunResult>(`/api/admin/batch-schedules/${JOB_NAME}/run`, null, {
      params: { targetDate },
      timeout: RUN_TIMEOUT_MS,
    })
    .then((r) => r.data);
