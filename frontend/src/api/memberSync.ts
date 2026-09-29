import client from './client';
import type { BatchSchedule, BatchRunResult } from '../types/batch';

const JOB_NAME = 'member-sync';

// 실제 회원 크롤링 API를 호출하는 동안 서버가 응답을 블로킹한다(수십 초 소요) — 기본 타임아웃(10s)보다 길게 잡는다.
const RUN_TIMEOUT_MS = 120_000;

export const getMemberSyncSchedule = (): Promise<BatchSchedule> =>
  client
    .get<BatchSchedule>(`/api/admin/batch-schedules/${JOB_NAME}`)
    .then((r) => r.data);

export const runMemberSyncNow = (): Promise<BatchRunResult> =>
  client
    .post<BatchRunResult>(`/api/admin/batch-schedules/${JOB_NAME}/run`, null, { timeout: RUN_TIMEOUT_MS })
    .then((r) => r.data);
