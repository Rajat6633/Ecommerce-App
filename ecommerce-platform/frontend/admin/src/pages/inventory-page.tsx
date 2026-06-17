import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  Loader2,
  Search,
  SlidersHorizontal,
} from 'lucide-react'
import type { Product, ProductSearchParams } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useProducts } from '@/hooks/use-admin-data'
import { InventoryAdjustDialog } from '@/components/inventory-adjust-dialog'

const PAGE_SIZE = 15

export function InventoryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const name = searchParams.get('q') ?? ''
  const page = Number(searchParams.get('page') ?? '0')

  const query = useMemo<ProductSearchParams>(
    () => ({
      name: name || undefined,
      page,
      size: PAGE_SIZE,
      sortBy: 'name',
      sortDirection: 'asc',
    }),
    [name, page],
  )
  const { data, isLoading, isError } = useProducts(query)
  const [selected, setSelected] = useState<Product | null>(null)

  function patch(p: Record<string, string | null>, resetPage = true) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      for (const [k, v] of Object.entries(p)) {
        if (v === null || v === '') next.delete(k)
        else next.set(k, v)
      }
      if (resetPage) next.delete('page')
      return next
    })
  }

  const totalPages = data?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Inventory</h1>
        <p className="text-muted-foreground">
          Pick a product to view and adjust its stock.
        </p>
      </div>

      <div className="relative max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          className="pl-9"
          placeholder="Search products…"
          defaultValue={name}
          onChange={(e) => patch({ q: e.target.value || null })}
        />
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Product</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead className="w-32 text-right">Manage</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={3} className="py-10 text-center">
                  <Loader2 className="mx-auto h-5 w-5 animate-spin text-muted-foreground" />
                </TableCell>
              </TableRow>
            )}
            {isError && (
              <TableRow>
                <TableCell
                  colSpan={3}
                  className="py-10 text-center text-muted-foreground"
                >
                  Couldn’t load products. Is the backend running?
                </TableCell>
              </TableRow>
            )}
            {!isLoading &&
              data?.content.map((p) => (
                <TableRow key={p.id}>
                  <TableCell className="font-medium">{p.name}</TableCell>
                  <TableCell className="font-mono text-xs text-muted-foreground">
                    {p.sku}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setSelected(p)}
                    >
                      <SlidersHorizontal className="h-4 w-4" /> Adjust
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            {!isLoading && !isError && data?.content.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={3}
                  className="py-10 text-center text-muted-foreground"
                >
                  No products found.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-4">
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => patch({ page: String(page - 1) }, false)}
          >
            <ChevronLeft /> Prev
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => patch({ page: String(page + 1) }, false)}
          >
            Next <ChevronRight />
          </Button>
        </div>
      )}

      <InventoryAdjustDialog
        product={selected}
        onClose={() => setSelected(null)}
      />
    </div>
  )
}
