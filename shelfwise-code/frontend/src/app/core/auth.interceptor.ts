import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { authToken, logout } from '../store/auth.store';

const PUBLIC_URLS = ['/api/auth/login', '/api/demo/login'];

/** Adds the bearer token to API calls and sends the user to the login page when the session is no longer valid. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const token = authToken();
  const isApi = request.url.startsWith('/api/');
  const isPublic = PUBLIC_URLS.some((url) => request.url.startsWith(url));
  const authorised = token && isApi && !isPublic ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authorised).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && token && !isPublic) {
        logout();
        void router.navigate(['/login'], { queryParams: { expired: true } });
      }
      return throwError(() => error);
    }),
  );
};
