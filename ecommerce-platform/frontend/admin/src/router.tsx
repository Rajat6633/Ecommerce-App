import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AdminLayout } from '@/components/layout/admin-layout'
import { ProtectedRoute } from '@/components/protected-route'
import { LoginPage } from '@/pages/login-page'
import { DashboardPage } from '@/pages/dashboard-page'
import { ProductsPage } from '@/pages/products-page'
import { ProductFormPage } from '@/pages/product-form-page'
import { CategoriesPage } from '@/pages/categories-page'
import { InventoryPage } from '@/pages/inventory-page'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { path: '/', element: <DashboardPage /> },
          { path: '/products', element: <ProductsPage /> },
          { path: '/products/new', element: <ProductFormPage /> },
          { path: '/products/:id', element: <ProductFormPage /> },
          { path: '/categories', element: <CategoriesPage /> },
          { path: '/inventory', element: <InventoryPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
