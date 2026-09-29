import { Bar } from 'react-chartjs-2';
import type { TopItem } from '../../types/dashboard';
import './chartSetup';
import styles from './TopItemsChart.module.scss';

interface Props {
  items: TopItem[];
  title: string;
}

const TopItemsChart = ({ items, title }: Props) => {
  const chartData = {
    labels: items.map((i) => i.productName),
    datasets: [
      {
        label: '주문금액',
        data: items.map((i) => i.revenue),
        backgroundColor: '#636efa',
      },
    ],
  };

  return (
    <div>
      <h4>{title}</h4>
      <div className={styles.chartBox}>
        <Bar
          data={chartData}
          options={{
            indexAxis: 'y' as const,
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              x: { title: { display: true, text: '주문금액 (원)' } },
            },
            plugins: {
              legend: { display: false },
              datalabels: {
                display: true,
                anchor: 'end',
                align: 'end',
                color: '#1f2933',
                formatter: (_value, context) => `${items[context.dataIndex].qty.toLocaleString('ko-KR')}개`,
              },
            },
          }}
        />
      </div>
    </div>
  );
};

export default TopItemsChart;
