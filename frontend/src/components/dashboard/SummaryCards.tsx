import { useState, useEffect, useRef } from 'react';
import styles from './SummaryCards.module.scss';
import type { BatchSchedule } from '../../types/batch';
import { formatLastRun } from '../../utils/formatLastRun';

interface Props {
  schedule: BatchSchedule | null;
  totalGoods: number;
  soldOutCount: number;
  soldOutDelta: number;
  todayChangedCount: number;
  todaySoldOutCount: number;
  todayOnSaleCount: number;
  running: boolean;
  onRunNow: () => void;
  onRequestIntervalChange: (minutes: number) => void;
  onClickSoldOut: () => void;
  onClickChanges: () => void;
}

const SummaryCards = ({
  schedule,
  totalGoods,
  soldOutCount,
  soldOutDelta,
  todayChangedCount,
  todaySoldOutCount,
  todayOnSaleCount,
  running,
  onRunNow,
  onRequestIntervalChange,
  onClickSoldOut,
  onClickChanges,
}: Props) => {
  const lastRun = formatLastRun(schedule);
  const intervalMinutes = schedule ? Math.round(schedule.fixedDelaySeconds / 60) : 10;

  const [showIntervalPopover, setShowIntervalPopover] = useState(false);
  const [intervalInput, setIntervalInput] = useState(String(intervalMinutes));
  const intervalWrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setIntervalInput(String(intervalMinutes));
  }, [intervalMinutes]);

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (intervalWrapRef.current && !intervalWrapRef.current.contains(e.target as Node)) {
        setShowIntervalPopover(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleApplyInterval = () => {
    const val = parseInt(intervalInput, 10);
    if (!isNaN(val) && val >= 1) {
      onRequestIntervalChange(val);
      setShowIntervalPopover(false);
    }
  };

  return (
    <div className={styles.wrap}>
      {/* 최근 동기화 시간 */}
      <div className={[styles.card, styles.ucard].join(' ')}>
          <div className={styles.sumLabel}>최근 동기화 시간</div>
          <div className={styles.sumValue}>
            <span className={styles.runDot} />
            {lastRun.value}
          </div>
          <div className={styles.sumSub}>{lastRun.sub}</div>
        </div>

        {/* 현재 품절 */}
        <button
          className={[styles.card, styles.ucard, styles.cardWarn, styles.cardClickable].join(' ')}
          onClick={onClickSoldOut}
          type="button"
        >
          <div className={styles.sumLabel}>현재 품절</div>
          <div className={[styles.sumValue, styles.sumValueRow].join(' ')}>
            <span className={styles.soldOutNum}>{soldOutCount}</span>
            <span className={styles.soldOutUnit}>개</span>
            <span className={styles.soldOutTotal}>/ {totalGoods}개</span>
          </div>
          <div className={styles.sumSub}>
            {soldOutDelta !== 0 ? (
              <span className={soldOutDelta > 0 ? styles.textCoral : styles.textPos}>
                전일 대비 {soldOutDelta > 0 ? '+' : ''}{soldOutDelta}
              </span>
            ) : (
              '전일 대비 동일'
            )}
          </div>
        </button>

        {/* 오늘 상태 변경 */}
        <button
          className={[styles.card, styles.ucard, styles.cardOk, styles.cardClickable].join(' ')}
          onClick={onClickChanges}
          type="button"
        >
          <div className={styles.sumLabel}>오늘 상태 변경</div>
          <div className={styles.sumValue}>
            <span className={styles.changedNum}>{todayChangedCount}</span>
            <span className={styles.changedUnit}>건</span>
          </div>
          <div className={styles.sumSub}>
            품절 전환 {todaySoldOutCount}건 · 판매 전환 {todayOnSaleCount}건
          </div>
        </button>

      {/* 컨트롤 바 — 카드와 같은 행 4번째 칸, 버튼 위·pill 아래 */}
      <div className={styles.controlBar}>
        <button
          className={[styles.runBtn, running ? styles.runBtnRunning : ''].join(' ')}
          onClick={onRunNow}
          disabled={running}
        >
          {running ? (
            <>
              <SpinnerIcon />
              <span>동기화 중...</span>
            </>
          ) : (
            <>
              <PlayIcon />
              <span>상태 동기화</span>
            </>
          )}
        </button>
        {schedule && (
          <div className={styles.autoRunWrap} ref={intervalWrapRef}>
            <button
              className={styles.autoRunPill}
              onClick={() => setShowIntervalPopover((v) => !v)}
            >
              <ClockIcon />
              <span>자동 실행 중 · {intervalMinutes}분</span>
            </button>
            {showIntervalPopover && (
              <div className={styles.autoRunPopover}>
                <span className={styles.autoRunPopoverLbl}>자동 실행 시간</span>
                <div className={styles.autoRunPopoverRow}>
                  <input
                    className={styles.autoRunInput}
                    type="number"
                    min={1}
                    max={60}
                    value={intervalInput}
                    onChange={(e) => setIntervalInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleApplyInterval();
                    }}
                    autoFocus
                  />
                  <span className={styles.autoRunUnit}>분</span>
                  <button className={styles.autoRunBtn} onClick={handleApplyInterval}>
                    적용
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

// ── SVG 아이콘 ─────────────────────────────────────────────────────────────────

const ClockIcon = () => (
  <svg
    style={{ width: 14, height: 14, flexShrink: 0 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
  >
    <circle cx="8" cy="8" r="6" />
    <polyline points="8,5 8,8 10,10" />
  </svg>
);

const PlayIcon = () => (
  <svg
    style={{ width: 14, height: 14, flexShrink: 0 }}
    viewBox="0 0 16 16"
    fill="none"
  >
    <polygon points="5,3 13,8 5,13" fill="currentColor" />
  </svg>
);

const SpinnerIcon = () => (
  <svg
    className={styles.spin}
    style={{ width: 14, height: 14, flexShrink: 0 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
  >
    <circle cx="8" cy="8" r="6" strokeOpacity={0.25} />
    <path d="M8 2a6 6 0 0 1 6 6" />
  </svg>
);

export default SummaryCards;
