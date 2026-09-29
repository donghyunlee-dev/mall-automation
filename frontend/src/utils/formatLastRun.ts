import type { BatchSchedule } from '../types/batch';

// 실행 시각의 날짜를 오늘/어제/MM-DD 로 표기 (자정 기준 일자 차이)
export const getDayLabel = (date: Date, now: Date): string => {
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const diffDays = Math.round((startOfDay(now) - startOfDay(date)) / 86400000);
  if (diffDays <= 0) return '오늘';
  if (diffDays === 1) return '어제';
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${mm}-${dd}`;
};

export const formatLastRun = (
  schedule: BatchSchedule | null,
  now: Date = new Date(),
): { value: string; sub: string } => {
  if (!schedule?.lastStartedAt) {
    return { value: '—', sub: '실행 기록 없음' };
  }
  const date = new Date(schedule.lastStartedAt);
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);

  let value: string;
  if (diffMin < 1) value = '방금 전';
  else if (diffMin < 60) value = `${diffMin}분 전`;
  else {
    const diffHour = Math.floor(diffMin / 60);
    value = `${diffHour}시간 전`;
  }

  const ampm = date.getHours() < 12 ? 'AM' : 'PM';
  const h = date.getHours() % 12 || 12;
  const m = String(date.getMinutes()).padStart(2, '0');
  const statusLabel =
    schedule.lastStatus === 'SUCCESS'
      ? '정상 완료'
      : schedule.lastStatus === 'FAIL'
        ? '오류 발생'
        : '건너뜀';

  return {
    value,
    sub: `${getDayLabel(date, now)} ${ampm} ${h}:${m} · ${statusLabel}`,
  };
};
