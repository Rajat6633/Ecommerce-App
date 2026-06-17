import type { OrderStatus } from '@/lib/api'
import { Badge } from '@/components/ui/badge'

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pending',
  INVENTORY_RESERVED: 'Inventory reserved',
  PAID: 'Paid',
  CONFIRMED: 'Confirmed',
  REJECTED: 'Rejected',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
}

type Variant = 'default' | 'secondary' | 'destructive' | 'success' | 'outline'

const VARIANTS: Record<OrderStatus, Variant> = {
  PENDING: 'secondary',
  INVENTORY_RESERVED: 'secondary',
  PAID: 'default',
  CONFIRMED: 'success',
  REJECTED: 'destructive',
  PAYMENT_FAILED: 'destructive',
  CANCELLED: 'destructive',
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <Badge variant={VARIANTS[status]}>{LABELS[status]}</Badge>
}

export { LABELS as ORDER_STATUS_LABELS }
