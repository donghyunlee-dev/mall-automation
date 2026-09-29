export const daysSinceMonday = (dateStr: string): number => {
  const day = new Date(`${dateStr}T00:00:00`).getDay();
  return ((day + 6) % 7) + 1;
};
