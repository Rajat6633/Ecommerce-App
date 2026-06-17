import { api } from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from './types'

export const authApi = {
  register: (body: RegisterRequest) =>
    api.post<UserResponse>('/auth/register', body).then((r) => r.data),

  login: (body: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', body).then((r) => r.data),

  me: () => api.get<UserResponse>('/auth/me').then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }).then((r) => r.data),
}
