import { useState, useEffect, useMemo, useCallback } from 'react';
import useMembers from '../hooks/useMembers';
import useMemberSync from '../hooks/useMemberSync';
import { formatLastRun } from '../utils/formatLastRun';
import MemberSearchBar from '../components/members/MemberSearchBar';
import MemberTable from '../components/members/MemberTable';
import MemberDetailModal from '../components/members/MemberDetailModal';
import MemberDeleteModal from '../components/members/MemberDeleteModal';
import MemberUploadModal from '../components/modals/MemberUploadModal';
import ExcelDownloadWarningModal, { type DownloadWarning } from '../components/modals/ExcelDownloadWarningModal';
import Toast from '../components/common/Toast';
import { getMemberDetail, updateMemberPartner } from '../api/members';
import { getCommonCodes } from '../api/commonCodes';
import type { MemberListItem, MemberDetail } from '../types/member';
import type { CommonCodeItem } from '../types/commonCode';
import { useLockBodyScroll } from '../hooks/useLockBodyScroll';
import modalStyles from '../components/modals/Modal.module.scss';
import styles from './MembersPage.module.scss';


interface ToastState {
  msg: string;
  type: 'success' | 'error';
  id: number;
}

const formatLastUpload = (syncedAt: string | null | undefined): string => {
  if (!syncedAt) return '—';
  return syncedAt.slice(0, 16).replace('T', ' ');
};

const MembersPage = () => {
  const { members, loading, error, searchParams, setSearchParams, resetSearchParams, reload, refresh } = useMembers();
  const { schedule: syncSchedule, running: syncing, runNow: runMemberSync } = useMemberSync();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [selectedMemNo, setSelectedMemNo] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MemberListItem | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [downloading, setDownloading] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [downloadWarnings, setDownloadWarnings] = useState<DownloadWarning[] | null>(null);
  const [bulkConfirm, setBulkConfirm] = useState<{ ready: MemberListItem[]; missing: MemberListItem[] } | null>(null);
  const [pendingDetails, setPendingDetails] = useState<MemberDetail[] | null>(null);
  const [preloadedServiceCodes, setPreloadedServiceCodes] = useState<CommonCodeItem[]>([]);
  const [preloadedItemCodes, setPreloadedItemCodes] = useState<CommonCodeItem[]>([]);

  useLockBodyScroll(bulkConfirm != null);

  useEffect(() => {
    Promise.all([getCommonCodes('SERVICE'), getCommonCodes('ITEM')])
      .then(([svc, itm]) => {
        setPreloadedServiceCodes(svc);
        setPreloadedItemCodes(itm);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const duration = toast.type === 'error' ? 6000 : 2500;
    const t = setTimeout(() => setToast(null), duration);
    return () => clearTimeout(t);
  }, [toast]);

  const showToast = useCallback((msg: string, type: 'success' | 'error') => {
    setToast({ msg, type, id: Date.now() });
  }, []);

  const totalElements = members?.totalElements ?? 0;
  const totalPages = Math.max(1, members?.totalPages ?? 1);
  // identity 고정 — members 로딩 전 매 렌더 새 배열이 생기면 toggleAll·suppressedDupIds가 재생성되어 MemberTable memo가 무력화됨
  const content = useMemo(() => members?.content ?? [], [members]);
  const lastSyncedAt = members?.content.length ? null : null; // summary API는 Iter 10에서 연동

  // 목록이 새로 조회되면(페이지·검색 변경 등) 선택 초기화 — 렌더 중 상태 조정 (React 권장 패턴)
  const [prevMembers, setPrevMembers] = useState(members);
  if (prevMembers !== members) {
    setPrevMembers(members);
    setSelectedIds(new Set());
  }

  // 정상 1개 + 나머지 불용인 중복 그룹은 (해소된 것으로 보아) 중복 표시 억제
  // 미등록이 하나라도 있거나, 그룹원이 현재 페이지에 모두 없으면 억제하지 않음
  const suppressedDupIds = useMemo(() => {
    const map = new Map(content.map((m) => [m.memNo, m]));
    const suppressed = new Set<number>();
    for (const m of content) {
      if (!m.duplicateMemNo) continue;
      const otherNos = m.duplicateMemNo.split(';').map(Number).filter(Boolean);
      const others = otherNos
        .map((no) => map.get(no))
        .filter((x): x is MemberListItem => x != null);
      // 그룹원이 현재 페이지에 모두 있어야 해소 여부를 판정할 수 있음
      if (others.length !== otherNos.length) continue;
      const group = [m, ...others];
      if (group.some((g) => !g.memberStatus || g.memberStatus === 'UNREGISTERED')) continue;
      if (group.filter((g) => g.memberStatus === 'NORMAL').length === 1) {
        group.forEach((g) => suppressed.add(g.memNo));
      }
    }
    return suppressed;
  }, [content]);

  const toggleRow = useCallback((memNo: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(memNo)) next.delete(memNo);
      else next.add(memNo);
      return next;
    });
  }, []);

  const toggleAll = useCallback((checked: boolean) => {
    setSelectedIds(checked ? new Set(content.map((m) => m.memNo)) : new Set());
  }, [content]);

  const handleDownloadExcel = async () => {
    const targets = content.filter((m) => selectedIds.has(m.memNo));
    if (targets.length === 0 || downloading) return;

    setDownloading(true);
    try {
      // 페이지 진입 시 preload된 공통코드가 있으면 재조회 생략
      const codesReady = preloadedServiceCodes.length > 0 && preloadedItemCodes.length > 0;
      const [details, serviceCodes, itemCodes] = await Promise.all([
        Promise.all(targets.map((m) => getMemberDetail(m.memNo))),
        codesReady ? Promise.resolve(preloadedServiceCodes) : getCommonCodes('SERVICE'),
        codesReady ? Promise.resolve(preloadedItemCodes) : getCommonCodes('ITEM'),
      ]);

      const warnings: DownloadWarning[] = [];

      const isValidCode = (
        list: CommonCodeItem[],
        code: string | null,
        name: string | null,
      ): boolean =>
        !!(code?.trim()) &&
        !!(name?.trim()) &&
        list.some((c) => c.code === code!.trim() && c.codeName === name!.trim());

      const invalidBiz = details.filter((d) => {
        if (!d.business) return false;
        if (!(d.business.businessNo?.trim() || d.business.company?.trim())) return false;
        return (
          !isValidCode(serviceCodes, d.business.serviceCode, d.business.serviceName) ||
          !isValidCode(itemCodes, d.business.itemCode, d.business.itemName)
        );
      });
      if (invalidBiz.length > 0) {
        warnings.push({ type: 'INVALID_BIZ_CODE', memIds: invalidBiz.map((d) => d.memId) });
      }

      const alreadyRegistered = targets.filter((m) => m.memberStatus === 'NORMAL');
      if (alreadyRegistered.length > 0) {
        warnings.push({ type: 'REGISTERED', memIds: alreadyRegistered.map((m) => m.memId) });
      }
      const inactivePartners = targets.filter((m) => m.memberStatus === 'INACTIVE');
      if (inactivePartners.length > 0) {
        warnings.push({ type: 'INACTIVE_PARTNER', memIds: inactivePartners.map((m) => m.memId) });
      }

      const withDuplicate = targets.filter(
        (m) => m.duplicateMemNo && !suppressedDupIds.has(m.memNo),
      );
      if (withDuplicate.length > 0) {
        warnings.push({ type: 'DUPLICATE', memIds: withDuplicate.map((m) => m.memId) });
      }

      if (warnings.length > 0) {
        setPendingDetails(details);
        setDownloadWarnings(warnings);
        return;
      }

      // xlsx 번들(~400kB)은 다운로드 시점에만 로드 (코드 분할)
      const { downloadMembersExcel } = await import('../utils/exportMembersExcel');
      downloadMembersExcel(details);
      showToast(`${details.length}명을 엑셀로 내려받았어요.`, 'success');
    } catch {
      showToast('엑셀 다운로드에 실패했어요. 다시 시도해 주세요.', 'error');
    } finally {
      setDownloading(false);
    }
  };

  const handleDownloadConfirm = async () => {
    if (!pendingDetails) return;
    try {
      const { downloadMembersExcel } = await import('../utils/exportMembersExcel');
      downloadMembersExcel(pendingDetails);
      showToast(`${pendingDetails.length}명을 엑셀로 내려받았어요.`, 'success');
    } catch {
      showToast('엑셀 다운로드에 실패했어요. 다시 시도해 주세요.', 'error');
    }
    setPendingDetails(null);
    setDownloadWarnings(null);
  };

  const handleDownloadCancel = () => {
    setDownloadWarnings(null);
    setPendingDetails(null);
  };

  // 체크한 회원 일괄 거래처 등록 — 실행 전 확인 모달을 띄운다
  // 거래처 코드가 없는 회원은 등록 대상에서 제외하고, 상세에서 코드 입력 후 등록하도록 안내
  const handleBulkPartnerRegister = () => {
    const targets = content.filter((m) => selectedIds.has(m.memNo));
    if (targets.length === 0 || registering) return;
    const ready = targets.filter((m) => m.partnerCode?.trim());
    const missing = targets.filter((m) => !m.partnerCode?.trim());
    setBulkConfirm({ ready, missing });
  };

  // 확인 모달에서 '등록' 클릭 시 실제 등록 — 기존 파트너 수정 API를 회원 수만큼 반복 호출
  const executeBulkRegister = async () => {
    if (!bulkConfirm) return;
    const targets = bulkConfirm.ready;
    setBulkConfirm(null);
    if (targets.length === 0) return;
    setRegistering(true);
    try {
      // SQLite는 동시 쓰기 락 충돌이 발생하므로 순차 실행
      let ok = 0;
      for (const m of targets) {
        try {
          await updateMemberPartner(m.memNo, {
            memberStatus: 'NORMAL',
            partnerCode: m.partnerCode,
            partnerName: m.partnerName,
            taxInvoiceFl: null,
            bizLicenseFile: null,
          });
          ok += 1;
        } catch {
          // 실패 건수는 아래에서 집계
        }
      }
      const failed = targets.length - ok;
      if (failed === 0) {
        showToast(`${ok}명을 거래처로 등록했어요.`, 'success');
      } else {
        showToast(`거래처 등록 완료 ${ok}건, 실패 ${failed}건이에요.`, failed === targets.length ? 'error' : 'success');
      }
      refresh();
    } finally {
      setRegistering(false);
    }
  };

  // 현재 페이지에 중복 강조(노란 행) 대상이 있는지 — 헤더 안내 아이콘 표시용
  const hasDup = content.some((m) => m.duplicateMemNo && !suppressedDupIds.has(m.memNo));

  // 검색·필터 조건이 하나라도 걸려 있는지 — 빈 목록 문구 분기용 (기본 가입일 7일 필터도 조건으로 취급)
  const hasActiveFilter = !!(
    searchParams.keyword ||
    searchParams.group !== 'ALL' ||
    searchParams.memberStatus ||
    searchParams.entryDtFrom ||
    searchParams.entryDtTo
  );

  const { sortKey, sortDir } = searchParams;
  const handleSort = useCallback((key: string) => {
    if (sortKey === key) {
      setSearchParams({ sortKey: key, sortDir: sortDir === 'asc' ? 'desc' : 'asc' });
    } else {
      setSearchParams({ sortKey: key, sortDir: 'desc' });
    }
  }, [sortKey, sortDir, setSearchParams]);

  const handlePage = useCallback((p: number) => setSearchParams({ page: p }), [setSearchParams]);
  const handleRowClick = useCallback((memNo: number) => setSelectedMemNo(memNo), []);
  const closeDetail = useCallback(() => setSelectedMemNo(null), []);

  const handleUploadSuccess = () => {
    reload();
  };

  return (
    <div className={styles.page}>
      <div className={styles.main}>
        <div className={styles.section}>
          <div className={styles.ucard}>
            {/* 카드 헤더 */}
            <div className={styles.cardHeader}>
              <span className={styles.cardTitle}>회원 목록</span>
              <span className={styles.pill}>{totalElements.toLocaleString()}명</span>
              {/* 중복 강조(노란 행) 안내 — hover 시 말풍선 툴팁 */}
              {hasDup && (
                <span className={styles.dupInfo} aria-label="중복 강조 안내">
                  i
                  <span className={styles.dupTooltip}>
                    노란색 행은 동일한 사업자번호로 가입한 중복 의심 회원입니다
                  </span>
                </span>
              )}
              <div className={styles.hdRight}>
                {lastSyncedAt && (
                  <span className={styles.cardMeta}>
                    마지막 업로드 {formatLastUpload(lastSyncedAt)}
                  </span>
                )}
                <span className={styles.cardMeta}>
                  회원 수집 {formatLastRun(syncSchedule).value} · {formatLastRun(syncSchedule).sub}
                </span>
                <button
                  type="button"
                  className={styles.btnPrimary}
                  onClick={() => void runMemberSync()}
                  disabled={syncing}
                >
                  {syncing ? '수집 중...' : '회원 수집'}
                </button>
                <button
                  type="button"
                  className={styles.btnOutline}
                  onClick={() => setUploadOpen(true)}
                >
                  엑셀 업로드
                </button>
                <button
                  type="button"
                  className={styles.btnOutline}
                  onClick={handleDownloadExcel}
                  disabled={selectedIds.size === 0 || downloading}
                >
                  {downloading
                    ? '다운로드 중...'
                    : `엑셀 다운로드${selectedIds.size > 0 ? ` (${selectedIds.size})` : ''}`}
                </button>
                <button
                  type="button"
                  className={styles.btnPrimary}
                  onClick={handleBulkPartnerRegister}
                  disabled={selectedIds.size === 0 || registering}
                >
                  {registering
                    ? '등록 중...'
                    : `거래처 일괄 등록${selectedIds.size > 0 ? ` (${selectedIds.size})` : ''}`}
                </button>
              </div>
            </div>

            {/* 검색 바 */}
            <MemberSearchBar
              searchParams={searchParams}
              onSearch={setSearchParams}
            />

            {/* 테이블 */}
            <MemberTable
              members={content}
              loading={loading}
              error={error}
              totalElements={totalElements}
              page={searchParams.page}
              totalPages={totalPages}
              sortKey={sortKey}
              sortDir={sortDir}
              onSort={handleSort}
              onPage={handlePage}
              onRowClick={handleRowClick}
              onReset={resetSearchParams}
              selectedIds={selectedIds}
              onToggleRow={toggleRow}
              onToggleAll={toggleAll}
              onDelete={setDeleteTarget}
              suppressedDupIds={suppressedDupIds}
              hasActiveFilter={hasActiveFilter}
            />
          </div>
        </div>
      </div>

      <MemberUploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSuccess={handleUploadSuccess}
      />

      <MemberDetailModal
        memNo={selectedMemNo}
        onClose={closeDetail}
        onToast={showToast}
        onSaved={refresh}
        onNavigate={handleRowClick}
        suppressDuplicate={selectedMemNo != null && suppressedDupIds.has(selectedMemNo)}
        serviceCodes={preloadedServiceCodes}
        itemCodes={preloadedItemCodes}
      />

      {deleteTarget && (
        <MemberDeleteModal
          member={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDeleted={refresh}
          onToast={showToast}
        />
      )}

      {downloadWarnings && (
        <ExcelDownloadWarningModal
          warnings={downloadWarnings}
          onConfirm={handleDownloadConfirm}
          onCancel={handleDownloadCancel}
        />
      )}

      {bulkConfirm && (
        <div className={modalStyles.modalBg} onClick={() => setBulkConfirm(null)}>
          <div className={modalStyles.modal} onClick={(e) => e.stopPropagation()}>
            <h3>거래처 일괄 등록</h3>
            {bulkConfirm.missing.length > 0 && (
              <div className={modalStyles.warnList}>
                <div className={[modalStyles.warnItem, styles.bulkCenter].join(' ')}>
                  <div className={modalStyles.warnItemTitle}>거래처 코드가 없는 회원 제외</div>
                  <div className={modalStyles.warnItemDesc}>
                    거래처 코드가 없어 일괄 등록에서 제외돼요.
                    <br />
                    상세 화면에서 거래처 코드를 입력하고 등록 상태로 변경해 주세요.
                  </div>
                  <div className={modalStyles.warnItemIds}>
                    {bulkConfirm.missing.map((m) => m.memId).join(', ')} ({bulkConfirm.missing.length}명)
                  </div>
                </div>
              </div>
            )}
            <p className={[modalStyles.confirmTextBlock, styles.bulkCenter].join(' ')}>
              {bulkConfirm.ready.length > 0
                ? `${bulkConfirm.ready.length}명을 거래처로 등록할까요?`
                : '등록할 수 있는 회원이 없어요.'}
            </p>
            <div className={modalStyles.modalFooter}>
              <button type="button" className={modalStyles.btnCancel} onClick={() => setBulkConfirm(null)}>
                취소
              </button>
              {bulkConfirm.ready.length > 0 && (
                <button type="button" className={modalStyles.btnSave} onClick={executeBulkRegister}>
                  등록 ({bulkConfirm.ready.length})
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {toast && <Toast message={toast.msg} type={toast.type} />}
    </div>
  );
};

export default MembersPage;
