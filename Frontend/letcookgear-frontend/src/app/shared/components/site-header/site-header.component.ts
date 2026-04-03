import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Input, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { CartItem } from '../../../core/models/cart.model';
import { RealtimeEvent } from '../../../core/models/realtime-event.model';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { RealtimeService } from '../../../core/services/realtime.service';

interface MegaMenuColumn {
  title: string;
  items: string[];
}

interface MegaMenuSection {
  id: string;
  label: string;
  icon: string;
  link: string;
  quickLinks?: string[];
  columns?: MegaMenuColumn[];
}

@Component({
  selector: 'app-site-header',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './site-header.component.html',
  styleUrl: './site-header.component.scss',
})
export class SiteHeaderComponent implements OnDestroy {
  private readonly hostRef = inject(ElementRef<HTMLElement>);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly realtimeService = inject(RealtimeService);
  private readonly router = inject(Router);

  private realtimeSubscription: Subscription | null = null;
  private paymentToastHandle: ReturnType<typeof setTimeout> | null = null;
  readonly itemActionState = signal<Record<number, boolean>>({});

  @Input() brandName = 'LetCookGear';

  readonly accountMenuOpen = signal(false);
  readonly cartPopupOpen = signal(false);
  readonly paymentToast = signal<string | null>(null);
  readonly isLoggedIn = this.authService.isAuthenticated;
  readonly currentUser = this.authService.currentUser;
  readonly cartData = this.cartService.cart;
  readonly cartTotalItems = this.cartService.totalItems;
  readonly cartCountLabel = computed(() => {
    const total = this.cartTotalItems();
    return total > 99 ? '99+' : String(total);
  });
  readonly activeMegaSection = signal<string | null>(null);
  readonly searchKeyword = signal('');

  constructor() {
    effect(() => {
      const loggedIn = this.isLoggedIn();

      if (!loggedIn) {
        this.disconnectRealtime();
        this.clearPaymentTracking();
        this.itemActionState.set({});
        this.closeCartPopup();
        return;
      }

      this.refreshCart();
      this.connectRealtime();
    });
  }

  readonly megaSections: MegaMenuSection[] = [
    {
      id: 'laptop',
      label: 'Laptop',
      icon: 'laptop',
      link: '/shop/products',
      quickLinks: ['Laptop Gaming', 'Laptop AI', 'Laptop Hi-End', 'Laptop Sinh viên'],
      columns: [
        { title: 'Thương hiệu', items: ['MSI', 'LENOVO', 'ASUS | ROG', 'GIGABYTE | AORUS', 'ACER | PREDATOR'] },
        { title: 'Giá bán', items: ['Dưới 20 triệu', '20 - 30 triệu', '30 - 40 triệu', '40 - 50 triệu', 'Trên 50 triệu'] },
        { title: 'Card đồ họa', items: ['RTX 3000 Series', 'RTX 4000 Series', 'RTX 5000 Series'] },
        { title: 'CPU Intel - AMD', items: ['Core i5', 'Core i7', 'Core i9', 'Ultra 7', 'Ryzen 7', 'Ryzen 9'] },
      ],
    },
    {
      id: 'handheld',
      label: 'Handheld / Console',
      icon: 'console',
      link: '/shop/products',
    },
    {
      id: 'pc',
      label: 'PC',
      icon: 'pc',
      link: '/shop/products',
      quickLinks: ['PC Gaming', 'PC AI', 'PC Văn phòng', 'Tự chọn cấu hình'],
      columns: [
        { title: 'PC theo nhu cầu', items: ['PC gaming', 'PC văn phòng', 'PC đồ họa', 'PC AI'] },
        { title: 'PC theo giá', items: ['Dưới 10 triệu', '10 - 15 triệu', '15 - 25 triệu', '25 - 35 triệu', 'Trên 35 triệu'] },
        { title: 'PC CPU Intel', items: ['Core i3', 'Core i5', 'Core i7', 'Core i9'] },
        { title: 'PC CPU AMD', items: ['Ryzen 3', 'Ryzen 5', 'Ryzen 7', 'Ryzen 9'] },
      ],
    },
    { id: 'monitor', label: 'Màn hình', icon: 'monitor', link: '/shop/products' },
    { id: 'component', label: 'Linh kiện', icon: 'chip', link: '/shop/products' },
    { id: 'gear', label: 'Gaming Gear', icon: 'keyboard', link: '/shop/products' },
    { id: 'build', label: 'Tự chọn cấu hình', icon: 'setting', link: '/shop/products' },
    { id: 'software', label: 'Phần mềm', icon: 'grid', link: '/shop/products' },
    { id: 'tablet', label: 'Tablet', icon: 'tablet', link: '/shop/products' },
    { id: 'service', label: 'Dịch vụ', icon: 'service', link: '/shop/products' },
  ];

  toggleAccountMenu(): void {
    this.accountMenuOpen.update((value) => !value);
  }

  closeAccountMenu(): void {
    this.accountMenuOpen.set(false);
  }

  closeCartPopup(): void {
    this.cartPopupOpen.set(false);
  }

  openMega(sectionId: string): void {
    this.activeMegaSection.set(sectionId);
  }

  closeMega(): void {
    this.activeMegaSection.set(null);
  }

  hasMega(section: MegaMenuSection): boolean {
    return (section.columns?.length ?? 0) > 0;
  }

  getActiveMegaColumns(): MegaMenuColumn[] {
    const active = this.megaSections.find((section) => section.id === this.activeMegaSection());
    return active?.columns ?? [];
  }

  getActiveMegaSection(): MegaMenuSection | null {
    return this.megaSections.find((section) => section.id === this.activeMegaSection()) ?? null;
  }

  logout(): void {
    this.authService.logout();
    this.cartService.clearLocalCart();
    this.closeCartPopup();
    this.closeAccountMenu();
    this.router.navigateByUrl('/');
  }

  openCart(): void {
    this.closeMega();
    this.closeAccountMenu();

    if (!this.isLoggedIn()) {
      this.router.navigate(['/auth/login'], {
        queryParams: {
          returnUrl: '/shop/cart',
        },
      });
      return;
    }

    const nextOpen = !this.cartPopupOpen();
    this.cartPopupOpen.set(nextOpen);
    if (nextOpen) {
      this.refreshCart();
    }
  }

  goToCart(): void {
    this.closeCartPopup();
    this.router.navigateByUrl('/shop/cart');
  }

  goToCheckout(): void {
    this.closeCartPopup();
    this.router.navigateByUrl('/shop/checkout');
  }

  trackByCartItemId(_: number, item: CartItem): number {
    return item.itemId;
  }

  isItemActionPending(itemId: number): boolean {
    return this.itemActionState()[itemId] === true;
  }

  increaseItemQuantity(item: CartItem): void {
    this.changeItemQuantity(item, item.quantity + 1);
  }

  decreaseItemQuantity(item: CartItem): void {
    if (item.quantity <= 1) {
      this.removeCartItem(item.itemId);
      return;
    }

    this.changeItemQuantity(item, item.quantity - 1);
  }

  onQuantityInputChange(item: CartItem, rawValue: string): void {
    const nextQuantity = Number.parseInt(rawValue, 10);

    if (!Number.isFinite(nextQuantity)) {
      return;
    }

    if (nextQuantity < 1) {
      this.removeCartItem(item.itemId);
      return;
    }

    this.changeItemQuantity(item, nextQuantity);
  }

  removeCartItem(itemId: number): void {
    if (this.isItemActionPending(itemId)) {
      return;
    }

    this.setItemActionPending(itemId, true);
    this.cartService.removeItem(itemId).subscribe({
      error: () => this.setItemActionPending(itemId, false),
      complete: () => this.setItemActionPending(itemId, false),
    });
  }

  resolveCartItemImage(item: CartItem): string {
    if (item.imageUrl) {
      return item.imageUrl;
    }

    const seed = encodeURIComponent(item.productSlug || String(item.itemId));
    return `https://picsum.photos/seed/lcg-${seed}/120/120`;
  }

  submitSearch(): void {
    const q = this.searchKeyword().trim();
    this.closeMega();
    this.closeCartPopup();

    this.router.navigate(['/shop/products'], {
      queryParams: q ? { q } : {},
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.hostRef.nativeElement.contains(event.target as Node)) {
      this.accountMenuOpen.set(false);
      this.closeCartPopup();
      this.closeMega();
    }
  }

  ngOnDestroy(): void {
    this.disconnectRealtime();
    if (this.paymentToastHandle !== null) {
      clearTimeout(this.paymentToastHandle);
      this.paymentToastHandle = null;
    }
  }

  private connectRealtime(): void {
    if (this.realtimeSubscription) {
      return;
    }

    this.realtimeService.connect();
    this.realtimeSubscription = this.realtimeService.events$.subscribe((event) => this.handleRealtimeEvent(event));
  }

  private disconnectRealtime(): void {
    if (this.realtimeSubscription) {
      this.realtimeSubscription.unsubscribe();
      this.realtimeSubscription = null;
    }

    this.realtimeService.disconnect();
  }

  private clearPaymentTracking(): void {
    this.paymentToast.set(null);

    if (this.paymentToastHandle !== null) {
      clearTimeout(this.paymentToastHandle);
      this.paymentToastHandle = null;
    }
  }

  private refreshCart(): void {
    this.cartService.loadMyCart().subscribe({
      error: () => this.cartService.clearLocalCart(),
    });
  }

  private handleRealtimeEvent(event: RealtimeEvent): void {
    if (!this.isEventForCurrentUser(event)) {
      return;
    }

    if (event.type === 'CART_UPDATED') {
      this.refreshCart();
      return;
    }

    if (event.type === 'PAYMENT_PAID') {
      const fallback = event.orderCode
        ? `Don ${event.orderCode} da thanh toan thanh cong.`
        : 'Thanh toan PayOS thanh cong.';
      this.showPaymentToast(event.message ?? fallback);
    }
  }

  private isEventForCurrentUser(event: RealtimeEvent): boolean {
    const currentEmail = this.currentUser()?.email?.toLowerCase();
    if (!currentEmail) {
      return false;
    }

    if (!event.userEmail) {
      return true;
    }

    return event.userEmail.toLowerCase() === currentEmail;
  }

  private changeItemQuantity(item: CartItem, nextQuantity: number): void {
    if (nextQuantity < 1 || nextQuantity === item.quantity || this.isItemActionPending(item.itemId)) {
      return;
    }

    this.setItemActionPending(item.itemId, true);
    this.cartService.updateItem(item.itemId, nextQuantity).subscribe({
      error: () => this.setItemActionPending(item.itemId, false),
      complete: () => this.setItemActionPending(item.itemId, false),
    });
  }

  private setItemActionPending(itemId: number, pending: boolean): void {
    this.itemActionState.update((current) => {
      const next = { ...current };

      if (pending) {
        next[itemId] = true;
      } else {
        delete next[itemId];
      }

      return next;
    });
  }

  private showPaymentToast(message: string): void {
    this.paymentToast.set(message);

    if (this.paymentToastHandle !== null) {
      clearTimeout(this.paymentToastHandle);
    }

    this.paymentToastHandle = setTimeout(() => {
      this.paymentToast.set(null);
      this.paymentToastHandle = null;
    }, 6000);
  }
}
