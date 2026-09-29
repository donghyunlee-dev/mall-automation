import client from './client';
import type { CommonCodeItem } from '../types/commonCode';

export async function getCommonCodes(groupCode: string): Promise<CommonCodeItem[]> {
  const res = await client.get<{ codes: CommonCodeItem[] }>('/api/common-codes', {
    params: { groupCode, useYn: 'Y' },
  });
  return res.data.codes;
}
