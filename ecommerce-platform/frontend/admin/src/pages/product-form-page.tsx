import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ChevronLeft, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select } from '@/components/ui/select'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  useCategories,
  useCreateProduct,
  useProduct,
  useUpdateProduct,
} from '@/hooks/use-admin-data'

interface FormState {
  sku: string
  name: string
  description: string
  price: string
  currency: string
  categoryId: string
  active: boolean
}

const EMPTY: FormState = {
  sku: '',
  name: '',
  description: '',
  price: '',
  currency: 'USD',
  categoryId: '',
  active: true,
}

export function ProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const navigate = useNavigate()

  const { data: categories } = useCategories()
  const { data: existing, isLoading: loadingProduct } = useProduct(id)
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct(id ?? '')

  const [form, setForm] = useState<FormState>(EMPTY)

  // Hydrate the form when editing once the product loads.
  useEffect(() => {
    if (existing) {
      setForm({
        sku: existing.sku,
        name: existing.name,
        description: existing.description,
        price: existing.price,
        currency: existing.currency,
        categoryId: existing.categoryId,
        active: existing.active,
      })
    }
  }, [existing])

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }))
  }

  const saving = createProduct.isPending || updateProduct.isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const onSuccess = () => navigate('/products')

    if (isEdit) {
      updateProduct.mutate(
        {
          name: form.name,
          description: form.description,
          price: form.price,
          currency: form.currency,
          categoryId: form.categoryId,
          active: form.active,
        },
        { onSuccess },
      )
    } else {
      createProduct.mutate(
        {
          sku: form.sku,
          name: form.name,
          description: form.description,
          price: form.price,
          currency: form.currency,
          categoryId: form.categoryId,
        },
        { onSuccess },
      )
    }
  }

  if (isEdit && loadingProduct) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <Button asChild variant="ghost" size="sm">
        <Link to="/products">
          <ChevronLeft /> Back to products
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <CardTitle className="text-2xl">
            {isEdit ? 'Edit product' : 'New product'}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="sku">SKU</Label>
              <Input
                id="sku"
                required={!isEdit}
                disabled={isEdit}
                value={form.sku}
                onChange={(e) => set('sku', e.target.value)}
                placeholder="PROD-001"
              />
              {isEdit && (
                <p className="text-xs text-muted-foreground">
                  SKU can’t be changed after creation.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                required
                value={form.name}
                onChange={(e) => set('name', e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                rows={4}
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="price">Price</Label>
                <Input
                  id="price"
                  required
                  inputMode="decimal"
                  value={form.price}
                  onChange={(e) => set('price', e.target.value)}
                  placeholder="99.99"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="currency">Currency</Label>
                <Input
                  id="currency"
                  required
                  maxLength={3}
                  value={form.currency}
                  onChange={(e) =>
                    set('currency', e.target.value.toUpperCase())
                  }
                  placeholder="USD"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Select
                id="category"
                required
                value={form.categoryId}
                onChange={(e) => set('categoryId', e.target.value)}
              >
                <option value="" disabled>
                  Select a category…
                </option>
                {categories?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
              {categories?.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  No categories yet —{' '}
                  <Link to="/categories" className="underline">
                    create one first
                  </Link>
                  .
                </p>
              )}
            </div>

            {isEdit && (
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4"
                  checked={form.active}
                  onChange={(e) => set('active', e.target.checked)}
                />
                Active (visible in the storefront)
              </label>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <Button asChild variant="outline" type="button">
                <Link to="/products">Cancel</Link>
              </Button>
              <Button type="submit" disabled={saving}>
                {saving && <Loader2 className="animate-spin" />}
                {isEdit ? 'Save changes' : 'Create product'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
