export interface MemberGroupInfo {
  label: string;
  badgeBg: string;
  badgeText: string;
}

const GROUP_MAP: Record<number, MemberGroupInfo> = {
  1: { label: '일반회원', badgeBg: 'var(--surface3)', badgeText: 'var(--text2)' },
  2: { label: '사업자회원_화이트', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  3: { label: '사업자회원_로제', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  5: { label: '사업자회원_레드', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  6: { label: '사업자회원_VIP', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  7: { label: '관리자', badgeBg: '#fef3c7', badgeText: '#92400e' },
  8: { label: '기능장회원', badgeBg: 'var(--surface3)', badgeText: 'var(--text2)' },
  9: { label: 'B2B 거래처', badgeBg: 'var(--purple-bg)', badgeText: 'var(--purple)' },
};

const FALLBACK: MemberGroupInfo = {
  label: '알 수 없음',
  badgeBg: 'var(--surface3)',
  badgeText: 'var(--text2)',
};

export function getMemberGroupInfo(groupSno: number): MemberGroupInfo {
  return GROUP_MAP[groupSno] ?? FALLBACK;
}

export function isBusiness(groupSno: number): boolean {
  return [2, 3, 5, 6, 9].includes(groupSno);
}
