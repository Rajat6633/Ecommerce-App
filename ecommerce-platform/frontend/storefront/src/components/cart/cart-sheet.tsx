import { Link } from 'react-router-dom'
import { Loader2, Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react'
import { formatMoney } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import {
  useCart,
  useRemoveCartItem,
  useUpdateCartQuantity,
} from '@/hooks/use-cart'
import { productLabel, useProductMap } from '@/hooks/use-product-map'
import { useCartUI } from '@/features/cart/cart-ui-context'

export function CartSheet() {
  const { isOpen, setOpen, close } = useCartUI()
  const { data: cart, isLoading } = useCart()
  const updateQty = useUpdateCartQuantity()
  const removeItem = useRemoveCartItem()

  const items = cart?.items ?? []
  const { map: productMap } = useProductMap(items.map((i) => i.productId))
  const isEmpty = !isLoading && items.length === 0

  return (
    <Sheet open={isOpen} onOpenChange={setOpen}>
      <SheetContent className="w-full sm:max-w-md">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <ShoppingBag className="h-5 w-5" />
            Your cart
          </SheetTitle>
        </SheetHeader>

        {isLoading && (
          <div className="flex flex-1 items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        )}

        {isEmpty && (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center">
            <ShoppingBag className="h-10 w-10 text-muted-foreground" />
            <p className="text-muted-foreground">Your cart is empty.</p>
            <Button asChild variant="outline" onClick={close}>
              <Link to="/products">Browse products</Link>
            </Button>
          </div>
        )}

        {!isLoading && items.length > 0 && (
          <>
            <div className="-mx-6 flex-1 overflow-y-auto px-6">
              <ul className="divide-y">
                {items.map((item) => (
                  <li key={item.productId} className="flex gap-3 py-4">
                    <div className="flex-1">
                      <Link
                        to={`/products/${item.productId}`}
                        onClick={close}
                        className="line-clamp-1 text-sm font-medium hover:underline"
                      >
                        {productLabel(productMap, item.productId)}
                      </Link>
                      <p className="text-sm text-muted-foreground">
                        {formatMoney(item.unitPrice)} each
                      </p>
                      <div className="mt-2 flex items-center gap-2">
                        <Button
                          size="icon"
                          variant="outline"
                          className="h-7 w-7"
                          disabled={updateQty.isPending}
                          onClick={() =>
                            updateQty.mutate({
                              productId: item.productId,
                              quantity: Math.max(1, item.quantity - 1),
                            })
                          }
                        >
                          <Minus />
                        </Button>
                        <span className="w-6 text-center text-sm tabular-nums">
                          {item.quantity}
                        </span>
                        <Button
                          size="icon"
                          variant="outline"
                          className="h-7 w-7"
                          disabled={updateQty.isPending}
                          onClick={() =>
                            updateQty.mutate({
                              productId: item.productId,
                              quantity: item.quantity + 1,
                            })
                          }
                        >
                          <Plus />
                        </Button>
                      </div>
                    </div>
                    <div className="flex flex-col items-end justify-between">
                      <span className="text-sm font-semibold">
                        {formatMoney(item.lineTotal)}
                      </span>
                      <Button
                        size="icon"
                        variant="ghost"
                        className="h-7 w-7 text-muted-foreground hover:text-destructive"
                        disabled={removeItem.isPending}
                        onClick={() => removeItem.mutate(item.productId)}
                        aria-label="Remove item"
                      >
                        <Trash2 />
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            </div>

            <SheetFooter className="border-t pt-4">
              <div className="flex items-center justify-between text-base font-semibold">
                <span>Subtotal</span>
                <span>{formatMoney(cart!.totalAmount)}</span>
              </div>
              <p className="text-xs text-muted-foreground">
                Shipping & taxes calculated at checkout.
              </p>
              <Button asChild className="w-full" onClick={close}>
                <Link to="/checkout">Checkout</Link>
              </Button>
            </SheetFooter>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}
