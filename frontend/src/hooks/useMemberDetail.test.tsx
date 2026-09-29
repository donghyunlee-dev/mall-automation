import { renderHook, waitFor, act } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import useMemberDetail from './useMemberDetail';
import { getMemberDetail, updateMemberPartner } from '../api/members';
import type { MemberDetail } from '../types/member';

vi.mock('../api/members', () => ({
  getMemberDetail: vi.fn(),
  updateMemberPartner: vi.fn(),
}));

const mockedGet = vi.mocked(getMemberDetail);
const mockedUpdate = vi.mocked(updateMemberPartner);

const axiosError = (status: number) =>
  Object.assign(new Error('http error'), { isAxiosError: true, response: { status } });

const sample: MemberDetail = {
  memNo: 12345,
  memId: 'hong123',
  memName: '홍길동',
  groupSno: 9,
  groupName: 'B2B 거래처',
  appFl: 'y',
  appDt: '2025-01-20',
  email: 'hong@example.com',
  cellPhone: '010-1234-5678',
  phone: '02-123-4567',
  address: '서울시 강남구',
  entryDt: '2025-01-15',
  lastLoginDt: '2026-06-01',
  entryPath: 'pc',
  maillingFl: 'y',
  smsFl: 'y',
  nickName: null,
  sexFl: 'm',
  birthDt: null,
  marriFl: null,
  marriDate: null,
  job: null,
  interest: null,
  loginCount: 42,
  saleAmount: 0,
  mileage: 0,
  deposit: 0,
  recommId: null,
  ex1: '식품제조',
  ex2: 'Y',
  ex3: null,
  ex4: null,
  ex5: null,
  ex6: null,
  memo: null,
  business: null,
  partner: {
    memberStatus: 'NORMAL' as const,
    partnerCode: '990954',
    partnerName: '테스트거래처',
    taxInvoiceFl: 'y' as const,
    updatedAt: '2026-06-11T14:30:00',
  },
};

describe('useMemberDetail', () => {
  beforeEach(() => vi.clearAllMocks());

  it('memNo 주어지면 getMemberDetail 호출 후 member 설정', async () => {
    mockedGet.mockResolvedValue(sample);

    const { result } = renderHook(() => useMemberDetail(12345));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mockedGet).toHaveBeenCalledWith(12345);
    expect(result.current.member?.memId).toBe('hong123');
  });

  it('memNo null이면 호출하지 않음', async () => {
    const { result } = renderHook(() => useMemberDetail(null));

    expect(mockedGet).not.toHaveBeenCalled();
    expect(result.current.member).toBeNull();
  });

  it('404 응답 → notFound true', async () => {
    mockedGet.mockRejectedValue(axiosError(404));

    const { result } = renderHook(() => useMemberDetail(999));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.notFound).toBe(true);
  });

  it('updatePartner 성공 → 저장 후 refetch (getMemberDetail 재호출)', async () => {
    mockedGet.mockResolvedValue(sample);
    mockedUpdate.mockResolvedValue({
      memberStatus: 'NORMAL' as const,
      partnerCode: '990954',
      partnerName: '테스트거래처',
      taxInvoiceFl: 'y' as const,
      bizLicenseFile: null,
      updatedAt: '2026-06-23T09:00:00',
    });

    const { result } = renderHook(() => useMemberDetail(12345));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mockedGet).toHaveBeenCalledTimes(1);

    let ok = false;
    await act(async () => {
      ok = await result.current.updatePartner({
        memberStatus: 'NORMAL' as const,
        partnerCode: '990954',
        partnerName: '테스트거래처',
        taxInvoiceFl: 'y',
        bizLicenseFile: null,
      });
    });

    expect(ok).toBe(true);
    expect(mockedUpdate).toHaveBeenCalledWith(12345, expect.objectContaining({ partnerCode: '990954' }));
    expect(mockedGet).toHaveBeenCalledTimes(2); // refetch
  });

  it('updatePartner 404(회원 없음) → "회원을 찾을 수 없어요" 메시지로 onPartnerError 호출', async () => {
    mockedGet.mockResolvedValue(sample);
    mockedUpdate.mockRejectedValue(axiosError(404));
    const onErr = vi.fn();

    const { result } = renderHook(() => useMemberDetail(12345, onErr));
    await waitFor(() => expect(result.current.loading).toBe(false));

    let ok = true;
    await act(async () => {
      ok = await result.current.updatePartner({
        memberStatus: 'NORMAL' as const,
        partnerCode: '990954',
        partnerName: null,
        taxInvoiceFl: 'n',
        bizLicenseFile: null,
      });
    });

    expect(ok).toBe(false);
    expect(onErr).toHaveBeenCalledWith('회원을 찾을 수 없어요. 목록에서 다시 확인해 주세요.');
  });

  it('updatePartner 409(거래처 코드 중복) → 중복 안내 메시지로 onPartnerError 호출', async () => {
    mockedGet.mockResolvedValue(sample);
    mockedUpdate.mockRejectedValue(axiosError(409));
    const onErr = vi.fn();

    const { result } = renderHook(() => useMemberDetail(12345, onErr));
    await waitFor(() => expect(result.current.loading).toBe(false));

    let ok = true;
    await act(async () => {
      ok = await result.current.updatePartner({
        memberStatus: 'NORMAL' as const,
        partnerCode: '990954',
        partnerName: null,
        taxInvoiceFl: 'n',
        bizLicenseFile: null,
      });
    });

    expect(ok).toBe(false);
    expect(onErr).toHaveBeenCalledWith('이미 다른 회원이 사용 중인 거래처 코드예요. 다른 코드를 입력해 주세요.');
  });

  it('updatePartner 400(상태 값 오류) → 상태 값 오류 메시지로 onPartnerError 호출', async () => {
    mockedGet.mockResolvedValue(sample);
    mockedUpdate.mockRejectedValue(axiosError(400));
    const onErr = vi.fn();

    const { result } = renderHook(() => useMemberDetail(12345, onErr));
    await waitFor(() => expect(result.current.loading).toBe(false));

    let ok = true;
    await act(async () => {
      ok = await result.current.updatePartner({
        memberStatus: 'NORMAL' as const,
        partnerCode: '990954',
        partnerName: null,
        taxInvoiceFl: 'n',
        bizLicenseFile: null,
      });
    });

    expect(ok).toBe(false);
    expect(onErr).toHaveBeenCalledWith('거래처 상태 값이 올바르지 않아요.');
  });

  it('updatePartner 500 등 그 외 오류 → 기본 안내 메시지로 onPartnerError 호출', async () => {
    mockedGet.mockResolvedValue(sample);
    mockedUpdate.mockRejectedValue(axiosError(500));
    const onErr = vi.fn();

    const { result } = renderHook(() => useMemberDetail(12345, onErr));
    await waitFor(() => expect(result.current.loading).toBe(false));

    let ok = true;
    await act(async () => {
      ok = await result.current.updatePartner({
        memberStatus: 'NORMAL' as const,
        partnerCode: '990954',
        partnerName: null,
        taxInvoiceFl: 'n',
        bizLicenseFile: null,
      });
    });

    expect(ok).toBe(false);
    expect(onErr).toHaveBeenCalledWith('거래처 정보 저장에 실패했어요. 다시 시도해 주세요.');
  });
});
