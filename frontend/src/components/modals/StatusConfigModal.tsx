import { useMemo, useState } from 'react';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';
import type {
  CommonStatusConfig,
  CommonStatusConfigScope,
} from '../../types/commonStatusConfig';
import CloseConfirmModal from './CloseConfirmModal';
import styles from './Modal.module.scss';

interface Props {
  configs: CommonStatusConfig[];
  loading: boolean;
  saving: boolean;
  error: string;
  onClose: () => void;
  onSave: (configs: CommonStatusConfig[]) => Promise<void>;
}

interface FormState {
  outOfStockQty: string;
  safetyRate: string;
  remainValidDays: string;
}

const SCOPE_LABEL: Record<CommonStatusConfigScope, string> = {
  GENERAL: '범용',
  EXCLUSIVE: '전용',
};

const SCOPE_ORDER: CommonStatusConfigScope[] = ['GENERAL', 'EXCLUSIVE'];
const METRIC_COLUMNS = [
  { key: 'outOfStockQty', label: '품절 기준 수량' },
  { key: 'onSaleQty', label: '판매 기준 수량' },
  { key: 'safetyRate', label: '잔여율 기준' },
  { key: 'remainValidDays', label: '잔여일 기준' },
] as const;

const toFormState = (config: CommonStatusConfig): FormState => ({
  outOfStockQty: String(config.outOfStockQty),
  safetyRate: String(config.safetyRate),
  remainValidDays: String(config.remainValidDays),
});

const parseNumber = (value: string): number => {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return 0;
  }
  return parsed;
};

const StatusConfigModal = ({
  configs,
  loading,
  saving,
  error,
  onClose,
  onSave,
}: Props) => {
  const [formByScope, setFormByScope] = useState<Partial<Record<CommonStatusConfigScope, FormState>>>({});
  const [submitError, setSubmitError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  useLockBodyScroll();

  const orderedConfigs = useMemo(
    () => [...configs].sort((a, b) => SCOPE_ORDER.indexOf(a.scope) - SCOPE_ORDER.indexOf(b.scope)),
    [configs],
  );

  const configFormByScope = useMemo(
    () => Object.fromEntries(
      configs.map((config) => [config.scope, toFormState(config)]),
    ) as Partial<Record<CommonStatusConfigScope, FormState>>,
    [configs],
  );

  const handleChange = (
    scope: CommonStatusConfigScope,
    key: keyof FormState,
    value: string,
  ) => {
    setFormByScope((current) => ({
      ...current,
      [scope]: {
        ...(
          current[scope]
          ?? configFormByScope[scope]
          ?? { outOfStockQty: '0', safetyRate: '0', remainValidDays: '0' }
        ),
        [key]: value,
      },
    }));
  };

  const handleSave = async () => {
    setSubmitError('');

    const nextConfigs = orderedConfigs.map((config) => {
      const form = formByScope[config.scope]
        ?? configFormByScope[config.scope]
        ?? { outOfStockQty: '0', safetyRate: '0', remainValidDays: '0' };
      const outOfStockQty = parseNumber(form.outOfStockQty);
      const safetyRate = parseNumber(form.safetyRate);
      const remainValidDays = parseNumber(form.remainValidDays);
      return {
        scope: config.scope,
        outOfStockQty,
        onSaleQty: outOfStockQty + 1,
        safetyRate,
        remainValidDays,
      };
    });

    try {
      await onSave(nextConfigs);
      onClose();
    } catch {
      setSubmitError('상태 기준 저장에 실패했어요. 다시 시도해주세요.');
    }
  };

  const canSave = !loading && !saving && orderedConfigs.length > 0;

  return (
    <>
      <div className={styles.modalBg} onClick={() => setShowCloseConfirm(true)}>
        <div
          className={`${styles.modal} ${styles.modalWide}`}
          onClick={(e) => e.stopPropagation()}
        >
          <h3>상태 기준 설정</h3>

          {(error || submitError) && (
            <div className={styles.errBox}>{submitError || error}</div>
          )}

          <div className={styles.configGuide}>
            전체 상품 상태 판정 기준을 관리해요.
            <br />
            판매 기준 수량은 직접 수정하지 않고 품절 기준 수량 + 1로 자동 적용돼요.
          </div>

          {loading ? (
            <div className={styles.placeholderBox}>상태 기준을 불러오는 중이에요.</div>
          ) : orderedConfigs.length === 0 ? (
            <div className={styles.placeholderBox}>표시할 상태 기준 데이터가 없어요.</div>
          ) : (
            <div className={styles.configTableWrap}>
              <table className={styles.configTable}>
                <thead>
                  <tr>
                    <th scope="col">구분</th>
                    {METRIC_COLUMNS.map((metric) => (
                      <th key={metric.key} scope="col">{metric.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {orderedConfigs.map((config) => {
                    const form = formByScope[config.scope]
                      ?? configFormByScope[config.scope]
                      ?? { outOfStockQty: '0', safetyRate: '0', remainValidDays: '0' };
                    const outOfStockQty = parseNumber(form.outOfStockQty);

                    return (
                      <tr key={config.scope}>
                        <th scope="row">{SCOPE_LABEL[config.scope]}</th>
                        {METRIC_COLUMNS.map((metric) => {
                          const value = metric.key === 'outOfStockQty'
                            ? form.outOfStockQty
                            : metric.key === 'safetyRate'
                              ? form.safetyRate
                              : metric.key === 'remainValidDays'
                                ? form.remainValidDays
                                : String(outOfStockQty + 1);

                          return (
                            <td key={`${config.scope}-${metric.key}`}>
                              <input
                                id={`status-config-${metric.key}-${config.scope}`}
                                type="number"
                                min={0}
                                inputMode="numeric"
                                value={value}
                                onChange={(e) => {
                                  if (metric.key === 'onSaleQty') {
                                    return;
                                  }
                                  handleChange(config.scope, metric.key, e.target.value);
                                }}
                                disabled={saving || metric.key === 'onSaleQty'}
                                aria-label={`${SCOPE_LABEL[config.scope]} ${metric.label}`}
                              />
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          <div className={styles.modalFooter}>
            <button type="button" className={styles.btnCancel} onClick={onClose} disabled={saving}>
              취소
            </button>
            <button type="button" className={styles.btnSave} onClick={() => void handleSave()} disabled={!canSave}>
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

export default StatusConfigModal;
