import { useCallback, useEffect, useState } from 'react';
import {
  getCommonStatusConfig,
  updateCommonStatusConfig,
} from '../api/commonStatusConfig';
import type {
  CommonStatusConfig,
  CommonStatusConfigScope,
  UpdateCommonStatusConfigBody,
} from '../types/commonStatusConfig';

interface UseCommonStatusConfigReturn {
  configs: CommonStatusConfig[];
  loading: boolean;
  saving: boolean;
  error: string;
  reload: () => Promise<void>;
  updateConfig: (
    scope: CommonStatusConfigScope,
    body: UpdateCommonStatusConfigBody,
  ) => Promise<CommonStatusConfig>;
}

const LOAD_ERROR_MESSAGE = '상태 기준 설정을 불러오지 못했어요.';
const UPDATE_ERROR_MESSAGE = '상태 기준 저장에 실패했어요. 다시 시도해주세요.';

const replaceConfig = (
  configs: CommonStatusConfig[],
  nextConfig: CommonStatusConfig,
): CommonStatusConfig[] => configs.map((config) => (
  config.scope === nextConfig.scope ? nextConfig : config
));

const useCommonStatusConfig = (): UseCommonStatusConfigReturn => {
  const [configs, setConfigs] = useState<CommonStatusConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const next = await getCommonStatusConfig();
      setConfigs(next);
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

  const updateConfig = useCallback(
    async (
      scope: CommonStatusConfigScope,
      body: UpdateCommonStatusConfigBody,
    ): Promise<CommonStatusConfig> => {
      const prevConfigs = configs;
      const optimisticConfig: CommonStatusConfig = {
        scope,
        outOfStockQty: body.outOfStockQty,
        onSaleQty: body.onSaleQty,
        safetyRate: body.safetyRate,
        remainValidDays: body.remainValidDays,
      };

      setConfigs((current) => replaceConfig(current, optimisticConfig));
      setSaving(true);
      setError('');

      try {
        const next = await updateCommonStatusConfig(scope, body);
        setConfigs((current) => replaceConfig(current, next));
        return next;
      } catch (error) {
        setConfigs(prevConfigs);
        setError(UPDATE_ERROR_MESSAGE);
        throw error;
      } finally {
        setSaving(false);
      }
    },
    [configs],
  );

  return {
    configs,
    loading,
    saving,
    error,
    reload: load,
    updateConfig,
  };
};

export default useCommonStatusConfig;
