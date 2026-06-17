import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '@/components/layout/app-layout'
import { ProtectedRoute } from '@/components/protected-route'
import { HomePage } from '@/pages/home-page'
import { ProductsPage } from '@/pages/products-page'
import { ProductDetailPage } from '@/pages/product-detail-page'
import { LoginPage } from '@/pages/login-page'
import { RegisterPage } from '@/pages/register-page'
import { CheckoutPage } from '@/pages/checkout-page'
import { OrdersPage } from '@/pages/orders-page'
import { OrderTrackingPage } from '@/pages/order-tracking-page'
import { NotFoundPage } from '@/pages/not-found-page'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/products', element: <ProductsPage /> },
      { path: '/products/:id', element: <ProductDetailPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      // Authenticated-only routes (cart/checkout/orders are per-user).
      {
        element: <ProtectedRoute />,
        children: [
          { path: '/checkout', element: <CheckoutPage /> },
          { path: '/orders', element: <OrdersPage /> },
          { path: '/orders/:id', element: <OrderTrackingPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
