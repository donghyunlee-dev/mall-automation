export const quarterEndDate = (year: number, quarter: number): string => {
  const lastMonth = quarter * 3; // Q1->3, Q2->6, Q3->9, Q4->12
  const lastDay = new Date(year, lastMonth, 0).getDate();
  return `${year}-${String(lastMonth).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
};
