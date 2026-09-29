import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import useAuth from './useAuth';
import { getSession } from '../api/auth';
import { redirectToMicrosoftLogin } from '../utils/msAuth';

vi.mock('../api/auth', () => ({
  requestOtp: vi.fn(),
  confirmOtp: vi.fn(),
  getSession: vi.fn(),
}));

vi.mock('../utils/msAuth', () => ({
  redirectToMicrosoftLogin: vi.fn(),
}));

describe('useAuth - MS login button', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('shows the OTP screen without auto-redirecting when session check fails', async () => {
    vi.mocked(getSession).mockRejectedValue(new Error('401'));

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.sessionChecked).toBe(true));
    expect(redirectToMicrosoftLogin).not.toHaveBeenCalled();
  });

  it('loginWithMicrosoft redirects to the AX-Auth login URL when clicked', async () => {
    vi.mocked(getSession).mockRejectedValue(new Error('401'));

    const { result } = renderHook(() => useAuth());
    await waitFor(() => expect(result.current.sessionChecked).toBe(true));

    result.current.loginWithMicrosoft();

    expect(redirectToMicrosoftLogin).toHaveBeenCalled();
  });
});
