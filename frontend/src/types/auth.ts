export interface OtpRequestBody {
  email: string;
}

export interface OtpRequestResponse {
  verificationKey: string;
  expiresInSeconds: number;
  message: string;
}

export interface OtpConfirmBody {
  verificationKey: string;
  email: string;
  code: string;
}

export interface OtpConfirmResponse {
  authenticated: boolean;
  email: string;
}

export interface SessionResponse {
  email: string;
}

export type NotificationYn = 'Y' | 'N';

export interface CurrentUserResponse {
  email: string;
  emailNotificationYn: NotificationYn;
  teamsNotificationYn: NotificationYn;
  lastLoginAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserNotificationsBody {
  emailNotificationYn: NotificationYn;
  teamsNotificationYn: NotificationYn;
}

export interface MsLoginVerifyBody {
  loginToken: string;
}
