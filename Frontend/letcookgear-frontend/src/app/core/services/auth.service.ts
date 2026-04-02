import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import {
  AuthMeResponse,
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
} from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenStorageKey = 'lcg_access_token';
  private readonly userState = signal<AuthUser | null>(null);

  readonly currentUser = computed(() => this.userState());
  readonly isAuthenticated = computed(() => this.userState() !== null);

  constructor(private readonly http: HttpClient) {}

  bootstrap(): Observable<AuthUser | null> {
    if (!this.getToken()) {
      this.userState.set(null);
      return of(null);
    }

    return this.me().pipe(
      map((user) => {
        this.userState.set(user);
        return user;
      }),
      catchError(() => {
        this.logout();
        return of(null);
      }),
    );
  }

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<ApiResponse<AuthResponse>>('/api/auth/login', payload)
      .pipe(
        map((res) => {
          if (!res.data) {
            throw new Error('Đăng nhập thất bại: thiếu dữ liệu token');
          }
          this.setToken(res.data.token);
          return {
            email: res.data.email,
            fullName: res.data.fullName,
            phone: null,
            roles: res.data.roles,
          } satisfies AuthUser;
        }),
        tap((user) => this.userState.set(user)),
      );
  }

  register(payload: RegisterRequest): Observable<AuthUser> {
    return this.http
      .post<ApiResponse<AuthResponse>>('/api/auth/register', payload)
      .pipe(
        map((res) => {
          if (!res.data) {
            throw new Error('Đăng ký thất bại: thiếu dữ liệu token');
          }
          this.setToken(res.data.token);
          return {
            email: res.data.email,
            fullName: res.data.fullName,
            phone: payload.phone ?? null,
            roles: res.data.roles,
          } satisfies AuthUser;
        }),
        tap((user) => this.userState.set(user)),
      );
  }

  me(): Observable<AuthUser> {
    return this.http.get<ApiResponse<AuthMeResponse>>('/api/auth/me').pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không thể lấy thông tin người dùng hiện tại');
        }
        return {
          email: res.data.email,
          fullName: res.data.fullName,
          phone: res.data.phone,
          roles: res.data.roles,
        } satisfies AuthUser;
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenStorageKey);
    this.userState.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenStorageKey);
  }

  hasRole(role: string): boolean {
    const user = this.userState();
    return user?.roles.includes(role) ?? false;
  }

  private setToken(token: string): void {
    localStorage.setItem(this.tokenStorageKey, token);
  }
}
