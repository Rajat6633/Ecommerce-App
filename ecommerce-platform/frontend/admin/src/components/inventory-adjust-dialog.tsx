import { useEffect, useState } from 'react'
import { AlertTriangle, Loader2 } from 'lucide-react'
import type { Product } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  useInventory,
  useReceiveStock,
  useUpsertStock,
} from '@/hooks/use-admin-data'

/**
 * View + adjust stock for one product. The inventory-service exposes only
 * per-product endpoints (no list), so this dialog is the unit of management:
 *  - "Set stock"    -> PUT  /api/inventory/{id}      (absolute onHand + reorder)
 *  - "Receive stock"-> POST /api/inventory/{id}/receive (additive)
 */
export function InventoryAdjustDialog({
  product,
  onClose,
}: {
  product: Product | null
  onClose: () => void
}) {
  const productId = product?.id
  const { data: inventory, isLoading, isError, error } = useInventory(productId)
  const upsert = useUpsertStock(productId ?? '')
  const receive = useReceiveStock(productId ?? '')

  const [onHand, setOnHand] = useState('')
  const [reorderLevel, setReorderLevel] = useState('')
  const [receiveQty, setReceiveQty] = useState('')

  // Sync local fields when the inventory record arrives.
  useEffect(() => {
    if (inventory) {
      setOnHand(String(inventory.onHand))
      setReorderLevel(String(inventory.reorderLevel))
    } else {
      setOnHand('')
      setReorderLevel('')
    }
  }, [inventory])

  // A 404 means no stock record exists yet — treat as "create new".
  const noRecord =
    isError &&
    (error as { response?: { status?: number } })?.response?.status === 404

  function handleUpsert(e: React.FormEvent) {
    e.preventDefault()
    upsert.mutate({
      onHand: Number(onHand) || 0,
      reorderLevel: Number(reorderLevel) || 0,
    })
  }

  function handleReceive(e: React.FormEvent) {
    e.preventDefault()
    const qty = Number(receiveQty)
    if (qty > 0) receive.mutate({ quantity: qty }, { onSuccess: () => setReceiveQty('') })
  }

  return (
    <Dialog open={!!product} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Manage stock</DialogTitle>
          <DialogDescription>
            {product?.name} · <span className="font-mono">{product?.sku}</span>
          </DialogDescription>
        </DialogHeader>

        {isLoading && (
          <div className="flex justify-center py-6">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        )}

        {isError && !noRecord && (
          <p className="text-sm text-destructive">Couldn’t load stock.</p>
        )}

        {(inventory || noRecord) && !isLoading && (
          <div className="space-y-5">
            {noRecord && (
              <div className="flex items-start gap-2 rounded-md border border-amber-300/50 bg-amber-50 p-3 text-sm dark:bg-amber-950/30">
                <AlertTriangle className="mt-0.5 h-4 w-4 text-amber-600" />
                <span>No stock record yet — set initial levels below.</span>
              </div>
            )}

            {inventory && (
              <div className="grid grid-cols-3 gap-2 text-center">
                <Stat label="On hand" value={inventory.onHand} />
                <Stat label="Reserved" value={inventory.reserved} />
                <Stat label="Available" value={inventory.available} />
              </div>
            )}

            {/* Set absolute stock */}
            <form onSubmit={handleUpsert} className="space-y-3 border-t pt-4">
              <p className="text-sm font-medium">Set stock levels</p>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="onHand">On hand</Label>
                  <Input
                    id="onHand"
                    type="number"
                    min={0}
                    value={onHand}
                    onChange={(e) => setOnHand(e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="reorder">Reorder level</Label>
                  <Input
                    id="reorder"
                    type="number"
                    min={0}
                    value={reorderLevel}
                    onChange={(e) => setReorderLevel(e.target.value)}
                  />
                </div>
              </div>
              <Button type="submit" size="sm" disabled={upsert.isPending}>
                {upsert.isPending && <Loader2 className="animate-spin" />}
                Save levels
              </Button>
            </form>

            {/* Receive (additive) */}
            {inventory && (
              <form onSubmit={handleReceive} className="space-y-3 border-t pt-4">
                <p className="text-sm font-medium">Receive stock</p>
                <div className="flex items-end gap-3">
                  <div className="flex-1 space-y-1.5">
                    <Label htmlFor="receive">Quantity to add</Label>
                    <Input
                      id="receive"
                      type="number"
                      min={1}
                      value={receiveQty}
                      onChange={(e) => setReceiveQty(e.target.value)}
                      placeholder="e.g. 50"
                    />
                  </div>
                  <Button
                    type="submit"
                    variant="secondary"
                    disabled={receive.isPending || !receiveQty}
                  >
                    {receive.isPending && <Loader2 className="animate-spin" />}
                    Receive
                  </Button>
                </div>
              </form>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border p-3">
      <p className="text-2xl font-bold tabular-nums">{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  )
}
