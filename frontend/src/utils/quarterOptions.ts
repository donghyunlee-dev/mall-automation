export interface QuarterValue {
  year: number;
  quarter: number;
}

const toOrdinal = ({ year, quarter }: QuarterValue): number => year * 4 + quarter;

const fromOrdinal = (ordinal: number): QuarterValue => ({
  year: Math.floor((ordinal - 1) / 4),
  quarter: ((ordinal - 1) % 4) + 1,
});

export const generateQuarterOptions = (start: QuarterValue, end: QuarterValue): QuarterValue[] => {
  const quarters: QuarterValue[] = [];
  for (let ordinal = toOrdinal(end); ordinal >= toOrdinal(start); ordinal -= 1) {
    quarters.push(fromOrdinal(ordinal));
  }
  return quarters;
};
