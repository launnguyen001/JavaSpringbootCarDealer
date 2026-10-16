import { apiRequest } from './http';
import type { AuthUser, UserRole } from '@/types';

// ---- Quản trị tài khoản (ADMIN) ----
export async function listUsers(): Promise<AuthUser[]> {
  return apiRequest<AuthUser[]>('/admin/users', { auth: true });
}

export async function updateUserStatus(id: number, enabled: boolean): Promise<AuthUser> {
  return apiRequest<AuthUser>(`/admin/users/${id}/status`, {
    method: 'PUT',
    body: { enabled },
    auth: true,
  });
}

export async function updateUserRole(id: number, role: UserRole): Promise<AuthUser> {
  return apiRequest<AuthUser>(`/admin/users/${id}/role`, {
    method: 'PUT',
    body: { role },
    auth: true,
  });
}
