import { describe, expect, it } from 'vitest';
import { formatLastRun } from './formatLastRun';
import type { BatchSchedule } from '../types/batch';

const sched = (lastStartedAt: string | null): BatchSchedule => ({
  jobName: 'product-sync',
  enabled: true,
  fixedDelaySeconds: 600,
  running: false,
  lastStartedAt,
  lastEndedAt: null,
  lastStatus: 'SUCCESS',
  lastMessage: null,
});

describe('formatLastRun', () => {
  it('실행 기록 없으면 — / 실행 기록 없음', () => {
    expect(formatLastRun(null)).toEqual({ value: '—', sub: '실행 기록 없음' });
  });

  it('23시간 전(어제 오후) → 어제 라벨 + 23시간 전', () => {
    const now = new Date('2026-06-23T14:43:00');
    const r = formatLastRun(sched('2026-06-22T15:15:00'), now);
    expect(r.value).toBe('23시간 전');
    expect(r.sub).toBe('어제 PM 3:15 · 정상 완료');
  });

  it('당일이면 오늘 라벨', () => {
    const now = new Date('2026-06-23T14:43:00');
    const r = formatLastRun(sched('2026-06-23T09:05:00'), now);
    expect(r.sub).toBe('오늘 AM 9:05 · 정상 완료');
  });

  it('이틀 이상 전이면 MM-DD 라벨', () => {
    const now = new Date('2026-06-23T14:43:00');
    const r = formatLastRun(sched('2026-06-20T18:30:00'), now);
    expect(r.sub).toBe('06-20 PM 6:30 · 정상 완료');
  });

  it('1분 미만이면 방금 전', () => {
    const now = new Date('2026-06-23T14:43:30');
    const r = formatLastRun(sched('2026-06-23T14:43:00'), now);
    expect(r.value).toBe('방금 전');
  });
});
