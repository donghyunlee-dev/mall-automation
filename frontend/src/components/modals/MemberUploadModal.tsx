import { useRef, useState } from 'react';
import useMemberUpload from '../../hooks/useMemberUpload';
import modalStyles from './Modal.module.scss';
import styles from './MemberUploadModal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const TYPE_LABELS: Record<string, string> = {
  LIST: '고도몰 회원 리스트 파일로 인식됨',
  TEMPLATE: '에쓰푸드몰 관리 템플릿 파일로 인식됨',
};

const MemberUploadModal = ({ open, onClose, onSuccess }: Props) => {
  useLockBodyScroll(open);
  const { status, detectedType, result, error, isDamagedFile, upload, reset } = useMemberUpload();
  const [file, setFile] = useState<File | null>(null);
  const [applyDeletion, setApplyDeletion] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  if (!open) return null;

  const handleFile = (f: File) => {
    const ext = f.name.split('.').pop()?.toLowerCase();
    if (ext !== 'xlsx' && ext !== 'xls') return;
    setFile(f);
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
    reset();
    if (inputRef.current) inputRef.current.value = '';
  };

  const handleUpload = () => {
    if (!file || status === 'parsing' || status === 'uploading') return;
    void upload(file, applyDeletion);
  };

  const handleClose = () => {
    if (status === 'parsing' || status === 'uploading') return;
    reset();
    setFile(null);
    setApplyDeletion(false);
    onClose();
  };

  const handleDone = () => {
    onSuccess();
    handleClose();
  };

  const isBusy = status === 'parsing' || status === 'uploading';

  return (
    <div className={modalStyles.modalBg} onClick={handleClose}>
      <div className={modalStyles.modal} onClick={(e) => e.stopPropagation()}>
        <h3>회원 엑셀 업로드</h3>

        {/* 단계: 완료 */}
        {status === 'done' && result && (
          <>
            <div className={styles.doneTitle}>✅ 업로드 완료 · {result.syncedAt.slice(0, 16).replace('T', ' ')}</div>
            <div className={styles.resultGrid}>
              <div className={styles.resultCell}>
                <div className={styles.rcNum}>{result.totalInserted.toLocaleString()}</div>
                <div className={styles.rcLbl}>신규</div>
              </div>
              <div className={styles.resultCell}>
                <div className={styles.rcNum}>{result.totalUpdated.toLocaleString()}</div>
                <div className={styles.rcLbl}>갱신</div>
              </div>
              <div className={styles.resultCell}>
                <div className={styles.rcNum}>{result.totalDeleted.toLocaleString()}</div>
                <div className={styles.rcLbl}>탈퇴 처리</div>
              </div>
              <div className={[styles.resultCell, styles.rcFail].join(' ')}>
                <div className={styles.rcNum}>{result.totalFailed.toLocaleString()}</div>
                <div className={styles.rcLbl}>실패</div>
              </div>
            </div>
            {result.deletionSkipped && (
              <div className={styles.warnBox}>
                삭제 대상이 전체 회원의 10%를 초과하여 탈퇴 처리가 보류되었습니다. 전체 파일인지 확인 후 재시도해 주세요.
              </div>
            )}
            {result.totalFailed > 0 && (
              <div className={styles.failBox}>
                실패 {result.totalFailed}건 — {result.failedRows.slice(0, 3).map((r) => `${r.rowIndex + 1}행: ${r.reason}`).join(' / ')}
                {result.failedRows.length > 3 && ` 외 ${result.failedRows.length - 3}건`}
              </div>
            )}
            <div className={modalStyles.modalFooter}>
              <button type="button" className={modalStyles.btnSave} onClick={handleDone}>
                닫기
              </button>
            </div>
          </>
        )}

        {/* 단계: 업로드 중 */}
        {isBusy && (
          <div className={styles.busyBox}>
            <span className={styles.busySpinner} />
            회원 데이터를 처리하고 있어요...
          </div>
        )}

        {/* 단계: 파일 선택 */}
        {(status === 'idle' || status === 'error') && (
          <>
            {error && <div className={modalStyles.errBox}>{error}</div>}

            {!file ? (
              <div
                className={[styles.dropZone, dragging ? styles.dragging : ''].join(' ')}
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
                <div className={styles.dropIcon}>📄</div>
                <div className={styles.dropText}>
                  <b>엑셀 파일을 끌어다 놓거나 클릭</b>해서 선택하세요
                </div>
                <div className={styles.dropHint}>.xls · .xlsx</div>
              </div>
            ) : (
              <div className={styles.fileRow}>
                <span>📄 {file.name}</span>
                <button
                  type="button"
                  className={styles.removeBtn}
                  onClick={handleRemoveFile}
                >
                  ×
                </button>
              </div>
            )}

            {file && isDamagedFile && (
              <div className={styles.warnBox}>
                이 파일은 업로드할 수 없습니다. × 버튼으로 삭제 후 다른 파일을 선택해 주세요.
              </div>
            )}

            {file && detectedType && !isDamagedFile && (
              <div className={styles.detectBox}>
                {TYPE_LABELS[detectedType] ?? detectedType}
              </div>
            )}

            {file && !isDamagedFile && (
              <label className={styles.optionRow}>
                <input
                  type="checkbox"
                  checked={applyDeletion}
                  onChange={(e) => setApplyDeletion(e.target.checked)}
                />
                <span className={styles.optionText}>
                  <b>미포함 회원 탈퇴 처리</b>
                  <small>전체 명단 업로드 시에만 체크하세요.</small>
                </span>
              </label>
            )}

            <div className={modalStyles.modalFooter}>
              <button type="button" className={modalStyles.btnCancel} onClick={handleClose}>
                취소
              </button>
              <button
                type="button"
                className={modalStyles.btnSave}
                onClick={handleUpload}
                disabled={!file || isDamagedFile}
              >
                업로드
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default MemberUploadModal;
