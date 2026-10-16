export type UserRole = 'ADMIN' | 'USER' | 'EMPLOYEE';

export interface AuthUser {
  id: number;
  username: string;
  email: string | null;
  role: UserRole;
  enabled: boolean;
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
