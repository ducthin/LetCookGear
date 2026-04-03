export interface CartItem {
  itemId: number;
  variantId: number;
  sku: string;
  variantName: string;
  productId: number;
  productName: string;
  productSlug: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  cartId: number;
  totalItems: number;
  subtotal: number;
  items: CartItem[];
}
