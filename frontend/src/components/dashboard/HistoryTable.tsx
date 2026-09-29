import { useState } from 'react';
import Pagination from './Pagination';
import styles from './HistoryTable.module.scss';
import type { GoodsItem, GoodsStatusLog } from '../../types/goods';
import buttonStyles from '../../styles/buttonStyles.module.scss';

interface Props {
  logs: GoodsStatusLog[];
  allItems: GoodsItem[];
  fromDate: string;
  toDate: string;
  maxDate: string;
  onChangeFromDate: (value: string) => void;
  onChangeToDate: (value: string) => void;
  onSelectRangePreset: (
    preset: 'today' | 'week' | 'oneMonth' | 'currentMonth' | 'previousMonth',
  ) => void;
  selectedRangePreset: 'today' | 'week' | 'oneMonth' | 'currentMonth' | 'previousMonth' | null;
  dateRangeError: string;
  onDownloadExcel: () => void;
  downloadDisabled: boolean;
}

const PAGE_SIZE = 5;
type HistorySortKey =
  | 'godoGoodsCode'
  | 'erpGoodsCode'
  | 'goodsName'
  | 'onlyMallYn'
  | 'wmsAvailableQty'
  | 'changeContent'
  | 'changedAt';
type HistorySortDir = 'asc' | 'desc';

const formatDateTime = (dateStr: string): string => {
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  return `${y}.${mo}.${day} ${h}:${m}`;
};

const STATUS_LABEL: Record<string, string> = {
  ON_SALE: '판매중',
  OUT_OF_STOCK: '품절',
};

interface SortableThProps {
  label: string;
  sortKey: HistorySortKey;
  currentKey: HistorySortKey;
  currentDir: HistorySortDir;
  onSort: (key: HistorySortKey) => void;
  center?: boolean;
}

const SortableTh = ({
  label,
  sortKey,
  currentKey,
  currentDir,
  onSort,
  center = false,
}: SortableThProps) => {
  const isActive = currentKey === sortKey;
  const icon = isActive ? (currentDir === 'asc' ? '↑' : '↓') : '↕';

  return (
    <th
      className={[styles.sortable, center ? styles.cellCenter : ''].join(' ')}
      onClick={() => onSort(sortKey)}
    >
      {label} <span className={styles.sortIcon}>{icon}</span>
    </th>
  );
};

const HistoryTable = ({
  logs,
  allItems,
  fromDate,
  toDate,
  maxDate,
  onChangeFromDate,
  onChangeToDate,
  onSelectRangePreset,
  selectedRangePreset,
  dateRangeError,
  onDownloadExcel,
  downloadDisabled,
}: Props) => {
  const [collapsed, setCollapsed] = useState(false);
  const [page, setPage] = useState(1);
  const [sortKey, setSortKey] = useState<HistorySortKey>('changedAt');
  const [sortDir, setSortDir] = useState<HistorySortDir>('desc');
  const goodsMap = new Map(allItems.map((g) => [g.godoGoodsCode, g]));

  const sorted = [...logs].sort((a, b) => {
    const goodsA = goodsMap.get(a.godoGoodsCode);
    const goodsB = goodsMap.get(b.godoGoodsCode);

    const valueFor = (log: GoodsStatusLog, goods?: GoodsItem): string | number => {
      switch (sortKey) {
        case 'godoGoodsCode':
          return log.godoGoodsCode;
        case 'erpGoodsCode':
          return log.erpGoodsCode;
        case 'goodsName':
          return goods?.goodsName ?? '';
        case 'onlyMallYn':
          return goods?.onlyMallYn ?? '';
        case 'wmsAvailableQty':
          return log.wmsAvailableQty;
        case 'changeContent':
          return `${STATUS_LABEL[log.beforeStatus]}-${STATUS_LABEL[log.afterStatus]}`;
        case 'changedAt':
        default:
          return new Date(log.changedAt).getTime();
      }
    };

    const va = valueFor(a, goodsA);
    const vb = valueFor(b, goodsB);
    const cmp = va < vb ? -1 : va > vb ? 1 : 0;
    return sortDir === 'asc' ? cmp : -cmp;
  });
  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pagedLogs = sorted.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
  const handleSort = (key: HistorySortKey) => {
    setPage(1);
    if (sortKey === key) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
      return;
    }
    setSortKey(key);
    setSortDir(key === 'changedAt' ? 'desc' : 'asc');
  };

  return (
    <div className={styles.tableFixed}>
      <div className={styles.tableHeader}>
        <span className={styles.title}>일자별 변경 이력</span>
        <span className={styles.pill}>{sorted.length}건</span>
        <div className={styles.dateRangeWrap}>
          <input
            type="date"
            className={styles.dateInput}
            value={fromDate}
            max={toDate < maxDate ? toDate : maxDate}
            onChange={(e) => {
              setCollapsed(false);
              setPage(1);
              onChangeFromDate(e.target.value);
            }}
            aria-label="변경 이력 조회 시작일"
          />
          <span className={styles.dateRangeDivider}>~</span>
          <input
            type="date"
            className={styles.dateInput}
            value={toDate}
            min={fromDate}
            max={maxDate}
            onChange={(e) => {
              setCollapsed(false);
              setPage(1);
              onChangeToDate(e.target.value);
            }}
            aria-label="변경 이력 조회 종료일"
          />
          <div className={styles.presetButtons}>
            <button
              type="button"
              className={[
                styles.presetBtn,
                selectedRangePreset === 'today' ? styles.presetBtnActive : '',
              ].join(' ')}
              onClick={() => {
                setCollapsed(false);
                setPage(1);
                onSelectRangePreset('today');
              }}
            >
              오늘
            </button>
            <button
              type="button"
              className={[
                styles.presetBtn,
                selectedRangePreset === 'week' ? styles.presetBtnActive : '',
              ].join(' ')}
              onClick={() => {
                setCollapsed(false);
                setPage(1);
                onSelectRangePreset('week');
              }}
            >
              일주일
            </button>
            <button
              type="button"
              className={[
                styles.presetBtn,
                selectedRangePreset === 'oneMonth' ? styles.presetBtnActive : '',
              ].join(' ')}
              onClick={() => {
                setCollapsed(false);
                setPage(1);
                onSelectRangePreset('oneMonth');
              }}
            >
              1개월
            </button>
            <button
              type="button"
              className={[
                styles.presetBtn,
                selectedRangePreset === 'currentMonth' ? styles.presetBtnActive : '',
              ].join(' ')}
              onClick={() => {
                setCollapsed(false);
                setPage(1);
                onSelectRangePreset('currentMonth');
              }}
            >
              이번달
            </button>
            <button
              type="button"
              className={[
                styles.presetBtn,
                selectedRangePreset === 'previousMonth' ? styles.presetBtnActive : '',
              ].join(' ')}
              onClick={() => {
                setCollapsed(false);
                setPage(1);
                onSelectRangePreset('previousMonth');
              }}
            >
              지난달
            </button>
          </div>
          {dateRangeError && <span className={styles.dateError}>{dateRangeError}</span>}
        </div>
        <div className={styles.headerActions}>
          <button
            type="button"
            className={buttonStyles.btnSmallOutline}
            onClick={onDownloadExcel}
            disabled={downloadDisabled}
          >
            엑셀 다운로드
          </button>
          <button
            type="button"
            className={styles.collapseBtn}
            onClick={() => setCollapsed((prev) => !prev)}
            aria-expanded={!collapsed}
            aria-label={collapsed ? '일자별 변경 이력 펼치기' : '일자별 변경 이력 접기'}
          >
            <svg
              className={`${styles.chevron} ${collapsed ? styles.chevronCollapsed : ''}`}
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="2,4 6,8 10,4" />
            </svg>
          </button>
        </div>
      </div>

      {!collapsed && (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <colgroup>
              <col style={{ width: '14%' }} />
              <col style={{ width: '11%' }} />
              <col style={{ width: '11%' }} />
              <col style={{ width: '26%' }} />
              <col style={{ width: '8%' }} />
              <col style={{ width: '10%' }} />
              <col style={{ width: '20%' }} />
            </colgroup>
            <thead>
              <tr>
                <SortableTh
                  label="변경 일시"
                  sortKey="changedAt"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                />
                <SortableTh
                  label="고도몰 상품코드"
                  sortKey="godoGoodsCode"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                  center
                />
                <SortableTh
                  label="ERP 품목코드"
                  sortKey="erpGoodsCode"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                  center
                />
                <SortableTh
                  label="상품명"
                  sortKey="goodsName"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                />
                <SortableTh
                  label="구분"
                  sortKey="onlyMallYn"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                  center
                />
                <SortableTh
                  label="가용수량"
                  sortKey="wmsAvailableQty"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                  center
                />
                <SortableTh
                  label="변경 내용"
                  sortKey="changeContent"
                  currentKey={sortKey}
                  currentDir={sortDir}
                  onSort={handleSort}
                  center
                />
              </tr>
            </thead>
            <tbody>
              {sorted.length === 0 ? (
                <tr>
                  <td colSpan={7} className={styles.emptyCell}>선택한 기간의 변경 이력 없음</td>
                </tr>
              ) : (
                <>
                  {pagedLogs.map((log) => {
                    const goods = goodsMap.get(log.godoGoodsCode);
                    const isOut = log.afterStatus === 'OUT_OF_STOCK';

                    return (
                      <tr key={log.id}>
                        <td className={styles.time}>{formatDateTime(log.changedAt)}</td>
                        <td className={[styles.code, styles.cellCenter].join(' ')}>{log.godoGoodsCode}</td>
                        <td className={[styles.code, styles.cellCenter].join(' ')}>{log.erpGoodsCode}</td>
                        <td>{goods?.goodsName ?? '—'}</td>
                        <td className={styles.cellCenter}>
                          {goods ? (
                            <span className={goods.onlyMallYn === 'Y' ? styles.bE : styles.bG}>
                              {goods.onlyMallYn === 'Y' ? '전용' : '범용'}
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className={[styles.num, styles.cellCenter].join(' ')}>
                          {log.wmsAvailableQty.toLocaleString()}
                        </td>
                        <td className={styles.cellCenter}>
                          <span className={isOut ? styles.bOut : styles.bOn}>
                            {STATUS_LABEL[log.beforeStatus]} → {STATUS_LABEL[log.afterStatus]}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                  {pagedLogs.length < PAGE_SIZE &&
                    Array.from({ length: PAGE_SIZE - pagedLogs.length }).map((_, i) => (
                      <tr key={`empty-${i}`} className={styles.emptyRow}>
                        <td colSpan={7} />
                      </tr>
                    ))}
                </>
              )}
            </tbody>
          </table>
        </div>
      )}

      {!collapsed && sorted.length > 0 && (
        <Pagination page={safePage} totalPages={totalPages} onPage={setPage} />
      )}
    </div>
  );
};

export default HistoryTable;
