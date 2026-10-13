import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from './models';
import { homeRoute, isLoggedIn, role } from '../store/auth.store';
import { refreshSystemInfo, systemInfo } from '../store/system.store';

/** Allows the route for the given roles only. Anonymous visitors are sent to the login page. */
export function roleGuard(...allowed: Role[]): CanActivateFn {
  return () => {
    const router = inject(Router);
    if (!isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }
    const current = role();
    return current && allowed.includes(current) ? true : router.createUrlTree([homeRoute(current)]);
  };
}

/** The training screens only exist in training mode (section 15). */
export const trainingGuard: CanActivateFn = async () => {
  const router = inject(Router);
  await refreshSystemInfo();
  return systemInfo()?.trainingMode ? true : router.createUrlTree(['/staff/admin']);
};
