import { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import { requestOtp, confirmOtp, getSession } from '../api/auth';
import { redirectToMicrosoftLogin } from '../utils/msAuth';

type Step = 'email' | 'otp';

interface AuthState {
  step: Step;
  email: string;
  verificationKey: string;
  loading: boolean;
  error: string;
  timerSec: number;
  resendCooldown: number;
  sessionChecked: boolean;
}

const TIMER_INITIAL = 300;
const RESEND_INITIAL = 60;

const OTP_REQUEST_ERRORS: Record<number, string> = {
  400: '이메일 형식이 올바르지 않거나 허용되지 않는 도메인이에요.',
  429: '잠시 후 다시 시도해주세요. (발송 횟수 초과)',
};

const OTP_CONFIRM_ERRORS: Record<number, string> = {
  401: '인증번호가 일치하지 않아요. 다시 확인해주세요.',
  409: '이미 처리된 인증이에요. 재발송해주세요.',
  410: '인증번호가 만료됐어요. 재발송 버튼을 눌러주세요.',
  429: '시도 횟수를 초과했어요. 재발송해주세요.',
};

const getStatusMessage = (
  map: Record<number, string>,
  fallback: string,
  err: unknown,
): string => {
  if (axios.isAxiosError(err) && err.response?.status) {
    return map[err.response.status] ?? fallback;
  }
  return fallback;
};

const useAuth = () => {
  const [state, setState] = useState<AuthState>({
    step: 'email',
    email: '',
    verificationKey: '',
    loading: false,
    error: '',
    timerSec: TIMER_INITIAL,
    resendCooldown: 0,
    sessionChecked: false,
  });

  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const cooldownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 세션 확인 (마운트 시)
  useEffect(() => {
    getSession()
      .then(() => {
        window.location.href = '/';
      })
      .catch(() => {
        setState((prev) => ({ ...prev, sessionChecked: true }));
      });
  }, []);

  const stopTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const startTimer = useCallback(() => {
    stopTimer();
    setState((prev) => ({ ...prev, timerSec: TIMER_INITIAL }));
    timerRef.current = setInterval(() => {
      setState((prev) => {
        const next = prev.timerSec - 1;
        if (next <= 0) {
          stopTimer();
          return {
            ...prev,
            timerSec: 0,
            error: '인증번호가 만료됐어요. 재발송 버튼을 눌러주세요.',
          };
        }
        return { ...prev, timerSec: next };
      });
    }, 1000);
  }, [stopTimer]);

  const startResendCooldown = useCallback(() => {
    if (cooldownRef.current) clearInterval(cooldownRef.current);
    setState((prev) => ({ ...prev, resendCooldown: RESEND_INITIAL }));
    cooldownRef.current = setInterval(() => {
      setState((prev) => {
        const next = prev.resendCooldown - 1;
        if (next <= 0) {
          if (cooldownRef.current) clearInterval(cooldownRef.current);
          cooldownRef.current = null;
          return { ...prev, resendCooldown: 0 };
        }
        return { ...prev, resendCooldown: next };
      });
    }, 1000);
  }, []);

  useEffect(() => {
    return () => {
      stopTimer();
      if (cooldownRef.current) clearInterval(cooldownRef.current);
    };
  }, [stopTimer]);

  const sendOtp = useCallback(async (email: string) => {
    setState((prev) => ({ ...prev, loading: true, error: '' }));
    try {
      const res = await requestOtp({ email });
      setState((prev) => ({
        ...prev,
        step: 'otp',
        email,
        verificationKey: res.verificationKey,
        loading: false,
        error: '',
      }));
      startTimer();
      startResendCooldown();
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: getStatusMessage(
          OTP_REQUEST_ERRORS,
          '오류가 발생했어요. 다시 시도해주세요.',
          err,
        ),
      }));
    }
  }, [startTimer, startResendCooldown]);

  const resendOtp = useCallback(async (email: string) => {
    stopTimer();
    setState((prev) => ({ ...prev, error: '' }));
    try {
      const res = await requestOtp({ email });
      setState((prev) => ({
        ...prev,
        verificationKey: res.verificationKey,
        timerSec: TIMER_INITIAL,
        error: '',
      }));
      startTimer();
      startResendCooldown();
    } catch (err) {
      setState((prev) => ({
        ...prev,
        error: getStatusMessage(
          OTP_REQUEST_ERRORS,
          '재발송 중 오류가 발생했어요.',
          err,
        ),
      }));
    }
  }, [stopTimer, startTimer, startResendCooldown]);

  const verifyOtp = useCallback(
    async (code: string) => {
      if (state.timerSec <= 0) {
        setState((prev) => ({
          ...prev,
          error: '인증번호가 만료됐어요. 재발송 버튼을 눌러주세요.',
        }));
        return;
      }
      setState((prev) => ({ ...prev, loading: true, error: '' }));
      try {
        await confirmOtp({
          verificationKey: state.verificationKey,
          email: state.email,
          code,
        });
        stopTimer();
        window.location.href = '/';
      } catch (err) {
        setState((prev) => ({
          ...prev,
          loading: false,
          error: getStatusMessage(
            OTP_CONFIRM_ERRORS,
            '인증에 실패했어요. 다시 시도해주세요.',
            err,
          ),
        }));
      }
    },
    [state.timerSec, state.verificationKey, state.email, stopTimer],
  );

  const goBackToEmail = useCallback(() => {
    stopTimer();
    if (cooldownRef.current) clearInterval(cooldownRef.current);
    setState((prev) => ({
      ...prev,
      step: 'email',
      error: '',
      verificationKey: '',
      timerSec: TIMER_INITIAL,
      resendCooldown: 0,
    }));
  }, [stopTimer]);

  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: '' }));
  }, []);

  const loginWithMicrosoft = useCallback(() => {
    try {
      redirectToMicrosoftLogin();
    } catch {
      setState((prev) => ({ ...prev, error: 'MS 로그인 설정에 문제가 있어요. 관리자에게 문의해주세요.' }));
    }
  }, []);

  return {
    ...state,
    sendOtp,
    resendOtp,
    verifyOtp,
    goBackToEmail,
    clearError,
    loginWithMicrosoft,
  };
};

export default useAuth;
