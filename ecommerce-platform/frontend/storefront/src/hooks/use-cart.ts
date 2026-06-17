import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { toast } from 'sonner'
import { cartApi, toApiError, type Cart } from '@/lib/api'
import { useAuth } from '@/features/auth/use-auth'

const CART_KEY = ['cart'] as const

export function useCart() {
  const { isAuthenticated } = useAuth()
  return useQuery({
    queryKey: CART_KEY,
    queryFn: () => cartApi.get(),
    enabled: isAuthenticated, // cart is per-user; skip when logged out
  })
}

/** Total item count across all lines — drives the header badge. */
export function useCartCount() {
  const { data } = useCart()
  return data?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0
}

export function useAddToCart() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { productId: string; quantity?: number }) =>
      cartApi.addItem({
        productId: vars.productId,
        quantity: vars.quantity ?? 1,
      }),
    onSuccess: (cart) => {
      qc.setQueryData(CART_KEY, cart)
      toast.success('Added to cart')
    },
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useUpdateCartQuantity() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { productId: string; quantity: number }) =>
      cartApi.updateQuantity(vars.productId, vars.quantity),
    onSuccess: (cart) => qc.setQueryData(CART_KEY, cart),
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useRemoveCartItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (productId: string) => cartApi.removeItem(productId),
    onSuccess: (cart) => qc.setQueryData(CART_KEY, cart),
    onError: (err) => toast.error(toApiError(err).message),
  })
}

export function useClearCart() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => cartApi.clear(),
    onSuccess: () => {
      qc.setQueryData<Cart | undefined>(CART_KEY, (prev) =>
        prev
          ? { ...prev, items: [], totalAmount: '0' }
          : prev,
      )
      void qc.invalidateQueries({ queryKey: CART_KEY })
    },
  })
}
