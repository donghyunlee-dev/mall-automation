import { useRef, useState } from 'react';
import axios from 'axios';
import * as XLSX from 'xlsx';
import { createGoods } from '../../api/goods';
import type { GoodsCreateItem, YN } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import uploadStyles from './ExcelUploadModal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  onClose: () => void;
  onUploaded: () => void;
  onToast: (msg: string, type: 'success' | 'error') => void;
}

const TEMPLATE_COLUMNS = [
  { column: 'A열', label: '고도몰 상품코드', example: '예: 1098701' },
  { column: 'B열', label: 'ERP 품목코드', example: '예: N14670100' },
  { column: 'C열', label: '상품명', example: '예: 모둠전 500g' },
  { column: 'D열', label: '구분', example: '범용 또는 전용' },
] as const;

const GODO_API_ERROR_MESSAGE = '엑셀 양식 안내를 확인하고 수정해서 다시 업로드해주세요.';

const getCreateGoodsErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error) && error.response?.data?.code === 'GODO_API_ERROR') {
    return GODO_API_ERROR_MESSAGE;
  }
  return fallback;
};

const parseOnlyMallYn = (val: unknown): YN => {
  const s = String(val ?? '').trim();
  if (s === '전용') return 'Y';
  if (s === '범용' || s === '') return 'N';
  throw new Error("구분 컬럼은 '범용' 또는 '전용'만 입력할 수 있어요. 엑셀 양식 안내를 확인하고 수정해주세요.");
};

const parseRows = (file: File): Promise<GoodsCreateItem[]> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = e.target?.result;
        const wb = XLSX.read(data, { type: 'array' });
        const ws = wb.Sheets[wb.SheetNames[0]];
        const rows = XLSX.utils.sheet_to_json<string[]>(ws, { header: 1 }) as unknown[][];
        const items: GoodsCreateItem[] = rows
          .slice(1)
          .filter((r) => r[0] || r[1] || r[2])
          .map((r, index) => {
            try {
              return {
                godoGoodsCode: String(r[0] ?? '').trim(),
                erpGoodsCode: String(r[1] ?? '').trim(),
                goodsName: String(r[2] ?? '').trim(),
                onlyMallYn: parseOnlyMallYn(r[3]),
              };
            } catch (error) {
              const rowNumber = index + 2;
              const message = error instanceof Error ? error.message : '엑셀 양식이 올바르지 않아요.';
              throw new Error(`${rowNumber}행 오류: ${message}`);
            }
          })
          .filter((item) => item.godoGoodsCode && item.erpGoodsCode && item.goodsName);
        resolve(items);
      } catch (error) {
        reject(error instanceof Error ? error : new Error('파일을 파싱할 수 없어요.'));
      }
    };
    reader.onerror = () => reject(new Error('파일을 읽을 수 없어요.'));
    reader.readAsArrayBuffer(file);
  });

const downloadTemplate = (onToast: Props['onToast']) => {
  const wb = XLSX.utils.book_new();
  const wsData = [
    ['고도몰 상품코드', 'ERP 품목코드', '상품명', '구분'],
    ['1098701', 'N14670100', '에쓰푸드 모둠전 500g', '범용'],
    ['1098702', 'N14670200', '에쓰푸드 불고기 400g', '전용'],
  ];
  const ws = XLSX.utils.aoa_to_sheet(wsData);
  ws['!cols'] = [{ wch: 18 }, { wch: 16 }, { wch: 28 }, { wch: 10 }];
  XLSX.utils.book_append_sheet(wb, ws, '상품목록');
  XLSX.writeFile(wb, '판매상태자동화_상품목록_양식.xlsx');
  onToast('양식 파일을 다운로드했어요.', 'success');
};

const ExcelUploadModal = ({ onClose, onUploaded, onToast }: Props) => {
  useLockBodyScroll();
  const [file, setFile] = useState<File | null>(null);
  const [rows, setRows] = useState<GoodsCreateItem[]>([]);
  const [parseError, setParseError] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [dragging, setDragging] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = (f: File) => {
    const ext = f.name.split('.').pop()?.toLowerCase();
    if (ext !== 'xlsx' && ext !== 'xls') {
      setParseError('xlsx 또는 xls 파일만 업로드할 수 있어요.');
      setFile(null);
      setRows([]);
      return;
    }
    setParseError('');
    setUploadError('');
    setFile(f);
    parseRows(f)
      .then((items) => setRows(items))
      .catch((err: Error) => {
        setParseError(err.message);
        setFile(null);
        setRows([]);
      });
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) handleFile(f);
  };

  const handleRemoveFile = () => {
    setFile(null);
    setRows([]);
    setParseError('');
    setUploadError('');
    if (inputRef.current) inputRef.current.value = '';
  };

  const handleUpload = () => {
    if (!file || rows.length === 0 || uploading) return;
    setUploading(true);
    setUploadError('');
    createGoods(rows)
      .then((res) => {
        onUploaded();
        onClose();
        onToast(`${res.items.length}개 상품을 업로드했어요.`, 'success');
      })
      .catch((error: unknown) => {
        setUploadError(getCreateGoodsErrorMessage(error, '업로드에 실패했어요. 다시 시도해주세요.'));
        setUploading(false);
      });
  };

  const canUpload = file !== null && rows.length > 0 && !uploading && !parseError;
  const handleBackdropClick = () => setShowCloseConfirm(true);

  return (
    <>
      <div className={styles.modalBg} onClick={handleBackdropClick}>
        <div
          className={`${styles.modal} ${styles.modalWide}`}
          onClick={(e) => e.stopPropagation()}
        >
          <h3>엑셀 업로드</h3>

          {(parseError || uploadError) && (
            <div className={styles.errBox}>{parseError || uploadError}</div>
          )}

          <div className={uploadStyles.formatGuide}>
            <div className={uploadStyles.guideTitle}>📋 엑셀 양식 컬럼 (순서 준수)</div>
            <div className={uploadStyles.guideGrid}>
              {TEMPLATE_COLUMNS.map((item) => (
                <div key={item.column} className={uploadStyles.guideCard}>
                  <div className={uploadStyles.guideColumn}>{item.column}</div>
                  <div className={uploadStyles.guideLabel}>{item.label}</div>
                  <div className={uploadStyles.guideExample}>{item.example}</div>
                </div>
              ))}
            </div>
            <div className={uploadStyles.guideActions}>
              <button
                type="button"
                className={uploadStyles.templateLink}
                onClick={() => downloadTemplate(onToast)}
                disabled={uploading}
              >
                📥 양식 파일 다운로드 (.xlsx)
              </button>
            </div>
          </div>

          {file ? (
            <div className={uploadStyles.fileInfo}>
              <span className={uploadStyles.fileIcon}>📊</span>
              <div className={uploadStyles.fileMeta}>
                <span className={uploadStyles.fileName}>{file.name}</span>
                <span className={uploadStyles.rowCount}>{rows.length}개 행 준비됨</span>
              </div>
              <button
                type="button"
                className={uploadStyles.removeBtn}
                onClick={handleRemoveFile}
                disabled={uploading}
              >
                삭제
              </button>
            </div>
          ) : (
            <div
              className={`${uploadStyles.dropZone} ${dragging ? uploadStyles.dragging : ''}`}
              onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
              onDragLeave={() => setDragging(false)}
              onDrop={handleDrop}
              onClick={() => inputRef.current?.click()}
            >
              <input
                ref={inputRef}
                type="file"
                accept=".xlsx,.xls"
                style={{ display: 'none' }}
                onChange={handleInputChange}
              />
              <div className={uploadStyles.dropIcon}>📊</div>
              <div className={uploadStyles.dropText}>
                엑셀 파일을 드래그하거나 클릭해서 선택
              </div>
              <div className={uploadStyles.dropHint}>.xlsx · .xls 지원</div>
            </div>
          )}

          <div className={styles.modalFooter}>
            <button type="button" className={styles.btnCancel} onClick={onClose} disabled={uploading}>
              취소
            </button>
            <button
              type="button"
              className={styles.btnSave}
              onClick={handleUpload}
              disabled={!canUpload}
            >
              {uploading ? '업로드 중...' : '업로드'}
            </button>
          </div>
        </div>
      </div>
      {showCloseConfirm && (
        <CloseConfirmModal
          onCancel={() => setShowCloseConfirm(false)}
          onConfirm={onClose}
        />
      )}
    </>
  );
};

export default ExcelUploadModal;
