import { useState } from 'react';
import { deleteGoods } from '../../api/goods';
import type { GoodsItem } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  item: GoodsItem;
  onClose: () => void;
  onDeleted: () => void;
  onToast: (msg: string, type: 'success' | 'error') => void;
}

const ProductDeleteModal = ({ item, onClose, onDeleted, onToast }: Props) => {
  useLockBodyScroll();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  const handleDelete = () => {
    setDeleting(true);
    setError('');
    deleteGoods(item.id)
      .then(() => {
        onDeleted();
        onClose();
        onToast('상품이 삭제되었어요.', 'success');
      })
      .catch(() => {
        setError('삭제에 실패했어요. 다시 시도해주세요.');
        setDeleting(false);
      });
  };

  const handleBackdropClick = () => setShowCloseConfirm(true);

  return (
    <>
      <div className={styles.modalBg} onClick={handleBackdropClick}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>상품 삭제</h3>

          {error && <div className={styles.errBox}>{error}</div>}

        <div className={styles.formRow}>
          <label>상품명</label>
          <input type="text" value={item.goodsName} disabled />
        </div>

        <div className={styles.formRow}>
          <label>고도몰 상품코드</label>
          <input type="text" value={item.godoGoodsCode} disabled />
        </div>

        <div className={styles.formRow}>
          <label>ERP 품목코드</label>
          <input type="text" value={item.erpGoodsCode} disabled />
        </div>

        <div className={styles.formRow}>
          <label>구분</label>
          <input
            type="text"
            value={item.onlyMallYn === 'Y' ? '전용' : '범용'}
            disabled
          />
        </div>

        <div className={`${styles.errBox} ${styles.warnBox}`}>
          삭제하면 자동화 대상에서 제외되며 복구할 수 없어요.
        </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.btnCancel} onClick={onClose} disabled={deleting}>
              취소
            </button>
            <button
              type="button"
              className={styles.btnDelCf}
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? '삭제 중...' : '삭제'}
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

export default ProductDeleteModal;
