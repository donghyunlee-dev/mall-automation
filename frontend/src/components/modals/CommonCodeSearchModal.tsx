import { useState, useEffect, useDeferredValue, useMemo, useRef } from 'react';
import type { CSSProperties, KeyboardEvent as ReactKeyboardEvent } from 'react';
import { getCommonCodes } from '../../api/commonCodes';
import type { CommonCodeItem, CommonCodeGroup } from '../../types/commonCode';
import styles from './CommonCodeSearchModal.module.scss';

interface Props {
  groupCode: CommonCodeGroup;
  onSelect: (code: string, name: string) => void;
  onClose: () => void;
  initialItems?: CommonCodeItem[];
  initialQuery?: string;
  autoSelectSingleMatch?: boolean;
}

const TITLE: Record<CommonCodeGroup, string> = {
  SERVICE: '업태 검색',
  ITEM: '업종 검색',
};

// 목록 상단 컬럼 헤더 (그룹별 명칭)
const COL_HEADERS: Record<CommonCodeGroup, { code: string; name: string }> = {
  SERVICE: { code: '업태코드', name: '업태명' },
  ITEM: { code: '업종코드', name: '업종명' },
};

// 가상 스크롤 설정 — 화면에 보이는 행 + 여유분만 렌더링해 목록이 수천 건이어도 가볍게 유지.
// ITEM_HEIGHT는 .item의 고정 높이(px)와 반드시 일치해야 한다.
const ITEM_HEIGHT = 34;
const OVERSCAN = 8;

const CommonCodeSearchModal = ({
  groupCode,
  onSelect,
  onClose,
  initialItems,
  initialQuery,
  autoSelectSingleMatch,
}: Props) => {
  // items가 null이면 아직 로딩 전 — loading은 파생 상태로 처리 (effect 내 동기 setState 제거)
  const [items, setItems] = useState<CommonCodeItem[] | null>(initialItems ?? null);
  const [query, setQuery] = useState(initialQuery ?? '');
  const [error, setError] = useState<string | null>(null);
  const loading = items == null && error == null;

  const deferredQuery = useDeferredValue(query);
  const isFiltering = query !== deferredQuery;

  const listRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const [viewportH, setViewportH] = useState(420);
  const [activeIdx, setActiveIdx] = useState(0);

  // ESC 닫기 — 캡처 단계에서 전파를 막아 뒤의 상세 모달까지 함께 닫히지 않도록 처리
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.stopPropagation();
      onClose();
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [onClose]);

  useEffect(() => {
    if (initialItems) return;
    getCommonCodes(groupCode)
      .then(setItems)
      .catch(() => setError('목록을 불러오지 못했어요.'));
  }, [groupCode, initialItems]);

  // 목록 뷰포트 높이 측정 (모달 높이는 고정이라 마운트 시 1회면 충분)
  useEffect(() => {
    if (listRef.current) setViewportH(listRef.current.clientHeight);
  }, []);

  // 스크롤(scrollTop 변경)마다 수천 건 재필터링하지 않도록 메모이제이션
  const q = deferredQuery.trim().toLowerCase();
  const filtered = useMemo(() => {
    const list = items ?? [];
    return q
      ? list.filter(
          (item) =>
            item.code.includes(q) ||
            item.codeName.toLowerCase().includes(q),
        )
      : list;
  }, [items, q]);

  // autoSelectSingleMatch: 모달이 열린 최초 필터링 결과가 정확히 1건이면 자동 선택.
  // 이후 사용자가 검색어를 더 타이핑해 결과가 바뀌는 경우에는 재적용하지 않는다(1회성).
  const autoSelectChecked = useRef(false);
  useEffect(() => {
    if (!autoSelectSingleMatch || autoSelectChecked.current || items == null) return;
    autoSelectChecked.current = true;
    if (filtered.length === 1) {
      onSelect(filtered[0].code, filtered[0].codeName);
    }
  }, [items, autoSelectSingleMatch, filtered, onSelect]);

  // 검색어가 바뀌면 목록 맨 위로 — 상태는 렌더 중 조정, DOM 스크롤은 effect에서 반영
  const [prevQuery, setPrevQuery] = useState(deferredQuery);
  if (prevQuery !== deferredQuery) {
    setPrevQuery(deferredQuery);
    setScrollTop(0);
    setActiveIdx(0);
  }
  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = 0;
  }, [deferredQuery]);

  // 가상 스크롤 계산 — 결과가 줄어 스크롤 위치가 넘칠 수 있어 clamp 처리
  const total = filtered.length;
  const maxScrollTop = Math.max(0, total * ITEM_HEIGHT - viewportH);
  const clampedTop = Math.min(scrollTop, maxScrollTop);
  const startIndex = Math.max(0, Math.floor(clampedTop / ITEM_HEIGHT) - OVERSCAN);
  const endIndex = Math.min(total, Math.ceil((clampedTop + viewportH) / ITEM_HEIGHT) + OVERSCAN);
  const visible = filtered.slice(startIndex, endIndex);
  const spacerStyle = { '--vlist-total': `${total * ITEM_HEIGHT}px` } as CSSProperties;
  const viewportStyle = { '--vlist-offset': `${startIndex * ITEM_HEIGHT}px` } as CSSProperties;

  // 키보드 탐색 — ↑↓로 하이라이트 이동, Enter로 선택
  const handleKeyDown = (e: ReactKeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      if (total === 0) return;
      const next = e.key === 'ArrowDown' ? Math.min(activeIdx + 1, total - 1) : Math.max(activeIdx - 1, 0);
      setActiveIdx(next);
      // 하이라이트 항목이 화면 밖이면 스크롤 이동 (setScrollTop은 onScroll로 반영)
      const el = listRef.current;
      if (el) {
        const top = next * ITEM_HEIGHT;
        const bottom = top + ITEM_HEIGHT;
        if (top < el.scrollTop) el.scrollTop = top;
        else if (bottom > el.scrollTop + el.clientHeight) el.scrollTop = bottom - el.clientHeight;
      }
    } else if (e.key === 'Enter') {
      const item = filtered[activeIdx];
      if (item) onSelect(item.code, item.codeName);
    }
  };

  return (
    <div className={styles.bg} onClick={onClose}>
      <div className={styles.panel} onClick={(e) => e.stopPropagation()}>
        <div className={styles.head}>
          <span className={styles.title}>{TITLE[groupCode]}</span>
          <button type="button" className={styles.close} onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        <div className={styles.searchWrap}>
          <input
            className={styles.searchInput}
            placeholder="코드 또는 명칭으로 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
          />
        </div>

        <div className={styles.colHead}>
          <span className={styles.code}>{COL_HEADERS[groupCode].code}</span>
          <span className={styles.sep}>|</span>
          <span className={styles.name}>{COL_HEADERS[groupCode].name}</span>
        </div>

        <div
          className={styles.list}
          ref={listRef}
          onScroll={(e) => setScrollTop(e.currentTarget.scrollTop)}
          style={{ opacity: isFiltering ? 0.5 : 1, transition: 'opacity 0.1s' }}
        >
          {loading && <div className={styles.msg}>불러오는 중...</div>}
          {error && <div className={styles.err}>{error}</div>}
          {!loading && !error && total === 0 && (
            <div className={styles.msg}>검색 결과가 없어요.</div>
          )}
          {!loading && !error && total > 0 && (
            <div className={styles.spacer} style={spacerStyle}>
              <div className={styles.viewport} style={viewportStyle}>
                {visible.map((item, i) => (
                  <button
                    key={item.id}
                    type="button"
                    className={[styles.item, startIndex + i === activeIdx ? styles.itemActive : ''].filter(Boolean).join(' ')}
                    onClick={() => onSelect(item.code, item.codeName)}
                  >
                    <span className={styles.code}>{item.code}</span>
                    <span className={styles.sep}>|</span>
                    <span className={styles.name}>{item.codeName}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CommonCodeSearchModal;
