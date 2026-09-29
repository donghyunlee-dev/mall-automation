import styles from './Toast.module.scss';

interface Props {
  message: string;
  type: 'success' | 'error';
}

const Toast = ({ message, type }: Props) => (
  <div className={`${styles.toast} ${type === 'error' ? styles.error : ''}`}>
    {message}
  </div>
);

export default Toast;
