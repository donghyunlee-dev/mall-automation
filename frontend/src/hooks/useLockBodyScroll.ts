import { useEffect } from 'react';

// 여러 모달이 동시에/중첩되어 열려도 스크롤 잠금이 꼬이지 않도록 참조 카운트로 관리한다.
let lockCount = 0;
let prevOverflow = '';

/**
 * 모달이 열려 있는 동안 body 스크롤을 잠근다.
 * @param locked 잠금 여부. 마운트로 열림/닫힘이 제어되는 모달은 기본값(true) 그대로 사용하고,
 *               prop으로 열림 상태를 제어하는 모달은 그 상태를 넘긴다.
 */
export function useLockBodyScroll(locked = true) {
  useEffect(() => {
    if (!locked) {
      return;
    }
    if (lockCount === 0) {
      prevOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
    }
    lockCount += 1;
    return () => {
      lockCount -= 1;
      if (lockCount === 0) {
        document.body.style.overflow = prevOverflow;
      }
    };
  }, [locked]);
}
