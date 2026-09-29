import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import MemberDeleteModal from './MemberDeleteModal';
import { deleteMember } from '../../api/members';
import type { MemberListItem } from '../../types/member';

vi.mock('../../api/members', () => ({
  deleteMember: vi.fn(),
}));
const mockedDelete = vi.mocked(deleteMember);

const member = (overrides: Partial<MemberListItem> = {}): MemberListItem => ({
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

describe('MemberDeleteModal', () => {
  beforeEach(() => vi.clearAllMocks());

  it('대상 회원 정보 표시', () => {
    render(<MemberDeleteModal member={member()} onClose={vi.fn()} onDeleted={vi.fn()} onToast={vi.fn()} />);
    expect(screen.getByDisplayValue('hong123')).toBeInTheDocument();
    expect(screen.getByDisplayValue('B2B 거래처')).toBeInTheDocument();
    expect(screen.getByDisplayValue('에쓰마트')).toBeInTheDocument();
  });

  it('삭제 클릭 → deleteMember 호출 후 onDeleted·onClose·onToast(success)', async () => {
    mockedDelete.mockResolvedValue();
    const onDeleted = vi.fn();
    const onClose = vi.fn();
    const onToast = vi.fn();
    render(
      <MemberDeleteModal
        member={member({ memNo: 555 })}
        onClose={onClose}
        onDeleted={onDeleted}
        onToast={onToast}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: '삭제' }));
    await waitFor(() => expect(mockedDelete).toHaveBeenCalledWith(555));
    expect(onDeleted).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    expect(onToast).toHaveBeenCalledWith(expect.any(String), 'success');
  });

  it('삭제 실패 → 에러 메시지 표시', async () => {
    mockedDelete.mockRejectedValue(new Error('fail'));
    render(<MemberDeleteModal member={member()} onClose={vi.fn()} onDeleted={vi.fn()} onToast={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: '삭제' }));
    await waitFor(() => expect(screen.getByText(/삭제에 실패/)).toBeInTheDocument());
  });
});
