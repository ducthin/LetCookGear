import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Order } from '../../../../core/models/order.model';
import { OrderService } from '../../../../core/services/order.service';

type PayOsNoticeKind = 'success' | 'warning' | 'error' | 'info';

interface PayOsNotice {
  kind: PayOsNoticeKind;
  message: string;
  orderId: number | null;
}

const PAYOS_ORDER_CODE_SUFFIX_FACTOR = 1_000_000;

@Component({
  selector: 'app-orders-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './orders-page.component.html',
  styleUrl: './orders-page.component.scss',
})
export class OrdersPageComponent {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

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
  readonly error = signal<string | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly payOsNotice = signal<PayOsNotice | null>(null);

  constructor() {
    this.capturePayOsReturnNotice();
    this.reload();
  }

  private capturePayOsReturnNotice(): void {
    this.route.queryParamMap.subscribe((params) => {
      const hasPayOsParams = ['code', 'id', 'cancel', 'status', 'orderCode'].some((key) => params.has(key));
      if (!hasPayOsParams) {
        return;
      }

      const code = (params.get('code') ?? '').trim();
      const status = (params.get('status') ?? '').trim().toUpperCase();
      const cancel = (params.get('cancel') ?? '').trim().toLowerCase();
      const orderId = this.parseOrderId(params.get('orderCode'));

      if (cancel === 'true') {
        this.payOsNotice.set({
          kind: 'warning',
          message: 'Bạn đã hủy thanh toán PayOS. Bạn có thể thanh toán lại trong chi tiết đơn hàng.',
          orderId,
        });
      } else if (code === '00' || status === 'PAID') {
        this.payOsNotice.set({
          kind: 'success',
          message: 'Thanh toán PayOS thành công. Trạng thái đơn hàng sẽ được cập nhật trong giây lát.',
          orderId,
        });
      } else if (code && code !== '00') {
        this.payOsNotice.set({
          kind: 'error',
          message: `Thanh toán PayOS chưa thành công (mã ${code}). Vui lòng thử lại.`,
          orderId,
        });
      } else {
        this.payOsNotice.set({
          kind: 'info',
          message: 'Đã nhận kết quả từ PayOS.',
          orderId,
        });
      }

      void this.router.navigate([], {
        relativeTo: this.route,
        replaceUrl: true,
        queryParamsHandling: 'merge',
        queryParams: {
          code: null,
          id: null,
          cancel: null,
          status: null,
          orderCode: null,
        },
      });
    });
  }

  private parseOrderId(rawOrderCode: string | null): number | null {
    if (!rawOrderCode) {
      return null;
    }

    const parsed = Number(rawOrderCode);
    if (!Number.isInteger(parsed) || parsed < 1) {
      return null;
    }

    if (parsed >= PAYOS_ORDER_CODE_SUFFIX_FACTOR) {
      const decodedOrderId = Math.floor(parsed / PAYOS_ORDER_CODE_SUFFIX_FACTOR);
      return decodedOrderId > 0 ? decodedOrderId : null;
    }

    return parsed;
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
