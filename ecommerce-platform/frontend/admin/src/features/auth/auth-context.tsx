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
  type Role,
  type UserResponse,
} from '@/lib/api'

/** Thrown by login() when valid credentials belong to a non-admin user. */
export class NotAuthorizedError extends Error {
  constructor() {
    super('This account does not have admin access.')
    this.name = 'NotAuthorizedError'
  }
}

export interface AuthState {
  user: UserResponse | null
  initializing: boolean
  isAuthenticated: boolean
  roles: Role[]
  login: (creds: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

function rolesFromToken(token: string | null): Role[] {
  if (!token) return []
  try {
    return (jwtDecode<JwtClaims>(token).roles ?? []) as Role[]
  } catch {
    return []
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<UserResponse | null>(null)
  const [initializing, setInitializing] = useState(true)
  const [roles, setRoles] = useState<Role[]>(() =>
    rolesFromToken(tokenStore.getAccessToken()),
  )

  useEffect(
    () =>
      tokenStore.subscribe(() =>
        setRoles(rolesFromToken(tokenStore.getAccessToken())),
      ),
    [],
  )

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setAuthFailureHandler(() => setUser(null))
  }, [])

  // Resurrect an admin session from a persisted refresh token on first load.
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
        const me = await authApi.me()
        if (!me.roles.includes('ADMIN')) {
          clearSession()
        } else {
          setUser(me)
          setRoles(rolesFromToken(tokenStore.getAccessToken()))
        }
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
      if (!me.roles.includes('ADMIN')) {
        // Valid credentials, but not an admin — refuse and tear down.
        clearSession()
        throw new NotAuthorizedError()
      }
      setUser(me)
    },
    [clearSession],
  )

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken()
    try {
      if (refreshToken) await authApi.logout(refreshToken)
    } catch {
      // best-effort
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
      login,
      logout,
    }),
    [user, initializing, roles, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
