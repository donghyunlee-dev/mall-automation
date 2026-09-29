export const generateMonthOptions = (startYearMonth: string, endYearMonth: string): string[] => {
  const [startYear, startMonth] = startYearMonth.split('-').map(Number);
  const [endYear, endMonth] = endYearMonth.split('-').map(Number);

  const months: string[] = [];
  let year = endYear;
  let month = endMonth;

  while (year > startYear || (year === startYear && month >= startMonth)) {
    months.push(`${year}-${String(month).padStart(2, '0')}`);
    month -= 1;
    if (month === 0) {
      month = 12;
      year -= 1;
    }
  }

  return months;
};
