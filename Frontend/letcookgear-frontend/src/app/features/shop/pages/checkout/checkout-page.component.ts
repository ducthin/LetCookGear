import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { CheckoutPayload, PaymentMethod } from '../../../../core/models/order.model';
import { CartService } from '../../../../core/services/cart.service';
import { OrderService } from '../../../../core/services/order.service';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.scss',
})
export class CheckoutPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  private readonly paymentMethodLabels: Record<PaymentMethod, string> = {
    COD: 'Thanh toán khi nhận hàng',
    BANK_TRANSFER: 'Chuyển khoản ngân hàng',
    VNPAY: 'VNPAY',
    MOMO: 'MoMo',
    PAYOS: 'PayOS',
  };

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly success = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly cart = this.cartService.cart;

  readonly paymentMethods: PaymentMethod[] = ['COD', 'BANK_TRANSFER', 'VNPAY', 'MOMO', 'PAYOS'];

  readonly form = this.fb.nonNullable.group({
    receiverName: ['', [Validators.required]],
    phone: ['', [Validators.required]],
    province: ['', [Validators.required]],
    district: ['', [Validators.required]],
    ward: ['', [Validators.required]],
    detail: ['', [Validators.required]],
    paymentMethod: ['COD' as PaymentMethod, [Validators.required]],
  });

  readonly canCheckout = computed(() => (this.cart()?.items.length ?? 0) > 0 && !this.submitting());

  constructor() {
    this.cartService.loadMyCart().subscribe({
      error: () => {
        this.error.set('Không thể tải giỏ hàng để thanh toán.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  submit(): void {
    this.error.set(null);
    this.success.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.cart()?.items.length) {
      this.error.set('Giỏ hàng của bạn đang trống.');
      return;
    }

    this.submitting.set(true);
    this.orderService.checkout(this.form.getRawValue() as CheckoutPayload).subscribe({
      next: (order) => {
        if (order.paymentMethod === 'PAYOS') {
          if (!order.checkoutUrl) {
            this.error.set('Không thể tạo liên kết thanh toán PayOS.');
            this.submitting.set(false);
            return;
          }

          this.success.set(`Đang chuyển tới PayOS cho đơn ${order.orderCode}...`);
          this.cartService.loadMyCart().subscribe();
          globalThis.location.assign(order.checkoutUrl);
          return;
        }

        this.success.set(`Đặt đơn ${order.orderCode} thành công.`);
        this.cartService.loadMyCart().subscribe();
        this.router.navigateByUrl('/shop/orders');
      },
      error: (err: unknown) => {
        const fallback = 'Thanh toán thất bại. Vui lòng kiểm tra lại thông tin.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
        this.submitting.set(false);
      },
      complete: () => this.submitting.set(false),
    });
  }

  displayPaymentMethod(method: PaymentMethod): string {
    return this.paymentMethodLabels[method] ?? method;
  }
}
