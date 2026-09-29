import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { verifyMsLogin } from '../api/auth';
import styles from './LoginPage.module.scss';

const AuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const ranRef = useRef(false);

  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;

    const loginToken = searchParams.get('login_token');
    if (!loginToken) {
      window.location.href = '/login';
      return;
    }

    verifyMsLogin({ loginToken })
      .then(() => {
        window.location.href = '/';
      })
      .catch(() => {
        window.location.href = '/login';
      });
  }, [searchParams]);

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <p role="status">로그인 처리 중...</p>
      </main>
    </div>
  );
};

export default AuthCallbackPage;
