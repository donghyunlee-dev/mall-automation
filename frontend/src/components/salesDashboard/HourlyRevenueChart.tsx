import { Line } from 'react-chartjs-2';
import type { HourlyRevenue } from '../../types/dashboard';
import '../salesDashboard/chartSetup';
import styles from './HourlyRevenueChart.module.scss';

interface Props {
  data: HourlyRevenue[];
}

const HourlyRevenueChart = ({ data }: Props) => {
  const chartData = {
    labels: data.map((d) => `${d.hour}시`),
    datasets: [
      {
        label: '매출액',
        data: data.map((d) => d.revenue),
        borderColor: '#636efa',
        backgroundColor: 'rgba(99, 110, 250, 0.15)',
        fill: true,
        tension: 0.3,
      },
    ],
  };

  return (
    <div>
      <h4>시간대별 매출 추이</h4>
      <div className={styles.chartBox}>
        <Line
          data={chartData}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              x: { title: { display: true, text: '주문시간' } },
              y: { beginAtZero: true, title: { display: true, text: '매출액' } },
            },
          }}
        />
      </div>
    </div>
  );
};

export default HourlyRevenueChart;
