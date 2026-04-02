import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { CartService } from './core/services/cart.service';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);

  readonly user = this.authService.currentUser;
  readonly isLoggedIn = this.authService.isAuthenticated;
  readonly cartTotalItems = this.cartService.totalItems;
  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));

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
