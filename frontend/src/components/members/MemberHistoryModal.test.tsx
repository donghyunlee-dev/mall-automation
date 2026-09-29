import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import MemberHistoryModal from './MemberHistoryModal';
import { getMemberBusinessLogs, getMemberPartnerLogs } from '../../api/members';
import type { MemberBusinessLogItem, MemberPartnerLogItem } from '../../types/member';

vi.mock('../../api/members', () => ({
  getMemberBusinessLogs: vi.fn(),
  getMemberPartnerLogs: vi.fn(),
}));

const mockedBizLogs = vi.mocked(getMemberBusinessLogs);
const mockedPartnerLogs = vi.mocked(getMemberPartnerLogs);

const bizLog = (overrides: Partial<MemberBusinessLogItem> = {}): MemberBusinessLogItem => ({
  id: 1,
  memNo: 12345,
  company: '(주)테스트',
  businessNo: '123-45-67890',
  ceoName: '홍길동',
  serviceName: '도매',
  serviceCode: '47',
  itemName: '식품',
  itemCode: '01',
  comAddress: '서울시 강남구',
  bizLicenseFile: null,
  loggedAt: '2026-01-01T00:00:00',
  loggedBy: 'admin@s-food.com',
  ...overrides,
});

const partnerLog = (overrides: Partial<MemberPartnerLogItem> = {}): MemberPartnerLogItem => ({
  id: 1,
  memNo: 12345,
  partnerCode: '990954',
  partnerName: '(에쓰푸드몰)테스트거래처',
  taxInvoiceFl: 'y',
  memberStatus: 'NORMAL',
  loggedAt: '2026-01-01T00:00:00',
  loggedBy: 'admin@s-food.com',
  ...overrides,
});

describe('MemberHistoryModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('kind가 null이면 이력을 조회하지 않는다', () => {
    render(<MemberHistoryModal kind={null} memNo={12345} onClose={vi.fn()} />);

    expect(mockedBizLogs).not.toHaveBeenCalled();
    expect(mockedPartnerLogs).not.toHaveBeenCalled();
  });

  it('BUSINESS로 열리면 getMemberBusinessLogs를 1번만 호출하고 결과를 표시한다', async () => {
    mockedBizLogs.mockResolvedValue([bizLog()]);

    render(<MemberHistoryModal kind="BUSINESS" memNo={12345} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getAllByText('(주)테스트').length).toBeGreaterThan(0));
    expect(mockedBizLogs).toHaveBeenCalledTimes(1);
    expect(mockedBizLogs).toHaveBeenCalledWith(12345);
    expect(mockedPartnerLogs).not.toHaveBeenCalled();
  });

  it('PARTNER로 열리면 getMemberPartnerLogs를 1번만 호출하고 결과를 표시한다', async () => {
    mockedPartnerLogs.mockResolvedValue([partnerLog()]);

    render(<MemberHistoryModal kind="PARTNER" memNo={12345} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getAllByText('(에쓰푸드몰)테스트거래처').length).toBeGreaterThan(0));
    expect(mockedPartnerLogs).toHaveBeenCalledTimes(1);
    expect(mockedPartnerLogs).toHaveBeenCalledWith(12345);
    expect(mockedBizLogs).not.toHaveBeenCalled();
  });

  it('이력 중 loggedAt이 null인 항목이 있어도 오류 없이 렌더링된다', async () => {
    mockedBizLogs.mockResolvedValue([
      bizLog({ id: 1, loggedAt: '2026-01-01T00:00:00' }),
      bizLog({ id: 2, loggedAt: null as unknown as string }),
      bizLog({ id: 3, loggedAt: '2026-03-01T00:00:00' }),
      bizLog({ id: 4, loggedAt: '2026-02-01T00:00:00' }),
    ]);

    render(<MemberHistoryModal kind="BUSINESS" memNo={12345} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getAllByText('(주)테스트').length).toBeGreaterThan(0));
  });

  it('조회 실패 시 에러 메시지를 표시한다', async () => {
    mockedBizLogs.mockRejectedValue(new Error('network error'));

    render(<MemberHistoryModal kind="BUSINESS" memNo={12345} onClose={vi.fn()} />);

    await waitFor(() => expect(screen.getByText('이력을 불러오지 못했어요.')).toBeInTheDocument());
  });
});
