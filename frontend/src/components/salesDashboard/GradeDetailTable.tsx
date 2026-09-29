import type { GradeDetail } from '../../types/dashboard';
import { sumGradeDetails } from '../../utils/gradeDetailTotal';
import { formatCount, formatDecimal, formatWon } from '../../utils/formatNumber';
import styles from './GradeDetailTable.module.scss';

interface Props {
  details: GradeDetail[];
  showArppu?: boolean;
  aovLabel?: string;
}

const GradeDetailTable = ({ details, showArppu = false, aovLabel = '객단가' }: Props) => {
  const total = sumGradeDetails(details);

  return (
    <div>
      <h4>등급별 상세 지표</h4>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>회원등급</th>
            <th>매출액</th>
            <th>주문건수</th>
            <th>주문자 수</th>
            <th>건당 SKU</th>
            <th>건당 수량</th>
            <th>{aovLabel}</th>
            {showArppu && <th>ARPPU</th>}
          </tr>
        </thead>
        <tbody>
          {details.map((d) => (
            <tr key={d.grade}>
              <td>{d.grade}</td>
              <td>{formatWon(d.revenue)}</td>
              <td>{formatCount(d.orderCount, '건')}</td>
              <td>{formatCount(d.buyerCount, '명')}</td>
              <td>{formatDecimal(d.avgSku)}</td>
              <td>{formatDecimal(d.avgQty)}</td>
              <td>{formatWon(d.aov)}</td>
              {showArppu && <td>{formatWon(d.arppu)}</td>}
            </tr>
          ))}
          <tr className={styles.totalRow}>
            <td>합계</td>
            <td>{formatWon(total.revenue)}</td>
            <td>{formatCount(total.orderCount, '건')}</td>
            <td>{formatCount(total.buyerCount, '명')}</td>
            <td>-</td>
            <td>-</td>
            <td>-</td>
            {showArppu && <td>-</td>}
          </tr>
        </tbody>
      </table>
    </div>
  );
};

export default GradeDetailTable;
