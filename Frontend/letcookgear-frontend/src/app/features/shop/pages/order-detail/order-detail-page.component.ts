import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, finalize, map, switchMap } from 'rxjs';

import { Order } from '../../../../core/models/order.model';
import { OrderService } from '../../../../core/services/order.service';

@Component({
  selector: 'app-order-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-detail-page.component.html',
  styleUrl: './order-detail-page.component.scss',
})
export class OrderDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
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
    PAYOS: 'PayOS',
  };

  readonly loading = signal(true);
  readonly cancelling = signal(false);
  readonly retryingPayOs = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly order = signal<Order | null>(null);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('orderId'))),
        switchMap((orderId) => {
          if (!Number.isFinite(orderId) || orderId < 1) {
            this.error.set('Mã đơn hàng không hợp lệ.');
            this.order.set(null);
            this.loading.set(false);
            return EMPTY;
          }

          this.loading.set(true);
          this.error.set(null);
          return this.orderService
            .getMyOrderById(orderId)
            .pipe(finalize(() => this.loading.set(false)));
        }),
      )
      .subscribe({
        next: (order) => this.order.set(order),
        error: () => {
          this.error.set('Không thể tải chi tiết đơn hàng.');
          this.order.set(null);
          this.loading.set(false);
        },
      });
  }

  canCancel(order: Order | null): boolean {
    return !!order && order.status === 'PENDING';
  }

  cancelOrder(orderId: number): void {
    this.cancelling.set(true);
    this.error.set(null);
    this.success.set(null);

    this.orderService.cancelMyOrder(orderId).subscribe({
      next: (updatedOrder) => {
        this.order.set(updatedOrder);
        this.success.set('Hủy đơn hàng thành công.');
      },
      error: (err: unknown) => {
        const fallback = 'Không thể hủy đơn hàng này.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
      },
      complete: () => this.cancelling.set(false),
    });
  }

  canRetryPayOs(order: Order | null): boolean {
    return !!order
      && order.paymentMethod === 'PAYOS'
      && order.paymentStatus !== 'PAID'
      && order.status !== 'CANCELLED'
      && order.status !== 'RETURNED';
  }

  retryPayOsPayment(orderId: number): void {
    this.retryingPayOs.set(true);
    this.error.set(null);
    this.success.set(null);

    this.orderService.retryPayOsCheckout(orderId).subscribe({
      next: (updatedOrder) => {
        this.order.set(updatedOrder);

        if (!updatedOrder.checkoutUrl) {
          this.error.set('Không thể tạo liên kết thanh toán PayOS.');
          this.retryingPayOs.set(false);
          return;
        }

        this.success.set(`Đang chuyển tới PayOS cho đơn ${updatedOrder.orderCode}...`);
        globalThis.location.assign(updatedOrder.checkoutUrl);
      },
      error: (err: unknown) => {
        const fallback = 'Không thể tạo lại liên kết PayOS.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
      },
      complete: () => this.retryingPayOs.set(false),
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
