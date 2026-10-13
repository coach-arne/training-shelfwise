import { apiGet, apiPost } from '../http';
import { DemoAccount, LoginResult, SystemInfo, UserProfile } from '../models';

export const authApi = {
  login: (username: string, password: string) => apiPost<LoginResult>('/api/auth/login', { username, password }),
  me: () => apiGet<UserProfile>('/api/auth/me'),
  systemInfo: () => apiGet<SystemInfo>('/api/system/info'),
  demoAccounts: () => apiGet<DemoAccount[]>('/api/demo/accounts'),
  demoLogin: (type: string, id: number) => apiPost<LoginResult>('/api/demo/login', { type, id }),
};
