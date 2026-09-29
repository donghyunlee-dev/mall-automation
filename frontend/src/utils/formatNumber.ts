export const formatWon = (value: number): string => `${Math.round(value).toLocaleString('ko-KR')}원`;

export const formatCount = (value: number, unit: string): string => `${Math.round(value).toLocaleString('ko-KR')}${unit}`;

export const formatDecimal = (value: number, digits = 1): string => value.toFixed(digits);
