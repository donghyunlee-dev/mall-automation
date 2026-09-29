import type { DashboardSummary } from '../../types/dashboard';
import { formatCount, formatDecimal, formatWon } from '../../utils/formatNumber';
import styles from './MetricCards.module.scss';

interface Props {
  summary: DashboardSummary;
  showArppu?: boolean;
  revenueCardLabel?: string;
}

const MetricCards = ({ summary, showArppu = false, revenueCardLabel = '총 매출 (VAT-)' }: Props) => {
  const cards: { label: string; value: string }[] = [
    { label: revenueCardLabel, value: formatWon(summary.revenue) },
    { label: '총 주문 건수', value: formatCount(summary.orderCount, '건') },
    { label: '총 주문자 수', value: formatCount(summary.buyerCount, '명') },
    { label: '총 주문 빈도', value: `${formatDecimal(summary.orderFrequency)}회` },
    { label: '주문당 평균 구매 SKU', value: `${formatDecimal(summary.avgSku)}개` },
    { label: '주문당 평균 구매 수량', value: `${formatDecimal(summary.avgQty)}개` },
    { label: '객단가 (AOV)', value: formatWon(summary.aov) },
    ...(showArppu ? [{ label: '결제자당 평균 매출 (ARPPU)', value: formatWon(summary.arppu) }] : []),
  ];

  return (
    <div className={styles.wrap}>
      {cards.map((card) => (
        <div key={card.label} className={styles.card}>
          <div className={styles.label}>{card.label}</div>
          <div className={styles.value}>{card.value}</div>
        </div>
      ))}
    </div>
  );
};

export default MetricCards;
