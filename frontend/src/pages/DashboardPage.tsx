import { useState } from 'react';
import MetricCards from '../components/salesDashboard/MetricCards';
import HourlyRevenueChart from '../components/salesDashboard/HourlyRevenueChart';
import GradeRevenuePie from '../components/salesDashboard/GradeRevenuePie';
import GradeDetailTable from '../components/salesDashboard/GradeDetailTable';
import TopItemsChart from '../components/salesDashboard/TopItemsChart';
import RevenueTrendChart from '../components/salesDashboard/RevenueTrendChart';
import useDailyDashboard from '../hooks/useDailyDashboard';
import useWeeklyDashboard from '../hooks/useWeeklyDashboard';
import useMonthlyDashboard from '../hooks/useMonthlyDashboard';
import useQuarterlyDashboard from '../hooks/useQuarterlyDashboard';
import GradePivotTable from '../components/salesDashboard/GradePivotTable';
import YoyComparisonChart from '../components/salesDashboard/YoyComparisonChart';
import OrderSyncControl from '../components/salesDashboard/OrderSyncControl';
import { generateMonthOptions } from '../utils/monthOptions';
import { generateQuarterOptions, type QuarterValue } from '../utils/quarterOptions';
import { formatShortDayLabel, formatShortMonthLabel, formatShortQuarterLabel } from '../utils/dashboardLabel';
import styles from './DashboardPage.module.scss';

type TabKey = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'DAILY', label: '⚡ 일간 (주문)' },
  { key: 'WEEKLY', label: '📈 주간 (주문)' },
  { key: 'MONTHLY', label: '🏆 월간 (출고)' },
  { key: 'QUARTERLY', label: '📊 분기 (출고)' },
];

const yesterday = (): string => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().slice(0, 10);
};

const DailyTab = () => {
  const [date, setDate] = useState(yesterday());
  const { summary, hourlyRevenue, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend20Days, loading, error, refetch } =
    useDailyDashboard(date);

  return (
    <div>
      <div className={styles.controls}>
        <input className={styles.dateInput} type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <button className={styles.searchBtn} onClick={refetch} disabled={loading}>
          🔍 조회
        </button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p>{error}</p>}

      {!loading && !error && summary && (
        <>
          <div className={styles.section}>
            <MetricCards summary={summary} revenueCardLabel="일간 총 매출 (VAT-)" />
          </div>

          <div className={`${styles.section} ${styles.grid2}`}>
            <HourlyRevenueChart data={hourlyRevenue} />
            <GradeRevenuePie data={gradeRevenue} />
          </div>

          <div className={`${styles.section} ${styles.grid2Even}`}>
            <GradeDetailTable details={gradeDetail} />
            <div>
              <TopItemsChart items={topItems} title="일간 인기 품목 Top 6 (주문금액 & 주문수량)" />
              {jambongBreakdown.length > 0 && (
                <TopItemsChart items={jambongBreakdown} title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)" />
              )}
            </div>
          </div>

          <div className={styles.section}>
            <RevenueTrendChart points={trend20Days} title="최근 20일 매출/주문 (추세 확인용)" granularity="DAY" />
          </div>
        </>
      )}
    </div>
  );
};

const WeeklyTab = () => {
  const [date, setDate] = useState(yesterday());
  const { summary, weekTrend, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend5Weeks, loading, error, refetch } =
    useWeeklyDashboard(date);

  return (
    <div>
      <div className={styles.controls}>
        <input className={styles.dateInput} type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <button className={styles.searchBtn} onClick={refetch} disabled={loading}>
          🔍 조회
        </button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p>{error}</p>}

      {!loading && !error && summary && (
        <>
          <div className={styles.section}>
            <MetricCards summary={summary} revenueCardLabel="주간 총 매출 (VAT-)" />
          </div>

          <div className={`${styles.section} ${styles.grid2}`}>
            <RevenueTrendChart points={weekTrend} title="이번 주 매출/주문 추이" granularity="DAY" />
            <GradeRevenuePie data={gradeRevenue} />
          </div>

          <div className={`${styles.section} ${styles.grid2Even}`}>
            <GradeDetailTable details={gradeDetail} />
            <div>
              <TopItemsChart items={topItems} title="주간 인기 품목 Top 6 (주문금액 & 주문수량)" />
              {jambongBreakdown.length > 0 && (
                <TopItemsChart items={jambongBreakdown} title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)" />
              )}
            </div>
          </div>

          <div className={styles.section}>
            <RevenueTrendChart points={trend5Weeks} title="최근 5주 매출/주문 (추세 확인용)" granularity="WEEK" />
          </div>
        </>
      )}
    </div>
  );
};

const MONTH_LIST_START = '2025-01';

const prevYearMonth = (yearMonth: string): string => {
  const [year, month] = yearMonth.split('-');
  return `${Number(year) - 1}-${month}`;
};

const DAILY_PIVOT_METRICS = [
  { key: 'REVENUE' as const, label: '매출액', isAvg: false },
  { key: 'ORDER_COUNT' as const, label: '주문 건수', isAvg: false },
  { key: 'BUYER_COUNT' as const, label: '주문자 수', isAvg: false },
];

const YEARLY_PIVOT_METRICS = [
  { key: 'REVENUE' as const, label: '매출액', isAvg: false },
  { key: 'ORDER_COUNT' as const, label: '주문 건수', isAvg: false },
  { key: 'BUYER_COUNT' as const, label: '주문자 수', isAvg: false },
  { key: 'AVG_QTY' as const, label: '건당 수량', isAvg: true },
  { key: 'AVG_SKU' as const, label: '건당 SKU', isAvg: true },
  { key: 'AOV' as const, label: 'AOV', isAvg: true },
  { key: 'ARPPU' as const, label: 'ARPPU', isAvg: true },
];

const MonthlyTab = () => {
  const referenceDate = yesterday();
  const monthOptions = generateMonthOptions(MONTH_LIST_START, referenceDate.slice(0, 7));
  const [yearMonth, setYearMonth] = useState(monthOptions[0]);
  const {
    summary,
    dailyTrend,
    gradeRevenue,
    gradeDetail,
    topItems,
    jambongBreakdown,
    dailyPivot,
    dailyPivotLabels,
    yoyComparison,
    yearlyTrend,
    yearlyPivot,
    yearlyPivotLabels,
    loading,
    error,
    refetch,
  } = useMonthlyDashboard(yearMonth, referenceDate);

  return (
    <div>
      <div className={styles.controls}>
        <select className={styles.selectInput} value={yearMonth} onChange={(e) => setYearMonth(e.target.value)}>
          {monthOptions.map((m) => (
            <option key={m} value={m}>
              {m}
            </option>
          ))}
        </select>
        <button className={styles.searchBtn} onClick={refetch} disabled={loading}>
          🔍 조회
        </button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p>{error}</p>}

      {!loading && !error && summary && (
        <>
          <p className={styles.memo}>이 대시보드는 월 단위로 확인 가능합니다</p>

          <div className={styles.section}>
            <MetricCards summary={summary} showArppu revenueCardLabel="월간 총 매출 (VAT-)" />
          </div>

          <div className={`${styles.section} ${styles.grid2}`}>
            <RevenueTrendChart points={dailyTrend} title="월간 출고 추이" granularity="DAY" />
            <GradeRevenuePie data={gradeRevenue} />
          </div>

          <div className={`${styles.section} ${styles.grid2Even}`}>
            <GradeDetailTable details={gradeDetail} showArppu aovLabel="AOV" />
            <div>
              <TopItemsChart items={topItems} title="월간 인기 품목 Top 6 (주문금액 & 주문수량)" />
              {jambongBreakdown.length > 0 && (
                <TopItemsChart items={jambongBreakdown} title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)" />
              )}
            </div>
          </div>

          <div className={styles.section}>
            <GradePivotTable
              title="일별 실적 상세"
              metrics={DAILY_PIVOT_METRICS}
              dataByMetric={dailyPivot}
              columnLabels={dailyPivotLabels.map(formatShortDayLabel)}
            />
          </div>

          {yoyComparison && (
            <div className={`${styles.section} ${styles.grid2Even}`}>
              <YoyComparisonChart
                title="전년 동월 비교 — 매출, 주문건수"
                labels={[prevYearMonth(yearMonth), yearMonth]}
                barLabel="매출액"
                barValues={[yoyComparison.previous.revenue, yoyComparison.current.revenue]}
                lineLabel="주문건수"
                lineValues={[yoyComparison.previous.orderCount, yoyComparison.current.orderCount]}
              />
              <YoyComparisonChart
                title="전년 동월 비교 — 구매자 수, AOV"
                labels={[prevYearMonth(yearMonth), yearMonth]}
                barLabel="구매자 수"
                barValues={[yoyComparison.previous.buyerCount, yoyComparison.current.buyerCount]}
                lineLabel="AOV"
                lineValues={[yoyComparison.previous.aov, yoyComparison.current.aov]}
              />
            </div>
          )}

          <div className={styles.section}>
            <RevenueTrendChart points={yearlyTrend} title="최근 2개년 추이" granularity="MONTH" />
          </div>

          <div className={styles.section}>
            <GradePivotTable
              title="월별 실적 상세"
              metrics={YEARLY_PIVOT_METRICS}
              dataByMetric={yearlyPivot}
              columnLabels={yearlyPivotLabels.map(formatShortMonthLabel)}
              showTrendChart
            />
          </div>
        </>
      )}
    </div>
  );
};

const QUARTER_LIST_START: QuarterValue = { year: 2025, quarter: 1 };

const currentQuarterOf = (dateStr: string): QuarterValue => {
  const [year, month] = dateStr.split('-').map(Number);
  return { year, quarter: Math.floor((month - 1) / 3) + 1 };
};

const formatQuarterLabel = ({ year, quarter }: QuarterValue): string => `${year}-${quarter}Q`;

const MONTHLY_PIVOT_METRICS = [
  { key: 'REVENUE' as const, label: '매출액', isAvg: false },
  { key: 'ORDER_COUNT' as const, label: '주문 건수', isAvg: false },
  { key: 'BUYER_COUNT' as const, label: '주문자 수', isAvg: false },
];

const QUARTERLY_PIVOT_METRICS = [
  { key: 'REVENUE' as const, label: '매출액', isAvg: false },
  { key: 'ORDER_COUNT' as const, label: '주문 건수', isAvg: false },
  { key: 'BUYER_COUNT' as const, label: '주문자 수', isAvg: false },
  { key: 'AVG_QTY' as const, label: '건당 수량', isAvg: true },
  { key: 'AVG_SKU' as const, label: '건당 SKU', isAvg: true },
  { key: 'AOV' as const, label: 'AOV', isAvg: true },
  { key: 'ARPPU' as const, label: 'ARPPU', isAvg: true },
];

const QuarterlyTab = () => {
  const referenceDate = yesterday();
  const quarterOptions = generateQuarterOptions(QUARTER_LIST_START, currentQuarterOf(referenceDate));
  const [selected, setSelected] = useState<QuarterValue>(quarterOptions[0]);
  const {
    summary,
    monthlyTrend,
    gradeRevenue,
    gradeDetail,
    topItems,
    jambongBreakdown,
    monthlyPivot,
    monthlyPivotLabels,
    yoyComparison,
    eightQuarterTrend,
    quarterlyPivot,
    quarterlyPivotLabels,
    loading,
    error,
    refetch,
  } = useQuarterlyDashboard(selected.year, selected.quarter);

  return (
    <div>
      <div className={styles.controls}>
        <select
          className={styles.selectInput}
          value={formatQuarterLabel(selected)}
          onChange={(e) => {
            const found = quarterOptions.find((q) => formatQuarterLabel(q) === e.target.value);
            if (found) setSelected(found);
          }}
        >
          {quarterOptions.map((q) => (
            <option key={formatQuarterLabel(q)} value={formatQuarterLabel(q)}>
              {formatQuarterLabel(q)}
            </option>
          ))}
        </select>
        <button className={styles.searchBtn} onClick={refetch} disabled={loading}>
          🔍 조회
        </button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p>{error}</p>}

      {!loading && !error && summary && (
        <>
          <p className={styles.memo}>이 대시보드는 분기 단위로 확인 가능합니다</p>

          <div className={styles.section}>
            <MetricCards summary={summary} showArppu revenueCardLabel="분기 총 매출 (VAT-)" />
          </div>

          <div className={`${styles.section} ${styles.grid2}`}>
            <RevenueTrendChart points={monthlyTrend} title="분기 내 월별 출고 추이" granularity="MONTH" />
            <GradeRevenuePie data={gradeRevenue} />
          </div>

          <div className={`${styles.section} ${styles.grid2Even}`}>
            <GradeDetailTable details={gradeDetail} showArppu aovLabel="AOV" />
            <div>
              <TopItemsChart items={topItems} title="분기 인기 품목 Top 6 (주문금액 & 주문수량)" />
              {jambongBreakdown.length > 0 && (
                <TopItemsChart items={jambongBreakdown} title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)" />
              )}
            </div>
          </div>

          <div className={styles.section}>
            <GradePivotTable
              title="월별 실적 상세"
              metrics={MONTHLY_PIVOT_METRICS}
              dataByMetric={monthlyPivot}
              columnLabels={monthlyPivotLabels.map(formatShortMonthLabel)}
            />
          </div>

          {yoyComparison && (
            <div className={`${styles.section} ${styles.grid2Even}`}>
              <YoyComparisonChart
                title="전년 동분기 비교 — 매출, 주문건수"
                labels={[formatQuarterLabel({ year: selected.year - 1, quarter: selected.quarter }), formatQuarterLabel(selected)]}
                barLabel="매출액"
                barValues={[yoyComparison.previous.revenue, yoyComparison.current.revenue]}
                lineLabel="주문건수"
                lineValues={[yoyComparison.previous.orderCount, yoyComparison.current.orderCount]}
              />
              <YoyComparisonChart
                title="전년 동분기 비교 — 구매자 수, AOV"
                labels={[formatQuarterLabel({ year: selected.year - 1, quarter: selected.quarter }), formatQuarterLabel(selected)]}
                barLabel="구매자 수"
                barValues={[yoyComparison.previous.buyerCount, yoyComparison.current.buyerCount]}
                lineLabel="AOV"
                lineValues={[yoyComparison.previous.aov, yoyComparison.current.aov]}
              />
            </div>
          )}

          <div className={styles.section}>
            <RevenueTrendChart points={eightQuarterTrend} title="최근 8분기 추이" granularity="QUARTER" />
          </div>

          <div className={styles.section}>
            <GradePivotTable
              title="분기별 실적 상세"
              metrics={QUARTERLY_PIVOT_METRICS}
              dataByMetric={quarterlyPivot}
              columnLabels={quarterlyPivotLabels.map(formatShortQuarterLabel)}
              showTrendChart
            />
          </div>
        </>
      )}
    </div>
  );
};

const DashboardPage = () => {
  const [tab, setTab] = useState<TabKey>('DAILY');

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.title}>💰 에쓰푸드몰 대시보드</div>
        <OrderSyncControl />
      </div>

      <div className={styles.tabs}>
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`${styles.tab} ${tab === t.key ? styles.tabActive : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'DAILY' && <DailyTab />}
      {tab === 'WEEKLY' && <WeeklyTab />}
      {tab === 'MONTHLY' && <MonthlyTab />}
      {tab === 'QUARTERLY' && <QuarterlyTab />}
    </div>
  );
};

export default DashboardPage;
