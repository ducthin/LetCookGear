import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Order } from '../../../../core/models/order.model';
import { OrderService } from '../../../../core/services/order.service';

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './orders-page.component.html',
  styleUrl: './orders-page.component.scss',
})
export class OrdersPageComponent {
  private readonly orderService = inject(OrderService);

  private readonly orderStatusLabels: Record<string, string> = {
    PENDING: 'Chờ xử lý',
    CONFIRMED: 'Đã xác nhận',
    PROCESSING: 'Đang xử lý',
    SHIPPED: 'Đang giao',
    DELIVERED: 'Đã giao',
    CANCELLED: 'Đã hủy',
    RETURNED: 'Đã trả hàng',
  };

  private readonly paymentStatusLabels: Record<string, string> = {
    UNPAID: 'Chưa thanh toán',
    PENDING: 'Chờ thanh toán',
    PAID: 'Đã thanh toán',
    FAILED: 'Thất bại',
    REFUNDED: 'Đã hoàn tiền',
  };

  private readonly paymentMethodLabels: Record<string, string> = {
    COD: 'Thanh toán khi nhận hàng',
    BANK_TRANSFER: 'Chuyển khoản ngân hàng',
    VNPAY: 'VNPAY',
    MOMO: 'MoMo',
  };

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly orders = signal<Order[]>([]);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    this.orderService.getMyOrders().subscribe({
      next: (orders) => this.orders.set(orders),
      error: () => {
        this.error.set('Không thể tải danh sách đơn hàng của bạn.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  displayOrderStatus(status: string): string {
    return this.orderStatusLabels[status] ?? status;
  }

  displayPaymentStatus(status: string): string {
    return this.paymentStatusLabels[status] ?? status;
  }

  displayPaymentMethod(method: string): string {
    return this.paymentMethodLabels[method] ?? method;
  }
}
