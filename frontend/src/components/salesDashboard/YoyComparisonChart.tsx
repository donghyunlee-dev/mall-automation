import { Chart } from 'react-chartjs-2';
import type { ChartData, ChartOptions } from 'chart.js';
import './chartSetup';
import styles from './YoyComparisonChart.module.scss';

interface Props {
  title: string;
  labels: [string, string];
  barLabel: string;
  barValues: [number, number];
  lineLabel: string;
  lineValues: [number, number];
}

const YoyComparisonChart = ({ title, labels, barLabel, barValues, lineLabel, lineValues }: Props) => {
  const data: ChartData<'bar' | 'line'> = {
    labels,
    datasets: [
      {
        type: 'bar' as const,
        label: barLabel,
        data: barValues,
        backgroundColor: '#a5acfc',
        yAxisID: 'y',
        order: 2,
        pointStyle: 'rect' as const,
      },
      {
        type: 'line' as const,
        label: lineLabel,
        data: lineValues,
        borderColor: '#2563eb',
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
      y: { type: 'linear', position: 'left', title: { display: true, text: barLabel } },
      y1: {
        type: 'linear',
        position: 'right',
        grid: { drawOnChartArea: false },
        title: { display: true, text: lineLabel },
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

export default YoyComparisonChart;
