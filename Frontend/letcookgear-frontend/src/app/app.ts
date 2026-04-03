import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { CartService } from './core/services/cart.service';
import { AuthService } from './core/services/auth.service';
import { SiteFooterComponent } from './shared/components/site-footer/site-footer.component';
import { SiteHeaderComponent } from './shared/components/site-header/site-header.component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, SiteHeaderComponent, SiteFooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly user = this.authService.currentUser;
  readonly isLoggedIn = this.authService.isAuthenticated;
  readonly cartTotalItems = this.cartService.totalItems;
  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));
  readonly showShellNav = computed(() => this.currentUrl() !== '/');

  constructor() {
    this.authService.bootstrap().subscribe({
      next: (user) => {
        if (user) {
          this.cartService.loadMyCart().subscribe({
            error: () => this.cartService.clearLocalCart(),
          });
          return;
        }
        this.cartService.clearLocalCart();
      },
      error: () => this.cartService.clearLocalCart(),
    });
  }

  logout(): void {
    this.authService.logout();
    this.cartService.clearLocalCart();
  }
}
