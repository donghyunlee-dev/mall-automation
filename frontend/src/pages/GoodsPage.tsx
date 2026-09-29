import { useEffect, useState, useRef } from 'react';
import { getSession } from '../api/auth';
import { forceSyncGodoStatus, getGoodsStatusLogs, updateGodoSyncBlock } from '../api/goods';
import useBatch from '../hooks/useBatch';
import useGoods, { applyFilter, applySort } from '../hooks/useGoods';
import SummaryCards from '../components/dashboard/SummaryCards';
import ProductSearchBar from '../components/dashboard/ProductSearchBar';
import ProductTable from '../components/dashboard/ProductTable';
import HistoryTable from '../components/dashboard/HistoryTable';
import ProductAddModal from '../components/modals/ProductAddModal';
import ProductEditModal from '../components/modals/ProductEditModal';
import ProductDeleteModal from '../components/modals/ProductDeleteModal';
import ExcelUploadModal from '../components/modals/ExcelUploadModal';
import IntervalConfirmModal from '../components/modals/IntervalConfirmModal';
import StatusConfigModal from '../components/modals/StatusConfigModal';
import GodoSyncBlockConfirmModal from '../components/modals/GodoSyncBlockConfirmModal';
import ForceGodoStatusConfirmModal from '../components/modals/ForceGodoStatusConfirmModal';
import Toast from '../components/common/Toast';
import useTeamsNotification from '../hooks/useTeamsNotification';
import useCommonStatusConfig from '../hooks/useCommonStatusConfig';
import type { GoodsItem, GoodsStatusLog } from '../types/goods';
import type { CommonStatusConfig } from '../types/commonStatusConfig';
import { isSaleStatusSoldOut } from '../utils/goodsSaleStatus';
import { downloadGoodsExcel } from '../utils/exportGoodsExcel';
import { downloadGoodsStatusLogsExcel } from '../utils/exportGoodsStatusLogsExcel';
import buttonStyles from '../styles/buttonStyles.module.scss';
import styles from './GoodsPage.module.scss';

interface ToastState {
  msg: string;
  type: 'success' | 'error';
  id: number;
}

type LogRangePreset = 'today' | 'week' | 'oneMonth' | 'currentMonth' | 'previousMonth';

const formatDateInputValue = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const getTodayDateValue = (): string => formatDateInputValue(new Date());
const getDateDaysAgoValue = (days: number): string => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return formatDateInputValue(date);
};
const getDateMonthsAgoValue = (months: number): string => {
  const date = new Date();
  date.setMonth(date.getMonth() - months);
  return formatDateInputValue(date);
};
const getStartOfCurrentMonthValue = (): string => {
  const date = new Date();
  date.setDate(1);
  return formatDateInputValue(date);
};
const getStartOfPreviousMonthValue = (): string => {
  const date = new Date();
  date.setMonth(date.getMonth() - 1, 1);
  return formatDateInputValue(date);
};
const getEndOfPreviousMonthValue = (): string => {
  const date = new Date();
  date.setDate(0);
  return formatDateInputValue(date);
};
const DATE_INPUT_VALUE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const isValidDateInputValue = (value: string): boolean => {
  if (!DATE_INPUT_VALUE_PATTERN.test(value)) {
    return false;
  }

  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return formatDateInputValue(date) === value;
};

const getStartOfDaySoldOutCount = (
  items: GoodsItem[],
  logs: GoodsStatusLog[],
): number => {
  const soldOutStateByCode = new Map(
    items.map((item) => [item.godoGoodsCode, isSaleStatusSoldOut(item)]),
  );

  [...logs]
    .sort((a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime())
    .forEach((log) => {
      if (!soldOutStateByCode.has(log.godoGoodsCode)) {
        return;
      }
      soldOutStateByCode.set(
        log.godoGoodsCode,
        log.beforeStatus === 'OUT_OF_STOCK',
      );
    });

  return Array.from(soldOutStateByCode.values()).filter(Boolean).length;
};

const getDateRange = (fromDate: string, toDate: string): { changedFrom: string; changedTo: string } => {
  const [fromYear, fromMonth, fromDay] = fromDate.split('-').map(Number);
  const [toYear, toMonth, toDay] = toDate.split('-').map(Number);
  const start = new Date(fromYear, fromMonth - 1, fromDay);
  start.setHours(0, 0, 0, 0);
  const end = new Date(toYear, toMonth - 1, toDay);
  end.setHours(23, 59, 59, 999);
  return {
    changedFrom: start.toISOString(),
    changedTo: end.toISOString(),
  };
};

const GoodsPage = () => {
  const [email, setEmail] = useState('');
  const [todayLogs, setTodayLogs] = useState<GoodsStatusLog[]>([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<GoodsItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<GoodsItem | null>(null);
  const [excelModalOpen, setExcelModalOpen] = useState(false);
  const [statusConfigModalOpen, setStatusConfigModalOpen] = useState(false);
  const [godoSyncBlockTarget, setGodoSyncBlockTarget] = useState<GoodsItem | null>(null);
  const [forceSyncTarget, setForceSyncTarget] = useState<GoodsItem | null>(null);
  const [intervalConfirmMinutes, setIntervalConfirmMinutes] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastState | null>(null);
  const [logFromDate, setLogFromDate] = useState(getTodayDateValue());
  const [logToDate, setLogToDate] = useState(getTodayDateValue());
  const [logDateRangeError, setLogDateRangeError] = useState('');
  const [selectedLogRangePreset, setSelectedLogRangePreset] = useState<LogRangePreset | null>('today');

  const productTableRef = useRef<HTMLDivElement>(null);
  const historyTableRef = useRef<HTMLDivElement>(null);

  useTeamsNotification();
  const {
    configs: statusConfigs,
    loading: statusConfigLoading,
    saving: statusConfigSaving,
    error: statusConfigError,
    updateConfig,
  } = useCommonStatusConfig();

  const { schedule, running, runNow, applyInterval, load: loadBatch } = useBatch();

  const {
    allItems,
    filteredItems,
    pagedItems,
    loading: goodsLoading,
    error: goodsError,
    keyword,
    filter,
    sortKey,
    sortDir,
    page,
    pageSize,
    totalPages,
    setKeyword,
    setStatusFilter,
    setTypeFilter,
    setBlockFilter,
    setPageSize,
    toggleSort,
    setPage,
    reload: reloadGoods,
  } = useGoods();

  // 세션에서 이메일 로드 (배치 간격 변경 API에 사용)
  useEffect(() => {
    getSession()
      .then((s) => setEmail(s.email))
      .catch(() => undefined);
  }, []);

  // 선택 일자 변경 이력 로드
  useEffect(() => {
    if (!isValidDateInputValue(logFromDate) || !isValidDateInputValue(logToDate)) {
      return;
    }

    const range = getDateRange(logFromDate, logToDate);
    getGoodsStatusLogs(range)
      .then((res) => setTodayLogs(res.items))
      .catch(() => undefined);
  }, [refreshKey, logFromDate, logToDate]);

  // 토스트 자동 닫기
  useEffect(() => {
    if (!toast) return;
    const duration = toast.type === 'error' ? 6000 : 2500;
    const t = setTimeout(() => setToast(null), duration);
    return () => clearTimeout(t);
  }, [toast]);

  const showToast = (msg: string, type: 'success' | 'error') => {
    setToast({ msg, type, id: Date.now() });
  };

  const handleChangeLogFromDate = (nextFromDate: string) => {
    if (!isValidDateInputValue(nextFromDate)) {
      setLogDateRangeError('올바른 날짜를 선택해주세요.');
      return;
    }

    const todayDate = getTodayDateValue();
    if (nextFromDate > todayDate) {
      setLogDateRangeError('시작일은 오늘보다 뒤로 설정할 수 없어요.');
      return;
    }
    if (nextFromDate > logToDate) {
      setLogDateRangeError('시작일은 종료일보다 뒤일 수 없어요.');
      return;
    }
    setLogDateRangeError('');
    setSelectedLogRangePreset(null);
    setLogFromDate(nextFromDate);
  };

  const handleChangeLogToDate = (nextToDate: string) => {
    if (!isValidDateInputValue(nextToDate)) {
      setLogDateRangeError('올바른 날짜를 선택해주세요.');
      return;
    }

    const todayDate = getTodayDateValue();
    if (nextToDate > todayDate) {
      setLogDateRangeError('종료일은 오늘보다 뒤로 설정할 수 없어요.');
      return;
    }
    if (nextToDate < logFromDate) {
      setLogDateRangeError('종료일은 시작일보다 앞설 수 없어요.');
      return;
    }
    setLogDateRangeError('');
    setSelectedLogRangePreset(null);
    setLogToDate(nextToDate);
  };

  const handleSelectLogRangePreset = (preset: LogRangePreset) => {
    const todayDate = getTodayDateValue();

    switch (preset) {
      case 'today':
        setLogFromDate(todayDate);
        setLogToDate(todayDate);
        break;
      case 'week':
        setLogFromDate(getDateDaysAgoValue(6));
        setLogToDate(todayDate);
        break;
      case 'oneMonth':
        setLogFromDate(getDateMonthsAgoValue(1));
        setLogToDate(todayDate);
        break;
      case 'currentMonth':
        setLogFromDate(getStartOfCurrentMonthValue());
        setLogToDate(todayDate);
        break;
      case 'previousMonth':
        setLogFromDate(getStartOfPreviousMonthValue());
        setLogToDate(getEndOfPreviousMonthValue());
        break;
      default:
        break;
    }

    setLogDateRangeError('');
    setSelectedLogRangePreset(preset);
  };

  const handleRunNow = () => {
    runNow(() => {
      reloadGoods();
      setRefreshKey((k) => k + 1);
    });
  };

  // 배치 간격 변경 — 확인 모달 열기
  const handleRequestInterval = (minutes: number) => {
    setIntervalConfirmMinutes(minutes);
  };

  // 간격 변경 확인 후 API 호출
  const handleIntervalConfirm = async () => {
    if (intervalConfirmMinutes === null) return;
    await applyInterval(intervalConfirmMinutes, email);
    await loadBatch();
    showToast(`배치 실행 간격이 ${intervalConfirmMinutes}분으로 변경되었어요.`, 'success');
    setIntervalConfirmMinutes(null);
  };

  const handleClickSoldOut = () => {
    setStatusFilter('out');
    productTableRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleClickChanges = () => {
    historyTableRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleEdit = (item: GoodsItem) => setEditTarget(item);
  const handleDelete = (item: GoodsItem) => setDeleteTarget(item);
  const handleRequestGodoSyncBlockToggle = (item: GoodsItem) => setGodoSyncBlockTarget(item);
  const handleRequestForceSync = (item: GoodsItem) => setForceSyncTarget(item);

  const handleGoodsSaved = async () => {
    await reloadGoods();
  };

  const handleGoodsAdded = async (newGoodsCode: string) => {
    const freshItems = await reloadGoods();
    const filtered = applyFilter(freshItems, filter, keyword);
    const sorted = applySort(filtered, sortKey, sortDir);
    const idx = sorted.findIndex((item) => item.godoGoodsCode === newGoodsCode);
    if (idx >= 0) {
      setPage(Math.floor(idx / pageSize) + 1);
    }
  };

  const handleSaveStatusConfigs = async (nextConfigs: CommonStatusConfig[]) => {
    for (const config of nextConfigs) {
      await updateConfig(config.scope, {
        outOfStockQty: config.outOfStockQty,
        onSaleQty: config.onSaleQty,
        safetyRate: config.safetyRate,
        remainValidDays: config.remainValidDays,
      });
    }
    showToast('상태 기준이 저장되었어요.', 'success');
  };

  const handleConfirmGodoSyncBlockToggle = async () => {
    if (!godoSyncBlockTarget) return;

    const nextBlockedYn = godoSyncBlockTarget.godoSyncBlockedYn === 'Y' ? 'N' : 'Y';
    await updateGodoSyncBlock(godoSyncBlockTarget.id, { blockedYn: nextBlockedYn });
    await reloadGoods();
    showToast(
      nextBlockedYn === 'Y'
        ? '상품 API 전송이 차단되었어요.'
        : '상품 API 전송 차단이 해제되었어요.',
      'success',
    );
    setGodoSyncBlockTarget(null);
  };

  const handleConfirmForceSync = async () => {
    if (!forceSyncTarget) return;

    const targetSoldOutYn = forceSyncTarget.soldOutYn === 'Y' ? 'N' : 'Y';
    const targetLabel = targetSoldOutYn === 'Y' ? '품절' : '판매중';
    const response = await forceSyncGodoStatus({
      godoGoodsCodes: [forceSyncTarget.godoGoodsCode],
      targetSoldOutYn,
    });
    const result = response.items.find(
      (item) => item.godoGoodsCode === forceSyncTarget.godoGoodsCode,
    );

    if (!result || result.result !== 'SUCCESS') {
      throw new Error(result?.message || `판매 상태를 ${targetLabel}으로 전송하지 못했어요.`);
    }

    await reloadGoods();
    setRefreshKey((k) => k + 1);
    showToast(`판매 상태를 ${targetLabel}으로 강제 전송했어요.`, 'success');
    setForceSyncTarget(null);
  };

  const handleDownloadExcel = () => {
    if (goodsLoading) return;
    if (allItems.length === 0) {
      showToast('다운로드할 상품 데이터가 없어요.', 'error');
      return;
    }

    try {
      const fileName = downloadGoodsExcel(allItems);
      showToast(`${fileName} 파일을 다운로드했어요.`, 'success');
    } catch {
      showToast('엑셀 다운로드에 실패했어요. 다시 시도해주세요.', 'error');
    }
  };

  const handleDownloadHistoryExcel = () => {
    if (todayLogs.length === 0) {
      showToast('다운로드할 변경 이력이 없어요.', 'error');
      return;
    }

    try {
      const fileName = downloadGoodsStatusLogsExcel(todayLogs, allItems);
      showToast(`${fileName} 파일을 다운로드했어요.`, 'success');
    } catch {
      showToast('변경 이력 엑셀 다운로드에 실패했어요. 다시 시도해주세요.', 'error');
    }
  };

  const soldOutCount = allItems.filter((g) => isSaleStatusSoldOut(g)).length;
  const todaySoldOut = todayLogs.filter((l) => l.afterStatus === 'OUT_OF_STOCK').length;
  const todayOnSale = todayLogs.filter((l) => l.afterStatus === 'ON_SALE').length;
  const startOfDaySoldOutCount = getStartOfDaySoldOutCount(allItems, todayLogs);
  const soldOutDelta = soldOutCount - startOfDaySoldOutCount;

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        {/* 현황 요약 */}
        <div className={styles.section}>
          <SummaryCards
            schedule={schedule}
            totalGoods={allItems.length}
            soldOutCount={soldOutCount}
            soldOutDelta={soldOutDelta}
            todayChangedCount={todayLogs.length}
            todaySoldOutCount={todaySoldOut}
            todayOnSaleCount={todayOnSale}
            running={running}
            onRunNow={handleRunNow}
            onRequestIntervalChange={handleRequestInterval}
            onClickSoldOut={handleClickSoldOut}
            onClickChanges={handleClickChanges}
          />
        </div>

        {/* 일자별 변경 이력 */}
        <div className={styles.section} ref={historyTableRef}>
          <div className={`${styles.ucard} ${styles.ucardOverflowVisible}`}>
            <HistoryTable
              logs={todayLogs}
              allItems={allItems}
              fromDate={logFromDate}
              toDate={logToDate}
              maxDate={getTodayDateValue()}
              onChangeFromDate={handleChangeLogFromDate}
              onChangeToDate={handleChangeLogToDate}
              onSelectRangePreset={handleSelectLogRangePreset}
              selectedRangePreset={selectedLogRangePreset}
              dateRangeError={logDateRangeError}
              onDownloadExcel={handleDownloadHistoryExcel}
              downloadDisabled={todayLogs.length === 0}
            />
          </div>
        </div>

        {/* 전체 상품 목록 */}
        <div className={styles.section} ref={productTableRef}>
          <div className={styles.ucard}>
            <div className={styles.cardHeader}>
              <span className={styles.cardTitle}>전체 상품 목록</span>
              <span className={styles.pill}>
                {filteredItems.length}/{allItems.length}개
              </span>
              <div className={styles.hdRight}>
                <button
                  type="button"
                  className={buttonStyles.btnSmallMuted}
                  onClick={() => setStatusConfigModalOpen(true)}
                >
                  상태 기준 설정
                </button>
                <button
                  type="button"
                  className={buttonStyles.btnSmallOutline}
                  onClick={() => setExcelModalOpen(true)}
                >
                  엑셀 업로드
                </button>
                <button
                  type="button"
                  className={buttonStyles.btnSmallOutline}
                  onClick={handleDownloadExcel}
                  disabled={goodsLoading}
                >
                  엑셀 다운로드
                </button>
                <button
                  type="button"
                  className={styles.btnSmall}
                  onClick={() => setAddModalOpen(true)}
                >
                  + 상품 추가
                </button>
              </div>
            </div>
            <ProductSearchBar
              keyword={keyword}
              filter={filter}
              pageSize={pageSize}
              onKeyword={setKeyword}
              onStatusFilter={setStatusFilter}
              onTypeFilter={setTypeFilter}
              onBlockFilter={setBlockFilter}
              onPageSize={setPageSize}
            />
            <ProductTable
              items={pagedItems}
              loading={goodsLoading}
              error={goodsError}
              sortKey={sortKey}
              sortDir={sortDir}
              page={page}
              pageSize={pageSize}
              totalPages={totalPages}
              onSort={toggleSort}
              onPage={setPage}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onToggleGodoSyncBlock={handleRequestGodoSyncBlockToggle}
              onForceGodoStatusSync={handleRequestForceSync}
            />
          </div>
        </div>
      </main>

      {addModalOpen && (
        <ProductAddModal
          statusConfigs={statusConfigs}
          onClose={() => setAddModalOpen(false)}
          onSaved={handleGoodsAdded}
          onToast={showToast}
        />
      )}

      {editTarget && (
        <ProductEditModal
          item={editTarget}
          statusConfigs={statusConfigs}
          onClose={() => setEditTarget(null)}
          onSaved={handleGoodsSaved}
          onToast={showToast}
        />
      )}

      {deleteTarget && (
        <ProductDeleteModal
          item={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDeleted={handleGoodsSaved}
          onToast={showToast}
        />
      )}

      {excelModalOpen && (
        <ExcelUploadModal
          onClose={() => setExcelModalOpen(false)}
          onUploaded={handleGoodsSaved}
          onToast={showToast}
        />
      )}

      {statusConfigModalOpen && (
        <StatusConfigModal
          configs={statusConfigs}
          loading={statusConfigLoading}
          saving={statusConfigSaving}
          error={statusConfigError}
          onClose={() => setStatusConfigModalOpen(false)}
          onSave={handleSaveStatusConfigs}
        />
      )}

      {godoSyncBlockTarget && (
        <GodoSyncBlockConfirmModal
          item={godoSyncBlockTarget}
          onClose={() => setGodoSyncBlockTarget(null)}
          onConfirm={handleConfirmGodoSyncBlockToggle}
        />
      )}

      {forceSyncTarget && (
        <ForceGodoStatusConfirmModal
          item={forceSyncTarget}
          onClose={() => setForceSyncTarget(null)}
          onConfirm={handleConfirmForceSync}
        />
      )}

      {intervalConfirmMinutes !== null && (
        <IntervalConfirmModal
          minutes={intervalConfirmMinutes}
          onClose={() => setIntervalConfirmMinutes(null)}
          onConfirm={handleIntervalConfirm}
        />
      )}

      {toast && <Toast message={toast.msg} type={toast.type} />}
    </div>
  );
};

export default GoodsPage;
