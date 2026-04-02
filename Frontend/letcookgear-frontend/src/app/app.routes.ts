import { Routes } from '@angular/router';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		redirectTo: 'shop/products',
	},
	{
		path: 'shop',
		loadChildren: () => import('./features/shop/shop.routes').then((m) => m.SHOP_ROUTES),
	},
	{
		path: 'auth',
		loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
	},
	{
		path: '**',
		redirectTo: 'shop/products',
	},
];
