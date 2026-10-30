import { ApiError } from './http';
import { getToken } from '@/lib/auth/token';
import type { ApiErrorBody } from '@/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api';

function baseUrl(): string {
  if (typeof window !== 'undefined') return '/api';
  return API_BASE_URL;
}

/**
 * Tải file nhị phân (PDF/Excel/CSV) kèm JWT và kích hoạt download ở trình duyệt.
 *
 * <p>Không dùng apiRequest vì hàm đó luôn parse JSON; file PDF/XLSX là blob.
 */
export async function downloadFile(path: string, fallbackName: string): Promise<void> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${baseUrl()}${path}`, { headers, cache: 'no-store' });
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
        path,
      };
    }
    throw new ApiError(res.status, body);
  }

  const blob = await res.blob();
  const filename = filenameFromHeader(res.headers.get('Content-Disposition')) ?? fallbackName;
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

function filenameFromHeader(header: string | null): string | null {
  if (!header) return null;
  // Hỗ trợ cả filename="..." và filename*=UTF-8''...
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (encoded?.[1]) {
    try {
      return decodeURIComponent(encoded[1]);
    } catch {
      return encoded[1];
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(header);
  return plain?.[1] ?? null;
}
