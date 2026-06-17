import { api } from './client'
import type { Category, CreateCategoryRequest } from './types'

export const categoriesApi = {
  list: () => api.get<Category[]>('/categories').then((r) => r.data),

  getById: (id: string) =>
    api.get<Category>(`/categories/${id}`).then((r) => r.data),

  create: (body: CreateCategoryRequest) =>
    api.post<Category>('/categories', body).then((r) => r.data),
}
