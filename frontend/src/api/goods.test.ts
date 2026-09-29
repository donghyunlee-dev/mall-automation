import { describe, expect, it, vi, beforeEach } from 'vitest';
import client from './client';
import { forceSyncGodoStatus } from './goods';

vi.mock('./client', () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockedClient = vi.mocked(client);

describe('goods api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends force sync request with list-based goods codes', async () => {
    const response = {
      items: [
        {
          godoGoodsCode: '10000001',
          result: 'SUCCESS' as const,
          message: null,
          soldOutYn: 'Y' as const,
        },
      ],
    };
    mockedClient.post.mockResolvedValueOnce({ data: response });

    await expect(
      forceSyncGodoStatus({
        godoGoodsCodes: ['10000001'],
        targetSoldOutYn: 'Y',
      }),
    ).resolves.toEqual(response);

    expect(mockedClient.post).toHaveBeenCalledWith('/api/goods/godo-status/force-sync', {
      godoGoodsCodes: ['10000001'],
      targetSoldOutYn: 'Y',
    });
  });
});
