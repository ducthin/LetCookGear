import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, finalize, map, switchMap } from 'rxjs';

import { Product, ProductVariant } from '../../../../core/models/catalog.model';
import { AuthService } from '../../../../core/services/auth.service';
import { CartService } from '../../../../core/services/cart.service';
import { CatalogService } from '../../../../core/services/catalog.service';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './product-detail-page.component.html',
  styleUrl: './product-detail-page.component.scss',
})
export class ProductDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly adding = signal(false);
  readonly error = signal<string | null>(null);

  readonly product = signal<Product | null>(null);
  readonly selectedVariantId = signal<number | null>(null);
  readonly quantity = signal(1);

  readonly selectedVariant = computed<ProductVariant | null>(() => {
    const current = this.product();
    const variantId = this.selectedVariantId();
    if (!current || variantId === null) {
      return null;
    }
    return current.variants.find((v) => v.id === variantId) ?? null;
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('slug')),
        switchMap((slug) => {
          if (!slug) {
            this.error.set('Thiếu slug sản phẩm.');
            this.product.set(null);
            this.loading.set(false);
            return EMPTY;
          }

          this.loading.set(true);
          this.error.set(null);
          return this.catalogService
            .getProductBySlug(slug)
            .pipe(finalize(() => this.loading.set(false)));
        }),
      )
      .subscribe({
        next: (product) => {
          this.product.set(product);
          this.selectedVariantId.set(
            product.variants.find((v) => v.status === 'ACTIVE')?.id ?? product.variants[0]?.id ?? null,
          );
        },
        error: () => {
          this.error.set('Không thể tải chi tiết sản phẩm.');
          this.product.set(null);
          this.loading.set(false);
        },
      });
  }

  setVariant(variantId: number): void {
    this.selectedVariantId.set(variantId);
  }

  setQuantity(value: string): void {
    const next = Number(value);
    if (!Number.isFinite(next) || next < 1) {
      this.quantity.set(1);
      return;
    }
    this.quantity.set(Math.floor(next));
  }

  addToCart(): void {
    const variant = this.selectedVariant();
    if (!variant) {
      this.error.set('Vui lòng chọn phiên bản trước.');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login'], {
        queryParams: { returnUrl: this.router.url },
      });
      return;
    }

    this.adding.set(true);
    this.cartService.addItem(variant.id, this.quantity()).subscribe({
      error: () => {
        this.error.set('Không thể thêm sản phẩm vào giỏ hàng.');
        this.adding.set(false);
      },
      complete: () => this.adding.set(false),
    });
  }
}
