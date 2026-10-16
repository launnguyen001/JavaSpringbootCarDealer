import type { AuthUser } from '@/types';

const TOKEN_KEY = 'cardealer_token';
const USER_KEY = 'cardealer_user';
const CUSTOMER_KEY = 'cardealer_customer_id';
const SESSION_KEY = 'cardealer_session_id';

function safeGet(key: string): string | null {
  if (typeof window === 'undefined') return null;
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key: string, value: string): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // ignore
  }
}

function safeRemove(key: string): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.removeItem(key);
  } catch {
    // ignore
  }
}

// ---- JWT ----
export function getToken(): string | null {
  return safeGet(TOKEN_KEY);
}

export function setToken(token: string): void {
  safeSet(TOKEN_KEY, token);
}

export function clearToken(): void {
  safeRemove(TOKEN_KEY);
}

// ---- Phiên đăng nhập (thông tin user) ----
export function getStoredUser(): AuthUser | null {
  const raw = safeGet(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function setStoredUser(user: AuthUser): void {
  safeSet(USER_KEY, JSON.stringify(user));
}

export function clearStoredUser(): void {
  safeRemove(USER_KEY);
}

// ---- Hồ sơ khách hàng (customerId) dùng cho đơn hàng / lái thử / đánh giá ----
export function getCustomerId(): number | null {
  const raw = safeGet(CUSTOMER_KEY);
  if (!raw) return null;
  const id = Number(raw);
  return Number.isFinite(id) ? id : null;
}

export function setCustomerId(id: number): void {
  safeSet(CUSTOMER_KEY, String(id));
}

export function clearCustomerId(): void {
  safeRemove(CUSTOMER_KEY);
}

// ---- sessionId cho giỏ hàng (Redis) ----
export function getSessionId(): string {
  let id = safeGet(SESSION_KEY);
  if (!id) {
    id =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `sess-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    safeSet(SESSION_KEY, id);
  }
  return id;
}
