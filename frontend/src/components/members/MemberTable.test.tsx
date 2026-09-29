import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MemberTable from './MemberTable';
import type { MemberListItem } from '../../types/member';

const makeItem = (overrides: Partial<MemberListItem> = {}): MemberListItem => ({
  memNo: 12345,
  memId: 'hong123',
  memName: '홍*동',
  groupSno: 9,
  groupName: 'B2B 거래처',
  email: 'ho***@example.com',
  cellPhone: '010-****-5678',
  company: '(주)테스트',
  businessNo: '123-45-67890',
  ceoName: '홍대표',
  serviceCode: '47',
  serviceName: '도매',
  itemCode: '01',
  itemName: '식품',
  comAddress: '서울시 강남구',
  memberStatus: 'NORMAL' as const,
  partnerCode: 'ERP-001',
  partnerName: '에쓰마트',
  entryDt: '2025-01-15 00:00:00',
  appFl: 'y',
  ...overrides,
});

const defaultProps = {
  members: [],
  loading: false,
  error: null,
  totalElements: 0,
  page: 1,
  totalPages: 1,
  sortKey: 'mem_no',
  sortDir: 'desc' as const,
  onSort: vi.fn(),
  onPage: vi.fn(),
  onRowClick: vi.fn(),
  onReset: vi.fn(),
  selectedIds: new Set<number>(),
  onToggleRow: vi.fn(),
  onToggleAll: vi.fn(),
  onDelete: vi.fn(),
  suppressedDupIds: new Set<number>(),
  hasActiveFilter: false,
};

describe('MemberTable', () => {
  it('마스킹된 이름·이메일 표시', () => {
    render(<MemberTable {...defaultProps} members={[makeItem()]} totalElements={1} />);
    expect(screen.getByText('홍*동')).toBeInTheDocument();
    expect(screen.getByText('ho***@example.com')).toBeInTheDocument();
  });

  it('group_sno 9 → B2B 거래처 배지 표시', () => {
    render(<MemberTable {...defaultProps} members={[makeItem({ groupSno: 9 })]} totalElements={1} />);
    expect(screen.getByText('B2B 거래처')).toBeInTheDocument();
  });

  it('group_sno 2 → 사업자 배지 표시', () => {
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ groupSno: 2, groupName: '사업자회원_화이트' })]}
        totalElements={1}
      />,
    );
    expect(screen.getByText('사업자회원_화이트')).toBeInTheDocument();
  });

  it('이름·이메일 컬럼은 sortable 아님 — th에 클릭 이벤트 없음', () => {
    render(<MemberTable {...defaultProps} members={[makeItem()]} totalElements={1} />);
    // 정렬 가능한 컬럼은 ↕ 아이콘 포함
    expect(screen.getByText('회원번호')).toBeTruthy();
    // 이름 헤더는 클릭 이벤트 없이 단순 th
    const nameTh = screen.getByText('이름');
    expect(nameTh.closest('th')?.className).not.toContain('sortable');
  });

  it('수정 버튼 클릭 → onRowClick 호출', () => {
    const onRowClick = vi.fn();
    render(<MemberTable {...defaultProps} members={[makeItem()]} totalElements={1} onRowClick={onRowClick} />);
    fireEvent.click(screen.getByRole('button', { name: '수정' }));
    expect(onRowClick).toHaveBeenCalledWith(12345);
  });

  it('행 클릭만으로는 onRowClick 미호출 (수정 버튼으로만 동작)', () => {
    const onRowClick = vi.fn();
    render(<MemberTable {...defaultProps} members={[makeItem()]} totalElements={1} onRowClick={onRowClick} />);
    fireEvent.click(screen.getByText('hong123').closest('tr')!);
    expect(onRowClick).not.toHaveBeenCalled();
  });

  it('삭제 버튼 클릭 → onDelete 호출', () => {
    const onDelete = vi.fn();
    const item = makeItem({ memNo: 555 });
    render(<MemberTable {...defaultProps} members={[item]} totalElements={1} onDelete={onDelete} />);
    fireEvent.click(screen.getByRole('button', { name: '삭제' }));
    expect(onDelete).toHaveBeenCalledWith(item);
  });

  it('데이터 없음 — 안내 문구 표시', () => {
    render(<MemberTable {...defaultProps} members={[]} totalElements={0} />);
    expect(screen.getByText('아직 등록된 회원이 없습니다')).toBeInTheDocument();
    expect(screen.getByText(/우측 상단의 엑셀 업로드/)).toBeInTheDocument();
  });

  it('검색 결과 없음 — 초기화 버튼 표시', () => {
    render(<MemberTable {...defaultProps} members={[]} totalElements={100} />);
    expect(screen.getByText('검색 결과가 없습니다')).toBeInTheDocument();
    expect(screen.getByText('초기화')).toBeInTheDocument();
  });

  it('조건 검색 결과 0명 — 조건 안내 문구 + 초기화 버튼 표시', () => {
    render(<MemberTable {...defaultProps} members={[]} totalElements={0} hasActiveFilter />);
    expect(screen.getByText('조건에 맞는 회원을 찾을 수 없습니다')).toBeInTheDocument();
    expect(screen.getByText('초기화')).toBeInTheDocument();
    expect(screen.queryByText('아직 등록된 회원이 없습니다')).not.toBeInTheDocument();
  });

  it('null 값 → — 표시', () => {
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ memName: null, email: null, partnerCode: null, partnerName: null })]}
        totalElements={1}
      />,
    );
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThanOrEqual(4);
  });

  it('행 체크박스 클릭 → onToggleRow 호출, onRowClick 미호출', () => {
    const onToggleRow = vi.fn();
    const onRowClick = vi.fn();
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ memNo: 777 })]}
        totalElements={1}
        onToggleRow={onToggleRow}
        onRowClick={onRowClick}
      />,
    );
    fireEvent.click(screen.getByLabelText('hong123 선택'));
    expect(onToggleRow).toHaveBeenCalledWith(777);
    expect(onRowClick).not.toHaveBeenCalled();
  });

  it('전체 선택 체크박스 → onToggleAll(true) 호출', () => {
    const onToggleAll = vi.fn();
    render(
      <MemberTable {...defaultProps} members={[makeItem()]} totalElements={1} onToggleAll={onToggleAll} />,
    );
    fireEvent.click(screen.getByLabelText('전체 선택'));
    expect(onToggleAll).toHaveBeenCalledWith(true);
  });

  it('duplicateMemNo 있는 행 → 중복 강조(dupRow) 클래스 적용', () => {
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ memNo: 777, duplicateMemNo: '888' })]}
        totalElements={1}
      />,
    );
    const row = screen.getByText('hong123').closest('tr')!;
    expect(row.className).toMatch(/dupRow/);
  });

  it('suppressedDupIds에 포함된 중복 행 → 강조 미적용', () => {
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ memNo: 777, duplicateMemNo: '888' })]}
        totalElements={1}
        suppressedDupIds={new Set([777])}
      />,
    );
    const row = screen.getByText('hong123').closest('tr')!;
    expect(row.className).not.toMatch(/dupRow/);
  });

  it('selectedIds에 포함된 행 → 체크박스 checked', () => {
    render(
      <MemberTable
        {...defaultProps}
        members={[makeItem({ memNo: 777 })]}
        totalElements={1}
        selectedIds={new Set([777])}
      />,
    );
    expect(screen.getByLabelText('hong123 선택')).toBeChecked();
  });
});
