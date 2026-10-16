import { ApiError } from '@/lib/api/http';

/**
 * Lấy message lỗi thân thiện từ lỗi bất kỳ (thường là ApiError).
 */
export function getErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 401) return 'Bạn cần đăng nhập để thực hiện thao tác này.';
    if (err.status === 403) return 'Bạn không có quyền thực hiện thao tác này.';
    return err.body?.message || err.message || 'Đã xảy ra lỗi.';
  }
  if (err instanceof Error) return err.message;
  return 'Đã xảy ra lỗi không xác định.';
}
