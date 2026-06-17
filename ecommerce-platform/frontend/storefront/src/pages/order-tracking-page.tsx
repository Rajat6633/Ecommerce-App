import { Link, useParams } from 'react-router-dom'
import {
  CheckCircle2,
  ChevronLeft,
  Loader2,
  XCircle,
} from 'lucide-react'
import { formatDate, formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { OrderStatusBadge } from '@/components/order/order-status-badge'
import { OrderStatusTimeline } from '@/components/order/order-status-timeline'
import { isTerminal, useOrder } from '@/hooks/use-orders'
import { productLabel, useProductMap } from '@/hooks/use-product-map'

export function OrderTrackingPage() {
  const { id } = useParams<{ id: string }>()
  const { data: order, isLoading, isError } = useOrder(id)
  const { map: productMap } = useProductMap(
    order?.items.map((i) => i.productId) ?? [],
  )

  if (isLoading) {
    return (
      <div className="container flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (isError || !order) {
    return (
      <div className="container py-20 text-center">
        <p className="text-muted-foreground">Order not found.</p>
        <Button asChild variant="link">
          <Link to="/orders">Back to orders</Link>
        </Button>
      </div>
    )
  }

  const terminal = isTerminal(order.status)
  const succeeded = order.status === 'CONFIRMED'
  const failed = terminal && !succeeded

  return (
    <div className="container max-w-3xl py-10">
      <Button asChild variant="ghost" size="sm" className="mb-6">
        <Link to="/orders">
          <ChevronLeft /> All orders
        </Link>
      </Button>

      {/* Outcome banner */}
      {succeeded && (
        <div className="mb-6 flex items-center gap-3 rounded-lg border border-success/30 bg-success/10 p-4">
          <CheckCircle2 className="h-6 w-6 text-success" />
          <div>
            <p className="font-medium">Your order is confirmed!</p>
            <p className="text-sm text-muted-foreground">
              Thanks for shopping with us.
            </p>
          </div>
        </div>
      )}
      {failed && (
        <div className="mb-6 flex items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/10 p-4">
          <XCircle className="h-6 w-6 text-destructive" />
          <div>
            <p className="font-medium">We couldn’t complete this order.</p>
            <p className="text-sm text-muted-foreground">
              No charge was made. Please try again.
            </p>
          </div>
        </div>
      )}

      <div className="grid gap-6 md:grid-cols-[1fr_320px]">
        {/* Live timeline */}
        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle className="text-xl">Order status</CardTitle>
            {!terminal && (
              <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Loader2 className="h-3 w-3 animate-spin" />
                Live
              </span>
            )}
          </CardHeader>
          <CardContent>
            <OrderStatusTimeline status={order.status} isTerminal={terminal} />
          </CardContent>
        </Card>

        {/* Order details */}
        <Card className="h-fit">
          <CardHeader>
            <CardTitle className="text-base">Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Order</span>
              <span className="font-mono">#{order.id.slice(0, 8)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Status</span>
              <OrderStatusBadge status={order.status} />
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Placed</span>
              <span>{formatDate(order.createdAt)}</span>
            </div>
            <div className="border-t pt-3">
              <ul className="space-y-2">
                {order.items.map((item) => (
                  <li key={item.productId} className="flex justify-between">
                    <span className="text-muted-foreground">
                      {productLabel(productMap, item.productId)} × {item.quantity}
                    </span>
                    <span>{formatMoney(item.lineTotal, order.currency)}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="flex justify-between border-t pt-3 text-base font-semibold">
              <span>Total</span>
              <span>{formatMoney(order.totalAmount, order.currency)}</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
