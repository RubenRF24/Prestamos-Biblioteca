import { Routes } from '@angular/router';
import { adminGuard, authGuard, bibliotecarioGuard, usuarioGuard } from './core/auth/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalog' },
  {
    path: 'login',
    loadComponent: () => import('./domains/auth/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./domains/auth/register').then((m) => m.Register),
  },
  {
    path: 'activate',
    loadComponent: () => import('./domains/auth/activate').then((m) => m.Activate),
  },
  {
    path: 'catalog',
    canActivate: [authGuard],
    loadComponent: () => import('./domains/books/catalog').then((m) => m.Catalog),
  },
  {
    path: 'my-loans',
    canActivate: [authGuard, usuarioGuard],
    loadComponent: () => import('./domains/loans/my-loans').then((m) => m.MyLoans),
  },
  {
    path: 'active-loans',
    canActivate: [authGuard, bibliotecarioGuard],
    loadComponent: () => import('./domains/loans/active-loans').then((m) => m.ActiveLoans),
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./domains/admin/admin-dashboard').then((m) => m.AdminDashboard),
  },
  { path: '**', redirectTo: 'catalog' },
];
