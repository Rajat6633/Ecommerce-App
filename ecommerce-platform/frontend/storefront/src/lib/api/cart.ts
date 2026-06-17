import { api } from './client'
import type { AddItemRequest, Cart } from './types'

export const cartApi = {
  get: () => api.get<Cart>('/cart').then((r) => r.data),

  addItem: (body: AddItemRequest) =>
    api.post<Cart>('/cart/items', body).then((r) => r.data),

  updateQuantity: (productId: string, quantity: number) =>
    api
      .put<Cart>(`/cart/items/${productId}`, { quantity })
      .then((r) => r.data),

  removeItem: (productId: string) =>
    api.delete<Cart>(`/cart/items/${productId}`).then((r) => r.data),

  clear: () => api.delete('/cart').then((r) => r.data),
}
