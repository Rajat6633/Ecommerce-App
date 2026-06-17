import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  Loader2,
  Pencil,
  Plus,
  Search,
  Trash2,
} from 'lucide-react'
import type { Category, Product, ProductSearchParams } from '@/lib/api'
import { formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  useCategories,
  useDeleteProduct,
  useProducts,
} from '@/hooks/use-admin-data'

const PAGE_SIZE = 15

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const name = searchParams.get('q') ?? ''
  const page = Number(searchParams.get('page') ?? '0')

  const { data: categories } = useCategories()
  const categoryName = useMemo(() => {
    const map = new Map<string, string>()
    categories?.forEach((c: Category) => map.set(c.id, c.name))
    return (id: string) => map.get(id) ?? '—'
  }, [categories])

  const query = useMemo<ProductSearchParams>(
    () => ({
      name: name || undefined,
      page,
      size: PAGE_SIZE,
      sortBy: 'createdAt',
      sortDirection: 'desc',
    }),
    [name, page],
  )
  const { data, isLoading, isError } = useProducts(query)
  const deleteProduct = useDeleteProduct()
  const [toDelete, setToDelete] = useState<Product | null>(null)

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

  function confirmDelete() {
    if (!toDelete) return
    deleteProduct.mutate(toDelete.id, { onSettled: () => setToDelete(null) })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Products</h1>
          <p className="text-muted-foreground">
            {data ? `${data.totalElements} total` : 'Manage your catalog'}
          </p>
        </div>
        <Button asChild>
          <Link to="/products/new">
            <Plus /> New product
          </Link>
        </Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          className="pl-9"
          placeholder="Search by name…"
          defaultValue={name}
          onChange={(e) => patch({ q: e.target.value || null })}
        />
      </div>

      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>SKU</TableHead>
              <TableHead>Category</TableHead>
              <TableHead className="text-right">Price</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-24 text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading && (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center">
                  <Loader2 className="mx-auto h-5 w-5 animate-spin text-muted-foreground" />
                </TableCell>
              </TableRow>
            )}
            {isError && (
              <TableRow>
                <TableCell
                  colSpan={6}
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
                  <TableCell>{categoryName(p.categoryId)}</TableCell>
                  <TableCell className="text-right">
                    {formatMoney(p.price, p.currency)}
                  </TableCell>
                  <TableCell>
                    {p.active ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Badge variant="secondary">Inactive</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button asChild size="icon" variant="ghost" className="h-8 w-8">
                        <Link to={`/products/${p.id}`} aria-label="Edit">
                          <Pencil className="h-4 w-4" />
                        </Link>
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        className="h-8 w-8 text-muted-foreground hover:text-destructive"
                        onClick={() => setToDelete(p)}
                        aria-label="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            {!isLoading && !isError && data?.content.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={6}
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

      {/* Delete confirmation */}
      <Dialog open={!!toDelete} onOpenChange={(o) => !o && setToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete product?</DialogTitle>
            <DialogDescription>
              “{toDelete?.name}” will be permanently removed. This cannot be
              undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setToDelete(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={confirmDelete}
              disabled={deleteProduct.isPending}
            >
              {deleteProduct.isPending && <Loader2 className="animate-spin" />}
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
