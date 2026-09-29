import { useState } from 'react';
import type { GoodsItem, YN } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  item: GoodsItem;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}

const getTargetSoldOutYn = (item: GoodsItem): YN => (item.soldOutYn === 'Y' ? 'N' : 'Y');

const ForceGodoStatusConfirmModal = ({ item, onClose, onConfirm }: Props) => {
  useLockBodyScroll();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const targetSoldOutYn = getTargetSoldOutYn(item);
  const targetLabel = targetSoldOutYn === 'Y' ? '품절' : '판매중';

  const handleConfirm = () => {
    setConfirming(true);
    setError('');
    onConfirm().catch((err) => {
      setError(err instanceof Error ? err.message : '강제 상태 전송에 실패했어요. 다시 시도해주세요.');
      setConfirming(false);
    });
  };

  return (
    <>
      <div className={styles.modalBg} onClick={() => setShowCloseConfirm(true)}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>판매 상태 강제 전송</h3>

          {error && <div className={styles.errBox}>{error}</div>}

          <div className={styles.confirmBody}>
            <strong className={styles.confirmName}>{item.goodsName}</strong>
            <p className={styles.confirmTextBlock}>
              배치 조건과 관계없이 고도몰에 {targetLabel} 상태를 강제로 전송합니다.
            </p>
            <p className={styles.confirmTextBlock}>
              응답이 성공한 경우에만 화면 상태가 갱신됩니다.
            </p>
            <p className={styles.confirmTextBlock}>
              {targetLabel} 상태로 전송하시겠습니까?
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
              {confirming ? '전송 중...' : '확인'}
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

export default ForceGodoStatusConfirmModal;
