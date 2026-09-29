import { memo, useEffect, useRef, useState } from 'react';
import styles from './MemberSearchBar.module.scss';
import type { MemberSearchParams, MemberStatus } from '../../types/member';

type SearchType = MemberSearchParams['searchType'];
type Group = MemberSearchParams['group'];
type PageSize = MemberSearchParams['size'];

const SEARCH_TYPE_OPTIONS: Array<{ value: SearchType; label: string }> = [
  { value: 'MEM_ID', label: '아이디' },
  { value: 'NAME', label: '이름' },
  { value: 'PARTNER_CODE', label: '거래처코드' },
  { value: 'PARTNER_NAME', label: '거래처명' },
  { value: 'EMAIL', label: '이메일' },
  { value: 'CELL_PHONE', label: '휴대폰' },
];

const GROUP_OPTIONS: Array<{ value: Group; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'GENERAL', label: '일반' },
  { value: 'BUSINESS', label: '사업자' },
  { value: 'SKILLED', label: '기능장' },
  { value: 'B2B', label: 'B2B' },
];

const PAGE_SIZE_OPTIONS: PageSize[] = [10, 20, 30, 50];

const PARTNER_STATUS_OPTIONS: Array<{ value: MemberStatus | undefined; label: string }> = [
  { value: undefined, label: '전체' },
  { value: 'NORMAL', label: '등록' },
  { value: 'UNREGISTERED', label: '미등록' },
  { value: 'INACTIVE', label: '불용' },
];

type Preset = '전체' | '오늘' | '일주일' | '1개월' | '이번달' | '지난달';

const PRESETS: Preset[] = ['오늘', '일주일', '1개월', '이번달', '지난달', '전체'];

function toDateStr(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function calcPresetDates(preset: Preset): { from: string; to: string } {
  const today = new Date();
  const todayStr = toDateStr(today);
  if (preset === '전체') return { from: '', to: '' };
  if (preset === '오늘') return { from: todayStr, to: todayStr };
  if (preset === '일주일') {
    const from = new Date(today);
    from.setDate(from.getDate() - 7);
    return { from: toDateStr(from), to: todayStr };
  }
  if (preset === '1개월') {
    const from = new Date(today);
    from.setDate(from.getDate() - 30);
    return { from: toDateStr(from), to: todayStr };
  }
  if (preset === '이번달') {
    const from = new Date(today.getFullYear(), today.getMonth(), 1);
    return { from: toDateStr(from), to: todayStr };
  }
  // 지난달
  const firstOfLastMonth = new Date(today.getFullYear(), today.getMonth() - 1, 1);
  const lastOfLastMonth = new Date(today.getFullYear(), today.getMonth(), 0);
  return { from: toDateStr(firstOfLastMonth), to: toDateStr(lastOfLastMonth) };
}

// 현재 날짜값과 일치하는 프리셋을 찾는다 (없으면 null = 직접 입력)
function matchPreset(from: string, to: string): Preset | null {
  if (!from && !to) return '전체';
  for (const p of PRESETS) {
    if (p === '전체') continue;
    const d = calcPresetDates(p);
    if (d.from === from && d.to === to) return p;
  }
  return null;
}

interface Props {
  searchParams: MemberSearchParams;
  onSearch: (params: Partial<MemberSearchParams>) => void;
}

const MemberSearchBar = ({ searchParams, onSearch }: Props) => {
  const [keyword, setKeyword] = useState(searchParams.keyword);
  const [searchType, setSearchType] = useState(searchParams.searchType);
  const [dateFrom, setDateFrom] = useState(searchParams.entryDtFrom);
  const [dateTo, setDateTo] = useState(searchParams.entryDtTo);

  const [groupOpen, setGroupOpen] = useState(false);
  const [stypeOpen, setStypeOpen] = useState(false);
  const [pageSizeOpen, setPageSizeOpen] = useState(false);

  const groupRef = useRef<HTMLDivElement>(null);
  const stypeRef = useRef<HTMLDivElement>(null);
  const pageSizeRef = useRef<HTMLDivElement>(null);

  // 키워드·검색타입과 날짜를 분리해서 동기화 — 날짜 변경이 키워드를 덮어쓰지 않도록 방지
  const [syncedSearch, setSyncedSearch] = useState({ kw: searchParams.keyword, st: searchParams.searchType });
  if (syncedSearch.kw !== searchParams.keyword || syncedSearch.st !== searchParams.searchType) {
    setSyncedSearch({ kw: searchParams.keyword, st: searchParams.searchType });
    setKeyword(searchParams.keyword);
    setSearchType(searchParams.searchType);
  }

  const [syncedDate, setSyncedDate] = useState({ df: searchParams.entryDtFrom, dt: searchParams.entryDtTo });
  if (syncedDate.df !== searchParams.entryDtFrom || syncedDate.dt !== searchParams.entryDtTo) {
    setSyncedDate({ df: searchParams.entryDtFrom, dt: searchParams.entryDtTo });
    setDateFrom(searchParams.entryDtFrom);
    setDateTo(searchParams.entryDtTo);
  }

  // 활성 프리셋은 현재 날짜값에서 파생 (별도 state 미사용)
  const activePreset = matchPreset(dateFrom, dateTo);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!groupRef.current?.contains(e.target as Node)) setGroupOpen(false);
      if (!stypeRef.current?.contains(e.target as Node)) setStypeOpen(false);
      if (!pageSizeRef.current?.contains(e.target as Node)) setPageSizeOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSubmit = () => {
    onSearch({ searchType, keyword, entryDtFrom: dateFrom, entryDtTo: dateTo });
  };

  const handlePreset = (preset: Preset) => {
    const { from, to } = calcPresetDates(preset);
    setDateFrom(from);
    setDateTo(to);
    onSearch({ entryDtFrom: from, entryDtTo: to });
  };

  const handleDateFromChange = (v: string) => {
    setDateFrom(v);
    onSearch({ entryDtFrom: v });
  };

  const handleDateToChange = (v: string) => {
    setDateTo(v);
    onSearch({ entryDtTo: v });
  };

  const selectedGroup = GROUP_OPTIONS.find((o) => o.value === searchParams.group);
  const selectedStype = SEARCH_TYPE_OPTIONS.find((o) => o.value === searchType);
  const placeholder = searchType === 'MEM_ID' ? '' : '정확히 일치하는 값 입력';

  return (
    <>
      {/* Row 1: 가입일 */}
      <div className={styles.searchRow}>
        <span className={styles.flbl}>가입일</span>
        <input
          type="date"
          className={styles.dateInput}
          value={dateFrom}
          onChange={(e) => handleDateFromChange(e.target.value)}
        />
        <span className={styles.dateSep}>~</span>
        <input
          type="date"
          className={styles.dateInput}
          value={dateTo}
          onChange={(e) => handleDateToChange(e.target.value)}
        />
        <div className={styles.presetGroup} data-testid="date-preset-group">
          {PRESETS.map((p) => (
            <button
              key={p}
              type="button"
              aria-pressed={activePreset === p}
              className={[styles.presetBtn, activePreset === p ? styles.presetActive : ''].join(' ')}
              onClick={() => handlePreset(p)}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      {/* Row 2: 등급 + 거래처 등록 + 검색 */}
      <div className={styles.searchRow}>
        <span className={styles.flbl}>등급</span>

        {/* 등급 드롭다운 */}
        <div className={styles.ddWrap} ref={groupRef}>
          <button
            type="button"
            className={[styles.ddBtn, styles.ddBtnGroup].join(' ')}
            onClick={() => setGroupOpen((v) => !v)}
            aria-label="등급 선택"
          >
            <span>{selectedGroup?.label ?? '전체'}</span>
            <span className={styles.ddCaret}>▾</span>
          </button>
          {groupOpen && (
            <div className={styles.ddMenu}>
              {GROUP_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  className={[
                    styles.ddOpt,
                    searchParams.group === opt.value ? styles.ddOptSelected : '',
                  ].join(' ')}
                  onClick={() => {
                    onSearch({ group: opt.value });
                    setGroupOpen(false);
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className={styles.sep} />

        <span className={styles.flbl}>거래처 상태</span>
        <div className={styles.presetGroup}>
          {PARTNER_STATUS_OPTIONS.map(({ value, label }) => {
            const isActive = searchParams.memberStatus === value;
            const activeClass = value === undefined ? styles.pFlAllActive
              : value === 'NORMAL' ? styles.pFlOnActive
              : value === 'INACTIVE' ? styles.pFlNegActive
              : styles.pFlOffActive;
            return (
              <button
                key={label}
                type="button"
                aria-pressed={isActive}
                className={[styles.presetBtn, isActive ? activeClass : ''].join(' ')}
                onClick={() => onSearch({ memberStatus: value })}
              >
                {label}
              </button>
            );
          })}
        </div>

        <div className={styles.sep} />

        {/* 검색 타입 드롭다운 */}
        <div className={styles.ddWrap} ref={stypeRef}>
          <button
            type="button"
            className={[styles.ddBtn, styles.ddBtnStype].join(' ')}
            onClick={() => setStypeOpen((v) => !v)}
            aria-label="검색 타입 선택"
          >
            <span>{selectedStype?.label ?? '아이디'}</span>
            <span className={styles.ddCaret}>▾</span>
          </button>
          {stypeOpen && (
            <div className={styles.ddMenu}>
              {SEARCH_TYPE_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  className={[
                    styles.ddOpt,
                    searchType === opt.value ? styles.ddOptSelected : '',
                  ].join(' ')}
                  onClick={() => {
                    setSearchType(opt.value);
                    setStypeOpen(false);
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 키워드 입력 */}
        <div className={styles.searchWrap}>
          <svg
            className={styles.searchIcon}
            viewBox="0 0 16 16"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
          >
            <circle cx="6.5" cy="6.5" r="4" />
            <line x1="10" y1="10" x2="14" y2="14" />
          </svg>
          <input
            className={styles.searchInput}
            placeholder={placeholder}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSubmit(); }}
          />
          {keyword && (
            <button
              type="button"
              className={styles.clearBtn}
              onClick={() => {
                setKeyword('');
                onSearch({ keyword: '' });
              }}
              aria-label="검색어 지우기"
            >
              ×
            </button>
          )}
        </div>

        <button type="button" className={styles.searchBtn} onClick={handleSubmit}>
          조회
        </button>

        {/* 페이지 크기 — 우측 끝 */}
        <div className={styles.pageSizeWrap} ref={pageSizeRef}>
          <button
            type="button"
            className={styles.pageSizeBtn}
            onClick={() => setPageSizeOpen((v) => !v)}
            aria-label="페이지 크기 선택"
          >
            <span>{searchParams.size}개씩 보기</span>
            <span className={styles.ddCaret}>▾</span>
          </button>
          {pageSizeOpen && (
            <div className={[styles.ddMenu, styles.ddMenuRight].join(' ')}>
              {PAGE_SIZE_OPTIONS.map((sz) => (
                <button
                  key={sz}
                  type="button"
                  className={[
                    styles.ddOpt,
                    searchParams.size === sz ? styles.ddOptSelected : '',
                  ].join(' ')}
                  onClick={() => {
                    onSearch({ size: sz });
                    setPageSizeOpen(false);
                  }}
                >
                  {sz}개씩 보기
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
};

// 체크박스 선택·토스트 등 검색 조건과 무관한 부모 상태 변경 시 리렌더 방지
export default memo(MemberSearchBar);
