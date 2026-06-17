import { Link, useNavigate } from 'react-router-dom'
import { Loader2, Plus } from 'lucide-react'
import type { Product } from '@/lib/api'
import { formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter } from '@/components/ui/card'
import { useAddToCart } from '@/hooks/use-cart'
import { useAuth } from '@/features/auth/use-auth'

/** Deterministic placeholder image per product (no image service in backend). */
function placeholderFor(seed: string) {
  return `https://picsum.photos/seed/${encodeURIComponent(seed)}/600/600`
}

export function ProductCard({ product }: { product: Product }) {
  const addToCart = useAddToCart()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  function handleAdd() {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    addToCart.mutate({ productId: product.id, quantity: 1 })
  }

  return (
    <Card className="group flex flex-col overflow-hidden transition-shadow hover:shadow-md">
      <Link to={`/products/${product.id}`} className="block overflow-hidden">
        <img
          src={placeholderFor(product.sku || product.id)}
          alt={product.name}
          loading="lazy"
          className="aspect-square w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </Link>
      <CardContent className="flex flex-1 flex-col gap-1 p-4">
        <Link
          to={`/products/${product.id}`}
          className="line-clamp-1 font-medium hover:underline"
        >
          {product.name}
        </Link>
        <p className="line-clamp-2 text-sm text-muted-foreground">
          {product.description}
        </p>
        <p className="mt-2 text-lg font-semibold">
          {formatMoney(product.price, product.currency)}
        </p>
      </CardContent>
      <CardFooter className="p-4 pt-0">
        <Button
          className="w-full"
          onClick={handleAdd}
          disabled={addToCart.isPending}
        >
          {addToCart.isPending ? (
            <Loader2 className="animate-spin" />
          ) : (
            <Plus />
          )}
          Add to cart
        </Button>
      </CardFooter>
    </Card>
  )
}
