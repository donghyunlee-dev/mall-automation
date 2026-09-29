import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Header from './Header';

const defaultProps = {
  email: 'tester@s-food.com',
  emailNotificationYn: 'N' as const,
  teamsNotificationYn: 'N' as const,
  notificationsLoading: false,
  notificationsSaving: false,
  notificationsError: '',
  logoutLoading: false,
  logoutError: '',
  onChangeEmailNotification: () => undefined,
  onChangeTeamsNotification: () => undefined,
  onLogoutConfirm: vi.fn().mockResolvedValue(undefined),
  onLogoutModalClose: () => undefined,
};

describe('Header', () => {
  it('opens logout modal and calls logout confirm handler', async () => {
    const user = userEvent.setup();
    const onLogoutConfirm = vi.fn().mockResolvedValue(undefined);

    render(
      <MemoryRouter>
        <Header {...defaultProps} onLogoutConfirm={onLogoutConfirm} />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: /tester@s-food.com/i }));
    await user.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(screen.getByText('위 계정에서 로그아웃할까요?')).toBeInTheDocument();
    expect(screen.getAllByText('tester@s-food.com')).toHaveLength(2);

    await user.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(onLogoutConfirm).toHaveBeenCalledTimes(1);
  });

  it('renders program name and excludes batch controls', () => {
    render(
      <MemoryRouter>
        <Header {...defaultProps} />
      </MemoryRouter>,
    );

    expect(screen.getByText('운영 관리')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /상태 동기화/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /자동 실행/i })).not.toBeInTheDocument();
  });

  it('shows logout error message in modal', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <Header
          {...defaultProps}
          logoutError="로그아웃에 실패했어요. 다시 시도해주세요."
        />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: /tester@s-food.com/i }));
    await user.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(
      screen.getByText('로그아웃에 실패했어요. 다시 시도해주세요.'),
    ).toBeInTheDocument();
  });
});
