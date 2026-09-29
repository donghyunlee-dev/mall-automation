export const DESIGN_THEME_STORAGE_KEY = 'sfood.designTheme';

export const DESIGN_THEMES = [
  { value: 'default', label: 'Default (기본)' },
  { value: 'admin', label: 'Admin' },
  { value: 'landing', label: 'Landing' },
  { value: 'ai-service', label: 'AI Service' },
  { value: 'fintech', label: 'Fintech' },
  { value: 'devtools', label: 'Devtools' },
] as const;

export type DesignTheme = (typeof DESIGN_THEMES)[number]['value'];

export const DEFAULT_DESIGN_THEME: DesignTheme = 'default';

export const isDesignTheme = (value: string | null): value is DesignTheme =>
  DESIGN_THEMES.some((theme) => theme.value === value);

export const getStoredDesignTheme = (): DesignTheme => {
  if (typeof window === 'undefined') return DEFAULT_DESIGN_THEME;

  const storedTheme = window.localStorage.getItem(DESIGN_THEME_STORAGE_KEY);
  return isDesignTheme(storedTheme) ? storedTheme : DEFAULT_DESIGN_THEME;
};

export const applyDesignTheme = (theme: DesignTheme) => {
  if (typeof document === 'undefined') return;

  document.documentElement.dataset.designTheme = theme;
};

export const storeDesignTheme = (theme: DesignTheme) => {
  if (typeof window === 'undefined') return;

  window.localStorage.setItem(DESIGN_THEME_STORAGE_KEY, theme);
};
