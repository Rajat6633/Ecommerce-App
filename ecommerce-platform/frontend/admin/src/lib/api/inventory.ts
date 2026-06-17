import { api } from './client'
import type {
  Inventory,
  ReceiveStockRequest,
  UpsertStockRequest,
} from './types'

export const inventoryApi = {
  get: (productId: string) =>
    api.get<Inventory>(`/inventory/${productId}`).then((r) => r.data),

  upsert: (productId: string, body: UpsertStockRequest) =>
    api.put<Inventory>(`/inventory/${productId}`, body).then((r) => r.data),

  receive: (productId: string, body: ReceiveStockRequest) =>
    api
      .post<Inventory>(`/inventory/${productId}/receive`, body)
      .then((r) => r.data),
}
