import { CommonModule } from '@angular/common';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';

import { Brand, Category, Product, ProductFacetGroup, ProductFilterQuery, ProductVariant } from '../../../../core/models/catalog.model';
import { AuthService } from '../../../../core/services/auth.service';
import { CartService } from '../../../../core/services/cart.service';
import { CatalogService } from '../../../../core/services/catalog.service';

type SortMode = 'featured' | 'priceAsc' | 'priceDesc' | 'nameAsc' | 'nameDesc' | 'newest' | 'bestSeller';
type PriceBucket = 'all' | 'under20' | '20to25' | '25to30' | '30to35' | '35to40' | 'over40';
type CategoryPreset = 'laptop' | 'pc' | 'monitor' | 'gear' | 'generic';
type ContextFilterParam = 'cpu' | 'ram' | 'storage' | 'gpu' | 'refreshRate' | 'panelType' | 'connectionType' | 'switchType' | 'sizeInch';

interface ContextFilterOptionDef {
  label: string;
  param: ContextFilterParam;
  value: string | number;
  match: (variant: ProductVariant) => boolean;
}

interface ContextFilterGroupDef {
  key: string;
  title: string;
  options: ContextFilterOptionDef[];
}

interface ContextualFilterOption {
  valueKey: string;
  label: string;
  count: number;
}

interface ContextualFilterGroup {
  key: string;
  title: string;
  options: ContextualFilterOption[];
}

const PRESET_FILTER_DEFS: Record<CategoryPreset, ContextFilterGroupDef[]> = {
  laptop: [
    {
      key: 'cpu',
      title: 'CPU',
      options: [
        { label: 'Intel i5', param: 'cpu', value: 'core i5', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i5') },
        { label: 'Intel i7', param: 'cpu', value: 'core i7', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i7') },
        { label: 'Intel i9', param: 'cpu', value: 'core i9', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i9') },
        { label: 'Ryzen 5', param: 'cpu', value: 'ryzen 5', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 5') },
        { label: 'Ryzen 7', param: 'cpu', value: 'ryzen 7', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 7') },
        { label: 'Ryzen 9', param: 'cpu', value: 'ryzen 9', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 9') },
      ],
    },
    {
      key: 'gpu',
      title: 'Card đồ họa',
      options: [
        { label: 'RTX 4050', param: 'gpu', value: 'rtx 4050', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4050') },
        { label: 'RTX 4060', param: 'gpu', value: 'rtx 4060', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4060') },
        { label: 'RTX 4070', param: 'gpu', value: 'rtx 4070', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4070') },
        { label: 'RTX 4080', param: 'gpu', value: 'rtx 4080', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4080') },
        { label: 'RTX 4090', param: 'gpu', value: 'rtx 4090', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4090') },
      ],
    },
    {
      key: 'ram',
      title: 'RAM',
      options: [
        { label: '16GB', param: 'ram', value: 16, match: (v) => v.ramGb === 16 },
        { label: '24GB', param: 'ram', value: 24, match: (v) => v.ramGb === 24 },
        { label: '32GB', param: 'ram', value: 32, match: (v) => v.ramGb === 32 },
      ],
    },
    {
      key: 'refreshRate',
      title: 'Màn hình',
      options: [
        { label: '144Hz', param: 'refreshRate', value: 144, match: (v) => v.refreshRateHz === 144 },
        { label: '165Hz', param: 'refreshRate', value: 165, match: (v) => v.refreshRateHz === 165 },
        { label: '240Hz', param: 'refreshRate', value: 240, match: (v) => v.refreshRateHz === 240 },
      ],
    },
  ],
  pc: [
    {
      key: 'cpu',
      title: 'CPU',
      options: [
        { label: 'Intel i5', param: 'cpu', value: 'core i5', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i5') },
        { label: 'Intel i7', param: 'cpu', value: 'core i7', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i7') },
        { label: 'Intel i9', param: 'cpu', value: 'core i9', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('core i9') },
        { label: 'Ryzen 5', param: 'cpu', value: 'ryzen 5', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 5') },
        { label: 'Ryzen 7', param: 'cpu', value: 'ryzen 7', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 7') },
        { label: 'Ryzen 9', param: 'cpu', value: 'ryzen 9', match: (v) => (v.cpuModel ?? '').toLowerCase().includes('ryzen 9') },
      ],
    },
    {
      key: 'gpu',
      title: 'GPU',
      options: [
        { label: 'RTX 4060', param: 'gpu', value: 'rtx 4060', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4060') },
        { label: 'RTX 4070', param: 'gpu', value: 'rtx 4070', match: (v) => (v.gpuModel ?? '').toLowerCase().includes('rtx 4070') },
        { label: 'RTX 4080+', param: 'gpu', value: 'rtx 4080', match: (v) => {
          const gpu = (v.gpuModel ?? '').toLowerCase();
          return gpu.includes('rtx 4080') || gpu.includes('rtx 4090');
        } },
      ],
    },
    {
      key: 'ram',
      title: 'RAM',
      options: [
        { label: '16GB', param: 'ram', value: 16, match: (v) => v.ramGb === 16 },
        { label: '32GB', param: 'ram', value: 32, match: (v) => v.ramGb === 32 },
        { label: '64GB', param: 'ram', value: 64, match: (v) => v.ramGb === 64 },
      ],
    },
    {
      key: 'storage',
      title: 'Lưu trữ',
      options: [
        { label: '512GB', param: 'storage', value: 512, match: (v) => v.storageGb === 512 },
        { label: '1TB', param: 'storage', value: 1024, match: (v) => v.storageGb === 1024 },
        { label: '2TB', param: 'storage', value: 2048, match: (v) => v.storageGb === 2048 },
      ],
    },
  ],
  monitor: [
    {
      key: 'size',
      title: 'Kích thước',
      options: [
        { label: '24 inch', param: 'sizeInch', value: 24, match: (v) => Number(v.sizeInch ?? 0) === 24 },
        { label: '27 inch', param: 'sizeInch', value: 27, match: (v) => Number(v.sizeInch ?? 0) === 27 },
        { label: '32 inch', param: 'sizeInch', value: 32, match: (v) => Number(v.sizeInch ?? 0) === 32 },
      ],
    },
    {
      key: 'refresh',
      title: 'Tần số quét',
      options: [
        { label: '144Hz', param: 'refreshRate', value: 144, match: (v) => v.refreshRateHz === 144 },
        { label: '165Hz', param: 'refreshRate', value: 165, match: (v) => v.refreshRateHz === 165 },
        { label: '180Hz+', param: 'refreshRate', value: 180, match: (v) => (v.refreshRateHz ?? 0) >= 180 },
      ],
    },
    {
      key: 'panelType',
      title: 'Tấm nền',
      options: [
        { label: 'IPS', param: 'panelType', value: 'ips', match: (v) => (v.panelType ?? '').toLowerCase().includes('ips') },
        { label: 'OLED', param: 'panelType', value: 'oled', match: (v) => (v.panelType ?? '').toLowerCase().includes('oled') },
        { label: 'VA', param: 'panelType', value: 'va', match: (v) => (v.panelType ?? '').toLowerCase().includes('va') },
      ],
    },
  ],
  gear: [
    {
      key: 'connection',
      title: 'Kết nối',
      options: [
        { label: 'Wireless', param: 'connectionType', value: 'wireless', match: (v) => (v.connectionType ?? '').toLowerCase().includes('wireless') },
        { label: 'Wired', param: 'connectionType', value: 'wired', match: (v) => (v.connectionType ?? '').toLowerCase().includes('wired') },
      ],
    },
    {
      key: 'switchType',
      title: 'Switch',
      options: [
        { label: 'Optical', param: 'switchType', value: 'optical', match: (v) => (v.switchType ?? '').toLowerCase().includes('optical') },
        { label: 'Linear', param: 'switchType', value: 'linear', match: (v) => (v.switchType ?? '').toLowerCase().includes('linear') },
        { label: 'Tactile', param: 'switchType', value: 'tactile', match: (v) => (v.switchType ?? '').toLowerCase().includes('tactile') },
      ],
    },
  ],
  generic: [],
};

@Component({
  selector: 'app-product-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './product-list-page.component.html',
  styleUrl: './product-list-page.component.scss',
})
export class ProductListPageComponent implements OnDestroy {
  private static readonly SEARCH_DEBOUNCE_MS = 300;

  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private searchDebounceHandle: ReturnType<typeof setTimeout> | null = null;
  private queryParamSubscription: Subscription | null = null;

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly addingProductId = signal<number | null>(null);

  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);

  readonly searchTerm = signal('');
  readonly selectedCategoryId = signal<number | null>(null);
  readonly selectedBrandIds = signal<number[]>([]);
  readonly selectedPriceBucket = signal<PriceBucket>('all');
  readonly selectedSort = signal<SortMode>('featured');
  readonly selectedContextFilters = signal<Record<string, string[]>>({});

  readonly priceBuckets: Array<{ id: PriceBucket; label: string }> = [
    { id: 'all', label: 'Tất cả' },
    { id: 'under20', label: 'Dưới 20 triệu' },
    { id: '20to25', label: 'Từ 20 đến 25 triệu' },
    { id: '25to30', label: 'Từ 25 đến 30 triệu' },
    { id: '30to35', label: 'Từ 30 đến 35 triệu' },
    { id: '35to40', label: 'Từ 35 đến 40 triệu' },
    { id: 'over40', label: 'Trên 40 triệu' },
  ];

  readonly sortChips: Array<{ id: SortMode; label: string }> = [
    { id: 'featured', label: 'Nổi bật' },
    { id: 'priceAsc', label: 'Giá: Tăng dần' },
    { id: 'priceDesc', label: 'Giá: Giảm dần' },
    { id: 'nameAsc', label: 'A-Z' },
    { id: 'nameDesc', label: 'Z-A' },
    { id: 'newest', label: 'Mới nhất' },
    { id: 'bestSeller', label: 'Bán chạy' },
  ];

  private readonly cardImages: string[] = [
    'https://images.unsplash.com/photo-1611078489935-0cb964de46d6?auto=format&fit=crop&w=1000&q=80',
    'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=1000&q=80',
    'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1000&q=80',
    'https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=1000&q=80',
    'https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=1000&q=80',
    'https://images.unsplash.com/photo-1591799265444-d66432b91588?auto=format&fit=crop&w=1000&q=80',
  ];

  readonly visibleBrands = computed(() => {
    const counts = new Map<number, number>();
    for (const product of this.products()) {
      counts.set(product.brandId, (counts.get(product.brandId) ?? 0) + 1);
    }

    return this.brands()
      .map((brand) => ({ ...brand, productCount: counts.get(brand.id) ?? 0 }))
      .filter((brand) => brand.productCount > 0 || this.selectedBrandIds().includes(brand.id));
  });

  readonly selectedCategory = computed(
    () => this.categories().find((category) => category.id === this.selectedCategoryId()) ?? null,
  );

  readonly activeFilterPreset = computed<CategoryPreset>(() => {
    const slug = this.selectedCategory()?.slug?.toLowerCase() ?? '';
    const name = this.selectedCategory()?.name?.toLowerCase() ?? '';
    const haystack = `${slug} ${name}`;

    if (haystack.includes('laptop')) {
      return 'laptop';
    }
    if (haystack.includes('pc')) {
      return 'pc';
    }
    if (haystack.includes('monitor') || haystack.includes('màn hình')) {
      return 'monitor';
    }
    if (
      haystack.includes('gear') ||
      haystack.includes('chuột') ||
      haystack.includes('bàn phím') ||
      haystack.includes('tai nghe')
    ) {
      return 'gear';
    }
    return 'generic';
  });

  readonly contextualFilterGroups = signal<ContextualFilterGroup[]>([]);

  readonly topTags = computed(() => {
    const names = this.visibleBrands().map((brand) => brand.name);
    return names.slice(0, 8);
  });

  readonly filteredProducts = computed(() => {
    const priceBucket = this.selectedPriceBucket();

    return this.products().filter((product) => {
      if (!this.isInPriceBucket(this.getPrimaryPrice(product), priceBucket)) {
        return false;
      }
      return true;
    });
  });

  readonly sortedProducts = computed(() => {
    const mode = this.selectedSort();
    const list = [...this.filteredProducts()];

    switch (mode) {
      case 'priceAsc':
        return list.sort((a, b) => this.getPrimaryPrice(a) - this.getPrimaryPrice(b));
      case 'priceDesc':
        return list.sort((a, b) => this.getPrimaryPrice(b) - this.getPrimaryPrice(a));
      case 'nameAsc':
        return list.sort((a, b) => a.name.localeCompare(b.name));
      case 'nameDesc':
        return list.sort((a, b) => b.name.localeCompare(a.name));
      case 'newest':
        return list.sort((a, b) => b.id - a.id);
      case 'bestSeller':
        return list.sort((a, b) => b.variants.length - a.variants.length || b.id - a.id);
      case 'featured':
      default:
        return list.sort((a, b) => a.id - b.id);
    }
  });

  constructor() {
    const initialQ = this.route.snapshot.queryParamMap.get('q')?.trim() ?? '';
    this.searchTerm.set(initialQ);

    this.reload();

    this.queryParamSubscription = this.route.queryParamMap.subscribe((params) => {
      const q = params.get('q')?.trim() ?? '';
      if (q === this.searchTerm().trim()) {
        return;
      }

      this.searchTerm.set(q);
      this.cancelPendingSearchRefresh();
      this.refreshProducts();
    });
  }

  ngOnDestroy(): void {
    this.queryParamSubscription?.unsubscribe();
    this.queryParamSubscription = null;
    this.cancelPendingSearchRefresh();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      categories: this.catalogService.getCategories(),
      brands: this.catalogService.getBrands(),
    }).subscribe({
      next: ({ categories, brands }) => {
        this.categories.set(categories);
        this.brands.set(brands);
        this.refreshProducts();
      },
      error: (err: unknown) => {
        const fallback = 'Không thể tải dữ liệu sản phẩm lúc này.';
        const message =
          typeof err === 'object' && err !== null && 'error' in err
            ? String((err as { error?: { message?: string } }).error?.message ?? fallback)
            : fallback;
        this.error.set(message);
        this.loading.set(false);
      },
    });
  }

  onSearchInput(value: string): void {
    this.searchTerm.set(value);
    this.scheduleSearchRefresh();
  }

  setCategory(categoryId: number | null): void {
    this.selectedCategoryId.set(categoryId);
    this.selectedBrandIds.set([]);
    this.selectedContextFilters.set({});
    this.refreshProducts();
  }

  toggleBrand(brandId: number, checked: boolean): void {
    const next = new Set(this.selectedBrandIds());
    if (checked) {
      next.add(brandId);
    } else {
      next.delete(brandId);
    }
    this.selectedBrandIds.set(Array.from(next));
    this.refreshProducts();
  }

  clearSelectedBrands(): void {
    this.selectedBrandIds.set([]);
    this.refreshProducts();
  }

  setPriceBucket(bucket: PriceBucket): void {
    this.selectedPriceBucket.set(bucket);
  }

  setSort(mode: SortMode): void {
    this.selectedSort.set(mode);
  }

  toggleContextFilter(groupKey: string, optionKey: string, checked: boolean): void {
    const current = this.selectedContextFilters();
    const selected = new Set(current[groupKey] ?? []);

    if (checked) {
      selected.add(optionKey);
    } else {
      selected.delete(optionKey);
    }

    this.selectedContextFilters.set({
      ...current,
      [groupKey]: Array.from(selected),
    });
    this.refreshProducts();
  }

  isContextFilterSelected(groupKey: string, optionKey: string): boolean {
    return (this.selectedContextFilters()[groupKey] ?? []).includes(optionKey);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategoryId.set(null);
    this.selectedBrandIds.set([]);
    this.selectedPriceBucket.set('all');
    this.selectedSort.set('featured');
    this.selectedContextFilters.set({});
    this.cancelPendingSearchRefresh();
    this.refreshProducts();
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

  getProductImage(index: number): string {
    return this.cardImages[index % this.cardImages.length];
  }

  formatProductPrice(value: number): string {
    return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value)} đ`;
  }

  getPrimaryPrice(product: Product): number {
    return product.variants[0]?.price ?? Number.MAX_SAFE_INTEGER;
  }

  getQuickSpecs(product: Product): string[] {
    const specs: string[] = [];
    const firstVariant = product.variants[0];
    if (firstVariant?.cpuModel) {
      specs.push(firstVariant.cpuModel);
    }
    if (firstVariant?.gpuModel) {
      specs.push(firstVariant.gpuModel);
    }
    if (firstVariant?.ramGb) {
      specs.push(`${firstVariant.ramGb}GB RAM`);
    }

    specs.push(product.brandName);
    return specs.slice(0, 3);
  }

  private isInPriceBucket(price: number, bucket: PriceBucket): boolean {
    const million = price / 1_000_000;

    switch (bucket) {
      case 'under20':
        return million < 20;
      case '20to25':
        return million >= 20 && million < 25;
      case '25to30':
        return million >= 25 && million < 30;
      case '30to35':
        return million >= 30 && million < 35;
      case '35to40':
        return million >= 35 && million < 40;
      case 'over40':
        return million >= 40;
      case 'all':
      default:
        return true;
    }
  }

  private refreshProducts(): void {
    this.cancelPendingSearchRefresh();
    this.loading.set(true);
    this.error.set(null);

    const query = this.buildBackendFilterQuery();

    forkJoin({
      products: this.catalogService.getProducts(query),
      facets: this.catalogService.getProductFacets(query),
    }).subscribe({
      next: ({ products, facets }) => {
        this.products.set(products);
        this.contextualFilterGroups.set(this.mapFacetGroups(facets));
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

  private mapFacetGroups(facets: ProductFacetGroup[]): ContextualFilterGroup[] {
    return facets
      .map((group) => ({
        key: group.key,
        title: group.title,
        options: group.options.map((option) => ({
          valueKey: this.toOptionKey(option.valueKey),
          label: option.label,
          count: option.count,
        })),
      }))
      .filter((group) => group.options.length > 0);
  }

  private buildBackendFilterQuery(): ProductFilterQuery {
    const query: ProductFilterQuery = {};

    if (this.searchTerm().trim()) {
      query.q = this.searchTerm().trim();
    }

    const category = this.selectedCategory();
    if (category?.slug) {
      query.categorySlug = category.slug;
    }

    const brandSlugs = this.selectedBrandIds()
      .map((id) => this.brands().find((brand) => brand.id === id)?.slug)
      .filter((slug): slug is string => !!slug);
    if (brandSlugs.length) {
      query.brandSlug = brandSlugs;
    }

    const selectedContext = this.selectedContextFilters();
    const presetDefs = PRESET_FILTER_DEFS[this.activeFilterPreset()];
    const optionsByGroup = new Map<string, ContextFilterOptionDef[]>();
    for (const group of presetDefs) {
      optionsByGroup.set(group.key, group.options);
    }

    for (const [groupKey, selectedKeys] of Object.entries(selectedContext)) {
      const options = optionsByGroup.get(groupKey) ?? [];
      const selectedOptions = options.filter((opt) => selectedKeys.includes(this.toOptionKey(opt.value)));

      for (const option of selectedOptions) {
        this.pushQueryValue(query, option.param, option.value);
      }
    }

    return query;
  }

  private pushQueryValue(query: ProductFilterQuery, param: ContextFilterParam, value: string | number): void {
    switch (param) {
      case 'cpu':
        query.cpu = [...(query.cpu ?? []), String(value)];
        break;
      case 'ram':
        query.ram = [...(query.ram ?? []), Number(value)];
        break;
      case 'storage':
        query.storage = [...(query.storage ?? []), Number(value)];
        break;
      case 'gpu':
        query.gpu = [...(query.gpu ?? []), String(value)];
        break;
      case 'refreshRate':
        query.refreshRate = [...(query.refreshRate ?? []), Number(value)];
        break;
      case 'panelType':
        query.panelType = [...(query.panelType ?? []), String(value)];
        break;
      case 'connectionType':
        query.connectionType = [...(query.connectionType ?? []), String(value)];
        break;
      case 'switchType':
        query.switchType = [...(query.switchType ?? []), String(value)];
        break;
      case 'sizeInch':
        query.sizeInch = [...(query.sizeInch ?? []), Number(value)];
        break;
    }
  }

  private toOptionKey(value: string | number): string {
    return String(value).toLowerCase();
  }

  private scheduleSearchRefresh(): void {
    this.cancelPendingSearchRefresh();
    this.searchDebounceHandle = setTimeout(() => {
      this.searchDebounceHandle = null;
      this.refreshProducts();
    }, ProductListPageComponent.SEARCH_DEBOUNCE_MS);
  }

  private cancelPendingSearchRefresh(): void {
    if (this.searchDebounceHandle !== null) {
      clearTimeout(this.searchDebounceHandle);
      this.searchDebounceHandle = null;
    }
  }
}
