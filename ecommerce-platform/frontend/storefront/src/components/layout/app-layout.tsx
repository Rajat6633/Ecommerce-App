import { Outlet } from 'react-router-dom'
import { Header } from './header'
import { Footer } from './footer'
import { CartSheet } from '@/components/cart/cart-sheet'

export function AppLayout() {
  return (
    <div className="flex min-h-full flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      <CartSheet />
    </div>
  )
}
