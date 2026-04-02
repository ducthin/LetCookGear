export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  isActive: boolean;
}

export interface Brand {
  id: number;
  name: string;
  slug: string;
  country: string | null;
  isActive: boolean;
}

export interface ProductVariant {
  id: number;
  sku: string;
  variantName: string;
  price: number;
  compareAtPrice: number | null;
  status: string;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  shortDescription: string | null;
  description: string | null;
  warrantyMonths: number;
  status: string;
  categoryId: number;
  categoryName: string;
  brandId: number;
  brandName: string;
  variants: ProductVariant[];
}
