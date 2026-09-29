import { describe, it, expect, vi, beforeEach } from 'vitest';
import { redirectToMicrosoftLogin } from './msAuth';

describe('redirectToMicrosoftLogin', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_AX_AUTH_BASE_URL', 'https://ax-auth.s-food.ai');
    vi.stubEnv('VITE_AX_CLIENT_ID', 'sfood-mall-automation');
    vi.stubEnv('VITE_AX_REDIRECT_URI', 'http://localhost:5173/auth/callback');
    delete (window as unknown as { location: unknown }).location;
    (window as unknown as { location: { href: string } }).location = { href: '' };
  });

  it('builds the AX-Auth login URL and navigates to it', () => {
    redirectToMicrosoftLogin();

    expect(window.location.href).toBe(
      'https://ax-auth.s-food.ai/auth/login/sfood-mall-automation?redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Fauth%2Fcallback',
    );
  });

  it('throws when required env vars are missing', () => {
    vi.stubEnv('VITE_AX_CLIENT_ID', '');

    expect(() => redirectToMicrosoftLogin()).toThrow();
  });
});
