import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

interface CartUIState {
  isOpen: boolean
  open: () => void
  close: () => void
  setOpen: (open: boolean) => void
}

const CartUIContext = createContext<CartUIState | null>(null)

/** Controls the slide-over cart drawer's open/closed state app-wide. */
export function CartUIProvider({ children }: { children: ReactNode }) {
  const [isOpen, setOpen] = useState(false)
  const value = useMemo<CartUIState>(
    () => ({
      isOpen,
      open: () => setOpen(true),
      close: () => setOpen(false),
      setOpen,
    }),
    [isOpen],
  )
  return <CartUIContext.Provider value={value}>{children}</CartUIContext.Provider>
}

export function useCartUI() {
  const ctx = useContext(CartUIContext)
  if (!ctx) throw new Error('useCartUI must be used within <CartUIProvider>')
  return ctx
}
