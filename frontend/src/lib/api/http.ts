import { ApiErrorBody } from '@/types';
import { getToken } from '@/lib/auth/token';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api';

class ApiError extends Error {
  status: number;
  body: ApiErrorBody;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || 'Lỗi gọi API');
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

// Ở browser dùng đường dẫn tương đối để đi qua rewrite của Next (tránh CORS).
// Ở server (RSC) phải dùng URL tuyệt đối tới backend.
function baseUrl(): string {
  if (typeof window !== 'undefined') return '/api';
  return API_BASE_URL;
}

export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  auth?: boolean;
  cache?: RequestCache;
  headers?: Record<string, string>;
}

/**
 * Gọi API tổng quát cho phía client (có thể gắn JWT).
 */
export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = false, cache = 'no-store', headers = {} } = options;

  const finalHeaders: Record<string, string> = {
    Accept: 'application/json',
    ...headers,
  };
  if (body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json';
  }
  if (auth) {
    const token = getToken();
    if (token) {
      finalHeaders['Authorization'] = `Bearer ${token}`;
    }
  }

  const res = await fetch(`${baseUrl()}${path}`, {
    method,
    cache,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!res.ok) {
    let errorBody: ApiErrorBody;
    try {
      errorBody = (await res.json()) as ApiErrorBody;
    } catch {
      errorBody = {
        timestamp: new Date().toISOString(),
        status: res.status,
        error: res.statusText,
        message: res.statusText,
        path,
      };
    }
    throw new ApiError(res.status, errorBody);
  }

  if (res.status === 204 || res.status === 205) {
    return null as unknown as T;
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

// ---- Dùng cho cả Server Components (RSC) lẫn client ----
// Ở browser dùng '/api' (đi qua rewrite của Next → same-origin, tránh CORS);
// ở server (RSC) dùng URL tuyệt đối tới backend.
async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const url = `${baseUrl()}${path}`;
  const res = await fetch(url, {
    cache: 'no-store',
    headers: {
      Accept: 'application/json',
      ...(init?.headers || {}),
    },
    ...init,
  });

  if (!res.ok) {
    let body: ApiErrorBody;
    try {
      body = (await res.json()) as ApiErrorBody;
    } catch {
      body = {
        timestamp: new Date().toISOString(),
        status: res.status,
        error: res.statusText,
        message: res.statusText,
        path: url,
      };
    }
    throw new ApiError(res.status, body);
  }

  if (res.status === 204 || res.status === 205) {
    return null as unknown as T;
  }

  return res.json();
}

/**
 * Gọi API và trả null khi 404 (dùng cho specs/media có thể thiếu).
 */
export async function fetchOptional<T>(path: string): Promise<T | null> {
  try {
    return await fetchJson<T>(path);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null;
    }
    throw err;
  }
}

export { fetchJson, ApiError };
export default { fetchJson, fetchOptional, apiRequest };
