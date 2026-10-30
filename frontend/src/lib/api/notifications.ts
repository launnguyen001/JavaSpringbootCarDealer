import { apiRequest } from './http';
import type { AppNotification } from '@/types';

/** Thông báo của tài khoản đang đăng nhập, mới nhất trước. */
export async function listNotifications(): Promise<AppNotification[]> {
  return apiRequest<AppNotification[]>('/notifications', { auth: true });
}

export async function getUnreadNotificationCount(): Promise<number> {
  const result = await apiRequest<{ unread: number }>('/notifications/unread-count', { auth: true });
  return result.unread;
}

export async function markNotificationRead(id: number): Promise<AppNotification> {
  return apiRequest<AppNotification>(`/notifications/${id}/read`, { method: 'PATCH', auth: true });
}

export async function markAllNotificationsRead(): Promise<number> {
  const result = await apiRequest<{ updated: number }>('/notifications/read-all', {
    method: 'PATCH',
    auth: true,
  });
  return result.updated;
}
