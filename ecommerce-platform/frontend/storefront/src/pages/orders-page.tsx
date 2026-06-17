import { Link } from 'react-router-dom'
import { ChevronRight, Loader2, Package } from 'lucide-react'
import { formatDate, formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { OrderStatusBadge } from '@/components/order/order-status-badge'
import { useOrders } from '@/hooks/use-orders'

export function OrdersPage() {
  const { data: orders, isLoading, isError } = useOrders()

  if (isLoading) {
    return (
      <div className="container flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="container py-20 text-center text-muted-foreground">
        Couldn’t load your orders. Please try again.
      </div>
    )
  }

  if (!orders || orders.length === 0) {
    return (
      <div className="container flex min-h-[50vh] flex-col items-center justify-center gap-4 text-center">
        <Package className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">No orders yet</h1>
        <p className="text-muted-foreground">
          When you place an order, it’ll show up here.
        </p>
        <Button asChild>
          <Link to="/products">Start shopping</Link>
        </Button>
      </div>
    )
  }

  // Newest first.
  const sorted = [...orders].sort(
    (a, b) => b.createdAt.localeCompare(a.createdAt),
  )

  return (
    <div className="container max-w-3xl py-10">
      <h1 className="mb-6 text-3xl font-bold tracking-tight">Your orders</h1>
      <div className="space-y-3">
        {sorted.map((order) => (
          <Link key={order.id} to={`/orders/${order.id}`} className="block">
            <Card className="transition-colors hover:bg-accent/40">
              <CardContent className="flex items-center justify-between gap-4 p-4">
                <div className="min-w-0">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm">
                      #{order.id.slice(0, 8)}
                    </span>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {formatDate(order.createdAt)} ·{' '}
                    {order.items.length} item
                    {order.items.length === 1 ? '' : 's'}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-semibold">
                    {formatMoney(order.totalAmount, order.currency)}
                  </span>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}
