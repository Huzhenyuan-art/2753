const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const ACCESS_TOKEN_EXPIRES_KEY = 'access_token_expires'

export interface JwtPayload {
  sub?: string
  userId?: number
  roles?: string[]
  permissions?: string[]
  tokenType?: 'access' | 'refresh'
  iat?: number
  exp?: number
}

export function parseJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    return null
  }
}

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function getAccessTokenExpires(): number {
  const val = localStorage.getItem(ACCESS_TOKEN_EXPIRES_KEY)
  return val ? parseInt(val, 10) : 0
}

export function setAccessToken(token: string, expiresIn?: number): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
  if (expiresIn) {
    const expiresAt = Date.now() + expiresIn
    localStorage.setItem(ACCESS_TOKEN_EXPIRES_KEY, String(expiresAt))
  }
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function setTokens(accessToken: string, refreshToken: string, accessTokenExpiresIn?: number): void {
  setAccessToken(accessToken, accessTokenExpiresIn)
  setRefreshToken(refreshToken)
}

export function removeAccessToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(ACCESS_TOKEN_EXPIRES_KEY)
}

export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function clearAllTokens(): void {
  removeAccessToken()
  removeRefreshToken()
}

export function isAccessTokenExpired(thresholdMs: number = 0): boolean {
  const expiresAt = getAccessTokenExpires()
  if (!expiresAt) return true
  return Date.now() + thresholdMs >= expiresAt
}

export function shouldRefreshToken(thresholdMs: number = 5 * 60 * 1000): boolean {
  const accessToken = getAccessToken()
  if (!accessToken) return false
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  return isAccessTokenExpired(thresholdMs)
}

export function getAccessTokenPayload(): JwtPayload | null {
  const token = getAccessToken()
  if (!token) return null
  return parseJwt(token)
}

export function getRefreshTokenPayload(): JwtPayload | null {
  const token = getRefreshToken()
  if (!token) return null
  return parseJwt(token)
}

export function isRefreshTokenExpired(): boolean {
  const payload = getRefreshTokenPayload()
  if (!payload || !payload.exp) return true
  return Date.now() >= payload.exp * 1000
}
