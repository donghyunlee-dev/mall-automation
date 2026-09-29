import { useState, useRef, useEffect } from 'react';
import styles from './AvatarDropdown.module.scss';
import type { NotificationYn } from '../../types/auth';
import {
  DESIGN_THEMES,
  applyDesignTheme,
  getStoredDesignTheme,
  isDesignTheme,
  storeDesignTheme,
  type DesignTheme,
} from '../../utils/designTheme';

interface Props {
  email: string;
  emailNotificationYn: NotificationYn;
  teamsNotificationYn: NotificationYn;
  notificationsLoading: boolean;
  notificationsSaving: boolean;
  notificationsError: string;
  onChangeEmailNotification: (value: NotificationYn) => void;
  onChangeTeamsNotification: (value: NotificationYn) => void;
  onLogout: () => void;
}

const normalizeEmail = (email: string | null | undefined): string => email?.trim() ?? '';

const getInitial = (email: string): string =>
  normalizeEmail(email).split('@')[0]?.charAt(0)?.toUpperCase() ?? '?';

const AvatarDropdown = ({
  email,
  emailNotificationYn,
  teamsNotificationYn,
  notificationsLoading,
  notificationsSaving,
  notificationsError,
  onChangeEmailNotification,
  onChangeTeamsNotification,
  onLogout,
}: Props) => {
  const [open, setOpen] = useState(false);
  const [designTheme, setDesignTheme] = useState<DesignTheme>(getStoredDesignTheme);
  const wrapRef = useRef<HTMLDivElement>(null);
  const safeEmail = normalizeEmail(email);

  useEffect(() => {
    applyDesignTheme(designTheme);
    storeDesignTheme(designTheme);
  }, [designTheme]);

  // 바깥 클릭 시 닫기
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const displayName = safeEmail.split('@')[0] || '사용자';

  return (
    <div className={styles.wrap} ref={wrapRef}>
      <button
        className={[styles.btn, open ? styles.btnOpen : ''].join(' ')}
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <div className={styles.avatar}>{getInitial(safeEmail)}</div>
        <span className={styles.email}>{safeEmail || '세션 확인 중'}</span>
        <span className={styles.caret}>▾</span>
      </button>

      {open && (
        <div className={styles.dropdown}>
          <div className={styles.ddHeader}>
            <div className={styles.ddName}>{displayName}</div>
            <div className={styles.ddEmail}>{safeEmail || '이메일 정보 없음'}</div>
          </div>

          <div className={styles.ddSection}>
            <label className={styles.themeField}>
              <span className={styles.ddLabel}>디자인 테마</span>
              <select
                className={styles.themeSelect}
                value={designTheme}
                onChange={(e) => {
                  const nextTheme = e.target.value;
                  if (isDesignTheme(nextTheme)) {
                    setDesignTheme(nextTheme);
                  }
                }}
              >
                {DESIGN_THEMES.map((theme) => (
                  <option key={theme.value} value={theme.value}>
                    {theme.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className={styles.ddSection}>
            <span className={styles.ddLabel}>알림 설정</span>

            {notificationsError && (
              <div className={styles.ddError}>{notificationsError}</div>
            )}

            <NotificationField
              title="이메일 알림"
              value={emailNotificationYn}
              disabled={notificationsLoading || notificationsSaving}
              onChange={onChangeEmailNotification}
            />

            <NotificationField
              title="Teams 알림"
              value={teamsNotificationYn}
              disabled={notificationsLoading || notificationsSaving}
              onChange={onChangeTeamsNotification}
            />
          </div>

          <div className={styles.ddDivider} />

          <button
            className={[styles.ddItem, styles.ddItemDanger].join(' ')}
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
          >
            <LogoutIcon />
            로그아웃
          </button>
        </div>
      )}
    </div>
  );
};

const LogoutIcon = () => (
  <svg
    style={{ width: 12, height: 12, flexShrink: 0 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
  >
    <path d="M6 3H3a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h3" />
    <polyline points="10,5 13,8 10,11" />
    <line x1="13" y1="8" x2="5" y2="8" />
  </svg>
);

interface NotificationFieldProps {
  title: string;
  value: NotificationYn;
  disabled: boolean;
  onChange: (value: NotificationYn) => void;
}

const NotificationField = ({
  title,
  value,
  disabled,
  onChange,
}: NotificationFieldProps) => (
  <div className={styles.ddNotification}>
    <div className={styles.ddNotificationTitle}>{title}</div>
    <label className={styles.ddSwitch}>
      <span className={styles.srOnly}>{title}</span>
      <input
        type="checkbox"
        role="switch"
        aria-label={title}
        checked={value === 'Y'}
        onChange={(e) => onChange(e.target.checked ? 'Y' : 'N')}
        disabled={disabled}
      />
      <span className={styles.ddSwitchTrack}>
        <span className={styles.ddSwitchThumb} />
      </span>
    </label>
  </div>
);

export default AvatarDropdown;
