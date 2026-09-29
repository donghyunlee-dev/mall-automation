import client from './client';
import type {
  BatchSchedule,
  BatchRunResult,
  BatchScheduleUpdateBody,
} from '../types/batch';

const JOB_NAME = 'product-sync';

export const getBatchSchedule = (): Promise<BatchSchedule> =>
  client
    .get<BatchSchedule>(`/api/admin/batch-schedules/${JOB_NAME}`)
    .then((r) => r.data);

export const runBatchNow = (): Promise<BatchRunResult> =>
  client
    .post<BatchRunResult>(`/api/admin/batch-schedules/${JOB_NAME}/run`)
    .then((r) => r.data);

export const updateBatchSchedule = (
  body: BatchScheduleUpdateBody,
): Promise<BatchSchedule> =>
  client
    .put<BatchSchedule>(`/api/admin/batch-schedules/${JOB_NAME}`, body)
    .then((r) => r.data);
