import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getMembers } from '../api/members';
import type { MemberPage, MemberSearchParams, MemberStatus } from '../types/member';

const DEFAULT_PARAMS: MemberSearchParams = {
  searchType: 'MEM_ID',
  keyword: '',
  group: 'ALL',
  sortKey: 'mem_no',
  sortDir: 'desc',
  page: 1,
  size: 10,
  entryDtFrom: '',
  entryDtTo: '',
};

const VALID_SEARCH_TYPES = ['MEM_ID', 'NAME', 'PARTNER_CODE', 'PARTNER_NAME', 'EMAIL', 'CELL_PHONE'] as const;
const VALID_DIRS = ['asc', 'desc'] as const;

// 조회 조건별 결과 캐시 (모듈 레벨 — 다른 화면에 다녀와도 유지)
// 방문했던 조건은 로딩 없이 즉시 표시하고 백그라운드로 재검증한다. 저장·삭제 등 변경 시 전체 무효화.
const pageCache = new Map<string, MemberPage>();

// 테스트에서 캐시 격리용
export function clearMembersCache(): void {
  pageCache.clear();
}

// 가입일 기본 범위(최근 7일). MemberSearchBar의 '일주일' 프리셋 계산과 동일해야 프리셋이 활성 표시됨
function defaultWeekRange(): { from: string; to: string } {
  const today = new Date();
  const to = today.toISOString().slice(0, 10);
  const fromDate = new Date(today);
  fromDate.setDate(fromDate.getDate() - 7);
  return { from: fromDate.toISOString().slice(0, 10), to };
}

// 가입일·거래처 등록여부·등급·키워드·페이지크기·페이지번호는 URL에 저장하지 않고 훅 상태로 관리
// (새로고침/재진입 시 항상 기본값으로 초기화)
function readParamsFromUrl(usp: URLSearchParams): Omit<MemberSearchParams, 'entryDtFrom' | 'entryDtTo' | 'memberStatus' | 'group' | 'keyword' | 'size' | 'page'> {
  const st = usp.get('st') ?? DEFAULT_PARAMS.searchType;
  const sd = usp.get('sd') ?? DEFAULT_PARAMS.sortDir;

  return {
    searchType: VALID_SEARCH_TYPES.includes(st as MemberSearchParams['searchType'])
      ? (st as MemberSearchParams['searchType'])
      : DEFAULT_PARAMS.searchType,
    sortKey: usp.get('sk') ?? DEFAULT_PARAMS.sortKey,
    sortDir: VALID_DIRS.includes(sd as MemberSearchParams['sortDir'])
      ? (sd as MemberSearchParams['sortDir'])
      : DEFAULT_PARAMS.sortDir,
  };
}

interface UseMembersReturn {
  members: MemberPage | null;
  loading: boolean;
  error: string | null;
  searchParams: MemberSearchParams;
  setSearchParams: (updates: Partial<MemberSearchParams>) => void;
  resetSearchParams: () => void;
  reload: () => void;
  refresh: () => void;
}

const useMembers = (): UseMembersReturn => {
  const [usp, setUsp] = useSearchParams();

  // 새로고침 시 URL 파라미터 전체 제거
  useEffect(() => {
    if (usp.toString()) {
      setUsp(new URLSearchParams(), { replace: true });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const [members, setMembers] = useState<MemberPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  // URL이 아닌 상태로 관리 → 새로고침 시 항상 기본값으로 복귀
  const [dateRange, setDateRange] = useState(defaultWeekRange);
  const [memberStatus, setMemberStatus] = useState<MemberStatus | undefined>(undefined);
  const [group, setGroup] = useState<MemberSearchParams['group']>('ALL');
  const [keyword, setKeyword] = useState<string>('');
  const [pageSize, setPageSize] = useState<MemberSearchParams['size']>(10);
  // 페이지 번호도 URL에 저장하지 않음 → 새로고침 시 항상 1페이지 (범위 밖 빈 페이지 방지)
  const [page, setPage] = useState(1);

  // 객체 identity 고정 — memo된 하위 컴포넌트(MemberSearchBar 등)의 불필요한 리렌더 방지
  const searchParams: MemberSearchParams = useMemo(
    () => ({
      ...readParamsFromUrl(usp),
      page,
      entryDtFrom: dateRange.from,
      entryDtTo: dateRange.to,
      memberStatus,
      group,
      keyword,
      size: pageSize,
    }),
    [usp, page, dateRange, memberStatus, group, keyword, pageSize],
  );

  // 마지막 조회 키·요청 순번 — 같은 조건 재조회는 스피너 없이 갱신, 늦게 도착한 응답은 무시
  const lastKeyRef = useRef<string | null>(null);
  const seqRef = useRef(0);

  const fetch = useCallback(async (params: MemberSearchParams) => {
    const apiParams = {
      // 키워드가 없으면 검색 자체를 생략 (PARTNER_CODE 등 빈 키워드 조회 오류 방지)
      searchType: params.keyword ? params.searchType : undefined,
      keyword: params.keyword || undefined,
      group: params.group,
      sortKey: params.sortKey,
      sortDir: params.sortDir,
      page: params.page - 1, // API는 0-based
      size: params.size,
      ...(params.entryDtFrom ? { entryDtFrom: params.entryDtFrom } : {}),
      ...(params.entryDtTo ? { entryDtTo: params.entryDtTo } : {}),
      ...(params.memberStatus ? { memberStatus: params.memberStatus } : {}),
    } as Partial<MemberSearchParams>;
    const cacheKey = JSON.stringify(apiParams);
    const cached = pageCache.get(cacheKey);
    const seq = ++seqRef.current;

    if (cached) {
      // 캐시 즉시 표시 후 백그라운드 재검증 (로딩 표시 없음)
      setMembers(cached);
      setError(null);
    } else if (lastKeyRef.current !== cacheKey) {
      setLoading(true);
      setError(null);
    } else {
      // 같은 조건 재조회(저장 후 refresh 등) — 기존 목록을 유지한 채 갱신
      setError(null);
    }
    lastKeyRef.current = cacheKey;

    try {
      const data = await getMembers(apiParams);
      if (seqRef.current !== seq) return; // 더 최신 요청이 있으면 무시
      const page: MemberPage = data;
      pageCache.set(cacheKey, page);
      // 내용이 같으면 이전 객체 유지 — 불필요한 리렌더·선택 초기화 방지
      setMembers((prev) => (prev && JSON.stringify(prev) === JSON.stringify(page) ? prev : page));
    } catch {
      if (seqRef.current === seq && !cached) setError('회원 목록을 불러오지 못했어요.');
    } finally {
      if (seqRef.current === seq) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetch(searchParams);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usp, reloadToken, dateRange, memberStatus, group, keyword, pageSize, page]);

  const setSearchParamsHelper = useCallback(
    (updates: Partial<MemberSearchParams>) => {
      // 페이지만 바뀌는 경우 URL 변경 없이 상태만 갱신
      if ('page' in updates && Object.keys(updates).length === 1) {
        setPage(updates.page ?? 1);
        return;
      }
      // URL이 아닌 상태로 관리하는 필드 처리
      if ('entryDtFrom' in updates || 'entryDtTo' in updates) {
        setDateRange((prev) => ({
          from: updates.entryDtFrom ?? prev.from,
          to: updates.entryDtTo ?? prev.to,
        }));
      }
      if ('memberStatus' in updates) {
        setMemberStatus(updates.memberStatus);
      }
      if ('group' in updates) {
        setGroup(updates.group ?? 'ALL');
      }
      if ('keyword' in updates) {
        setKeyword(updates.keyword ?? '');
      }
      if ('size' in updates) {
        setPageSize(updates.size ?? 10);
      }
      // 페이지 외 조건 변경 시 첫 페이지로
      setPage('page' in updates ? (updates.page ?? 1) : 1);
      const current = readParamsFromUrl(usp);
      const next = { ...current, ...updates };
      const newUsp = new URLSearchParams();
      if (next.searchType !== DEFAULT_PARAMS.searchType) newUsp.set('st', next.searchType);
      if (next.sortKey !== DEFAULT_PARAMS.sortKey) newUsp.set('sk', next.sortKey);
      if (next.sortDir !== DEFAULT_PARAMS.sortDir) newUsp.set('sd', next.sortDir);
      setUsp(newUsp, { replace: false });
    },
    [usp, setUsp],
  );

  const resetSearchParams = useCallback(() => {
    setDateRange(defaultWeekRange());
    setMemberStatus(undefined);
    setGroup('ALL');
    setKeyword('');
    setPageSize(10);
    setPage(1);
    setUsp(new URLSearchParams(), { replace: false });
  }, [setUsp]);

  // 검색 조건을 기본값으로 되돌리고 강제 재조회 (URL이 이미 기본값이어도 fetch 보장)
  const reload = useCallback(() => {
    pageCache.clear(); // 데이터 변경(업로드 등) — 캐시 전체 무효화
    setReloadToken((t) => t + 1);
    setDateRange(defaultWeekRange());
    setMemberStatus(undefined);
    setGroup('ALL');
    setKeyword('');
    setPageSize(10);
    setPage(1);
    setUsp(new URLSearchParams(), { replace: false });
  }, [setUsp]);

  // 현재 조회 조건을 유지한 채 재조회 (삭제 후 목록 갱신 등)
  const refresh = useCallback(() => {
    pageCache.clear(); // 데이터 변경 — 캐시 전체 무효화
    setReloadToken((t) => t + 1);
  }, []);

  return {
    members,
    loading,
    error,
    searchParams,
    setSearchParams: setSearchParamsHelper,
    resetSearchParams,
    reload,
    refresh,
  };
};

export default useMembers;
