import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Search, X } from 'lucide-react'
import type { ProductSearchParams } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { ProductCard } from '@/components/product/product-card'
import { ProductCardSkeleton } from '@/components/product/product-card-skeleton'
import { useCategories, useProducts } from '@/hooks/use-products'

const PAGE_SIZE = 12

const SORT_OPTIONS = [
  { label: 'Newest', sortBy: 'createdAt', sortDirection: 'desc' },
  { label: 'Price: Low to High', sortBy: 'price', sortDirection: 'asc' },
  { label: 'Price: High to Low', sortBy: 'price', sortDirection: 'desc' },
  { label: 'Name: A–Z', sortBy: 'name', sortDirection: 'asc' },
] as const

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  // URL is the single source of truth for filters (shareable, back-button safe).
  const name = searchParams.get('q') ?? ''
  const categoryId = searchParams.get('category') ?? ''
  const sortIdx = Number(searchParams.get('sort') ?? '0')
  const page = Number(searchParams.get('page') ?? '0')
  const sort = SORT_OPTIONS[sortIdx] ?? SORT_OPTIONS[0]

  const { data: categories } = useCategories()

  const query = useMemo<ProductSearchParams>(
    () => ({
      name: name || undefined,
      categoryId: categoryId || undefined,
      activeOnly: true,
      page,
      size: PAGE_SIZE,
      sortBy: sort.sortBy,
      sortDirection: sort.sortDirection,
    }),
    [name, categoryId, page, sort],
  )

  const { data, isLoading, isError } = useProducts(query)

  function patchParams(patch: Record<string, string | null>, resetPage = true) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      for (const [key, value] of Object.entries(patch)) {
        if (value === null || value === '') next.delete(key)
        else next.set(key, value)
      }
      if (resetPage) next.delete('page')
      return next
    })
  }

  const totalPages = data?.totalPages ?? 0
  const hasFilters = !!name || !!categoryId

  return (
    <div className="container py-8">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Shop</h1>
          <p className="text-muted-foreground">
            {data ? `${data.totalElements} products` : 'Browse the catalog'}
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="space-y-1">
            <Label htmlFor="search" className="sr-only">
              Search
            </Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="search"
                className="w-full pl-9 sm:w-64"
                placeholder="Search products…"
                defaultValue={name}
                onChange={(e) => patchParams({ q: e.target.value || null })}
              />
            </div>
          </div>

          <div className="space-y-1">
            <Label htmlFor="category" className="sr-only">
              Category
            </Label>
            <Select
              id="category"
              className="sm:w-44"
              value={categoryId}
              onChange={(e) => patchParams({ category: e.target.value || null })}
            >
              <option value="">All categories</option>
              {categories?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
          </div>

          <div className="space-y-1">
            <Label htmlFor="sort" className="sr-only">
              Sort
            </Label>
            <Select
              id="sort"
              className="sm:w-44"
              value={String(sortIdx)}
              onChange={(e) => patchParams({ sort: e.target.value })}
            >
              {SORT_OPTIONS.map((o, i) => (
                <option key={o.label} value={i}>
                  {o.label}
                </option>
              ))}
            </Select>
          </div>
        </div>
      </div>

      {hasFilters && (
        <div className="mb-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => patchParams({ q: null, category: null })}
          >
            <X /> Clear filters
          </Button>
        </div>
      )}

      {isError ? (
        <div className="py-20 text-center">
          <p className="text-muted-foreground">
            Couldn’t load products. Is the backend running?
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {isLoading
            ? Array.from({ length: PAGE_SIZE }).map((_, i) => (
                <ProductCardSkeleton key={i} />
              ))
            : data?.content.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}

      {!isLoading && !isError && (data?.content.length ?? 0) === 0 && (
        <p className="py-20 text-center text-muted-foreground">
          No products match your filters.
        </p>
      )}

      {totalPages > 1 && (
        <div className="mt-10 flex items-center justify-center gap-4">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => patchParams({ page: String(page - 1) }, false)}
          >
            <ChevronLeft /> Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => patchParams({ page: String(page + 1) }, false)}
          >
            Next <ChevronRight />
          </Button>
        </div>
      )}
    </div>
  )
}
