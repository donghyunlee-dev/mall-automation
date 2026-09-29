import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AvatarDropdown from './AvatarDropdown';

const openDropdown = async () => {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /donghyun\.lee@s-food\.com/i }));
  return user;
};

describe('AvatarDropdown', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-design-theme');
  });

  it('renders current notification switch selections', async () => {
    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="Y"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError=""
        onChangeEmailNotification={vi.fn()}
        onChangeTeamsNotification={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await openDropdown();

    expect(screen.getByRole('switch', { name: '이메일 알림' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Teams 알림' })).not.toBeChecked();
  });

  it('calls handlers when notification switches change', async () => {
    const onChangeEmailNotification = vi.fn();
    const onChangeTeamsNotification = vi.fn();

    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="N"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError=""
        onChangeEmailNotification={onChangeEmailNotification}
        onChangeTeamsNotification={onChangeTeamsNotification}
        onLogout={vi.fn()}
      />,
    );

    const user = await openDropdown();

    const emailSwitch = screen.getByRole('switch', { name: '이메일 알림' });
    const teamsSwitch = screen.getByRole('switch', { name: 'Teams 알림' });

    await user.click(emailSwitch);
    await user.click(teamsSwitch);

    expect(onChangeEmailNotification).toHaveBeenCalledWith('Y');
    expect(onChangeTeamsNotification).toHaveBeenCalledWith('Y');
  });

  it('shows the notification error message', async () => {
    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="N"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError="알림 설정 변경에 실패했어요."
        onChangeEmailNotification={vi.fn()}
        onChangeTeamsNotification={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await openDropdown();

    expect(screen.getByText('알림 설정 변경에 실패했어요.')).toBeInTheDocument();
  });

  it('stores and applies selected design theme', async () => {
    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="N"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError=""
        onChangeEmailNotification={vi.fn()}
        onChangeTeamsNotification={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    const user = await openDropdown();

    await user.selectOptions(screen.getByLabelText('디자인 테마'), 'devtools');

    expect(window.localStorage.getItem('sfood.designTheme')).toBe('devtools');
    expect(document.documentElement.dataset.designTheme).toBe('devtools');
  });

  it('starts with the stored design theme', async () => {
    window.localStorage.setItem('sfood.designTheme', 'fintech');

    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="N"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError=""
        onChangeEmailNotification={vi.fn()}
        onChangeTeamsNotification={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await openDropdown();

    expect(screen.getByLabelText('디자인 테마')).toHaveValue('fintech');
    expect(document.documentElement.dataset.designTheme).toBe('fintech');
  });

  it('does not show auto-run section in dropdown', async () => {
    render(
      <AvatarDropdown
        email="donghyun.lee@s-food.com"
        emailNotificationYn="N"
        teamsNotificationYn="N"
        notificationsLoading={false}
        notificationsSaving={false}
        notificationsError=""
        onChangeEmailNotification={vi.fn()}
        onChangeTeamsNotification={vi.fn()}
        onLogout={vi.fn()}
      />,
    );

    await openDropdown();

    expect(screen.queryByText('자동 실행 설정')).not.toBeInTheDocument();
    expect(screen.queryByText('실행 시간')).not.toBeInTheDocument();
    expect(screen.queryByText('자동 실행 시간')).not.toBeInTheDocument();
  });
});
