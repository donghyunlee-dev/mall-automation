import { useState, useRef, useEffect } from 'react';
import useAuth from '../hooks/useAuth';
import styles from './LoginPage.module.scss';

// 타이머 포맷: 300 → "5:00"
const formatTimer = (sec: number): string => {
  const m = Math.floor(sec / 60);
  const s = String(sec % 60).padStart(2, '0');
  return `${m}:${s}`;
};

const LoginPage = () => {
  const {
    step,
    email: confirmedEmail,
    loading,
    error,
    timerSec,
    resendCooldown,
    sessionChecked,
    sendOtp,
    resendOtp,
    verifyOtp,
    goBackToEmail,
    clearError,
    loginWithMicrosoft,
  } = useAuth();

  const [emailInput, setEmailInput] = useState('');
  const [otpInput, setOtpInput] = useState('');

  const emailRef = useRef<HTMLInputElement>(null);
  const otpRef = useRef<HTMLInputElement>(null);
  const lastAutoVerifiedOtpRef = useRef('');

  // 스텝 전환 시 포커스 이동
  useEffect(() => {
    if (step === 'email') {
      emailRef.current?.focus();
    } else {
      otpRef.current?.focus();
    }
  }, [step]);

  const isEmailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.trim());
  const isOtpReady = otpInput.length === 6 && timerSec > 0;

  useEffect(() => {
    if (step !== 'otp' || loading) return;

    if (otpInput.length < 6) {
      lastAutoVerifiedOtpRef.current = '';
      return;
    }

    if (!isOtpReady || lastAutoVerifiedOtpRef.current === otpInput) return;

    lastAutoVerifiedOtpRef.current = otpInput;
    verifyOtp(otpInput);
  }, [isOtpReady, loading, otpInput, step, verifyOtp]);

  const handleSendOtp = () => {
    if (!isEmailValid || loading) return;
    sendOtp(emailInput.trim().toLowerCase());
  };

  const handleVerifyOtp = () => {
    if (!isOtpReady || loading) return;
    verifyOtp(otpInput);
  };

  const handleResend = () => {
    if (resendCooldown > 0) return;
    setOtpInput('');
    lastAutoVerifiedOtpRef.current = '';
    resendOtp(confirmedEmail);
  };

  const handleGoBack = () => {
    setOtpInput('');
    lastAutoVerifiedOtpRef.current = '';
    goBackToEmail();
  };

  const handleOtpInput = (v: string) => {
    const digits = v.replace(/\D/g, '').slice(0, 6);
    setOtpInput(digits);
    if (error) clearError();
  };

  if (!sessionChecked) {
    // 세션 확인 중 — 빈 화면 (리다이렉트 대기)
    return null;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.logo}>
          에쓰푸드몰 <em>운영 관리</em>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.card}>
          {/* 카드 상단 */}
          <div className={styles.cardTop}>
            <div className={styles.cardIcon}>
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>
            <div className={styles.cardTitle}>로그인</div>
            <div className={styles.cardDesc}>회사 계정으로 로그인해주세요</div>
          </div>

          {/* MS 로그인 (기본 로그인 방식) */}
          <button className={styles.msLoginBtn} onClick={loginWithMicrosoft}>
            <MicrosoftLogo />
            Microsoft 계정으로 로그인
          </button>

          <div className={styles.divider}>
            <span>또는 회사 이메일로 로그인</span>
          </div>

          {/* 스텝 인디케이터 */}
          <div className={styles.steps}>
            <div
              className={[
                styles.step,
                step === 'email' ? styles.stepActive : styles.stepDone,
              ].join(' ')}
            >
              <div className={styles.stepNum}>
                {step === 'email' ? '1' : '✓'}
              </div>
              <span className={styles.stepLbl}>이메일 입력</span>
            </div>
            <div className={styles.stepLine} />
            <div
              className={[
                styles.step,
                step === 'otp' ? styles.stepActive : '',
              ].join(' ')}
            >
              <div className={styles.stepNum}>2</div>
              <span className={styles.stepLbl}>인증번호 확인</span>
            </div>
          </div>

          {/* ── Step 1: 이메일 입력 ── */}
          {step === 'email' && (
            <div>
              {error && (
                <div className={styles.errBox}>
                  <ErrorIcon />
                  <span>{error}</span>
                </div>
              )}
              <div className={styles.formRow}>
                <label htmlFor="email-input">이메일</label>
                <input
                  id="email-input"
                  ref={emailRef}
                  type="email"
                  placeholder="name@s-food.com"
                  autoComplete="email"
                  value={emailInput}
                  onChange={(e) => {
                    setEmailInput(e.target.value);
                    if (error) clearError();
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSendOtp();
                  }}
                />
              </div>
              <button
                className={[styles.primaryBtn, loading ? styles.loading : ''].join(' ')}
                onClick={handleSendOtp}
                disabled={!isEmailValid || loading}
              >
                {loading ? (
                  <>
                    <SpinnerIcon />
                    처리 중...
                  </>
                ) : (
                  <>
                    인증코드 발송
                  </>
                )}
              </button>
            </div>
          )}

          {/* ── Step 2: OTP 입력 ── */}
          {step === 'otp' && (
            <div>
              {!error && (
                <div className={styles.okBox}>
                  <CheckIcon />
                  <span>이메일로 인증번호를 발송했어요</span>
                </div>
              )}
              {error && (
                <div className={styles.errBox}>
                  <ErrorIcon />
                  <span>{error}</span>
                </div>
              )}
              <div className={styles.formRow}>
                <label htmlFor="otp-input">인증번호 6자리</label>
                <input
                  id="otp-input"
                  ref={otpRef}
                  type="text"
                  className={styles.otpInput}
                  placeholder="000000"
                  maxLength={6}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  value={otpInput}
                  onChange={(e) => handleOtpInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleVerifyOtp();
                  }}
                />
              </div>

              <div className={styles.timerRow}>
                <div className={styles.timerLabel}>
                  유효 시간{' '}
                  <span
                    className={[
                      styles.timerVal,
                      timerSec <= 60 ? styles.timerWarn : '',
                    ].join(' ')}
                  >
                    {formatTimer(timerSec)}
                  </span>
                </div>
                <button
                  className={styles.resendBtn}
                  onClick={handleResend}
                  disabled={resendCooldown > 0}
                >
                  {resendCooldown > 0 ? `재발송 (${resendCooldown}초)` : '재발송'}
                </button>
              </div>

              <button
                className={[styles.primaryBtn, loading ? styles.loading : ''].join(' ')}
                onClick={handleVerifyOtp}
                disabled={!isOtpReady || loading}
              >
                {loading ? (
                  <>
                    <SpinnerIcon />
                    처리 중...
                  </>
                ) : (
                  '로그인'
                )}
              </button>

              <div className={styles.backRow}>
                <button className={styles.backBtn} onClick={handleGoBack}>
                  <ChevronLeftIcon />
                  이메일 다시 입력
                </button>
              </div>
            </div>
          )}

          <div className={styles.cardFooter}>
            접근 권한 문의 · IT팀 담당자에게 연락해주세요
          </div>
        </div>
      </main>
    </div>
  );
};

// ── SVG 아이콘 ────────────────────────────────────────────────────────────────

const MicrosoftLogo = () => (
  <svg style={{ width: 16, height: 16 }} viewBox="0 0 21 21">
    <rect x="1" y="1" width="9" height="9" fill="#F25022" />
    <rect x="11" y="1" width="9" height="9" fill="#7FBA00" />
    <rect x="1" y="11" width="9" height="9" fill="#00A4EF" />
    <rect x="11" y="11" width="9" height="9" fill="#FFB900" />
  </svg>
);

const SpinnerIcon = () => (
  <svg
    className={styles.spin}
    style={{ width: 14, height: 14 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
  >
    <circle cx="8" cy="8" r="6" strokeOpacity={0.25} />
    <path d="M8 2a6 6 0 0 1 6 6" />
  </svg>
);

const ErrorIcon = () => (
  <svg
    style={{ width: 13, height: 13, flexShrink: 0, marginTop: 1 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
  >
    <circle cx="8" cy="8" r="6" />
    <line x1="8" y1="5.5" x2="8" y2="8.5" />
    <circle cx="8" cy="11" r=".6" fill="currentColor" stroke="none" />
  </svg>
);

const CheckIcon = () => (
  <svg
    style={{ width: 13, height: 13, flexShrink: 0, marginTop: 1 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
  >
    <circle cx="8" cy="8" r="6" />
    <polyline points="5.5,8 7,9.5 10.5,6" />
  </svg>
);

const ChevronLeftIcon = () => (
  <svg
    style={{ width: 11, height: 11 }}
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
  >
    <polyline points="10,3 5,8 10,13" />
  </svg>
);

export default LoginPage;
