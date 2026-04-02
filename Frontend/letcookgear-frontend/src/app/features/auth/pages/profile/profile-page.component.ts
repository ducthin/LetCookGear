import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';

import { AuthUser } from '../../../../core/models/auth.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export class ProfilePageComponent {
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly profile = signal<AuthUser | null>(null);

  constructor(private readonly authService: AuthService) {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);

    this.authService.me().subscribe({
      next: (user) => this.profile.set(user),
      error: () => this.error.set('Không thể tải hồ sơ. Vui lòng đăng nhập lại.'),
      complete: () => this.loading.set(false),
    });
  }
}
