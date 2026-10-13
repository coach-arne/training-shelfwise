import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { authInterceptor } from './core/auth.interceptor';
import { registerHttpClient } from './core/http';
import { routes } from './app.routes';
import { refreshSystemInfo } from './store/system.store';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
    provideAppInitializer(() => {
      registerHttpClient(inject(HttpClient));
      void refreshSystemInfo();
    }),
  ],
};
