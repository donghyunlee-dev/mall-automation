import { describe, expect, it } from 'vitest';
import { getMemberGroupInfo, isBusiness } from './memberGroup';

describe('getMemberGroupInfo', () => {
  it('group_sno 1 → 일반회원', () => {
    expect(getMemberGroupInfo(1).label).toBe('일반회원');
  });

  it('group_sno 2 → 사업자회원_화이트', () => {
    expect(getMemberGroupInfo(2).label).toBe('사업자회원_화이트');
  });

  it('group_sno 3 → 사업자회원_로제', () => {
    expect(getMemberGroupInfo(3).label).toBe('사업자회원_로제');
  });

  it('group_sno 5 → 사업자회원_레드', () => {
    expect(getMemberGroupInfo(5).label).toBe('사업자회원_레드');
  });

  it('group_sno 6 → 사업자회원_VIP', () => {
    expect(getMemberGroupInfo(6).label).toBe('사업자회원_VIP');
  });

  it('group_sno 7 → 관리자', () => {
    expect(getMemberGroupInfo(7).label).toBe('관리자');
  });

  it('group_sno 8 → 기능장회원', () => {
    expect(getMemberGroupInfo(8).label).toBe('기능장회원');
  });

  it('group_sno 9 → B2B 거래처', () => {
    expect(getMemberGroupInfo(9).label).toBe('B2B 거래처');
  });

  it('알 수 없는 group_sno → 폴백 반환', () => {
    expect(getMemberGroupInfo(99).label).toBe('알 수 없음');
  });
});

describe('isBusiness', () => {
  it.each([2, 3, 5, 6, 9])('group_sno %i → true', (sno) => {
    expect(isBusiness(sno)).toBe(true);
  });

  it.each([1, 7, 8])('group_sno %i → false', (sno) => {
    expect(isBusiness(sno)).toBe(false);
  });
});
