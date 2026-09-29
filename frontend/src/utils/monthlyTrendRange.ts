export interface MonthlyTrendRange {
  start: string;
  end: string;
  count: number;
}

export const monthEndDate = (yearMonth: string): string => {
  const [year, month] = yearMonth.split('-').map(Number);
  const lastDay = new Date(year, month, 0).getDate();
  return `${yearMonth}-${String(lastDay).padStart(2, '0')}`;
};

export const monthlyTrendRange = (yearMonth: string, referenceDate: string): MonthlyTrendRange => {
  const start = `${yearMonth}-01`;
  const monthEnd = monthEndDate(yearMonth);
  const end = referenceDate < monthEnd ? referenceDate : monthEnd;

  const startMs = new Date(`${start}T00:00:00`).getTime();
  const endMs = new Date(`${end}T00:00:00`).getTime();
  const count = Math.round((endMs - startMs) / 86_400_000) + 1;

  return { start, end, count };
};
