// ---------------------------------------------------------------------------
// Token storage strategy
//
// The backend issues a short-lived access token (~15 min) and a longer-lived
// refresh token (~7 days) as plain strings — there is no httpOnly cookie. So:
//   - accessToken  : kept in MEMORY only (not persisted) — minimises XSS blast
//                    radius; it's cheap to re-mint from the refresh token.
//   - refreshToken : persisted to localStorage so a page reload can silently
//                    re-establish a session. This is the pragmatic SPA trade-off.
//
// A tiny subscribe() lets the AuthContext react to token changes (e.g. when the
// refresh interceptor rotates tokens or a 401 forces a logout).
// ---------------------------------------------------------------------------

const REFRESH_KEY = 'ecom.refreshToken'

let accessToken: string | null = null
type Listener = () => void
const listeners = new Set<Listener>()

function emit() {
  listeners.forEach((l) => l())
}

export const tokenStore = {
  getAccessToken(): string | null {
    return accessToken
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY)
  },

  /** Persist a freshly-issued token pair. */
  set(accessTokenValue: string, refreshToken: string) {
    accessToken = accessTokenValue
    localStorage.setItem(REFRESH_KEY, refreshToken)
    emit()
  },

  /** Update just the access token (refresh response may rotate both — prefer set). */
  setAccessToken(value: string | null) {
    accessToken = value
    emit()
  },

  clear() {
    accessToken = null
    localStorage.removeItem(REFRESH_KEY)
    emit()
  },

  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
