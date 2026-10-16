import { apiRequest } from './http';
import type { AuthResponse, AuthUser, RegisterRequest, UpdateMeRequest } from '@/types';

export async function login(payload: { username: string; password: string }): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/auth/login', { method: 'POST', body: payload });
}

export async function register(payload: RegisterRequest): Promise<AuthUser> {
  return apiRequest<AuthUser>('/auth/register', { method: 'POST', body: payload });
}

export async function me(): Promise<AuthUser> {
  return apiRequest<AuthUser>('/auth/me', { auth: true });
}

/** Người dùng tự cập nhật email / mật khẩu của mình. */
export async function updateMe(payload: UpdateMeRequest): Promise<AuthUser> {
  return apiRequest<AuthUser>('/auth/me', { method: 'PUT', body: payload, auth: true });
}

export async function logout(): Promise<void> {
  return apiRequest<void>('/auth/logout', { method: 'POST', auth: true });
}
