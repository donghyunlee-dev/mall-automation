import { useState } from 'react';
import { deleteMember } from '../../api/members';
import type { MemberListItem } from '../../types/member';
import CloseConfirmModal from '../modals/CloseConfirmModal';
import styles from '../modals/Modal.module.scss';
import { useLockBodyScroll } from '../../hooks/useLockBodyScroll';

interface Props {
  member: MemberListItem;
  onClose: () => void;
  onDeleted: () => void;
  onToast: (msg: string, type: 'success' | 'error') => void;
}

const MemberDeleteModal = ({ member, onClose, onDeleted, onToast }: Props) => {
  useLockBodyScroll();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  const handleDelete = () => {
    setDeleting(true);
    setError('');
    deleteMember(member.memNo)
      .then(() => {
        onDeleted();
        onClose();
        onToast('회원이 삭제되었어요.', 'success');
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
          <h3>회원 삭제</h3>

          {error && <div className={styles.errBox}>{error}</div>}

          <div className={styles.formRow}>
            <label>아이디</label>
            <input type="text" value={member.memId} disabled />
          </div>

          <div className={styles.formRow}>
            <label>이름</label>
            <input type="text" value={member.memName ?? '—'} disabled />
          </div>

          <div className={styles.formRow}>
            <label>등급</label>
            <input type="text" value={member.groupName} disabled />
          </div>

          <div className={styles.formRow}>
            <label>거래처</label>
            <input
              type="text"
              value={member.partnerName ?? member.partnerCode ?? '—'}
              disabled
            />
          </div>

          <div className={`${styles.errBox} ${styles.warnBox}`}>
            삭제하면 회원 목록에서 제외되며 되돌릴 수 없어요.
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

export default MemberDeleteModal;
