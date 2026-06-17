import { api } from './client'
import type {
  Category,
  PageResponse,
  Product,
  ProductSearchParams,
} from './types'

export const productsApi = {
  search: (params: ProductSearchParams) =>
    api
      .get<PageResponse<Product>>('/products', { params })
      .then((r) => r.data),

  getById: (id: string) =>
    api.get<Product>(`/products/${id}`).then((r) => r.data),

  categories: () =>
    api.get<Category[]>('/categories').then((r) => r.data),
}
