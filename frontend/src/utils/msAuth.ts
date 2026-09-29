export function redirectToMicrosoftLogin(): void {
  const clientId = import.meta.env.VITE_AX_CLIENT_ID;
  const authBaseUrl = import.meta.env.VITE_AX_AUTH_BASE_URL;
  const redirectUri = import.meta.env.VITE_AX_REDIRECT_URI;
  if (!clientId || !authBaseUrl || !redirectUri) {
    throw new Error('AX-Auth 환경변수가 설정되지 않았습니다.');
  }
  const params = new URLSearchParams({ redirect_uri: redirectUri });
  window.location.href = `${authBaseUrl}/auth/login/${clientId}?${params.toString()}`;
}
