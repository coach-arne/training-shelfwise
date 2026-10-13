import { Routes } from '@angular/router';
import { roleGuard, trainingGuard } from './core/guards';

const DESK = ['LIBRARIAN', 'BRANCH_MANAGER'] as const;
const STAFF = [...DESK, 'ADMINISTRATOR'] as const;

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalogue' },
  { path: 'login', loadComponent: () => import('./pages/login/login.page').then((m) => m.LoginPage) },
  { path: 'catalogue', loadComponent: () => import('./pages/catalogue/catalogue.page').then((m) => m.CataloguePage) },
  { path: 'pay/:reference', loadComponent: () => import('./pages/payment/payment-provider.page').then((m) => m.PaymentProviderPage) },
  {
    path: 'portal',
    canActivate: [roleGuard('MEMBER')],
    loadComponent: () => import('./pages/portal/portal.page').then((m) => m.PortalPage),
  },
  {
    path: 'staff',
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'desk' },
      {
        path: 'desk',
        canActivate: [roleGuard(...DESK)],
        loadComponent: () => import('./pages/desk/desk.page').then((m) => m.DeskPage),
      },
      {
        path: 'returns',
        canActivate: [roleGuard(...DESK)],
        loadComponent: () => import('./pages/returns/returns.page').then((m) => m.ReturnsPage),
      },
      {
        path: 'register',
        canActivate: [roleGuard(...DESK)],
        loadComponent: () => import('./pages/register/register-member.page').then((m) => m.RegisterMemberPage),
      },
      {
        path: 'titles',
        canActivate: [roleGuard(...DESK)],
        loadComponent: () => import('./pages/titles/title-maintenance.page').then((m) => m.TitleMaintenancePage),
      },
      {
        path: 'reports',
        canActivate: [roleGuard(...STAFF)],
        loadComponent: () => import('./pages/reports/reports.page').then((m) => m.ReportsPage),
      },
      {
        path: 'admin',
        canActivate: [roleGuard('ADMINISTRATOR')],
        loadComponent: () => import('./pages/admin/admin.page').then((m) => m.AdminPage),
      },
      {
        path: 'training',
        canActivate: [roleGuard('ADMINISTRATOR'), trainingGuard],
        loadComponent: () => import('./pages/training/training.page').then((m) => m.TrainingPage),
      },
    ],
  },
  { path: '**', redirectTo: 'catalogue' },
];
