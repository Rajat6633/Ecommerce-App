import { Link, useNavigate } from 'react-router-dom'
import { Loader2, ShoppingBag } from 'lucide-react'
import { toast } from 'sonner'
import { formatMoney } from '@/lib/utils'
import { toApiError } from '@/lib/api'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useCart } from '@/hooks/use-cart'
import { usePlaceOrder } from '@/hooks/use-orders'
import { productLabel, useProductMap } from '@/hooks/use-product-map'

export function CheckoutPage() {
  const { data: cart, isLoading } = useCart()
  const placeOrder = usePlaceOrder()
  const navigate = useNavigate()

  const items = cart?.items ?? []
  const { map: productMap } = useProductMap(items.map((i) => i.productId))
  const isEmpty = !isLoading && items.length === 0

  function handlePlaceOrder() {
    placeOrder.mutate(undefined, {
      onSuccess: (order) => {
        toast.success('Order placed!')
        // Hand off to live tracking — the saga resolves asynchronously.
        navigate(`/orders/${order.id}`, { replace: true })
      },
      onError: (err) => toast.error(toApiError(err).message),
    })
  }

  if (isLoading) {
    return (
      <div className="container flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (isEmpty) {
    return (
      <div className="container flex min-h-[50vh] flex-col items-center justify-center gap-4 text-center">
        <ShoppingBag className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">Your cart is empty</h1>
        <Button asChild>
          <Link to="/products">Browse products</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="container max-w-2xl py-10">
      <h1 className="mb-6 text-3xl font-bold tracking-tight">Checkout</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Order summary</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <ul className="divide-y">
            {items.map((item) => (
              <li
                key={item.productId}
                className="flex items-center justify-between py-3 text-sm"
              >
                <span className="text-muted-foreground">
                  {productLabel(productMap, item.productId)} × {item.quantity}
                </span>
                <span className="font-medium">
                  {formatMoney(item.lineTotal)}
                </span>
              </li>
            ))}
          </ul>

          <div className="flex items-center justify-between border-t pt-4 text-lg font-semibold">
            <span>Total</span>
            <span>{formatMoney(cart!.totalAmount)}</span>
          </div>

          <p className="text-sm text-muted-foreground">
            This is a demo checkout — payment is simulated by the backend.
            Placing the order kicks off the inventory → payment → confirmation
            saga, which you’ll watch update live on the next screen.
          </p>

          <Button
            className="w-full"
            size="lg"
            onClick={handlePlaceOrder}
            disabled={placeOrder.isPending}
          >
            {placeOrder.isPending && <Loader2 className="animate-spin" />}
            Place order
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
