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
  cpuModel: string | null;
  ramGb: number | null;
  storageGb: number | null;
  gpuModel: string | null;
  refreshRateHz: number | null;
  panelType: string | null;
  connectionType: string | null;
  switchType: string | null;
  sizeInch: number | null;
  status: string;
}

export interface ProductFilterQuery {
  q?: string;
  categorySlug?: string;
  brandSlug?: string[];
  cpu?: string[];
  ram?: number[];
  storage?: number[];
  gpu?: string[];
  refreshRate?: number[];
  panelType?: string[];
  connectionType?: string[];
  switchType?: string[];
  sizeInch?: number[];
}

export interface ProductFacetOption {
  valueKey: string;
  label: string;
  count: number;
}

export interface ProductFacetGroup {
  key: string;
  title: string;
  options: ProductFacetOption[];
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
