import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
    provideAppInitializer(() => {
      registerHttpClient(inject(HttpClient));
      void refreshSystemInfo();
    }),
  ],
};
