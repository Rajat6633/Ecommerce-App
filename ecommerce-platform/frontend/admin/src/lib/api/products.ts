import { api } from './client'
import type {
  CreateProductRequest,
  PageResponse,
  Product,
  ProductSearchParams,
  UpdateProductRequest,
} from './types'

export const productsApi = {
  search: (params: ProductSearchParams) =>
    api
      .get<PageResponse<Product>>('/products', { params })
      .then((r) => r.data),

  getById: (id: string) =>
    api.get<Product>(`/products/${id}`).then((r) => r.data),

  create: (body: CreateProductRequest) =>
    api.post<Product>('/products', body).then((r) => r.data),

  update: (id: string, body: UpdateProductRequest) =>
    api.put<Product>(`/products/${id}`, body).then((r) => r.data),

  remove: (id: string) =>
    api.delete(`/products/${id}`).then((r) => r.data),
}
