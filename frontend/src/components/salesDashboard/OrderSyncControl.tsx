import { useState } from 'react';
import useOrderSync from '../../hooks/useOrderSync';
import { formatLastRun } from '../../utils/formatLastRun';
import styles from './OrderSyncControl.module.scss';

const yesterday = (): string => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().slice(0, 10);
};

const OrderSyncControl = () => {
  const [targetDate, setTargetDate] = useState(yesterday());
  const { schedule, running, error, runNow } = useOrderSync();
  const lastRun = formatLastRun(schedule);

  return (
    <div className={styles.wrap}>
      <div className={styles.row}>
        <div className={styles.status}>
          <span className={styles.label}>주문 동기화</span>
          <span className={styles.value}>{lastRun.value}</span>
          <span className={styles.sub}>{lastRun.sub}</span>
        </div>
        <input
          className={styles.dateInput}
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
        />
        <button
          className={[styles.runBtn, running ? styles.runBtnRunning : ''].join(' ')}
          onClick={() => void runNow(targetDate)}
          disabled={running}
        >
          {running ? '실행 중...' : '▶ 주문 동기화 실행'}
        </button>
      </div>
      {error && <span className={styles.error}>{error}</span>}
    </div>
  );
};

export default OrderSyncControl;
