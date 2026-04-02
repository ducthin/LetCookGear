import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { Order } from '../../../../core/models/order.model';
import { OrderService } from '../../../../core/services/order.service';

@Component({
  selector: 'app-admin-orders-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-orders-page.component.html',
  styleUrl: './admin-orders-page.component.scss',
})
export class AdminOrdersPageComponent {
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
  readonly updatingOrderId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly orders = signal<Order[]>([]);

  readonly transitions = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED'];

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    this.orderService.getAdminOrders().subscribe({
      next: (orders) => this.orders.set(orders),
      error: () => {
        this.error.set('Không thể tải danh sách đơn hàng quản trị.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  updateStatus(order: Order, status: string): void {
    if (!status || status === order.status) {
      return;
    }

    this.updatingOrderId.set(order.orderId);
    this.error.set(null);
    this.success.set(null);

    this.orderService.updateAdminOrderStatus(order.orderId, { status }).subscribe({
      next: (updated) => {
        this.orders.update((current) => current.map((o) => (o.orderId === updated.orderId ? updated : o)));
        this.success.set(`Đã cập nhật đơn ${updated.orderCode} sang ${this.displayOrderStatus(updated.status)}.`);
      },
      error: (err: unknown) => {
        const fallback = 'Không thể cập nhật trạng thái đơn hàng.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
      },
      complete: () => this.updatingOrderId.set(null),
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
