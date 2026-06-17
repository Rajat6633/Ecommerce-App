import { api } from './client'
import type { Order, OrderStatusResponse } from './types'

export const ordersApi = {
  /** Place an order from the current cart. Returns immediately as PENDING. */
  place: () => api.post<Order>('/orders', {}).then((r) => r.data),

  list: () => api.get<Order[]>('/orders').then((r) => r.data),

  getById: (id: string) =>
    api.get<Order>(`/orders/${id}`).then((r) => r.data),

  /** Lightweight status poll used by the order-tracking timeline. */
  getStatus: (id: string) =>
    api
      .get<OrderStatusResponse>(`/orders/${id}/status`)
      .then((r) => r.data),
}
