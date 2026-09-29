export type GoodsStatus = 'NORMAL' | 'UNVERIFIED' | 'ERROR' | 'HIDE';
export type YN = 'Y' | 'N';

export interface GoodsItem {
  id: number;
  godoGoodsCode: string;
  erpGoodsCode: string;
  goodsName: string;
  onlyMallYn: YN;
  soldOutYn: YN;
  availableQty: number;
  safetyRate: number;
  remainValidDays: number;
  godoSyncYn: YN;
  erpSyncYn: YN;
  godoSyncBlockedYn: YN;
  message: string | null;
  status: GoodsStatus;
  createdId?: string;
  createdAt?: string;
  updatedId?: string | null;
  updatedAt?: string | null;
}

export interface GoodsListResponse {
  items: GoodsItem[];
}

export interface GoodsListParams {
  soldOutYn?: YN;
  onlyMallYn?: YN;
  status?: GoodsStatus;
  keyword?: string;
}

export interface GoodsCreateItem {
  godoGoodsCode: string;
  erpGoodsCode: string;
  goodsName: string;
  onlyMallYn: YN;
}

export interface GoodsCreateResponse {
  items: Array<{
    id: number;
    godoGoodsCode: string;
    erpGoodsCode: string;
    status: GoodsStatus;
    message: string | null;
  }>;
}

export interface GoodsUpdateBody {
  erpGoodsCode: string;
  goodsName: string;
  onlyMallYn: YN;
  availableQty: number;
  safetyRate: number;
  remainValidDays: number;
}

export interface UpdateGodoSyncBlockBody {
  blockedYn: YN;
}

export interface ForceSyncGodoStatusBody {
  godoGoodsCodes: string[];
  targetSoldOutYn: YN;
}

export interface ForceSyncGodoStatusItem {
  godoGoodsCode: string;
  result: 'SUCCESS' | 'SKIPPED' | 'FAILED' | 'NOT_FOUND';
  message: string | null;
  soldOutYn: YN | null;
}

export interface ForceSyncGodoStatusResponse {
  items: ForceSyncGodoStatusItem[];
}

export interface GoodsStatusLog {
  id: number;
  godoGoodsCode: string;
  erpGoodsCode: string;
  wmsAvailableQty: number;
  criteriaQty: number;
  safetyRate: number;
  validPercent: number | null;
  remainValidDays: number | null;
  beforeStatus: 'ON_SALE' | 'OUT_OF_STOCK';
  afterStatus: 'ON_SALE' | 'OUT_OF_STOCK';
  changedAt: string;
  createdAt: string;
}

export interface GoodsStatusLogResponse {
  items: GoodsStatusLog[];
}

export interface GoodsStatusLogParams {
  godoGoodsCode?: string;
  changedFrom?: string;
  changedTo?: string;
}
