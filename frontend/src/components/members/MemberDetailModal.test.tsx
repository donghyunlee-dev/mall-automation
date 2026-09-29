import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import MemberDetailModal from './MemberDetailModal';
import useMemberDetail from '../../hooks/useMemberDetail';
import { getCommonCodes } from '../../api/commonCodes';
import { getMemberBusinessLogs, getMemberPartnerLogs } from '../../api/members';
import type { MemberDetail } from '../../types/member';
import type { CommonCodeItem } from '../../types/commonCode';

vi.mock('../../hooks/useMemberDetail');
const mockedHook = vi.mocked(useMemberDetail);

vi.mock('../../api/commonCodes');
const mockedCodes = vi.mocked(getCommonCodes);

vi.mock('../../api/members', () => ({
  getMemberBusinessLogs: vi.fn(),
  getMemberPartnerLogs: vi.fn(),
}));

const mockedBizLogs = vi.mocked(getMemberBusinessLogs);
const mockedPartnerLogs = vi.mocked(getMemberPartnerLogs);

const code = (group: string, c: string, name: string): CommonCodeItem => ({
  id: c.length + name.length,
  groupCode: group,
  groupName: group === 'SERVICE' ? '업태' : '업종',
  code: c,
  codeName: name,
  note: null,
  useYn: 'Y',
  createdAt: '',
  updatedAt: '',
});

const baseMember = (overrides: Partial<MemberDetail> = {}): MemberDetail => ({
  memNo: 12345,
  memId: 'hong123',
  memName: '홍길동',
  groupSno: 1,
  groupName: '일반회원',
  appFl: 'y',
  appDt: null,
  email: 'hong@example.com',
  cellPhone: '010-1234-5678',
  phone: null,
  address: null,
  entryDt: '2025-01-15',
  lastLoginDt: null,
  entryPath: 'pc',
  maillingFl: 'n',
  smsFl: 'n',
  nickName: null,
  sexFl: null,
  birthDt: null,
  marriFl: null,
  marriDate: null,
  job: null,
  interest: null,
  loginCount: 0,
  saleAmount: 0,
  mileage: 0,
  deposit: 0,
  recommId: null,
  ex1: null,
  ex2: null,
  ex3: null,
  ex4: null,
  ex5: null,
  ex6: null,
  memo: null,
  business: null,
  partner: null,
  ...overrides,
});

const updatePartner = vi.fn().mockResolvedValue(true);

const setHook = (member: MemberDetail | null) => {
  mockedHook.mockReturnValue({
    member,
    loading: false,
    error: null,
    notFound: false,
    updatePartner,
    partnerUpdating: false,
    updateBusiness: vi.fn().mockResolvedValue(true),
    businessUpdating: false,
    refetch: vi.fn(),
  });
};

const business = {
  company: '(주)테스트',
  businessNo: '123-45-67890',
  ceoName: '홍길동',
  serviceCode: '47',
  serviceName: '도매',
  itemCode: '01',
  itemName: '식품',
  comAddress: '서울시 강남구',
  bizLicenseFile: 'license.pdf',
  duplicateMemNo: null,
};

const partner = {
  memberStatus: 'NORMAL' as const,
  partnerCode: '990954',
  partnerName: '테스트거래처',
  taxInvoiceFl: 'y' as const,
  updatedAt: '2026-06-11T14:30:00',
};

describe('MemberDetailModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedBizLogs.mockResolvedValue([]);
    mockedPartnerLogs.mockResolvedValue([]);
    updatePartner.mockResolvedValue(true);
    // business 샘플(도매/47, 식품/01)이 유효 매핑이 되도록 기본 목록 제공
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE' ? [code('SERVICE', '47', '도매')] : [code('ITEM', '01', '식품')],
      ),
    );
  });

  it('사업자 그룹 → 사업자 정보 섹션 표시(추가 정보·사업자등록증 미표시)', () => {
    setHook(baseMember({ groupSno: 2, groupName: '사업자회원_화이트', business, ex1: '식품제조', ex2: 'Y' }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);
    expect(screen.getByText('사업자 정보')).toBeInTheDocument();
    expect(screen.queryByText('추가 정보')).not.toBeInTheDocument();
    expect(screen.queryByText('사업자등록증')).not.toBeInTheDocument();
  });

  it('business=null이어도 사업자 정보 섹션 표시(빈 폼)', () => {
    setHook(baseMember({ groupSno: 1 }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);
    expect(screen.getByText('사업자 정보')).toBeInTheDocument();
  });

  it('group_sno=9 → 에쓰푸드 거래처 정보 섹션 표시', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);
    expect(screen.getByText('에쓰푸드 거래처 정보')).toBeInTheDocument();
  });

  it('partner=null이어도 에쓰푸드 거래처 정보 섹션 표시(신규 등록 폼)', () => {
    setHook(baseMember({ groupSno: 2, groupName: '사업자회원_화이트', business, partner: null }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);
    expect(screen.getByText('에쓰푸드 거래처 정보')).toBeInTheDocument();
  });

  it('거래처 코드 미입력 + 등록됨 저장 → 검증 오류, updatePartner 미호출', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', partner: { ...partner, partnerCode: null } }));
    const onToast = vi.fn();
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={onToast} />);

    // 사업자·거래처 카드에 각각 저장 버튼이 있으므로 거래처 카드로 범위를 좁힌다
    const partnerHeader = screen.getByText('에쓰푸드 거래처 정보').parentElement!;
    fireEvent.click(within(partnerHeader).getByText('저장'));

    expect(onToast).toHaveBeenCalledWith('거래처 코드를 입력해 주세요.', 'error');
    expect(updatePartner).not.toHaveBeenCalled();
  });

  it('사업자번호 중복 → 배너는 문구만, 사업자 정보 옆 중복 회원번호 버튼 나열·클릭 시 onNavigate', () => {
    setHook(
      baseMember({
        groupSno: 9,
        groupName: 'B2B 거래처',
        business: { ...business, duplicateMemNo: '900102;900103' },
        partner,
      }),
    );
    const onNavigate = vi.fn();
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} onNavigate={onNavigate} />);

    expect(screen.getByText('⚠ 중복 가능성이 있는 회원이 있습니다')).toBeInTheDocument();
    expect(screen.queryByText('중복 회원 상세 보기 →')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '900103' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '900102' }));
    expect(onNavigate).toHaveBeenCalledWith(900102);
  });

  it('업태 코드·명이 공통코드에 없으면 빨간색(labelInvalid) 표시, 유효한 업종은 정상', async () => {
    setHook(
      baseMember({
        groupSno: 9,
        groupName: 'B2B 거래처',
        business: { ...business, serviceCode: '', serviceName: '음식점업' },
        partner,
      }),
    );
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    await waitFor(() => expect(screen.getByText('업태').className).toMatch(/labelInvalid/));
    expect(screen.getByText('업종').className).not.toMatch(/labelInvalid/);
  });

  it('× 버튼 클릭 → onClose 호출', () => {
    setHook(baseMember({ groupSno: 1 }));
    const onClose = vi.fn();
    render(<MemberDetailModal memNo={12345} onClose={onClose} onToast={vi.fn()} />);

    fireEvent.click(screen.getByLabelText('닫기'));
    expect(onClose).toHaveBeenCalled();
  });

  it('업태 코드를 수정하면 명이 즉시 비워진다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });

    expect(nameInput).toHaveValue('');
  });

  it('업태 명을 수정하면 코드가 즉시 비워진다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(nameInput, { target: { value: '소매' } });

    expect(codeInput).toHaveValue('');
  });

  it('코드·명이 그대로인데 blur만 하면 검색 모달이 열리지 않는다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    fireEvent.blur(codeInput);

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('코드 없이 명만 있는(불완전한) 상태에서 같은 명을 다시 입력하고 blur해도 검색 모달이 열린다', () => {
    setHook(
      baseMember({
        groupSno: 9,
        groupName: 'B2B 거래처',
        business: { ...business, serviceCode: '', serviceName: '일반음식' },
        partner,
      }),
    );
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(nameInput, { target: { value: '일반음식' } });
    fireEvent.blur(nameInput);

    expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toBeInTheDocument();
  });

  it('코드를 비우고 blur하면 검색 모달이 열리지 않는다', () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    fireEvent.change(codeInput, { target: { value: '' } });
    fireEvent.blur(codeInput);

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('업태 코드 변경 후 blur, 검색 결과 1건 → 코드+명 자동 채움, 모달이 닫힌다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '99', '음식점업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(nameInput).toHaveValue('음식점업'));
    expect(codeInput).toHaveValue('99');
    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
  });

  it('업태 코드 입력 후 Enter → 검색 결과 1건이면 자동으로 채워진다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '99', '음식점업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '99' } });
    fireEvent.keyDown(codeInput, { key: 'Enter' });

    await waitFor(() => expect(nameInput).toHaveValue('음식점업'));
  });

  it('업태 코드 변경 후 blur, 검색 결과 2건 이상 → 모달이 열린 채로 유지된다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '48', '도매업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');
    const nameInput = within(serviceRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '4' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toBeInTheDocument());
    expect(nameInput).toHaveValue('');
  });

  it('자동으로 열린 검색모달을 선택 없이 닫으면 입력값이 그대로 남는다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '48', '도매업')]
          : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const codeInput = within(serviceRow).getByPlaceholderText('코드');

    fireEvent.change(codeInput, { target: { value: '4' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(screen.getByText('업태 검색')).toBeInTheDocument());
    const searchModalHead = screen.getByText('업태 검색').parentElement!;
    fireEvent.click(within(searchModalHead).getByLabelText('닫기'));

    expect(screen.queryByPlaceholderText('코드 또는 명칭으로 검색')).not.toBeInTheDocument();
    expect(codeInput).toHaveValue('4');
  });

  it('한 필드의 검색모달이 열려 있는 동안 다른 필드를 blur해도 열린 모달이 유지된다', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매'), code('SERVICE', '48', '도매업')]
          : [code('ITEM', '01', '식품'), code('ITEM', '02', '음료')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    const itemRow = screen.getByText('업종').parentElement!;
    const serviceCodeInput = within(serviceRow).getByPlaceholderText('코드');
    const itemCodeInput = within(itemRow).getByPlaceholderText('코드');

    // 업태 코드를 바꿔 blur → 결과 2건 이상이라 업태 검색 모달이 열린 채로 유지됨
    fireEvent.change(serviceCodeInput, { target: { value: '4' } });
    fireEvent.blur(serviceCodeInput);
    await waitFor(() => expect(screen.getByText('업태 검색')).toBeInTheDocument());

    // 업태 검색모달이 열려 있는 동안 업종 코드를 바꿔 blur해도, 업태 모달이 업종 모달로 대체되지 않는다
    fireEvent.change(itemCodeInput, { target: { value: '0' } });
    fireEvent.blur(itemCodeInput);

    expect(screen.getByText('업태 검색')).toBeInTheDocument();
    expect(screen.queryByText('업종 검색')).not.toBeInTheDocument();
  });

  it('업종 코드 변경 후 blur, 검색 결과 1건 → 코드+명 자동 채움(업태와 동일 로직 적용 확인)', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE'
          ? [code('SERVICE', '47', '도매')]
          : [code('ITEM', '01', '식품'), code('ITEM', '02', '음료')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const itemRow = screen.getByText('업종').parentElement!;
    const codeInput = within(itemRow).getByPlaceholderText('코드');
    const nameInput = within(itemRow).getByPlaceholderText('명칭');

    fireEvent.change(codeInput, { target: { value: '02' } });
    fireEvent.blur(codeInput);

    await waitFor(() => expect(nameInput).toHaveValue('음료'));
  });

  it('수동 검색(찾기) 버튼 클릭 → 결과가 1건이어도 자동으로 닫히지 않는다(기존 수동 탐색 동작 보존)', async () => {
    mockedCodes.mockImplementation((group: string) =>
      Promise.resolve(
        group === 'SERVICE' ? [code('SERVICE', '47', '도매')] : [code('ITEM', '01', '식품')],
      ),
    );
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    const serviceRow = screen.getByText('업태').parentElement!;
    fireEvent.click(within(serviceRow).getByTitle('검색'));

    await waitFor(() => expect(screen.getByText('업태 검색')).toBeInTheDocument());
    expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toHaveValue('');
    // 결과가 1건이어도(도매 1건) 자동 선택되지 않아 모달이 계속 열려 있어야 한다
    expect(screen.getByText('업태 검색')).toBeInTheDocument();
  });

  it('상세보기를 여는 것만으로는 이력 API를 호출하지 않는다(수정이력을 눌러야 조회)', async () => {
    setHook(baseMember({ groupSno: 9, groupName: 'B2B 거래처', business, partner }));
    render(<MemberDetailModal memNo={12345} onClose={vi.fn()} onToast={vi.fn()} />);

    await waitFor(() => expect(screen.getByText('사업자 정보')).toBeInTheDocument());
    expect(mockedBizLogs).not.toHaveBeenCalled();
    expect(mockedPartnerLogs).not.toHaveBeenCalled();
  });
});
