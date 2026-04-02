import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Brand, Category, Product } from '../../../../core/models/catalog.model';
import { AuthService } from '../../../../core/services/auth.service';
import { CartService } from '../../../../core/services/cart.service';
import { CatalogService } from '../../../../core/services/catalog.service';

@Component({
  selector: 'app-product-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './product-list-page.component.html',
  styleUrl: './product-list-page.component.scss',
})
export class ProductListPageComponent {
  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly addingProductId = signal<number | null>(null);

  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);

  readonly searchTerm = signal('');
  readonly selectedCategoryId = signal<number | null>(null);
  readonly selectedBrandId = signal<number | null>(null);

  readonly filteredProducts = computed(() => {
    const keyword = this.searchTerm().trim().toLowerCase();
    const categoryId = this.selectedCategoryId();
    const brandId = this.selectedBrandId();

    return this.products().filter((product) => {
      if (categoryId !== null && product.categoryId !== categoryId) {
        return false;
      }
      if (brandId !== null && product.brandId !== brandId) {
        return false;
      }
      if (!keyword) {
        return true;
      }
      return (
        product.name.toLowerCase().includes(keyword) ||
        product.slug.toLowerCase().includes(keyword) ||
        (product.shortDescription ?? '').toLowerCase().includes(keyword)
      );
    });
  });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      products: this.catalogService.getProducts(),
      categories: this.catalogService.getCategories(),
      brands: this.catalogService.getBrands(),
    }).subscribe({
      next: ({ products, categories, brands }) => {
        this.products.set(products);
        this.categories.set(categories);
        this.brands.set(brands);
      },
      error: (err: unknown) => {
        const fallback = 'Không thể tải dữ liệu sản phẩm lúc này.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
      },
      complete: () => this.loading.set(false),
    });
  }

  onSearchInput(value: string): void {
    this.searchTerm.set(value);
  }

  onCategoryChange(value: string): void {
    this.selectedCategoryId.set(value ? Number(value) : null);
  }

  onBrandChange(value: string): void {
    this.selectedBrandId.set(value ? Number(value) : null);
  }

  addToCart(product: Product): void {
    const defaultVariant = product.variants.find((v) => v.status === 'ACTIVE') ?? product.variants[0];
    if (!defaultVariant) {
      this.error.set('Sản phẩm không có phiên bản để mua.');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login'], {
        queryParams: { returnUrl: this.router.url },
      });
      return;
    }

    this.addingProductId.set(product.id);
    this.cartService.addItem(defaultVariant.id, 1).subscribe({
      error: () => this.error.set('Không thể thêm vào giỏ hàng. Vui lòng thử lại.'),
      complete: () => this.addingProductId.set(null),
    });
  }
}
