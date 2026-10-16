export type UserRole = 'ADMIN' | 'USER' | 'EMPLOYEE';

export interface AuthUser {
  id: number;
  username: string;
  email: string | null;
  role: UserRole;
  enabled: boolean;
  createdAt?: string | null;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: UserRole;
  expiresIn: number;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email?: string | null;
}

/** Người dùng tự cập nhật thông tin đăng nhập (bỏ trống = giữ nguyên). */
export interface UpdateMeRequest {
  email?: string | null;
  password?: string | null;
}

/** Admin bật/khoá một tài khoản. */
export interface UserStatusRequest {
  enabled: boolean;
}

/** Admin đổi vai trò một tài khoản. */
export interface UserRoleRequest {
  role: UserRole;
}
