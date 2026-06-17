import { Check, Loader2, X } from 'lucide-react'
import type { OrderStatus } from '@/lib/api'
import { cn } from '@/lib/utils'

/**
 * Visualises the order saga as a 4-step stepper:
 *   Placed → Inventory reserved → Payment → Confirmed
 *
 * The backend saga is choreographed over Kafka and resolves asynchronously,
 * so this component is driven purely by the current OrderStatus (which the
 * page polls). Failure statuses mark the step that failed in red.
 */

type StepState = 'done' | 'active' | 'pending' | 'failed'

interface Step {
  key: string
  label: string
  description: string
}

const STEPS: Step[] = [
  { key: 'placed', label: 'Order placed', description: 'We received your order.' },
  {
    key: 'inventory',
    label: 'Inventory reserved',
    description: 'Items set aside for you.',
  },
  { key: 'payment', label: 'Payment', description: 'Processing your payment.' },
  {
    key: 'confirmed',
    label: 'Confirmed',
    description: 'Your order is confirmed!',
  },
]

/** Index of the step a given status has reached (0-based), plus failure info. */
function resolve(status: OrderStatus): { reached: number; failedAt: number | null } {
  switch (status) {
    case 'PENDING':
      return { reached: 0, failedAt: null }
    case 'INVENTORY_RESERVED':
      return { reached: 1, failedAt: null }
    case 'PAID':
      return { reached: 2, failedAt: null }
    case 'CONFIRMED':
      return { reached: 3, failedAt: null }
    case 'REJECTED':
      // Inventory reservation failed.
      return { reached: 0, failedAt: 1 }
    case 'PAYMENT_FAILED':
      // Inventory was reserved, payment then failed.
      return { reached: 1, failedAt: 2 }
    case 'CANCELLED':
      return { reached: 0, failedAt: 0 }
  }
}

function stepState(
  index: number,
  reached: number,
  failedAt: number | null,
  isTerminal: boolean,
): StepState {
  if (failedAt === index) return 'failed'
  if (index <= reached) return 'done'
  // The first not-yet-reached step is "active" (in progress) unless we've
  // already hit a terminal/failed state.
  if (index === reached + 1 && failedAt === null && !isTerminal) return 'active'
  return 'pending'
}

export function OrderStatusTimeline({
  status,
  isTerminal,
}: {
  status: OrderStatus
  isTerminal: boolean
}) {
  const { reached, failedAt } = resolve(status)

  return (
    <ol className="space-y-0">
      {STEPS.map((step, index) => {
        const state = stepState(index, reached, failedAt, isTerminal)
        const isLast = index === STEPS.length - 1
        return (
          <li key={step.key} className="flex gap-4">
            {/* Marker + connector rail */}
            <div className="flex flex-col items-center">
              <span
                className={cn(
                  'flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 transition-colors',
                  state === 'done' &&
                    'border-success bg-success text-success-foreground',
                  state === 'active' &&
                    'border-primary text-primary',
                  state === 'failed' &&
                    'border-destructive bg-destructive text-destructive-foreground',
                  state === 'pending' &&
                    'border-muted-foreground/30 text-muted-foreground/40',
                )}
              >
                {state === 'done' && <Check className="h-4 w-4" />}
                {state === 'active' && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
                {state === 'failed' && <X className="h-4 w-4" />}
                {state === 'pending' && (
                  <span className="text-xs font-semibold">{index + 1}</span>
                )}
              </span>
              {!isLast && (
                <span
                  className={cn(
                    'my-1 w-0.5 flex-1',
                    index < reached ? 'bg-success' : 'bg-muted-foreground/20',
                  )}
                  style={{ minHeight: '1.5rem' }}
                />
              )}
            </div>

            {/* Label */}
            <div className={cn('pb-6', isLast && 'pb-0')}>
              <p
                className={cn(
                  'font-medium',
                  state === 'pending' && 'text-muted-foreground',
                  state === 'failed' && 'text-destructive',
                )}
              >
                {step.label}
              </p>
              <p className="text-sm text-muted-foreground">
                {state === 'failed' && index === 1
                  ? 'Some items were out of stock — your order was rejected.'
                  : state === 'failed' && index === 2
                    ? 'Payment could not be completed. Reserved stock was released.'
                    : step.description}
              </p>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
