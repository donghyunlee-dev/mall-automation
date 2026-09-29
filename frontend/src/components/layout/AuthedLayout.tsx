import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { getSession } from '../../api/auth';
import Header from './Header';
import useLogout from '../../hooks/useLogout';
import useUserNotifications from '../../hooks/useUserNotifications';
import type { NotificationYn } from '../../types/auth';

const AuthedLayout = () => {
  const [email, setEmail] = useState('');
  const {
    loading: logoutLoading,
    error: logoutError,
    clearError: clearLogoutError,
    logout,
  } = useLogout();
  const {
    profile,
    loading: notificationsLoading,
    saving: notificationsSaving,
    error: notificationsError,
    updateNotifications,
  } = useUserNotifications();

  useEffect(() => {
    getSession()
      .then((s) => setEmail(s.email))
      .catch(() => undefined);
  }, []);

  const accountEmail = profile?.email ?? email;

  const handleChangeEmailNotification = (value: NotificationYn) => {
    if (!profile) return;
    void updateNotifications({
      emailNotificationYn: value,
      teamsNotificationYn: profile.teamsNotificationYn,
    });
  };

  const handleChangeTeamsNotification = (value: NotificationYn) => {
    if (!profile) return;
    void updateNotifications({
      emailNotificationYn: profile.emailNotificationYn,
      teamsNotificationYn: value,
    });
  };

  return (
    <>
      <Header
        email={accountEmail}
        emailNotificationYn={profile?.emailNotificationYn ?? 'N'}
        teamsNotificationYn={profile?.teamsNotificationYn ?? 'N'}
        notificationsLoading={notificationsLoading}
        notificationsSaving={notificationsSaving}
        notificationsError={notificationsError}
        logoutLoading={logoutLoading}
        logoutError={logoutError}
        onChangeEmailNotification={handleChangeEmailNotification}
        onChangeTeamsNotification={handleChangeTeamsNotification}
        onLogoutConfirm={logout}
        onLogoutModalClose={clearLogoutError}
      />
      <Outlet />
    </>
  );
};

export default AuthedLayout;
