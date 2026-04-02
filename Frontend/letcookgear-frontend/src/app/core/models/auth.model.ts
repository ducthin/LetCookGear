export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  email: string;
  fullName: string;
  roles: string[];
}

export interface AuthMeResponse {
  email: string;
  fullName: string;
  phone: string | null;
  roles: string[];
}

export interface AuthUser {
  email: string;
  fullName: string;
  phone: string | null;
  roles: string[];
}
