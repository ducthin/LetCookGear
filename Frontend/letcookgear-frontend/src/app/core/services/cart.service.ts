import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import { Cart } from '../models/cart.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly cartState = signal<Cart | null>(null);

  readonly cart = computed(() => this.cartState());
  readonly totalItems = computed(() => this.cartState()?.totalItems ?? 0);

  constructor(private readonly http: HttpClient) {}

  loadMyCart(): Observable<Cart> {
    return this.http.get<ApiResponse<Cart>>('/api/cart').pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không tìm thấy giỏ hàng');
        }
        return res.data;
      }),
      tap((cart) => this.cartState.set(cart)),
    );
  }

  addItem(variantId: number, quantity: number): Observable<Cart> {
    return this.http
      .post<ApiResponse<Cart>>('/api/cart/items', { variantId, quantity })
      .pipe(
        map((res) => {
          if (!res.data) {
            throw new Error('Không thể thêm sản phẩm vào giỏ hàng');
          }
          return res.data;
        }),
        tap((cart) => this.cartState.set(cart)),
      );
  }

  updateItem(itemId: number, quantity: number): Observable<Cart> {
    return this.http
      .put<ApiResponse<Cart>>(`/api/cart/items/${itemId}`, { quantity })
      .pipe(
        map((res) => {
          if (!res.data) {
            throw new Error('Không thể cập nhật sản phẩm trong giỏ hàng');
          }
          return res.data;
        }),
        tap((cart) => this.cartState.set(cart)),
      );
  }

  removeItem(itemId: number): Observable<Cart> {
    return this.http.delete<ApiResponse<Cart>>(`/api/cart/items/${itemId}`).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không thể xóa sản phẩm khỏi giỏ hàng');
        }
        return res.data;
      }),
      tap((cart) => this.cartState.set(cart)),
    );
  }

  clearCart(): Observable<Cart> {
    return this.http.delete<ApiResponse<Cart>>('/api/cart/clear').pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không thể xóa toàn bộ giỏ hàng');
        }
        return res.data;
      }),
      tap((cart) => this.cartState.set(cart)),
    );
  }

  clearLocalCart(): void {
    this.cartState.set(null);
  }
}
