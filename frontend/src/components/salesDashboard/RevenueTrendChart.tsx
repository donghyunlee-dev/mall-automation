import { Chart } from 'react-chartjs-2';
import type { ChartData, ChartOptions } from 'chart.js';
import type { DashboardTrendPoint } from '../../types/dashboard';
import { formatTrendPointLabel, getTrendAxisTitle, type TrendGranularity } from '../../utils/dashboardLabel';
import './chartSetup';
import styles from './RevenueTrendChart.module.scss';

interface Props {
  points: DashboardTrendPoint[];
  title: string;
  granularity: TrendGranularity;
}

const RevenueTrendChart = ({ points, title, granularity }: Props) => {
  const data: ChartData<'bar' | 'line'> = {
    labels: points.map((p) => formatTrendPointLabel(granularity, p)),
    datasets: [
      {
        type: 'bar' as const,
        label: '매출액',
        data: points.map((p) => p.revenue),
        backgroundColor: '#636efa',
        yAxisID: 'y',
        order: 2,
        pointStyle: 'rect' as const,
      },
      {
        type: 'line' as const,
        label: '주문건수',
        data: points.map((p) => p.orderCount),
        borderColor: '#ef553b',
        yAxisID: 'y1',
        order: 1,
        pointStyle: 'line' as const,
        fill: false,
      },
    ],
  };

  const options: ChartOptions<'bar' | 'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { labels: { usePointStyle: true } } },
    scales: {
      x: { title: { display: true, text: getTrendAxisTitle(granularity) } },
      y: { type: 'linear', position: 'left', title: { display: true, text: '매출액' } },
      y1: {
        type: 'linear',
        position: 'right',
        grid: { drawOnChartArea: false },
        title: { display: true, text: '주문건수' },
      },
    },
  };

  return (
    <div>
      <h4>{title}</h4>
      <div className={styles.chartBox}>
        <Chart type="bar" data={data} options={options} />
      </div>
    </div>
  );
};

export default RevenueTrendChart;
