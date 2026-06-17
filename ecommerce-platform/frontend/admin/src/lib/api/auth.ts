import { api } from './client'
import type { AuthResponse, LoginRequest, UserResponse } from './types'

export const authApi = {
  login: (body: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', body).then((r) => r.data),

  me: () => api.get<UserResponse>('/auth/me').then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }).then((r) => r.data),
}
