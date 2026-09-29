import type { GoodsItem } from '../types/goods';

export const isSaleStatusUnavailable = (item: Pick<GoodsItem, 'godoSyncYn'>): boolean =>
  item.godoSyncYn === 'N';

export const isSaleStatusSoldOut = (
  item: Pick<GoodsItem, 'godoSyncYn' | 'soldOutYn'>,
): boolean => item.godoSyncYn === 'Y' && item.soldOutYn === 'Y';

export const isSaleStatusOnSale = (
  item: Pick<GoodsItem, 'godoSyncYn' | 'soldOutYn'>,
): boolean => item.godoSyncYn === 'Y' && item.soldOutYn === 'N';
