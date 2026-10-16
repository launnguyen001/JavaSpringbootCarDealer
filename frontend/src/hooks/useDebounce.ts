import { useCallback, useRef, useEffect } from 'react';

export function useDebounce<T extends (...args: any[]) => void>(
  callback: T,
  delay: number,
): T {
  const callbackRef = useRef(callback);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Giữ tham chiếu tới callback mới nhất để hàm trả về có danh tính ổn định,
  // tránh việc useEffect phụ thuộc vào nó chạy lại sau mỗi lần render.
  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  return useCallback(
    (...args: any[]) => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      timerRef.current = setTimeout(() => {
        callbackRef.current(...args);
      }, delay);
    },
    [delay],
  ) as T;
}
