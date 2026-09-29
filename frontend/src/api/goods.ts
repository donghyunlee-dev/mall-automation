import client from './client';
import type {
  GoodsItem,
  GoodsListResponse,
  GoodsListParams,
  GoodsCreateItem,
  GoodsCreateResponse,
  GoodsUpdateBody,
  GoodsStatusLogResponse,
  GoodsStatusLogParams,
  UpdateGodoSyncBlockBody,
  ForceSyncGodoStatusBody,
  ForceSyncGodoStatusResponse,
} from '../types/goods';

type RawGoodsItem = Partial<GoodsItem> & {
  recentUpdatedAt?: string | null;
  recentUpdatedBy?: string | null;
};

type RawGoodsListResponse = {
  items?: unknown;
};

type RawGoodsStatusLogResponse = {
  items?: unknown;
};

const normalizeGoodsItem = (item: RawGoodsItem): GoodsItem => ({
  id: Number(item.id ?? 0),
  godoGoodsCode: item.godoGoodsCode ?? '',
  erpGoodsCode: item.erpGoodsCode ?? '',
  goodsName: item.goodsName ?? '',
  onlyMallYn: item.onlyMallYn === 'Y' ? 'Y' : 'N',
  soldOutYn: item.soldOutYn === 'Y' ? 'Y' : 'N',
  availableQty: Number(item.availableQty ?? 0),
  safetyRate: Number(item.safetyRate ?? 0),
  remainValidDays: Number(item.remainValidDays ?? 0),
  godoSyncYn: item.godoSyncYn === 'Y' ? 'Y' : 'N',
  erpSyncYn: item.erpSyncYn === 'Y' ? 'Y' : 'N',
  godoSyncBlockedYn: item.godoSyncBlockedYn === 'Y' ? 'Y' : 'N',
  message: item.message ?? null,
  status: item.status ?? 'UNVERIFIED',
  createdId: item.createdId ?? item.recentUpdatedBy ?? undefined,
  createdAt: item.createdAt ?? item.recentUpdatedAt ?? undefined,
  updatedId: item.updatedId ?? item.recentUpdatedBy ?? undefined,
  updatedAt: item.updatedAt ?? item.recentUpdatedAt ?? undefined,
});

const normalizeGoodsListResponse = (data: RawGoodsListResponse): GoodsListResponse => {
  const items = Array.isArray(data?.items) ? data.items : [];
  return {
    items: items.map((item) => normalizeGoodsItem(item as RawGoodsItem)),
  };
};

const normalizeGoodsStatusLogResponse = (data: RawGoodsStatusLogResponse): GoodsStatusLogResponse => ({
  items: Array.isArray(data?.items) ? data.items : [],
});

export const getGoods = (params?: GoodsListParams): Promise<GoodsListResponse> =>
  client
    .get<RawGoodsListResponse>('/api/goods', { params })
    .then((r) => normalizeGoodsListResponse(r.data));

export const getGoodsById = (id: number): Promise<GoodsItem> =>
  client.get<RawGoodsItem>(`/api/goods/${id}`).then((r) => normalizeGoodsItem(r.data));

export const createGoods = (
  items: GoodsCreateItem[],
): Promise<GoodsCreateResponse> =>
  client.post<GoodsCreateResponse>('/api/goods', items).then((r) => r.data);

export const updateGoods = (
  id: number,
  body: GoodsUpdateBody,
): Promise<GoodsItem> =>
  client.put<RawGoodsItem>(`/api/goods/${id}`, body).then((r) => normalizeGoodsItem(r.data));

export const updateGodoSyncBlock = (
  id: number,
  body: UpdateGodoSyncBlockBody,
): Promise<GoodsItem> =>
  client.patch<RawGoodsItem>(`/api/goods/${id}/godo-sync-block`, body).then((r) => normalizeGoodsItem(r.data));

export const forceSyncGodoStatus = (
  body: ForceSyncGodoStatusBody,
): Promise<ForceSyncGodoStatusResponse> =>
  client.post<ForceSyncGodoStatusResponse>('/api/goods/godo-status/force-sync', body).then((r) => r.data);

export const deleteGoods = (id: number): Promise<void> =>
  client.delete(`/api/goods/${id}`).then(() => undefined);

export const getGoodsStatusLogs = (
  params?: GoodsStatusLogParams,
): Promise<GoodsStatusLogResponse> =>
  client
    .get<RawGoodsStatusLogResponse>('/api/goods/status-logs', { params })
    .then((r) => normalizeGoodsStatusLogResponse(r.data));
