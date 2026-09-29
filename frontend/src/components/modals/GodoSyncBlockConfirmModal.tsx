import { useState } from 'react';
import type { GoodsItem } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  item: GoodsItem;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

const GodoSyncBlockConfirmModal = ({ item, onClose, onConfirm }: Props) => {
  useLockBodyScroll();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const nextBlockedYn = item.godoSyncBlockedYn === 'Y' ? 'N' : 'Y';
  const isBlocking = nextBlockedYn === 'Y';

  const handleConfirm = () => {
    setConfirming(true);
    setError('');
    onConfirm().catch(() => {
      setError(
        isBlocking
          ? '전송 차단 설정에 실패했어요. 다시 시도해주세요.'
          : '전송 차단 해제에 실패했어요. 다시 시도해주세요.',
      );
      setConfirming(false);
    });
  };

  return (
    <>
      <div className={styles.modalBg} onClick={() => setShowCloseConfirm(true)}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>자동 동기화 여부 변경</h3>

          {error && <div className={styles.errBox}>{error}</div>}

          <div className={styles.confirmBody}>
            <strong className={styles.confirmName}>{item.goodsName}</strong>
            <p className={styles.confirmTextBlock}>
              {isBlocking
                ? '이 품목은 외부 API로 상태 변경 요청을 보내지 않게 됩니다.'
                : '이 품목은 다시 외부 API 상태 변경 전송 대상에 포함됩니다.'}
            </p>
            <p className={styles.confirmTextBlock}>
              {isBlocking ? '차단하시겠습니까?' : '허용하시겠습니까?'}
            </p>
          </div>

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
              {confirming ? '처리 중...' : '확인'}
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

export default GodoSyncBlockConfirmModal;
