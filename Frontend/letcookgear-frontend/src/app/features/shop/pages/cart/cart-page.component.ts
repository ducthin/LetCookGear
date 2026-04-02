import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CartItem } from '../../../../core/models/cart.model';
import { CartService } from '../../../../core/services/cart.service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-page.component.html',
  styleUrl: './cart-page.component.scss',
})
export class CartPageComponent {
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cart = this.cartService.cart;

  readonly hasItems = computed(() => (this.cart()?.items.length ?? 0) > 0);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cartService.loadMyCart().subscribe({
      error: () => {
        this.error.set('Không thể tải giỏ hàng lúc này.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  increase(item: CartItem): void {
    this.cartService.updateItem(item.itemId, item.quantity + 1).subscribe({
      error: () => this.error.set('Không thể cập nhật số lượng.'),
    });
  }

  decrease(item: CartItem): void {
    const next = item.quantity - 1;
    if (next < 1) {
      this.remove(item.itemId);
      return;
    }
    this.cartService.updateItem(item.itemId, next).subscribe({
      error: () => this.error.set('Không thể cập nhật số lượng.'),
    });
  }

  remove(itemId: number): void {
    this.cartService.removeItem(itemId).subscribe({
      error: () => this.error.set('Không thể xóa sản phẩm khỏi giỏ hàng.'),
    });
  }

  clearCart(): void {
    this.cartService.clearCart().subscribe({
      error: () => this.error.set('Không thể xóa toàn bộ giỏ hàng.'),
    });
  }

  goCheckout(): void {
    this.router.navigateByUrl('/shop/checkout');
  }
}
