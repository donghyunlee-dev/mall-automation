import { useState } from 'react';
import axios from 'axios';
import { createGoods } from '../../api/goods';
import type { CommonStatusConfig } from '../../types/commonStatusConfig';
import type { YN } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  statusConfigs: CommonStatusConfig[];
  onClose: () => void;
  onSaved: (godoGoodsCode: string) => void;
  onToast: (msg: string, type: 'success' | 'error') => void;
}

const GODO_API_ERROR_MESSAGE = '고도몰 상품코드를 숫자로 입력했는지 확인해주세요.';
const DUPLICATE_GOODS_ERROR_MESSAGE = '코드 중복 여부를 확인한 후 다시 작성해주세요.';

const getCreateGoodsErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const errorCode = error.response?.data?.code;
    if (errorCode === 'GODO_API_ERROR') {
      return GODO_API_ERROR_MESSAGE;
    }
    if (errorCode === 'DUPLICATE_GOODS') {
      return DUPLICATE_GOODS_ERROR_MESSAGE;
    }
  }
  return fallback;
};

const getConfigReferenceText = (
  statusConfigs: CommonStatusConfig[],
  onlyMallYn: YN,
): string => {
  const scope = onlyMallYn === 'Y' ? 'EXCLUSIVE' : 'GENERAL';
  const config = statusConfigs.find((item) => item.scope === scope);

  if (!config) {
    return '현재 상태 기준을 불러오는 중이에요.';
  }

  return `현재 기준: 품절 ${config.outOfStockQty} 이하 / 판매 ${config.onSaleQty} 이상 / 잔여율 ${config.safetyRate}% / 잔여일 ${config.remainValidDays}일`;
};

const ProductAddModal = ({ statusConfigs, onClose, onSaved, onToast }: Props) => {
  useLockBodyScroll();
  const [godoGoodsCode, setGodoGoodsCode] = useState('');
  const [erpGoodsCode, setErpGoodsCode] = useState('');
  const [goodsName, setGoodsName] = useState('');
  const [onlyMallYn, setOnlyMallYn] = useState<YN>('N');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  const canSave = godoGoodsCode.trim() && erpGoodsCode.trim() && goodsName.trim() && !saving;

  const handleSave = () => {
    if (!canSave) return;
    setSaving(true);
    setError('');
    createGoods([
      {
        godoGoodsCode: godoGoodsCode.trim(),
        erpGoodsCode: erpGoodsCode.trim(),
        goodsName: goodsName.trim(),
        onlyMallYn,
      },
    ])
      .then(() => {
        onSaved(godoGoodsCode.trim());
        onClose();
        onToast('상품이 추가되었어요.', 'success');
      })
      .catch((error: unknown) => {
        setError(getCreateGoodsErrorMessage(error, '상품 추가에 실패했어요. 다시 시도해주세요.'));
        setSaving(false);
      });
  };

  const handleBackdropClick = () => setShowCloseConfirm(true);

  return (
    <>
      <div className={styles.modalBg} onClick={handleBackdropClick}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>상품 추가</h3>

          {error && <div className={styles.errBox}>{error}</div>}

          <div className={styles.formRow}>
            <label>고도몰 상품코드 *</label>
            <input
              type="text"
              value={godoGoodsCode}
              onChange={(e) => setGodoGoodsCode(e.target.value)}
              placeholder="예: 1887001 (*숫자만)"
              disabled={saving}
            />
          </div>

          <div className={styles.formRow}>
            <label>ERP 품목코드 *</label>
            <input
              type="text"
              value={erpGoodsCode}
              onChange={(e) => setErpGoodsCode(e.target.value)}
              placeholder="예: N14670100"
              disabled={saving}
            />
          </div>

          <div className={styles.formRow}>
            <label>상품명 *</label>
            <input
              type="text"
              value={goodsName}
              onChange={(e) => setGoodsName(e.target.value)}
              placeholder="예: 에쓰푸드 모둠전 500g"
              disabled={saving}
            />
          </div>

          <div className={styles.formRow}>
            <label>구분</label>
            <div className={styles.typeSel}>
              <button
                type="button"
                className={`${styles.typeOpt} ${onlyMallYn === 'N' ? styles.typeOptGeneral : ''}`}
                onClick={() => setOnlyMallYn('N')}
                disabled={saving}
              >
                <span className={styles.typeOptTitle}>범용</span>
              </button>
              <button
                type="button"
                className={`${styles.typeOpt} ${onlyMallYn === 'Y' ? styles.typeOptExclusive : ''}`}
                onClick={() => setOnlyMallYn('Y')}
                disabled={saving}
              >
                <span className={styles.typeOptTitle}>전용</span>
              </button>
            </div>
            <div className={styles.fieldHint}>
              {getConfigReferenceText(statusConfigs, onlyMallYn)}
            </div>
          </div>

          <div className={styles.modalFooter}>
            <button type="button" className={styles.btnCancel} onClick={onClose} disabled={saving}>
              취소
            </button>
            <button
              type="button"
              className={styles.btnSave}
              onClick={handleSave}
              disabled={!canSave}
            >
              {saving ? '저장 중...' : '저장'}
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

export default ProductAddModal;
