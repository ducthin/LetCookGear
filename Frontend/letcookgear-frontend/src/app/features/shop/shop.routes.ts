import { Routes } from '@angular/router';

import { adminGuard } from '../../core/guards/admin.guard';
import { authGuard } from '../../core/guards/auth.guard';
import { AdminOrdersPageComponent } from './pages/admin-orders/admin-orders-page.component';
import { CartPageComponent } from './pages/cart/cart-page.component';
import { CheckoutPageComponent } from './pages/checkout/checkout-page.component';
import { OrderDetailPageComponent } from './pages/order-detail/order-detail-page.component';
import { OrdersPageComponent } from './pages/orders/orders-page.component';
import { ProductDetailPageComponent } from './pages/product-detail/product-detail-page.component';
import { ProductListPageComponent } from './pages/product-list/product-list-page.component';

export const SHOP_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'products',
  },
  {
    path: 'products',
    component: ProductListPageComponent,
  },
  {
    path: 'products/:slug',
    component: ProductDetailPageComponent,
  },
  {
    path: 'cart',
    component: CartPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'checkout',
    component: CheckoutPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'orders',
    component: OrdersPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'orders/:orderId',
    component: OrderDetailPageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'admin/orders',
    component: AdminOrdersPageComponent,
    canActivate: [authGuard, adminGuard],
  },
];
