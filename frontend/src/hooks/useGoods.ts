import { useState, useEffect, useCallback } from 'react';
import { getGoods } from '../api/goods';
import type { GoodsItem } from '../types/goods';
import {
  isSaleStatusOnSale,
  isSaleStatusSoldOut,
  isSaleStatusUnavailable,
} from '../utils/goodsSaleStatus';

export type SortKey = keyof Pick<
  GoodsItem,
  | 'godoGoodsCode'
  | 'erpGoodsCode'
  | 'goodsName'
  | 'onlyMallYn'
  | 'soldOutYn'
  | 'godoSyncBlockedYn'
  | 'updatedAt'
  | 'createdAt'
> | 'syncDate' | 'codeValidation';

export type SortDir = 'asc' | 'desc';
export type StatusFilterKey = 'all' | 'out' | 'on' | 'unavailable';
export type TypeFilterKey = 'all' | 'general' | 'exclusive';
export type BlockFilterKey = 'all' | 'blocked';

export interface GoodsFilter {
  status: StatusFilterKey;
  type: TypeFilterKey;
  block: BlockFilterKey;
}

const DEFAULT_FILTER: GoodsFilter = { status: 'all', type: 'all', block: 'all' };
export type PageSize = 10 | 20 | 30 | 50;

const isGodoSyncBlockVisible = (item: GoodsItem): boolean =>
  item.godoSyncYn === 'Y' &&
  item.erpSyncYn === 'Y' &&
  item.status === 'NORMAL';

const DEFAULT_PAGE_SIZE: PageSize = 10;
export const applyFilter = (items: GoodsItem[], filter: GoodsFilter, keyword: string): GoodsItem[] => {
  let result = items;

  switch (filter.status) {
    case 'out':
      result = result.filter((g) => isSaleStatusSoldOut(g));
      break;
    case 'on':
      result = result.filter((g) => isSaleStatusOnSale(g));
      break;
    case 'unavailable':
      result = result.filter((g) => isSaleStatusUnavailable(g));
      break;
    default:
      break;
  }

  switch (filter.type) {
    case 'general':
      result = result.filter((g) => g.onlyMallYn === 'N');
      break;
    case 'exclusive':
      result = result.filter((g) => g.onlyMallYn === 'Y');
      break;
    default:
      break;
  }

  if (filter.block === 'blocked') {
    result = result.filter((g) => isGodoSyncBlockVisible(g) && g.godoSyncBlockedYn === 'Y');
  }

  if (keyword.trim()) {
    const kw = keyword.trim().toLowerCase();
    result = result.filter(
      (g) =>
        g.godoGoodsCode.toLowerCase().includes(kw) ||
        g.erpGoodsCode.toLowerCase().includes(kw) ||
        g.goodsName.toLowerCase().includes(kw),
    );
  }

  return result;
};

const codeValidationRank = (item: GoodsItem): number => {
  if (item.erpSyncYn === 'Y' && item.godoSyncYn === 'Y') return 0;
  if (item.erpSyncYn === 'N' && item.godoSyncYn === 'Y') return 1;
  if (item.erpSyncYn === 'Y' && item.godoSyncYn === 'N') return 2;
  return 3;
};

const compareValue = (a: GoodsItem, b: GoodsItem, key: SortKey): number => {
  if (key === 'codeValidation') {
    return codeValidationRank(a) - codeValidationRank(b);
  }

  if (key === 'syncDate') {
    const va = a.godoSyncYn === 'Y' ? a.updatedAt ?? '' : '';
    const vb = b.godoSyncYn === 'Y' ? b.updatedAt ?? '' : '';
    if (va < vb) return -1;
    if (va > vb) return 1;
    return 0;
  }

  const va = a[key] ?? '';
  const vb = b[key] ?? '';
  if (va < vb) return -1;
  if (va > vb) return 1;
  return 0;
};

export const applySort = (items: GoodsItem[], key: SortKey, dir: SortDir): GoodsItem[] =>
  [...items].sort((a, b) => {
    const cmp = compareValue(a, b, key);
    return dir === 'asc' ? cmp : -cmp;
  });

interface UseGoodsReturn {
  allItems: GoodsItem[];
  filteredItems: GoodsItem[];
  pagedItems: GoodsItem[];
  loading: boolean;
  error: string;
  keyword: string;
  filter: GoodsFilter;
  sortKey: SortKey;
  sortDir: SortDir;
  page: number;
  pageSize: PageSize;
  totalPages: number;
  setKeyword: (v: string) => void;
  setStatusFilter: (v: StatusFilterKey) => void;
  setTypeFilter: (v: TypeFilterKey) => void;
  setBlockFilter: (v: BlockFilterKey) => void;
  setPageSize: (v: PageSize) => void;
  toggleSort: (key: SortKey) => void;
  setPage: (p: number) => void;
  reload: () => Promise<GoodsItem[]>;
}

const LOAD_ERROR_MESSAGE = '상품 목록을 불러오지 못했어요.';

const useGoods = (): UseGoodsReturn => {
  const [allItems, setAllItems] = useState<GoodsItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState<GoodsFilter>(DEFAULT_FILTER);
  const [sortKey, setSortKey] = useState<SortKey>('godoGoodsCode');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState<PageSize>(DEFAULT_PAGE_SIZE);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getGoods();
      setAllItems(res.items);
      setError('');
    } catch {
      setError(LOAD_ERROR_MESSAGE);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const reload = useCallback(async (): Promise<GoodsItem[]> => {
    setLoading(true);
    try {
      const res = await getGoods();
      setAllItems(res.items);
      setError('');
      return res.items;
    } catch {
      setError(LOAD_ERROR_MESSAGE);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  const filteredItems = applySort(
    applyFilter(allItems, filter, keyword),
    sortKey,
    sortDir,
  );

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const safePage = Math.min(page, totalPages);
  const pagedItems = filteredItems.slice(
    (safePage - 1) * pageSize,
    safePage * pageSize,
  );

  const handleSetKeyword = (v: string) => {
    setKeyword(v);
    setPage(1);
  };

  const handleSetStatusFilter = (v: StatusFilterKey) => {
    if (v === 'all') {
      setFilter(DEFAULT_FILTER);
      setPage(1);
      return;
    }
    setFilter((prev) => ({
      ...prev,
      status: prev.status === v ? 'all' : v,
    }));
    setPage(1);
  };

  const handleSetTypeFilter = (v: TypeFilterKey) => {
    setFilter((prev) => ({
      ...prev,
      type: prev.type === v ? 'all' : v,
    }));
    setPage(1);
  };

  const handleSetBlockFilter = (v: BlockFilterKey) => {
    setFilter((prev) => ({
      ...prev,
      block: prev.block === v ? 'all' : v,
    }));
    setPage(1);
  };

  const handleSetPageSize = (v: PageSize) => {
    setPageSize(v);
    setPage(1);
  };

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
    setPage(1);
  };

  return {
    allItems,
    filteredItems,
    pagedItems,
    loading,
    error,
    keyword,
    filter,
    sortKey,
    sortDir,
    page: safePage,
    pageSize,
    totalPages,
    setKeyword: handleSetKeyword,
    setStatusFilter: handleSetStatusFilter,
    setTypeFilter: handleSetTypeFilter,
    setBlockFilter: handleSetBlockFilter,
    setPageSize: handleSetPageSize,
    toggleSort,
    setPage,
    reload,
  };
};

export default useGoods;
