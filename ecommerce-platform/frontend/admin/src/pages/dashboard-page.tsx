import { Link } from 'react-router-dom'
import { ArrowRight, Package, Tags } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useCategories, useProducts } from '@/hooks/use-admin-data'

export function DashboardPage() {
  // Cheap counts: page 0 size 1 just to read totalElements.
  const products = useProducts({ page: 0, size: 1 })
  const categories = useCategories()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Manage your catalog and stock.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Products</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {products.isLoading ? '—' : (products.data?.totalElements ?? 0)}
            </div>
            <Button asChild variant="link" className="mt-2 h-auto p-0">
              <Link to="/products">
                Manage products <ArrowRight className="h-3 w-3" />
              </Link>
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Categories</CardTitle>
            <Tags className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">
              {categories.isLoading ? '—' : (categories.data?.length ?? 0)}
            </div>
            <Button asChild variant="link" className="mt-2 h-auto p-0">
              <Link to="/categories">
                Manage categories <ArrowRight className="h-3 w-3" />
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Quick actions</CardTitle>
          <CardDescription>Common catalog tasks.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          <Button asChild>
            <Link to="/products/new">
              <Package /> New product
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/categories">
              <Tags /> New category
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
