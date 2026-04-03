import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import { Brand, Category, Product, ProductFacetGroup, ProductFilterQuery } from '../models/catalog.model';

interface FacetCacheEntry {
  data: ProductFacetGroup[];
  expiresAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class CatalogService {
  private static readonly FACETS_CACHE_TTL_MS = 2 * 60 * 1000;
  private static readonly FACETS_CACHE_MAX_ENTRIES = 100;

  private readonly facetsCache = new Map<string, FacetCacheEntry>();

  constructor(private readonly http: HttpClient) {}

  private buildParams(filters: ProductFilterQuery = {}): HttpParams {
    let params = new HttpParams();

    if (filters.q?.trim()) {
      params = params.set('q', filters.q.trim());
    }

    if (filters.categorySlug?.trim()) {
      params = params.set('categorySlug', filters.categorySlug.trim());
    }

    for (const brandSlug of filters.brandSlug ?? []) {
      if (brandSlug?.trim()) {
        params = params.append('brandSlug', brandSlug.trim());
      }
    }

    for (const cpu of filters.cpu ?? []) {
      if (cpu?.trim()) {
        params = params.append('cpu', cpu.trim());
      }
    }

    for (const value of filters.ram ?? []) {
      params = params.append('ram', String(value));
    }

    for (const value of filters.storage ?? []) {
      params = params.append('storage', String(value));
    }

    for (const gpu of filters.gpu ?? []) {
      if (gpu?.trim()) {
        params = params.append('gpu', gpu.trim());
      }
    }

    for (const value of filters.refreshRate ?? []) {
      params = params.append('refreshRate', String(value));
    }

    for (const panelType of filters.panelType ?? []) {
      if (panelType?.trim()) {
        params = params.append('panelType', panelType.trim());
      }
    }

    for (const connectionType of filters.connectionType ?? []) {
      if (connectionType?.trim()) {
        params = params.append('connectionType', connectionType.trim());
      }
    }

    for (const switchType of filters.switchType ?? []) {
      if (switchType?.trim()) {
        params = params.append('switchType', switchType.trim());
      }
    }

    for (const value of filters.sizeInch ?? []) {
      params = params.append('sizeInch', String(value));
    }

    return params;
  }

  private buildCacheKey(filters: ProductFilterQuery = {}): string {
    const params = this.buildParams(filters);
    return params.keys().sort().map((key) => {
      const values = (params.getAll(key) ?? []).slice().sort();
      return `${key}=${values.join(',')}`;
    }).join('&');
  }

  getProducts(filters: ProductFilterQuery = {}): Observable<Product[]> {
    const params = this.buildParams(filters);

    return this.http.get<ApiResponse<Product[]>>('/api/products', { params }).pipe(
      map((res) => res.data ?? []),
    );
  }

  getProductFacets(filters: ProductFilterQuery = {}): Observable<ProductFacetGroup[]> {
    this.cleanupExpiredFacetCache();

    const cacheKey = this.buildCacheKey(filters);
    const cached = this.facetsCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) {
      return of(cached.data);
    }

    if (cached) {
      this.facetsCache.delete(cacheKey);
    }

    const params = this.buildParams(filters);

    return this.http.get<ApiResponse<ProductFacetGroup[]>>('/api/products/facets', { params }).pipe(
      map((res) => {
        const data = res.data ?? [];
        this.facetsCache.set(cacheKey, {
          data,
          expiresAt: Date.now() + CatalogService.FACETS_CACHE_TTL_MS,
        });
        this.trimFacetCacheIfNeeded();
        return data;
      }),
    );
  }

  invalidateFacetCache(): void {
    this.facetsCache.clear();
  }

  private cleanupExpiredFacetCache(): void {
    const now = Date.now();
    for (const [key, entry] of this.facetsCache.entries()) {
      if (entry.expiresAt <= now) {
        this.facetsCache.delete(key);
      }
    }
  }

  private trimFacetCacheIfNeeded(): void {
    if (this.facetsCache.size <= CatalogService.FACETS_CACHE_MAX_ENTRIES) {
      return;
    }

    this.cleanupExpiredFacetCache();
    if (this.facetsCache.size <= CatalogService.FACETS_CACHE_MAX_ENTRIES) {
      return;
    }

    this.facetsCache.clear();
  }

  getProductBySlug(slug: string): Observable<Product> {
    return this.http.get<ApiResponse<Product>>(`/api/products/slug/${slug}`).pipe(
      map((res) => {
        if (!res.data) {
          throw new Error('Không tìm thấy sản phẩm');
        }
        return res.data;
      }),
    );
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>('/api/categories').pipe(
      map((res) => res.data ?? []),
    );
  }

  getBrands(): Observable<Brand[]> {
    return this.http.get<ApiResponse<Brand[]>>('/api/brands').pipe(
      map((res) => res.data ?? []),
    );
  }
}
