import { renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import useMembers, { clearMembersCache } from './useMembers';
import { getMembers } from '../api/members';

vi.mock('../api/members', () => ({
  getMembers: vi.fn(),
}));

const mockedGetMembers = vi.mocked(getMembers);

const emptyPage = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
};

function wrapper({ children }: { children: ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>;
}

function wrapperWithUrl(initialEntry: string) {
  return ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
  );
}

describe('useMembers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearMembersCache(); // 모듈 레벨 캐시가 테스트 간에 공유되지 않도록 초기화
  });

  it('마운트 시 기본 파라미터로 API 호출', async () => {
    mockedGetMembers.mockResolvedValue(emptyPage);

    const { result } = renderHook(() => useMembers(), { wrapper });

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(mockedGetMembers).toHaveBeenCalledWith(
      expect.objectContaining({
        searchType: undefined, // 키워드 없으면 검색타입 미전송
        group: 'ALL',
        sortKey: 'mem_no',
        sortDir: 'desc',
        page: 0, // 1-based 1 → 0-based 0
        size: 10,
      }),
    );
  });

  it('로딩 완료 후 members 설정', async () => {
    mockedGetMembers.mockResolvedValue(emptyPage);

    const { result } = renderHook(() => useMembers(), { wrapper });

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.members).not.toBeNull();
  });

  it('기본 searchParams 확인', async () => {
    mockedGetMembers.mockResolvedValue(emptyPage);

    const { result } = renderHook(() => useMembers(), { wrapper });

    expect(result.current.searchParams.searchType).toBe('MEM_ID');
    expect(result.current.searchParams.group).toBe('ALL');
    expect(result.current.searchParams.page).toBe(1);
    expect(result.current.searchParams.size).toBe(10);
  });

  it('가입일은 URL이 아닌 기본 일주일 범위로 시작 (새로고침 시 초기화)', async () => {
    mockedGetMembers.mockResolvedValue(emptyPage);

    // URL에 과거 가입일이 남아 있어도 무시하고 일주일 기본값으로 시작해야 함
    const { result } = renderHook(() => useMembers(), {
      wrapper: wrapperWithUrl('/?df=2020-01-01&dt=2020-01-02'),
    });

    const { entryDtFrom, entryDtTo } = result.current.searchParams;
    expect(entryDtFrom).not.toBe('2020-01-01'); // URL 값이 적용되지 않음
    expect(entryDtFrom).toBeTruthy();
    expect(entryDtTo).toBeTruthy();
    expect(entryDtFrom < entryDtTo).toBe(true); // 7일 범위 (from < to)
  });

  it('API 오류 시 error 상태 설정', async () => {
    mockedGetMembers.mockRejectedValue(new Error('network error'));

    const { result } = renderHook(() => useMembers(), { wrapper });

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.error).toBeTruthy();
    expect(result.current.members).toBeNull();
  });
});
