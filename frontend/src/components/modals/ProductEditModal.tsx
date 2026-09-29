import { useState } from 'react';
import { updateGoods } from '../../api/goods';
import type { CommonStatusConfig } from '../../types/commonStatusConfig';
import type { GoodsItem, YN } from '../../types/goods';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  item: GoodsItem;
  statusConfigs: CommonStatusConfig[];
  onClose: () => void;
  onSaved: () => void;
  onToast: (msg: string, type: 'success' | 'error') => void;
}

const getConfigReferenceText = (
  statusConfigs: CommonStatusConfig[],
  onlyMallYn: YN,
): string => {
  const config = statusConfigs.find((statusConfig) => (
    statusConfig.scope === (onlyMallYn === 'Y' ? 'EXCLUSIVE' : 'GENERAL')
  ));

  if (!config) {
    return '현재 상태 기준을 불러오는 중이에요.';
  }

  return `현재 기준: 품절 ${config.outOfStockQty} 이하 / 판매 ${config.onSaleQty} 이상 / 잔여율 ${config.safetyRate}% / 잔여일 ${config.remainValidDays}일`;
};

const getStatusConfigByType = (
  statusConfigs: CommonStatusConfig[],
  onlyMallYn: YN,
): CommonStatusConfig | undefined => statusConfigs.find((statusConfig) => (
  statusConfig.scope === (onlyMallYn === 'Y' ? 'EXCLUSIVE' : 'GENERAL')
));

const ProductEditModal = ({ item, statusConfigs, onClose, onSaved, onToast }: Props) => {
  useLockBodyScroll();
  const [erpGoodsCode, setErpGoodsCode] = useState(item.erpGoodsCode);
  const [goodsName, setGoodsName] = useState(item.goodsName);
  const [onlyMallYn, setOnlyMallYn] = useState<YN>(item.onlyMallYn);
  const [availableQty, setAvailableQty] = useState(String(item.availableQty ?? 0));
  const [safetyRate, setSafetyRate] = useState(String(item.safetyRate ?? 0));
  const [remainValidDays, setRemainValidDays] = useState(String(item.remainValidDays ?? 0));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const erpCodeLocked = item.erpSyncYn === 'Y';

  const canSave = erpGoodsCode.trim() && goodsName.trim() && !saving;

  const handleTypeChange = (nextOnlyMallYn: YN) => {
    setOnlyMallYn(nextOnlyMallYn);
    const nextConfig = getStatusConfigByType(statusConfigs, nextOnlyMallYn);
    if (!nextConfig) {
      return;
    }
    setAvailableQty(String(nextConfig.outOfStockQty));
    setSafetyRate(String(nextConfig.safetyRate));
    setRemainValidDays(String(nextConfig.remainValidDays));
  };

  const handleSave = () => {
    if (!canSave) return;
    setSaving(true);
    setError('');
    updateGoods(item.id, {
      erpGoodsCode: erpGoodsCode.trim(),
      goodsName: goodsName.trim(),
      onlyMallYn,
      availableQty: Number(availableQty || 0),
      safetyRate: Number(safetyRate || 0),
      remainValidDays: Number(remainValidDays || 0),
    })
      .then(() => {
        onSaved();
        onClose();
        onToast('상품 정보가 수정되었어요.', 'success');
      })
      .catch(() => {
        setError('수정에 실패했어요. 다시 시도해주세요.');
        setSaving(false);
      });
  };

  const handleBackdropClick = () => setShowCloseConfirm(true);

  return (
    <>
      <div className={styles.modalBg} onClick={handleBackdropClick}>
        <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
          <h3>상품 수정</h3>

          {error && <div className={styles.errBox}>{error}</div>}

        <div className={styles.formRow}>
          <label>고도몰 상품코드</label>
          <input type="text" value={item.godoGoodsCode} disabled />
        </div>

        <div className={styles.formRow}>
          <label htmlFor="edit-erp-goods-code">ERP 품목코드 *</label>
          <input
            id="edit-erp-goods-code"
            type="text"
            value={erpGoodsCode}
            onChange={(e) => setErpGoodsCode(e.target.value)}
            disabled={saving || erpCodeLocked}
          />
          {erpCodeLocked && (
            <div className={styles.fieldHint}>
              ERP 연동이 완료된 품목은 코드 수정이 불가능해요.
            </div>
          )}
        </div>

        <div className={styles.formRow}>
          <label>상품명 *</label>
          <input
            type="text"
            value={goodsName}
            onChange={(e) => setGoodsName(e.target.value)}
            disabled={saving}
          />
        </div>

        <div className={styles.criteriaGrid}>
          <div className={styles.formRow}>
            <label htmlFor="edit-available-qty">가용재고 기준</label>
            <input
              id="edit-available-qty"
              type="number"
              min={0}
              inputMode="numeric"
              value={availableQty}
              onChange={(e) => setAvailableQty(e.target.value)}
              disabled={saving}
            />
          </div>

          <div className={styles.formRow}>
            <label htmlFor="edit-safety-rate">잔여율 기준</label>
            <input
              id="edit-safety-rate"
              type="number"
              min={0}
              inputMode="numeric"
              value={safetyRate}
              onChange={(e) => setSafetyRate(e.target.value)}
              disabled={saving}
            />
          </div>

          <div className={styles.formRow}>
            <label htmlFor="edit-remain-valid-days">잔여일 기준</label>
            <input
              id="edit-remain-valid-days"
              type="number"
              min={0}
              inputMode="numeric"
              value={remainValidDays}
              onChange={(e) => setRemainValidDays(e.target.value)}
              disabled={saving}
            />
          </div>
        </div>

        <div className={styles.formRow}>
          <label>구분</label>
          <div className={styles.typeSel}>
            <button
              type="button"
              className={`${styles.typeOpt} ${onlyMallYn === 'N' ? styles.typeOptGeneral : ''}`}
              onClick={() => handleTypeChange('N')}
              disabled={saving}
            >
              <span className={styles.typeOptTitle}>범용</span>
            </button>
            <button
              type="button"
              className={`${styles.typeOpt} ${onlyMallYn === 'Y' ? styles.typeOptExclusive : ''}`}
              onClick={() => handleTypeChange('Y')}
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

export default ProductEditModal;
