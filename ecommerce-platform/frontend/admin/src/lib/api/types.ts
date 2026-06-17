// ---------------------------------------------------------------------------
// API contract types for the ADMIN console — mirror backend DTOs.
// Money amounts arrive/leave as strings (BigDecimal) to avoid float drift.
// ---------------------------------------------------------------------------

export type Role = 'CUSTOMER' | 'ADMIN'

// --- auth-service ----------------------------------------------------------
export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface UserResponse {
  id: string
  email: string
  fullName: string
  roles: Role[]
}

export interface JwtClaims {
  sub: string
  roles?: Role[] | string[]
  exp: number
  iat: number
}

// --- product-service -------------------------------------------------------
export interface Product {
  id: string
  sku: string
  name: string
  description: string
  price: string
  currency: string
  categoryId: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: string
  name: string
  parentId: string | null
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProductSearchParams {
  name?: string
  categoryId?: string
  activeOnly?: boolean
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
}

export interface CreateProductRequest {
  sku: string
  name: string
  description: string
  price: string
  currency: string
  categoryId: string
}

export interface UpdateProductRequest {
  name: string
  description: string
  price: string
  currency: string
  categoryId: string
  active: boolean
}

export interface CreateCategoryRequest {
  name: string
  parentId: string | null
}

// --- inventory-service -----------------------------------------------------
export interface Inventory {
  productId: string
  onHand: number
  reserved: number
  available: number
  reorderLevel: number
}

export interface UpsertStockRequest {
  onHand: number
  reorderLevel: number
}

export interface ReceiveStockRequest {
  quantity: number
}

// --- shared error envelope -------------------------------------------------
export interface ApiError {
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
