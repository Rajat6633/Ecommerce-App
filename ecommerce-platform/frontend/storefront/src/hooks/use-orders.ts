import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi, type Order, type OrderStatus } from '@/lib/api'

/** Statuses where the saga has come to rest — stop polling here. */
const TERMINAL_STATUSES: OrderStatus[] = [
  'CONFIRMED',
  'REJECTED',
  'PAYMENT_FAILED',
  'CANCELLED',
]

export function isTerminal(status: OrderStatus) {
  return TERMINAL_STATUSES.includes(status)
}

export function useOrders() {
  return useQuery({
    queryKey: ['orders'],
    queryFn: () => ordersApi.list(),
  })
}

/**
 * Track a single order. Orders resolve asynchronously via the Kafka saga, so
 * we POLL until the status reaches a terminal state, then back off.
 */
export function useOrder(id: string | undefined) {
  return useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(id!),
    enabled: !!id,
    refetchInterval: (query) => {
      const order = query.state.data as Order | undefined
      if (!order) return 2000
      return isTerminal(order.status) ? false : 2000
    },
  })
}

export function usePlaceOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => ordersApi.place(),
    onSuccess: (order) => {
      qc.setQueryData(['order', order.id], order)
      void qc.invalidateQueries({ queryKey: ['orders'] })
      void qc.invalidateQueries({ queryKey: ['cart'] }) // server clears it
    },
  })
}
