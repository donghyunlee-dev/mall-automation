import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import CommonCodeSearchModal from './CommonCodeSearchModal';
import type { CommonCodeItem } from '../../types/commonCode';

vi.mock('../../api/commonCodes');

const code = (c: string, name: string): CommonCodeItem => ({
  id: Number(c) || c.length,
  groupCode: 'SERVICE',
  groupName: '업태',
  code: c,
  codeName: name,
  note: null,
  useYn: 'Y',
  createdAt: '',
  updatedAt: '',
});

describe('CommonCodeSearchModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initialQuery가 있으면 검색창에 초기값이 채워진다', () => {
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '소매')]}
        initialQuery="47"
        onSelect={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    expect(screen.getByPlaceholderText('코드 또는 명칭으로 검색')).toHaveValue('47');
    expect(screen.getByText('도매')).toBeInTheDocument();
    expect(screen.queryByText('소매')).not.toBeInTheDocument();
  });

  it('autoSelectSingleMatch=true + 결과 1건 → onSelect 자동 호출', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '소매')]}
        initialQuery="47"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(onSelect).toHaveBeenCalledWith('47', '도매'));
  });

  it('autoSelectSingleMatch=true + 결과 0건 → onSelect 호출 안 됨, 결과 없음 표시', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매')]}
        initialQuery="존재안함"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('검색 결과가 없어요.')).toBeInTheDocument());
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('autoSelectSingleMatch=true + 결과 2건 이상 → onSelect 호출 안 됨, 목록 유지', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매'), code('48', '도매업')]}
        initialQuery="도매"
        autoSelectSingleMatch
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('도매')).toBeInTheDocument());
    expect(screen.getByText('도매업')).toBeInTheDocument();
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('autoSelectSingleMatch 미지정(기본값) + 결과 1건이어도 onSelect 자동 호출 안 됨', async () => {
    const onSelect = vi.fn();
    render(
      <CommonCodeSearchModal
        groupCode="SERVICE"
        initialItems={[code('47', '도매')]}
        initialQuery="47"
        onSelect={onSelect}
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(screen.getByText('도매')).toBeInTheDocument());
    expect(onSelect).not.toHaveBeenCalled();
  });
});
