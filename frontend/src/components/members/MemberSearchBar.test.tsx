import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MemberSearchBar from './MemberSearchBar';
import type { MemberSearchParams } from '../../types/member';

const baseParams = (o: Partial<MemberSearchParams> = {}): MemberSearchParams => ({
  searchType: 'MEM_ID',
  keyword: '',
  group: 'ALL',
  sortKey: 'mem_no',
  sortDir: 'desc',
  page: 1,
  size: 10,
  entryDtFrom: '',
  entryDtTo: '',
  ...o,
});

describe('MemberSearchBar 가입일 프리셋', () => {
  it('가입일 파라미터가 없으면 전체가 기본 활성', () => {
    render(<MemberSearchBar searchParams={baseParams()} onSearch={vi.fn()} />);
    const datePresets = within(screen.getByTestId('date-preset-group'));
    expect(datePresets.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('가입일 파라미터가 있으면 전체 비활성', () => {
    render(
      <MemberSearchBar
        searchParams={baseParams({ entryDtFrom: '2026-06-01', entryDtTo: '2026-06-23' })}
        onSearch={vi.fn()}
      />,
    );
    const datePresets = within(screen.getByTestId('date-preset-group'));
    expect(datePresets.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('오늘 프리셋 클릭 시 오늘이 활성, 전체는 비활성', () => {
    render(<MemberSearchBar searchParams={baseParams()} onSearch={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: '오늘' }));
    expect(screen.getByRole('button', { name: '오늘' })).toHaveAttribute('aria-pressed', 'true');
    const datePresets = within(screen.getByTestId('date-preset-group'));
    expect(datePresets.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('마운트만으로는 onSearch를 호출하지 않음 (기본 가입일은 훅에서 관리)', () => {
    const onSearch = vi.fn();
    render(<MemberSearchBar searchParams={baseParams()} onSearch={onSearch} />);
    expect(onSearch).not.toHaveBeenCalled();
  });

  it('가입일 직접 변경 시 즉시 조회한다', () => {
    const onSearch = vi.fn();
    const { container } = render(
      <MemberSearchBar searchParams={baseParams()} onSearch={onSearch} />,
    );
    const dateInputs = container.querySelectorAll('input[type="date"]');
    fireEvent.change(dateInputs[0], { target: { value: '2026-06-10' } });
    expect(onSearch).toHaveBeenCalledWith({ entryDtFrom: '2026-06-10' });

    fireEvent.change(dateInputs[1], { target: { value: '2026-06-20' } });
    expect(onSearch).toHaveBeenCalledWith({ entryDtTo: '2026-06-20' });
  });

  it('검색타입 드롭다운 변경은 즉시 조회하지 않고, 조회 버튼에서 적용', () => {
    const onSearch = vi.fn();
    render(
      <MemberSearchBar
        searchParams={baseParams({ entryDtFrom: '2026-06-01', entryDtTo: '2026-06-23' })}
        onSearch={onSearch}
      />,
    );
    fireEvent.click(screen.getByLabelText('검색 타입 선택'));
    fireEvent.click(screen.getByRole('button', { name: '거래처코드' }));
    expect(onSearch).not.toHaveBeenCalled(); // 드롭다운 변경만으로는 조회 안 함

    fireEvent.click(screen.getByRole('button', { name: '조회' }));
    expect(onSearch).toHaveBeenCalledWith(expect.objectContaining({ searchType: 'PARTNER_CODE' }));
  });
});
