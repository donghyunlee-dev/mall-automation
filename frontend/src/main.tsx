import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './styles/global.scss';
import App from './App';
import { applyDesignTheme, getStoredDesignTheme } from './utils/designTheme';

const root = document.getElementById('root');
if (!root) throw new Error('root element not found');

applyDesignTheme(getStoredDesignTheme());

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
