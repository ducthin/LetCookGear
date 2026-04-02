import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import { CheckoutPayload, Order, OrderStatusUpdatePayload } from '../models/order.model';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  constructor(private readonly http: HttpClient) {}

  checkout(payload: CheckoutPayload): Observable<Order> {
    return this.http.post<ApiResponse<Order>>('/api/orders/checkout', payload).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Thanh toán thất bại');
        }
        return res.data;
      }),
    );
  }

  getMyOrders(): Observable<Order[]> {
    return this.http.get<ApiResponse<Order[]>>('/api/orders/me').pipe(
      map((res) => res.data ?? []),
    );
  }

  getMyOrderById(orderId: number): Observable<Order> {
    return this.http.get<ApiResponse<Order>>(`/api/orders/me/${orderId}`).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không tìm thấy đơn hàng');
        }
        return res.data;
      }),
    );
  }

  cancelMyOrder(orderId: number): Observable<Order> {
    return this.http.post<ApiResponse<Order>>(`/api/orders/me/${orderId}/cancel`, {}).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không thể hủy đơn hàng');
        }
        return res.data;
      }),
    );
  }

  getAdminOrders(): Observable<Order[]> {
    return this.http.get<ApiResponse<Order[]>>('/api/admin/orders').pipe(
      map((res) => res.data ?? []),
    );
  }

  getAdminOrderById(orderId: number): Observable<Order> {
    return this.http.get<ApiResponse<Order>>(`/api/admin/orders/${orderId}`).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không tìm thấy đơn hàng quản trị');
        }
        return res.data;
      }),
    );
  }

  updateAdminOrderStatus(orderId: number, payload: OrderStatusUpdatePayload): Observable<Order> {
    return this.http.put<ApiResponse<Order>>(`/api/admin/orders/${orderId}/status`, payload).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không thể cập nhật trạng thái đơn hàng');
        }
        return res.data;
      }),
    );
  }
}
