import { useQueries } from '@tanstack/react-query'
import { productsApi, type Product } from '@/lib/api'

/**
 * Resolve a set of product IDs to their full products, keyed by ID.
 *
 * The cart/order DTOs carry only `productId` (no name), so views that want to
 * show a human-readable label look it up here. Each product is fetched with the
 * same query key the product pages use (`['product', id]`), so detail-page
 * visits and these lookups share one cache entry — no duplicate requests.
 */
export function useProductMap(ids: string[]) {
  // De-dupe so repeated IDs across cart lines don't spawn duplicate queries.
  const uniqueIds = Array.from(new Set(ids))

  const results = useQueries({
    queries: uniqueIds.map((id) => ({
      queryKey: ['product', id],
      queryFn: () => productsApi.getById(id),
      staleTime: 5 * 60 * 1000,
    })),
  })

  const map: Record<string, Product> = {}
  results.forEach((r, i) => {
    if (r.data) map[uniqueIds[i]] = r.data
  })

  return {
    map,
    isLoading: results.some((r) => r.isLoading),
  }
}

/** Friendly label for a product ID, falling back to a short ref while loading. */
export function productLabel(
  map: Record<string, Product>,
  productId: string,
): string {
  return map[productId]?.name ?? `Item ${productId.slice(0, 8)}`
}
