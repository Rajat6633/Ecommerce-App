import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { jwtDecode } from 'jwt-decode'
import { useQueryClient } from '@tanstack/react-query'
import {
  authApi,
  setAuthFailureHandler,
  tokenStore,
  type JwtClaims,
  type LoginRequest,
  type RegisterRequest,
  type Role,
  type UserResponse,
} from '@/lib/api'

export interface AuthState {
  user: UserResponse | null
  /** True while we attempt a silent refresh on first load. */
  initializing: boolean
  isAuthenticated: boolean
  roles: Role[]
  isAdmin: boolean
  login: (creds: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

function rolesFromToken(token: string | null): Role[] {
  if (!token) return []
  try {
    const claims = jwtDecode<JwtClaims>(token)
    return (claims.roles ?? []) as Role[]
  } catch {
    return []
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<UserResponse | null>(null)
  const [initializing, setInitializing] = useState(true)
  // Roles are derived from the in-memory access token; refresh this on changes.
  const [roles, setRoles] = useState<Role[]>(() =>
    rolesFromToken(tokenStore.getAccessToken()),
  )

  // Keep `roles` in sync with whatever the token store currently holds.
  useEffect(
    () =>
      tokenStore.subscribe(() => {
        setRoles(rolesFromToken(tokenStore.getAccessToken()))
      }),
    [],
  )

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  // When the axios refresh flow gives up, drop the whole session — including
  // cached queries, so no stale user-scoped data survives the logout.
  useEffect(() => {
    setAuthFailureHandler(() => {
      clearSession()
    })
  }, [clearSession])

  // On first mount, try to resurrect a session from a persisted refresh token.
  const didInit = useRef(false)
  useEffect(() => {
    if (didInit.current) return
    didInit.current = true

    async function bootstrap() {
      if (!tokenStore.getRefreshToken()) {
        setInitializing(false)
        return
      }
      try {
        // `me()` will trigger the 401->refresh interceptor if needed, minting
        // a fresh access token from the stored refresh token.
        const me = await authApi.me()
        setUser(me)
        setRoles(rolesFromToken(tokenStore.getAccessToken()))
      } catch {
        clearSession()
      } finally {
        setInitializing(false)
      }
    }
    void bootstrap()
  }, [clearSession])

  const login = useCallback(
    async (creds: LoginRequest) => {
      const tokens = await authApi.login(creds)
      tokenStore.set(tokens.accessToken, tokens.refreshToken)
      const me = await authApi.me()
      setUser(me)
    },
    [],
  )

  const register = useCallback(
    async (body: RegisterRequest) => {
      await authApi.register(body)
      // Backend returns the user, not tokens — log in to obtain a session.
      await login({ email: body.email, password: body.password })
    },
    [login],
  )

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken()
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch {
      // Best-effort server-side revocation; clear locally regardless.
    } finally {
      clearSession()
    }
  }, [clearSession])

  const value = useMemo<AuthState>(
    () => ({
      user,
      initializing,
      isAuthenticated: !!user,
      roles,
      isAdmin: roles.includes('ADMIN'),
      login,
      register,
      logout,
    }),
    [user, initializing, roles, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
