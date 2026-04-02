import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { LoginPageComponent } from './pages/login/login-page.component';
import { ProfilePageComponent } from './pages/profile/profile-page.component';
import { RegisterPageComponent } from './pages/register/register-page.component';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  {
    path: 'register',
    component: RegisterPageComponent,
  },
  {
    path: 'me',
    component: ProfilePageComponent,
    canActivate: [authGuard],
  },
];
