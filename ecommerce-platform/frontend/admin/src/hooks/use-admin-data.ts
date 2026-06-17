import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  categoriesApi,
  inventoryApi,
  productsApi,
  toApiError,
  type CreateCategoryRequest,
  type CreateProductRequest,
  type ProductSearchParams,
  type ReceiveStockRequest,
  type UpdateProductRequest,
  type UpsertStockRequest,
} from '@/lib/api'

// --- products --------------------------------------------------------------
export function useProducts(params: ProductSearchParams) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => productsApi.search(params),
    placeholderData: keepPreviousData,
  })
}

export function useProduct(id: string | undefined) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(id!),
    enabled: !!id,
  })
}

export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateProductRequest) => productsApi.create(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['products'] })
      toast.success('Product created')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useUpdateProduct(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UpdateProductRequest) => productsApi.update(id, body),
    onSuccess: (product) => {
      qc.setQueryData(['product', id], product)
      void qc.invalidateQueries({ queryKey: ['products'] })
      toast.success('Product updated')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useDeleteProduct() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => productsApi.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['products'] })
      toast.success('Product deleted')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

// --- categories ------------------------------------------------------------
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.list(),
    staleTime: 5 * 60 * 1000,
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateCategoryRequest) => categoriesApi.create(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['categories'] })
      toast.success('Category created')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

// --- inventory -------------------------------------------------------------
export function useInventory(productId: string | undefined) {
  return useQuery({
    queryKey: ['inventory', productId],
    queryFn: () => inventoryApi.get(productId!),
    enabled: !!productId,
    // A 404 means "no stock record yet" — surface as null rather than retry.
    retry: false,
  })
}

export function useUpsertStock(productId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UpsertStockRequest) =>
      inventoryApi.upsert(productId, body),
    onSuccess: (inv) => {
      qc.setQueryData(['inventory', productId], inv)
      toast.success('Stock updated')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useReceiveStock(productId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ReceiveStockRequest) =>
      inventoryApi.receive(productId, body),
    onSuccess: (inv) => {
      qc.setQueryData(['inventory', productId], inv)
      toast.success('Stock received')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}
