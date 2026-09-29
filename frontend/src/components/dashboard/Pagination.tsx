import styles from './Pagination.module.scss';

interface Props {
  page: number;
  totalPages: number;
  onPage: (p: number) => void;
}

const MAX_VISIBLE = 5;

const getPageRange = (current: number, total: number): number[] => {
  const half = Math.floor(MAX_VISIBLE / 2);
  let start = Math.max(1, current - half);
  const end = Math.min(total, start + MAX_VISIBLE - 1);
  start = Math.max(1, end - MAX_VISIBLE + 1);
  const pages: number[] = [];
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
};

const Pagination = ({ page, totalPages, onPage }: Props) => {
  const pages = getPageRange(page, totalPages);

  return (
    <div className={styles.pagination}>
      <button
        className={styles.pgBtn}
        onClick={() => onPage(page - 1)}
        disabled={page <= 1}
      >
        이전
      </button>

      {pages.map((p) => (
        <button
          key={p}
          className={[styles.pgBtn, p === page ? styles.active : ''].join(' ')}
          onClick={() => onPage(p)}
        >
          {p}
        </button>
      ))}

      <button
        className={styles.pgBtn}
        onClick={() => onPage(page + 1)}
        disabled={page >= totalPages}
      >
        다음
      </button>
    </div>
  );
};

export default Pagination;
