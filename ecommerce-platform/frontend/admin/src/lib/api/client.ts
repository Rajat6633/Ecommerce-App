import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios'
import { tokenStore } from './token-store'
import type { ApiError, AuthResponse } from './types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** Main client — carries the Authorization header and auto-refreshes on 401. */
export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * A SEPARATE bare client for the refresh call itself. Using `api` would recurse
 * through the response interceptor (a failed refresh would try to refresh).
 */
const refreshClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// --- request: attach bearer token -----------------------------------------
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// --- response: single-flight refresh on 401 --------------------------------
// When several requests 401 at once we must refresh ONCE and replay them all,
// not fire N concurrent refreshes (which would rotate the token N times and
// invalidate each other). `refreshPromise` is the single-flight latch.
let refreshPromise: Promise<string> | null = null

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/** Callback invoked when refresh definitively fails (session is dead). */
let onAuthFailure: (() => void) | null = null
export function setAuthFailureHandler(handler: () => void) {
  onAuthFailure = handler
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.getRefreshToken()
  if (!refreshToken) throw new Error('No refresh token')

  const { data } = await refreshClient.post<AuthResponse>('/auth/refresh', {
    refreshToken,
  })
  tokenStore.set(data.accessToken, data.refreshToken)
  return data.accessToken
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined
    const status = error.response?.status

    const isAuthRoute = original?.url?.includes('/auth/')
    const canRetry =
      status === 401 &&
      original &&
      !original._retried &&
      !isAuthRoute &&
      tokenStore.getRefreshToken()

    if (canRetry) {
      original._retried = true
      try {
        refreshPromise = refreshPromise ?? refreshAccessToken()
        const newToken = await refreshPromise
        original.headers.set('Authorization', `Bearer ${newToken}`)
        return api(original)
      } catch (refreshErr) {
        tokenStore.clear()
        onAuthFailure?.()
        return Promise.reject(refreshErr)
      } finally {
        refreshPromise = null
      }
    }

    return Promise.reject(error)
  },
)

/** Normalise any axios error into our ApiError envelope for the UI. */
export function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as Partial<ApiError> | undefined
    return {
      status: err.response?.status ?? 0,
      message:
        data?.message ||
        err.message ||
        'Something went wrong. Please try again.',
      fieldErrors: data?.fieldErrors,
    }
  }
  return { status: 0, message: 'Unexpected error. Please try again.' }
}
