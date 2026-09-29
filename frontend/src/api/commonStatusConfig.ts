import client from './client';
import type {
  CommonStatusConfig,
  CommonStatusConfigScope,
  UpdateCommonStatusConfigBody,
} from '../types/commonStatusConfig';

export const getCommonStatusConfig = (): Promise<CommonStatusConfig[]> =>
  client.get<CommonStatusConfig[]>('/api/config/status').then((r) => r.data);

export const updateCommonStatusConfig = (
  scope: CommonStatusConfigScope,
  body: UpdateCommonStatusConfigBody,
): Promise<CommonStatusConfig> =>
  client.put<CommonStatusConfig>(`/api/config/status/${scope}`, body).then((r) => r.data);
