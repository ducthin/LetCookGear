import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import { Brand, Category, Product } from '../models/catalog.model';

@Injectable({
  providedIn: 'root',
})
export class CatalogService {
  constructor(private readonly http: HttpClient) {}

  getProducts(search: string = ''): Observable<Product[]> {
    let params = new HttpParams();
    if (search.trim()) {
      params = params.set('q', search.trim());
    }

    return this.http.get<ApiResponse<Product[]>>('/api/products', { params }).pipe(
      map((res) => res.data ?? []),
    );
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
