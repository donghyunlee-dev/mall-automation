import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import AvatarDropdown from './AvatarDropdown';
import styles from './Header.module.scss';
import type { NotificationYn } from '../../types/auth';

interface Props {
  email: string;
  emailNotificationYn: NotificationYn;
  teamsNotificationYn: NotificationYn;
  notificationsLoading: boolean;
  notificationsSaving: boolean;
  notificationsError: string;
  logoutLoading: boolean;
  logoutError: string;
  onChangeEmailNotification: (value: NotificationYn) => void;
  onChangeTeamsNotification: (value: NotificationYn) => void;
  onLogoutConfirm: () => Promise<void>;
  onLogoutModalClose: () => void;
}

const Header = ({
  email,
  emailNotificationYn,
  teamsNotificationYn,
  notificationsLoading,
  notificationsSaving,
  notificationsError,
  logoutLoading,
  logoutError,
  onChangeEmailNotification,
  onChangeTeamsNotification,
  onLogoutConfirm,
  onLogoutModalClose,
}: Props) => {
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const closeLogoutModal = () => {
    setShowLogoutConfirm(false);
    onLogoutModalClose();
  };

  return (
    <>
      <header className={styles.header}>
        <div className={styles.logo}>
          에쓰푸드몰 <em>운영 관리</em>
        </div>

        <nav className={styles.hdrTabs}>
          <NavLink
            to="/dashboard"
            className={({ isActive }) => [styles.tabNav, isActive ? styles.active : ''].join(' ')}
            end={false}
          >
            대시보드
          </NavLink>
          <NavLink
            to="/goods"
            className={({ isActive }) => [styles.tabNav, isActive ? styles.active : ''].join(' ')}
            end={false}
          >
            상품 관리
          </NavLink>
          <NavLink
            to="/members"
            className={({ isActive }) => [styles.tabNav, isActive ? styles.active : ''].join(' ')}
            end={false}
          >
            회원 관리
          </NavLink>
        </nav>

        <div className={styles.hdrRight}>
          <AvatarDropdown
            email={email}
            emailNotificationYn={emailNotificationYn}
            teamsNotificationYn={teamsNotificationYn}
            notificationsLoading={notificationsLoading}
            notificationsSaving={notificationsSaving}
            notificationsError={notificationsError}
            onChangeEmailNotification={onChangeEmailNotification}
            onChangeTeamsNotification={onChangeTeamsNotification}
            onLogout={() => {
              onLogoutModalClose();
              setShowLogoutConfirm(true);
            }}
          />
        </div>
      </header>

      {showLogoutConfirm && (
        <div className={styles.modalBg} onClick={closeLogoutModal}>
          <div
            className={styles.confirmModal}
            onClick={(e) => e.stopPropagation()}
          >
            <h3>로그아웃</h3>
            {logoutError && <div className={styles.errBox}>{logoutError}</div>}
            <p>
              <span className={styles.logoutEmail}>{email}</span>
              <br />
              위 계정에서 로그아웃할까요?
            </p>
            <div className={styles.modalFooter}>
              <button
                className={styles.btnCancel}
                onClick={closeLogoutModal}
                disabled={logoutLoading}
              >
                취소
              </button>
              <button
                className={styles.btnLogout}
                onClick={() => {
                  void onLogoutConfirm();
                }}
                disabled={logoutLoading}
              >
                {logoutLoading ? '로그아웃 중...' : '로그아웃'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Header;
