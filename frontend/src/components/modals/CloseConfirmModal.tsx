import styles from './Modal.module.scss';

interface Props {
  onCancel: () => void;
  onConfirm: () => void;
}

const CloseConfirmModal = ({ onCancel, onConfirm }: Props) => (
  <div className={styles.modalBg} onClick={onCancel}>
    <div className={styles.confirmModal} onClick={(e) => e.stopPropagation()}>
      <h3>모달 닫기</h3>
      <p className={styles.confirmText}>
        입력 중인 내용은 저장되지 않을 수 있어요.
        <br />
        정말 닫을까요?
      </p>
      <div className={styles.modalFooter}>
        <button type="button" className={styles.confirmStayBtn} onClick={onCancel}>
          계속 입력
        </button>
        <button type="button" className={styles.confirmCloseBtn} onClick={onConfirm}>
          닫기
        </button>
      </div>
    </div>
  </div>
);

export default CloseConfirmModal;
