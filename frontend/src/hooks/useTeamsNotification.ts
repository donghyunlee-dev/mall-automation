import { useEffect, useRef } from 'react';
import { getBatchSchedule } from '../api/batch';
import { getGoodsStatusLogs } from '../api/goods';
import {
  buildStatusChangeCard,
  buildErrorCard,
  sendTeamsNotification,
} from '../api/teams';

const POLL_INTERVAL_MS = 30_000;

const getTodayRange = (): { changedFrom: string; changedTo: string } => {
  const now = new Date();
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  const end = new Date(now);
  end.setHours(23, 59, 59, 999);
  return {
    changedFrom: start.toISOString(),
    changedTo: end.toISOString(),
  };
};

const DASHBOARD_URL = import.meta.env.VITE_DASHBOARD_URL as string | undefined;

const useTeamsNotification = () => {
  const prevEndedAt = useRef<string | null | undefined>(undefined);

  useEffect(() => {
    const poll = () => {
      getBatchSchedule()
        .then((schedule) => {
          // 최초 폴링: 기준값 초기화만 수행, 알림 미발송
          if (prevEndedAt.current === undefined) {
            prevEndedAt.current = schedule.lastEndedAt;
            return;
          }

          const endedAt = schedule.lastEndedAt;
          if (!endedAt || endedAt === prevEndedAt.current) return;
          prevEndedAt.current = endedAt;

          if (schedule.lastStatus === 'FAIL') {
            void sendTeamsNotification(buildErrorCard(schedule));
            return;
          }

          if (schedule.lastStatus === 'SUCCESS') {
            const range = getTodayRange();
            getGoodsStatusLogs(range)
              .then((res) => {
                if (res.items.length > 0) {
                  const payload = buildStatusChangeCard(
                    schedule,
                    res.items,
                    DASHBOARD_URL ?? window.location.origin,
                  );
                  void sendTeamsNotification(payload);
                }
              })
              .catch(() => undefined);
          }
        })
        .catch(() => undefined);
    };

    const id = setInterval(poll, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, []);
};

export default useTeamsNotification;
