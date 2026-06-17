import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { productsApi } from '@/lib/api'
import type { ProductSearchParams } from '@/lib/api'

export function useProducts(params: ProductSearchParams) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => productsApi.search(params),
    placeholderData: keepPreviousData, // smooth pagination/filtering
  })
}

export function useProduct(id: string | undefined) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(id!),
    enabled: !!id,
  })
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => productsApi.categories(),
    staleTime: 5 * 60 * 1000, // categories rarely change
  })
}
