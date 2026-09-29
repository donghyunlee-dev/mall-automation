/** 선택월 기준 "전년 1월 ~ 선택월"까지의 개월 수(원본: start_date_y = Timestamp(year-1, 1, 1)). */
export const twoYearMonthCount = (yearMonth: string): number => {
  const month = Number(yearMonth.split('-')[1]);
  return month + 12;
};
