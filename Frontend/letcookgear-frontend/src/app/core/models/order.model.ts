export type PaymentMethod = 'COD' | 'BANK_TRANSFER' | 'VNPAY' | 'MOMO';

export interface CheckoutPayload {
  receiverName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  detail: string;
  paymentMethod: PaymentMethod;
}

export interface OrderItem {
  variantId: number;
  sku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  orderId: number;
  orderCode: string;
  status: string;
  paymentStatus: string;
  paymentMethod: PaymentMethod;
  totalAmount: number;
  shippingFee: number;
  discountAmount: number;
  finalAmount: number;
  placedAt: string;
  items: OrderItem[];
}

export interface OrderStatusUpdatePayload {
  status: string;
}
