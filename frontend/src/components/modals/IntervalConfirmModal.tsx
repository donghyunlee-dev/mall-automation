import { useState } from 'react';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  minutes: number;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

const IntervalConfirmModal = ({ minutes, onClose, onConfirm }: Props) => {
  useLockBodyScroll();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  const handleConfirm = () => {
    setConfirming(true);
    setError('');
    onConfirm()
      .catch(() => {
        setError('간격 변경에 실패했어요. 다시 시도해주세요.');
        setConfirming(false);
      });
  };

  const handleBackdropClick = () => setShowCloseConfirm(true);

  return (
    <>
      <div className={styles.modalBg} onClick={handleBackdropClick}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>배치 간격 변경</h3>

          {error && <div className={styles.errBox}>{error}</div>}

          <p style={{ fontSize: 13, color: 'var(--text)', lineHeight: 1.6, margin: '0 0 1.1rem' }}>
            배치 실행 간격을 <strong>{minutes}분</strong>으로 변경할까요?
          </p>

          <div className={styles.modalFooter}>
            <button
              type="button"
              className={styles.btnCancel}
              onClick={onClose}
              disabled={confirming}
            >
              취소
            </button>
            <button
              type="button"
              className={styles.btnSave}
              onClick={handleConfirm}
              disabled={confirming}
            >
              {confirming ? '적용 중...' : '확인'}
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

export default IntervalConfirmModal;
