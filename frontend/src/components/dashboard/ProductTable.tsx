import styles from './ProductTable.module.scss';
import Pagination from './Pagination';
import type { GoodsItem } from '../../types/goods';
import type { SortKey, SortDir, PageSize } from '../../hooks/useGoods';
import { isSaleStatusUnavailable } from '../../utils/goodsSaleStatus';

interface Props {
  items: GoodsItem[];
  loading: boolean;
  error: string;
  sortKey: SortKey;
  sortDir: SortDir;
  page: number;
  pageSize: PageSize;
  totalPages: number;
  onSort: (key: SortKey) => void;
  onPage: (p: number) => void;
  onEdit: (item: GoodsItem) => void;
  onDelete: (item: GoodsItem) => void;
  onToggleGodoSyncBlock: (item: GoodsItem) => void;
  onForceGodoStatusSync: (item: GoodsItem) => void;
}

const formatDate = (dateStr: string | null | undefined): string => {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}.${m}.${day}`;
};

const getCodeValidationLabel = (item: GoodsItem): string => {
  if (item.erpSyncYn === 'Y' && item.godoSyncYn === 'Y') return '검증완료';
  if (item.erpSyncYn === 'N' && item.godoSyncYn === 'N') return '고도몰/ERP X';
  if (item.erpSyncYn === 'N') return 'ERP X';
  return '고도몰 X';
};

const getCodeValidationClassName = (item: GoodsItem): string => {
  if (item.erpSyncYn === 'Y' && item.godoSyncYn === 'Y') return styles.bCodeOk;
  if (item.erpSyncYn === 'N' && item.godoSyncYn === 'N') return styles.bCodeAllX;
  if (item.erpSyncYn === 'N') return styles.bCodeErpX;
  return styles.bCodeGodoX;
};

const isGodoSyncBlockAvailable = (item: GoodsItem): boolean =>
  item.godoSyncYn === 'Y' &&
  item.erpSyncYn === 'Y' &&
  item.status === 'NORMAL';

const isForceSyncAvailable = (item: GoodsItem): boolean =>
  item.godoSyncYn === 'Y' &&
  item.godoSyncBlockedYn === 'N';

interface SortableThProps {
  label: string;
  sortK: SortKey;
  currentKey: SortKey;
  currentDir: SortDir;
  onSort: (key: SortKey) => void;
}

const SortableTh = ({ label, sortK, currentKey, currentDir, onSort }: SortableThProps) => {
  const isActive = currentKey === sortK;
  const icon = isActive ? (currentDir === 'asc' ? '↑' : '↓') : '↕';
  const centerHeader =
    sortK === 'godoGoodsCode' ||
    sortK === 'erpGoodsCode' ||
    sortK === 'onlyMallYn' ||
    sortK === 'soldOutYn' ||
    sortK === 'godoSyncBlockedYn' ||
    sortK === 'codeValidation' ||
    sortK === 'updatedAt' ||
    sortK === 'createdAt';
  return (
    <th
      className={[styles.sortable, centerHeader ? styles.cellCenter : ''].join(' ')}
      onClick={() => onSort(sortK)}
    >
      {label} <span className={styles.sortIcon}>{icon}</span>
    </th>
  );
};

const ProductTable = ({
  items,
  loading,
  error,
  sortKey,
  sortDir,
  page,
  pageSize,
  totalPages,
  onSort,
  onPage,
  onEdit,
  onDelete,
  onToggleGodoSyncBlock,
  onForceGodoStatusSync,
}: Props) => {
  const EMPTY_ROWS = pageSize;
  const tableWrapStyle = {
    ['--product-visible-rows' as string]: String(pageSize),
  };

  return (
    <div className={styles.tableFixed}>
      <div className={styles.tableWrap} style={tableWrapStyle}>
        <table>
          <colgroup>
            <col style={{ width: '10%' }} />
            <col style={{ width: '9%' }} />
            <col style={{ width: '29%' }} />
            <col style={{ width: '6%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '7%' }} />
            <col style={{ width: '9%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '10%' }} />
            <col style={{ width: '6%' }} />
          </colgroup>
          <thead>
            <tr>
              <SortableTh
                label="고도몰 상품코드"
                sortK="godoGoodsCode"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="ERP 품목코드"
                sortK="erpGoodsCode"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="상품명"
                sortK="goodsName"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="구분"
                sortK="onlyMallYn"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="판매 상태"
                sortK="soldOutYn"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="자동 동기화"
                sortK="godoSyncBlockedYn"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="코드검증 상태"
                sortK="codeValidation"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="등록/수정일자"
                sortK="updatedAt"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <SortableTh
                label="등록/수정자"
                sortK="createdAt"
                currentKey={sortKey}
                currentDir={sortDir}
                onSort={onSort}
              />
              <th className={styles.cellCenter}>수정/삭제</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={10} className={styles.stateCell}>
                  불러오는 중...
                </td>
              </tr>
            )}
            {!loading && error && (
              <tr>
                <td colSpan={10} className={styles.stateCell}>
                  {error}
                </td>
              </tr>
            )}
            {!loading && !error && items.length === 0 && (
              <tr>
                <td colSpan={10} className={styles.stateCell}>
                  검색 결과가 없어요.
                </td>
              </tr>
            )}
            {!loading &&
              !error &&
              items.map((item) => {
                const editDate = item.updatedAt ? formatDate(item.updatedAt) : formatDate(item.createdAt);
                const editor = item.updatedId ?? item.createdId ?? '';
                const editorName = editor.split('@')[0] ?? editor;
                const canControlSyncBlock = isGodoSyncBlockAvailable(item);
                const canForceSyncStatus = isForceSyncAvailable(item);
                const syncBlockLabel = canControlSyncBlock
                  ? item.godoSyncBlockedYn === 'Y'
                    ? '차단'
                    : '허용'
                  : '불가';

                return (
                  <tr key={item.id}>
                    <td className={styles.cellCenter}>{item.godoGoodsCode}</td>
                    <td className={styles.cellCenter}>{item.erpGoodsCode}</td>
                    <td>{item.goodsName}</td>
                    <td className={styles.cellCenter}>
                      {item.onlyMallYn === 'Y' ? (
                        <span className={[styles.badge, styles.badgeType, styles.bE].join(' ')}>
                          전용
                        </span>
                      ) : (
                        <span className={[styles.badge, styles.badgeType, styles.bG].join(' ')}>
                          범용
                        </span>
                      )}
                    </td>
                    <td className={styles.cellCenter}>
                      {isSaleStatusUnavailable(item) ? (
                        <button
                          type="button"
                          className={[styles.badge, styles.badgeStatus, styles.saleStatusButton, styles.bErrS].join(' ')}
                          disabled
                          title="판매 상태가 확인불가인 품목은 강제 전송할 수 없어요."
                        >
                          확인불가
                        </button>
                      ) : item.soldOutYn === 'Y' ? (
                        <button
                          type="button"
                          className={[
                            styles.badge,
                            styles.badgeStatus,
                            styles.saleStatusButton,
                            canForceSyncStatus ? styles.saleStatusInteractive : '',
                            styles.bOutS,
                          ].join(' ')}
                          onClick={() => {
                            if (!canForceSyncStatus) return;
                            onForceGodoStatusSync(item);
                          }}
                          disabled={!canForceSyncStatus}
                          title={!canForceSyncStatus ? '고도몰 연동 완료, 전송 허용 품목만 강제 전송할 수 있어요.' : '판매중으로 강제 전송'}
                        >
                          품절
                        </button>
                      ) : (
                        <button
                          type="button"
                          className={[
                            styles.badge,
                            styles.badgeStatus,
                            styles.saleStatusButton,
                            canForceSyncStatus ? styles.saleStatusInteractive : '',
                            styles.bResS,
                          ].join(' ')}
                          onClick={() => {
                            if (!canForceSyncStatus) return;
                            onForceGodoStatusSync(item);
                          }}
                          disabled={!canForceSyncStatus}
                          title={!canForceSyncStatus ? '고도몰 연동 완료, 전송 허용 품목만 강제 전송할 수 있어요.' : '품절로 강제 전송'}
                        >
                          판매중
                        </button>
                      )}
                    </td>
                    <td className={styles.cellCenter}>
                      <div className={styles.blockCell}>
                        <button
                          type="button"
                          className={[
                            styles.blockToggle,
                            styles.badgeTransfer,
                            canControlSyncBlock ? styles.blockToggleInteractive : '',
                            canControlSyncBlock
                              ? item.godoSyncBlockedYn === 'Y'
                                ? styles.bBlockY
                                : styles.bBlockN
                              : styles.bBlockX,
                          ].join(' ')}
                          onClick={() => {
                            if (!canControlSyncBlock) return;
                            onToggleGodoSyncBlock(item);
                          }}
                          disabled={!canControlSyncBlock}
                          title={!canControlSyncBlock ? '미검증 품목은 전송 차단 여부와 관계없이 전송되지 않아요.' : undefined}
                        >
                          {syncBlockLabel}
                        </button>
                      </div>
                    </td>
                    <td className={styles.cellCenter}>
                      <span
                        className={[
                          styles.badge,
                          styles.badgeCode,
                          getCodeValidationClassName(item),
                        ].join(' ')}
                      >
                        {getCodeValidationLabel(item)}
                      </span>
                    </td>
                    <td className={styles.cellCenter}>
                      <span className={styles.timeText}>{editDate}</span>
                    </td>
                    <td className={styles.cellCenter}>{editorName}</td>
                    <td className={[styles.actsCell, styles.cellCenter].join(' ')}>
                      <div className={styles.rowActs}>
                        <button
                          className={styles.rowBtn}
                          onClick={() => onEdit(item)}
                        >
                          수정
                        </button>
                        <button
                          className={[styles.rowBtn, styles.del].join(' ')}
                          onClick={() => onDelete(item)}
                        >
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            {!loading &&
              !error &&
              items.length > 0 &&
              items.length < EMPTY_ROWS &&
              Array.from({ length: EMPTY_ROWS - items.length }).map((_, i) => (
                <tr key={`empty-${i}`} className={styles.emptyRow}>
                  <td colSpan={10} />
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} totalPages={totalPages} onPage={onPage} />
    </div>
  );
};

export default ProductTable;
