import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Loader2, Minus, Plus, ShoppingCart } from 'lucide-react'
import { formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { useProduct } from '@/hooks/use-products'
import { useAddToCart } from '@/hooks/use-cart'
import { useAuth } from '@/features/auth/use-auth'
import { useCartUI } from '@/features/cart/cart-ui-context'

function imageFor(seed: string) {
  return `https://picsum.photos/seed/${encodeURIComponent(seed)}/800/800`
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: product, isLoading, isError } = useProduct(id)
  const addToCart = useAddToCart()
  const { isAuthenticated } = useAuth()
  const { open } = useCartUI()
  const navigate = useNavigate()
  const [qty, setQty] = useState(1)

  function handleAdd() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    addToCart.mutate(
      { productId: product!.id, quantity: qty },
      { onSuccess: () => open() },
    )
  }

  if (isError) {
    return (
      <div className="container py-20 text-center">
        <p className="text-muted-foreground">Product not found.</p>
        <Button asChild variant="link">
          <Link to="/products">Back to shop</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="container py-8">
      <Button asChild variant="ghost" size="sm" className="mb-6">
        <Link to="/products">
          <ChevronLeft /> Back to shop
        </Link>
      </Button>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="overflow-hidden rounded-lg border bg-muted">
          {isLoading ? (
            <Skeleton className="aspect-square w-full" />
          ) : (
            <img
              src={imageFor(product!.sku || product!.id)}
              alt={product!.name}
              className="aspect-square w-full object-cover"
            />
          )}
        </div>

        <div className="flex flex-col gap-4">
          {isLoading ? (
            <>
              <Skeleton className="h-9 w-3/4" />
              <Skeleton className="h-6 w-1/4" />
              <Skeleton className="h-24 w-full" />
            </>
          ) : (
            <>
              <div className="flex items-start justify-between gap-4">
                <h1 className="text-3xl font-bold tracking-tight">
                  {product!.name}
                </h1>
                {!product!.active && <Badge variant="secondary">Inactive</Badge>}
              </div>
              <p className="text-sm text-muted-foreground">
                SKU: {product!.sku}
              </p>
              <p className="text-3xl font-semibold">
                {formatMoney(product!.price, product!.currency)}
              </p>
              <p className="leading-relaxed text-muted-foreground">
                {product!.description}
              </p>

              <div className="mt-2 flex items-center gap-4">
                <div className="flex items-center rounded-md border">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-10 w-10"
                    onClick={() => setQty((q) => Math.max(1, q - 1))}
                  >
                    <Minus />
                  </Button>
                  <span className="w-10 text-center tabular-nums">{qty}</span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-10 w-10"
                    onClick={() => setQty((q) => q + 1)}
                  >
                    <Plus />
                  </Button>
                </div>

                <Button
                  size="lg"
                  className="flex-1"
                  onClick={handleAdd}
                  disabled={addToCart.isPending}
                >
                  {addToCart.isPending ? (
                    <Loader2 className="animate-spin" />
                  ) : (
                    <ShoppingCart />
                  )}
                  Add to cart
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
