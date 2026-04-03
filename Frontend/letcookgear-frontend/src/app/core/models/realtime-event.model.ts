export type RealtimeEventType = 'CART_UPDATED' | 'PAYMENT_PAID';

export interface RealtimeEvent {
  type: RealtimeEventType;
  userEmail?: string | null;
  orderId?: number | null;
  orderCode?: string | null;
  paymentStatus?: string | null;
  orderStatus?: string | null;
  totalItems?: number | null;
  message?: string | null;
}