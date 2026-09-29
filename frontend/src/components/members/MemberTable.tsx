import { memo } from 'react';
import styles from './MemberTable.module.scss';
import Pagination from '../dashboard/Pagination';
import { getMemberGroupInfo } from '../../utils/memberGroup';
import type { MemberListItem, MemberSearchParams } from '../../types/member';

const SORTABLE_KEYS = ['mem_no', 'mem_id', 'group_sno', 'app_fl', 'entry_dt', 'last_login_dt', 'sale_amount'];

interface SortableThProps {
  label: string;
  sortK: string;
  currentKey: string;
  currentDir: MemberSearchParams['sortDir'];
  onSort: (key: string) => void;
}

const SortableTh = ({ label, sortK, currentKey, currentDir, onSort }: SortableThProps) => {
  const isActive = currentKey === sortK;
  const icon = isActive ? (currentDir === 'asc' ? '↑' : '↓') : '↕';
  return (
    <th className={styles.sortable} onClick={() => onSort(sortK)}>
      {label} <span className={styles.sortIcon}>{icon}</span>
    </th>
  );
};

const formatEntryDt = (dt: string): string => {
  if (!dt) return '—';
  return dt.slice(0, 10);
};

interface Props {
  members: MemberListItem[];
  loading: boolean;
  error: string | null;
  totalElements: number;
  page: number;
  totalPages: number;
  sortKey: string;
  sortDir: MemberSearchParams['sortDir'];
  onSort: (key: string) => void;
  onPage: (p: number) => void;
  onRowClick: (memNo: number) => void;
  onReset: () => void;
  selectedIds: Set<number>;
  onToggleRow: (memNo: number) => void;
  onToggleAll: (checked: boolean) => void;
  onDelete: (member: MemberListItem) => void;
  suppressedDupIds: Set<number>;
  // 검색·필터 조건이 걸려 있는지 — 빈 목록 문구 분기용
  hasActiveFilter: boolean;
}

const MemberTable = ({
  members,
  loading,
  error,
  totalElements,
  page,
  totalPages,
  sortKey,
  sortDir,
  onSort,
  onPage,
  onRowClick,
  onReset,
  selectedIds,
  onToggleRow,
  onToggleAll,
  onDelete,
  suppressedDupIds,
  hasActiveFilter,
}: Props) => {
  const allChecked = members.length > 0 && members.every((m) => selectedIds.has(m.memNo));

  return (
    <div className={styles.wrap}>
      <div className={styles.tableWrap}>
        <table>
          <colgroup>
            <col style={{ width: '1px' }} />
            <col style={{ width: '7%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '5%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '12%' }} />
            <col style={{ width: '9%' }} />
            <col style={{ width: '9%' }} />
            <col style={{ width: '17%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '8%' }} />
            <col style={{ width: '76px' }} />
          </colgroup>
          <thead>
            <tr>
              <th className={styles.checkCell}>
                <input
                  type="checkbox"
                  className={styles.checkbox}
                  checked={allChecked}
                  onChange={(e) => onToggleAll(e.target.checked)}
                  aria-label="전체 선택"
                />
              </th>
              <SortableTh label="회원번호" sortK="mem_no" currentKey={sortKey} currentDir={sortDir} onSort={onSort} />
              <SortableTh label="아이디" sortK="mem_id" currentKey={sortKey} currentDir={sortDir} onSort={onSort} />
              <th>이름</th>
              <SortableTh label="등급" sortK="group_sno" currentKey={sortKey} currentDir={sortDir} onSort={onSort} />
              <th>이메일</th>
              <th>휴대폰</th>
              <th>거래처코드</th>
              <th>거래처명</th>
              <th>거래처 상태</th>
              <SortableTh label="가입일" sortK="entry_dt" currentKey={sortKey} currentDir={sortDir} onSort={onSort} />
              <th className={styles.cellCenter}>수정/삭제</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={12} className={styles.stateCell}>
                  <span className={styles.spinner} />
                  불러오는 중...
                </td>
              </tr>
            )}
            {!loading && error && (
              <tr>
                <td colSpan={12} className={styles.stateCell}>
                  {error}
                </td>
              </tr>
            )}
            {!loading &&
              !error &&
              members.map((m) => {
                const grp = getMemberGroupInfo(m.groupSno);
                const isDup = !!m.duplicateMemNo && !suppressedDupIds.has(m.memNo);
                return (
                  <tr
                    key={m.memNo}
                    className={isDup ? styles.dupRow : ''}
                    title={isDup ? '동일한 사업자번호로 가입한 중복 의심 회원입니다' : undefined}
                  >
                    <td className={styles.checkCell}>
                      <input
                        type="checkbox"
                        className={styles.checkbox}
                        checked={selectedIds.has(m.memNo)}
                        onChange={() => onToggleRow(m.memNo)}
                        aria-label={`${m.memId} 선택`}
                      />
                    </td>
                    <td>{m.memNo}</td>
                    <td>
                      {m.memId}
                      {/* 고도몰 링크 아이콘 자리 — Iteration 10에서 구현 */}
                    </td>
                    <td>{m.memName ?? '—'}</td>
                    <td>
                      <span
                        className={styles.badge}
                        style={{ background: grp.badgeBg, color: grp.badgeText }}
                      >
                        {m.groupName || grp.label}
                      </span>
                    </td>
                    <td>{m.email ?? '—'}</td>
                    <td>{m.cellPhone ?? '—'}</td>
                    <td>{m.partnerCode ?? '—'}</td>
                    <td>{m.partnerName ?? '—'}</td>
                    <td>
                      {m.memberStatus === 'NORMAL' ? (
                        <span className={[styles.badge, styles.statusActive].join(' ')}>등록</span>
                      ) : m.memberStatus === 'UNREGISTERED' ? (
                        <span className={[styles.badge, styles.statusUnregistered].join(' ')}>미등록</span>
                      ) : m.memberStatus === 'INACTIVE' ? (
                        <span className={[styles.badge, styles.statusInactive].join(' ')}>불용</span>
                      ) : '—'}
                    </td>
                    <td>{formatEntryDt(m.entryDt)}</td>
                    <td className={[styles.actsCell, styles.cellCenter].join(' ')}>
                      <div className={styles.rowActs}>
                        <button
                          type="button"
                          className={styles.rowBtn}
                          onClick={() => onRowClick(m.memNo)}
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          className={[styles.rowBtn, styles.del].join(' ')}
                          onClick={() => onDelete(m)}
                        >
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
        {/* 조건 없이 전체가 0명 → 최초 사용 안내 / 조건이 걸려 있으면 조건 안내 + 초기화 */}
        {!loading && !error && members.length === 0 && totalElements === 0 && !hasActiveFilter && (
          <div className={styles.emptyOverlay}>
            <div className={styles.emptyMsg}>아직 등록된 회원이 없습니다</div>
            <div className={styles.emptyHint}>우측 상단의 엑셀 업로드 버튼을 눌러 회원을 등록해 주세요.</div>
          </div>
        )}
        {!loading && !error && members.length === 0 && totalElements === 0 && hasActiveFilter && (
          <div className={styles.emptyOverlay}>
            <div className={styles.emptyMsg}>조건에 맞는 회원을 찾을 수 없습니다</div>
            <button type="button" className={styles.emptyBtn} onClick={onReset} style={{ pointerEvents: 'auto' }}>
              초기화
            </button>
          </div>
        )}
        {!loading && !error && members.length === 0 && totalElements > 0 && (
          <div className={styles.emptyOverlay}>
            <div className={styles.emptyMsg}>검색 결과가 없습니다</div>
            <button type="button" className={styles.emptyBtn} onClick={onReset} style={{ pointerEvents: 'auto' }}>
              초기화
            </button>
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={totalPages} onPage={onPage} />
    </div>
  );
};

export { SORTABLE_KEYS };
// 토스트·모달 등 목록과 무관한 부모 상태 변경 시 테이블 리렌더 방지
export default memo(MemberTable);
