package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.ProductRequest;
import com.ducthin.LetCookGear.dto.ProductResponse;
import com.ducthin.LetCookGear.dto.ProductVariantSummaryResponse;
import com.ducthin.LetCookGear.entity.Brand;
import com.ducthin.LetCookGear.entity.Category;
import com.ducthin.LetCookGear.entity.Product;
import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import com.ducthin.LetCookGear.repository.BrandRepository;
import com.ducthin.LetCookGear.repository.CategoryRepository;
import com.ducthin.LetCookGear.repository.ProductRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> findAll(@RequestParam(required = false) String q) {
        List<ProductResponse> data = productRepository.findAllWithDetails().stream()
                .filter(product -> {
                    if (q == null || q.isBlank()) {
                        return true;
                    }
                    return product.getName().toLowerCase().contains(q.toLowerCase())
                            || product.getSlug().toLowerCase().contains(q.toLowerCase());
                })
                .map(this::toResponse)
                .toList();
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        Product product = productRepository
                .findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm thành công", toResponse(product)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> findBySlug(@PathVariable String slug) {
        Product product = productRepository
                .findBySlugWithDetails(slug)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        return ResponseEntity.ok(ApiResponse.success("Lấy sản phẩm thành công", toResponse(product)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo sản phẩm thành công", toResponse(productRepository.save(product))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = getProductOrThrow(id);
        applyRequest(product, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", toResponse(productRepository.save(product))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productRepository.delete(getProductOrThrow(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công"));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
    }

    private Brand getBrandOrThrow(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu"));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setWarrantyMonths(request.getWarrantyMonths() == null ? 0 : request.getWarrantyMonths());
        product.setStatus(request.getStatus() == null ? ProductStatus.DRAFT : request.getStatus());
        product.setCategory(getCategoryOrThrow(request.getCategoryId()));
        product.setBrand(getBrandOrThrow(request.getBrandId()));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getWarrantyMonths(),
                product.getStatus(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getVariants().stream()
                    .map(v -> new ProductVariantSummaryResponse(
                        v.getId(),
                        v.getSku(),
                        v.getVariantName(),
                        v.getPrice(),
                        v.getCompareAtPrice(),
                        v.getStatus()))
                    .toList());
    }
}
