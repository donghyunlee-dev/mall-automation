import { useState } from 'react';
import { Line } from 'react-chartjs-2';
import type { GradePivotMetric, GradePivotResponse } from '../../types/dashboard';
import './chartSetup';
import styles from './GradePivotTable.module.scss';

export interface GradePivotMetricOption {
  key: GradePivotMetric;
  label: string;
  /** 평균 지표(건당 SKU/건당 수량/AOV/ARPPU)면 합계 행을 '-'로 표시한다. */
  isAvg: boolean;
}

interface Props {
  title: string;
  metrics: GradePivotMetricOption[];
  dataByMetric: Partial<Record<GradePivotMetric, GradePivotResponse>>;
  columnLabels: string[];
  /** 선택 항목의 등급별 추이 라인 차트를 함께 표시할지 여부(월별 실적 상세 전용). */
  showTrendChart?: boolean;
}

const LINE_COLORS = ['#636efa', '#ef553b', '#00cc96', '#ab63fa', '#ffa15a', '#19d3f3'];

const formatValue = (value: number, isAvg: boolean): string =>
  value.toLocaleString('ko-KR', { maximumFractionDigits: isAvg ? 1 : 0 });

const scaledValue = (metricKey: GradePivotMetric, value: number): number =>
  metricKey === 'REVENUE' ? value / 1000 : value;

const GradePivotTable = ({ title, metrics, dataByMetric, columnLabels, showTrendChart = false }: Props) => {
  const [selectedKey, setSelectedKey] = useState<GradePivotMetric>(metrics[0].key);
  const selectedMetric = metrics.find((m) => m.key === selectedKey) ?? metrics[0];
  const data = dataByMetric[selectedKey];

  if (!data) {
    return null;
  }

  const grades = Object.keys(data.byGrade);
  const totals = columnLabels.map((_, colIndex) =>
    selectedMetric.isAvg
      ? null
      : grades.reduce((sum, grade) => sum + scaledValue(selectedKey, data.byGrade[grade][colIndex]), 0),
  );

  return (
    <div>
      <h4>
        {title}
        {selectedKey === 'REVENUE' && ' (매출액 단위: 천 원)'}
      </h4>

      <div className={styles.segmented}>
        {metrics.map((m) => (
          <button
            key={m.key}
            className={`${styles.segBtn} ${m.key === selectedKey ? styles.segBtnActive : ''}`}
            onClick={() => setSelectedKey(m.key)}
          >
            {m.label}
          </button>
        ))}
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>회원등급</th>
              {columnLabels.map((label) => (
                <th key={label}>{label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {grades.map((grade) => (
              <tr key={grade}>
                <td>{grade}</td>
                {data.byGrade[grade].map((value, i) => (
                  <td key={columnLabels[i]}>{formatValue(scaledValue(selectedKey, value), selectedMetric.isAvg)}</td>
                ))}
              </tr>
            ))}
            <tr className={styles.totalRow}>
              <td>합계</td>
              {totals.map((total, i) => (
                <td key={columnLabels[i]}>{total === null ? '-' : formatValue(total, false)}</td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>

      {showTrendChart && (
        <div className={styles.chartBox}>
          <Line
            data={{
              labels: columnLabels,
              datasets: grades.map((grade, i) => ({
                label: grade,
                data: data.byGrade[grade].map((v) => scaledValue(selectedKey, v)),
                borderColor: LINE_COLORS[i % LINE_COLORS.length],
                backgroundColor: LINE_COLORS[i % LINE_COLORS.length],
                tension: 0.2,
              })),
            }}
            options={{
              responsive: true,
              maintainAspectRatio: false,
              plugins: { title: { display: true, text: `등급별 ${selectedMetric.label} 추이 (위 테이블에 연동)` } },
            }}
          />
        </div>
      )}
    </div>
  );
};

export default GradePivotTable;
