import client from './client';
import type {
  OtpRequestBody,
  OtpRequestResponse,
  OtpConfirmBody,
  OtpConfirmResponse,
  SessionResponse,
  CurrentUserResponse,
  UpdateUserNotificationsBody,
  MsLoginVerifyBody,
} from '../types/auth';

export const requestOtp = (body: OtpRequestBody): Promise<OtpRequestResponse> =>
  client.post<OtpRequestResponse>('/api/auth/email-verifications', body).then((r) => r.data);

export const confirmOtp = (body: OtpConfirmBody): Promise<OtpConfirmResponse> =>
  client
    .post<OtpConfirmResponse>('/api/auth/email-verifications/confirm', body)
    .then((r) => r.data);

export const getSession = (): Promise<SessionResponse> =>
  client.get<SessionResponse>('/api/auth/session').then((r) => r.data);

export const getCurrentUser = (): Promise<CurrentUserResponse> =>
  client.get<CurrentUserResponse>('/api/auth/me').then((r) => r.data);

export const updateCurrentUserNotifications = (
  body: UpdateUserNotificationsBody,
): Promise<CurrentUserResponse> =>
  client.put<CurrentUserResponse>('/api/auth/me/notifications', body).then((r) => r.data);

export const logout = (): Promise<void> =>
  client.post('/api/auth/logout').then(() => undefined);

export const verifyMsLogin = (body: MsLoginVerifyBody): Promise<OtpConfirmResponse> =>
  client.post<OtpConfirmResponse>('/api/auth/ms-login/verify', body).then((r) => r.data);
