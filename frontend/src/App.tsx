import { lazy, Suspense, useEffect, useState, type ReactElement } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { getSession } from './api/auth';
import LoginPage from './pages/LoginPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import MembersPage from './pages/MembersPage';
import DashboardPage from './pages/DashboardPage';

// GoodsPage는 xlsx(~840kB min) 정적 의존이 있어 라우트 단위로 분리 — 회원관리 등 다른 페이지 초기 로딩에서 제외
const GoodsPage = lazy(() => import('./pages/GoodsPage'));
import AuthedLayout from './components/layout/AuthedLayout';

const RequireAuth = ({ children }: { children: ReactElement }) => {
  const [status, setStatus] = useState<'checking' | 'authenticated' | 'unauthenticated'>('checking');

  useEffect(() => {
    getSession()
      .then(() => setStatus('authenticated'))
      .catch(() => setStatus('unauthenticated'));
  }, []);

  if (status === 'checking') {
    return null;
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route element={<AuthedLayout />}>
          <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
          <Route path="/goods" element={<RequireAuth><Suspense fallback={null}><GoodsPage /></Suspense></RequireAuth>} />
          <Route path="/members" element={<RequireAuth><MembersPage /></RequireAuth>} />
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
