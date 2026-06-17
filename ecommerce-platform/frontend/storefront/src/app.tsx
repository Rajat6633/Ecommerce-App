import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AuthProvider } from '@/features/auth/auth-context'
import { CartUIProvider } from '@/features/cart/cart-ui-context'
import { router } from '@/router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <CartUIProvider>
          <RouterProvider router={router} />
          <Toaster richColors position="top-center" />
        </CartUIProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}
