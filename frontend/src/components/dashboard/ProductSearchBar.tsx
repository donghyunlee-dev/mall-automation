import { useEffect, useRef, useState } from 'react';
import styles from './ProductSearchBar.module.scss';
import type {
  BlockFilterKey,
  GoodsFilter,
  PageSize,
  StatusFilterKey,
  TypeFilterKey,
} from '../../hooks/useGoods';

interface Props {
  keyword: string;
  filter: GoodsFilter;
  pageSize: PageSize;
  onKeyword: (v: string) => void;
  onStatusFilter: (v: StatusFilterKey) => void;
  onTypeFilter: (v: TypeFilterKey) => void;
  onBlockFilter: (v: BlockFilterKey) => void;
  onPageSize: (v: PageSize) => void;
}

const STATUS_FILTER_BUTTONS: Array<{ key: StatusFilterKey; label: string; cls: string }> = [
  { key: 'all', label: '전체', cls: styles.fa },
  { key: 'out', label: '품절', cls: styles.fo },
  { key: 'on', label: '판매중', cls: styles.fn },
  { key: 'unavailable', label: '확인불가', cls: styles.fu },
];

const TYPE_FILTER_BUTTONS: Array<{ key: TypeFilterKey; label: string; cls: string }> = [
  { key: 'general', label: '범용', cls: styles.fg },
  { key: 'exclusive', label: '전용', cls: styles.fe },
];

const BLOCK_FILTER_BUTTONS: Array<{ key: BlockFilterKey; label: string; cls: string }> = [
  { key: 'blocked', label: '전송 차단', cls: styles.fb },
];

const PAGE_SIZE_OPTIONS: PageSize[] = [10, 20, 30, 50];

const ProductSearchBar = ({
  keyword,
  filter,
  pageSize,
  onKeyword,
  onStatusFilter,
  onTypeFilter,
  onBlockFilter,
  onPageSize,
}: Props) => {
  const [pageSizeOpen, setPageSizeOpen] = useState(false);
  const pageSizeRef = useRef<HTMLDivElement>(null);
  const isAllActive = filter.status === 'all' && filter.type === 'all';
  const getStatusActiveClass = (key: StatusFilterKey): string =>
    key === 'all' ? (isAllActive ? styles.active : '') : filter.status === key ? styles.active : '';
  const getTypeActiveClass = (key: TypeFilterKey): string =>
    filter.type === key ? styles.active : '';
  const getBlockActiveClass = (key: BlockFilterKey): string =>
    filter.block === key ? styles.active : '';

  useEffect(() => {
    if (!pageSizeOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (!pageSizeRef.current?.contains(event.target as Node)) {
        setPageSizeOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [pageSizeOpen]);

  return (
    <div className={styles.row}>
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
          placeholder="고도몰 상품코드, ERP 품목코드, 상품명..."
          value={keyword}
          onChange={(e) => onKeyword(e.target.value)}
        />
      </div>

      <div className={styles.fbtns}>
        {/* 품절/판매중 그룹 */}
        <div className={styles.filterGroup}>
          {STATUS_FILTER_BUTTONS.map(({ key, label, cls }) => (
            <button
              key={key}
              className={[styles.fbtn, cls, getStatusActiveClass(key)].join(' ')}
              onClick={() => onStatusFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
        <div className={styles.filterSep} />
        <div className={styles.filterGroup}>
          {TYPE_FILTER_BUTTONS.map(({ key, label, cls }) => (
            <button
              key={key}
              className={[styles.fbtn, cls, getTypeActiveClass(key)].join(' ')}
              onClick={() => onTypeFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
        <div className={styles.filterSep} />
        <div className={styles.filterGroup}>
          {BLOCK_FILTER_BUTTONS.map(({ key, label, cls }) => (
            <button
              key={key}
              className={[styles.fbtn, cls, getBlockActiveClass(key)].join(' ')}
              onClick={() => onBlockFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.pageSizeWrap} ref={pageSizeRef}>
        <button
          type="button"
          className={styles.pageSizeButton}
          onClick={() => setPageSizeOpen((prev) => !prev)}
          aria-expanded={pageSizeOpen}
          aria-label="상품 목록 페이지 크기"
        >
          {pageSize}개씩 보기
          <span className={styles.pageSizeCaret}>▾</span>
        </button>

        {pageSizeOpen && (
          <div className={styles.pageSizeMenu}>
            {PAGE_SIZE_OPTIONS.map((option) => (
              <button
                key={option}
                type="button"
                className={[
                  styles.pageSizeOption,
                  pageSize === option ? styles.pageSizeOptionActive : '',
                ].join(' ')}
                onClick={() => {
                  onPageSize(option);
                  setPageSizeOpen(false);
                }}
              >
                {option}개씩 보기
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductSearchBar;
