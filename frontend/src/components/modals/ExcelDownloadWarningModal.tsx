import styles from './Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

export type DownloadWarningType = 'INVALID_BIZ_CODE' | 'REGISTERED' | 'INACTIVE_PARTNER' | 'DUPLICATE';

export interface DownloadWarning {
  type: DownloadWarningType;
  memIds: string[];
}

const WARNING_META: Record<DownloadWarningType, { title: string; desc: string }> = {
  INVALID_BIZ_CODE: {
    title: '업태·업종 확인 필요',
    desc: '업태 또는 업종이 올바르게 설정되지 않은 회원이 있어요. 상세 화면에서 수정 후 다운로드하는 것을 권장해요.',
  },
  REGISTERED: {
    title: '이미 거래처로 등록된 회원',
    desc: '거래처로 이미 등록되어 있는 회원이에요.\n포함 여부를 다시 확인해 주세요.',
  },
  INACTIVE_PARTNER: {
    title: '불용 회원 포함',
    desc: '거래처 상태가 불용인 회원이에요. 포함 여부를 다시 확인해 주세요.',
  },
  DUPLICATE: {
    title: '중복 회원 포함',
    desc: '동일한 사업자번호로 여러 계정이 등록된 회원이에요.',
  },
};

interface Props {
  warnings: DownloadWarning[];
  onConfirm: () => void;
  onCancel: () => void;
}

const ExcelDownloadWarningModal = ({ warnings, onConfirm, onCancel }: Props) => {
  useLockBodyScroll();

  return (
    <div className={styles.modalBg} onClick={onCancel}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3>엑셀 다운로드 전 확인</h3>
        <div className={styles.warnList}>
          {warnings.map(({ type, memIds }) => {
            const { title, desc } = WARNING_META[type];
            return (
              <div key={type} className={[styles.warnItem, styles.centerText].join(' ')}>
                <div className={styles.warnItemTitle}>{title}</div>
                <div className={styles.warnItemDesc}>{desc}</div>
                <div className={styles.warnItemIds}>
                  {memIds.join(', ')} ({memIds.length}명)
                </div>
              </div>
            );
          })}
        </div>
        <p className={[styles.confirmTextBlock, styles.centerText].join(' ')}>계속 진행하시겠어요?</p>
        <div className={styles.modalFooter}>
          <button type="button" className={styles.btnCancel} onClick={onCancel}>
            취소
          </button>
          <button type="button" className={styles.btnSave} onClick={onConfirm}>
            계속 다운로드
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExcelDownloadWarningModal;
