import { Doughnut } from 'react-chartjs-2';
import type { GradeRevenue } from '../../types/dashboard';
import './chartSetup';
import styles from './GradeRevenuePie.module.scss';

interface Props {
  data: GradeRevenue[];
}

const COLORS = ['#636efa', '#ef553b', '#00cc96', '#ab63fa', '#ffa15a', '#19d3f3'];

const GradeRevenuePie = ({ data }: Props) => {
  const chartData = {
    labels: data.map((d) => d.grade),
    datasets: [
      {
        data: data.map((d) => d.revenue),
        backgroundColor: data.map((_, i) => COLORS[i % COLORS.length]),
      },
    ],
  };

  const total = data.reduce((sum, d) => sum + d.revenue, 0);

  return (
    <div>
      <h4>등급별 매출 비중</h4>
      <div className={styles.chartBox}>
        <Doughnut
          data={chartData}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            cutout: '40%',
            plugins: {
              datalabels: {
                display: true,
                color: '#fff',
                formatter: (value: number) => (total > 0 ? `${((value / total) * 100).toFixed(1)}%` : ''),
              },
            },
          }}
        />
      </div>
    </div>
  );
};

export default GradeRevenuePie;
