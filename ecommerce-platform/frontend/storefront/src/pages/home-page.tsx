import { Link } from 'react-router-dom'
import { ArrowRight, PackageCheck, ShieldCheck, Truck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ProductCard } from '@/components/product/product-card'
import { ProductCardSkeleton } from '@/components/product/product-card-skeleton'
import { useProducts } from '@/hooks/use-products'

export function HomePage() {
  // Feature a small set of the newest active products on the landing page.
  const { data, isLoading } = useProducts({
    activeOnly: true,
    size: 8,
    sortBy: 'createdAt',
    sortDirection: 'desc',
  })

  return (
    <div>
      {/* Hero */}
      <section className="border-b bg-muted/30">
        <div className="container flex flex-col items-start gap-6 py-20">
          <h1 className="max-w-2xl text-4xl font-bold tracking-tight sm:text-5xl">
            Everything you need, delivered with confidence.
          </h1>
          <p className="max-w-xl text-lg text-muted-foreground">
            Browse the catalog, build your cart, and track every order in real
            time as it moves through our fulfillment pipeline.
          </p>
          <Button asChild size="lg">
            <Link to="/products">
              Start shopping
              <ArrowRight />
            </Link>
          </Button>
        </div>
      </section>

      {/* Trust strip */}
      <section className="border-b">
        <div className="container grid gap-6 py-8 sm:grid-cols-3">
          {[
            { icon: Truck, title: 'Fast fulfillment', desc: 'Orders processed in real time.' },
            { icon: ShieldCheck, title: 'Secure checkout', desc: 'JWT-protected, end to end.' },
            { icon: PackageCheck, title: 'Live tracking', desc: 'Watch your order status update live.' },
          ].map(({ icon: Icon, title, desc }) => (
            <div key={title} className="flex items-center gap-3">
              <Icon className="h-8 w-8 shrink-0 text-primary" />
              <div>
                <p className="font-medium">{title}</p>
                <p className="text-sm text-muted-foreground">{desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Featured products */}
      <section className="container py-12">
        <div className="mb-6 flex items-end justify-between">
          <h2 className="text-2xl font-semibold tracking-tight">New arrivals</h2>
          <Link
            to="/products"
            className="text-sm font-medium text-primary hover:underline"
          >
            View all
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {isLoading
            ? Array.from({ length: 8 }).map((_, i) => (
                <ProductCardSkeleton key={i} />
              ))
            : data?.content.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
        {!isLoading && (data?.content.length ?? 0) === 0 && (
          <p className="py-12 text-center text-muted-foreground">
            No products available yet. Check back soon.
          </p>
        )}
      </section>
    </div>
  )
}
