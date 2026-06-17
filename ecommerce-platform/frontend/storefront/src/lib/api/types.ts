// ---------------------------------------------------------------------------
// API contract types — mirror the backend DTOs (see docs/10-16-*.md).
// Money amounts arrive as JSON strings (BigDecimal); we keep them as strings
// and parse only for display (formatMoney) to avoid float rounding drift.
// ---------------------------------------------------------------------------

export type Role = 'CUSTOMER' | 'ADMIN'

// --- auth-service ----------------------------------------------------------
export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string // "Bearer"
  expiresIn: number // seconds
}

export interface UserResponse {
  id: string
  email: string
  fullName: string
  roles: Role[]
}

/** Shape of our RS256 JWT claims (decoded client-side for quick role checks). */
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
  minPrice?: number
  maxPrice?: number
  activeOnly?: boolean
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
}

// --- cart-service ----------------------------------------------------------
export interface CartItem {
  productId: string
  quantity: number
  unitPrice: string
  lineTotal: string
}

export interface Cart {
  userId: string
  items: CartItem[]
  totalAmount: string
  updatedAt: string
}

export interface AddItemRequest {
  productId: string
  quantity: number
}

// --- order-service ---------------------------------------------------------
export type OrderStatus =
  | 'PENDING'
  | 'INVENTORY_RESERVED'
  | 'PAID'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'PAYMENT_FAILED'
  | 'CANCELLED'

export interface OrderItem {
  productId: string
  quantity: number
  unitPrice: string
  lineTotal: string
}

export interface Order {
  id: string
  userId: string
  status: OrderStatus
  totalAmount: string
  currency: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface OrderStatusResponse {
  orderId: string
  status: OrderStatus
}

// --- shared error envelope -------------------------------------------------
export interface ApiError {
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
