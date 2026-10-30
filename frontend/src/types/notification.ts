export type NotificationType =
  | 'ORDER'
  | 'PAYMENT'
  | 'CONTRACT'
  | 'INVOICE'
  | 'TEST_DRIVE'
  | 'SYSTEM';

/** Thông báo in-app của tài khoản đang đăng nhập (khớp NotificationResponse backend). */
export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  entityType: string | null;
  entityId: number | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}
