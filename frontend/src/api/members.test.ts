import { describe, expect, it, vi, beforeEach } from 'vitest';
import client from './client';
import { getMembers, uploadMembers } from './members';

vi.mock('./client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockedClient = vi.mocked(client);

describe('getMembers', () => {
  beforeEach(() => vi.clearAllMocks());

  it('빈 배열 기본값 — content 없을 때', async () => {
    mockedClient.get.mockResolvedValueOnce({ data: {} });
    const result = await getMembers({});
    expect(result.content).toEqual([]);
    expect(result.totalElements).toBe(0);
  });

  it('정상 응답을 그대로 반환', async () => {
    const data = {
      content: [{ memNo: 1, memId: 'user1' }],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    };
    mockedClient.get.mockResolvedValueOnce({ data });
    const result = await getMembers({ page: 0, size: 10 });
    expect(result.content).toHaveLength(1);
    expect(result.totalElements).toBe(1);
  });

  it('GET /api/members 호출', async () => {
    mockedClient.get.mockResolvedValueOnce({ data: {} });
    await getMembers({ searchType: 'MEM_ID', keyword: 'test' });
    expect(mockedClient.get).toHaveBeenCalledWith('/api/members', {
      params: { searchType: 'MEM_ID', keyword: 'test' },
    });
  });
});

describe('uploadMembers', () => {
  beforeEach(() => vi.clearAllMocks());

  it('POST /api/members/uploads 호출', async () => {
    const responseData = {
      totalInserted: 10,
      totalUpdated: 0,
      totalDeleted: 0,
      totalFailed: 0,
      failedRows: [],
      deletionSkipped: false,
      deletionSkippedReason: null,
      syncedAt: '2026-06-22T00:00:00Z',
    };
    mockedClient.post.mockResolvedValueOnce({ data: responseData });

    const req = {
      type: 'LIST' as const,
      fileName: 'test.xlsx',
      applyDeletion: false,
      rows: [[12345, '2025-01-15', null]],
    };

    const result = await uploadMembers(req);

    expect(mockedClient.post).toHaveBeenCalledWith('/api/members/uploads', req);
    expect(result.totalInserted).toBe(10);
  });
});
