package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.BrandRequest;
import com.ducthin.LetCookGear.dto.BrandResponse;
import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.entity.Brand;
import com.ducthin.LetCookGear.repository.BrandRepository;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> findAll() {
        List<BrandResponse> data = brandRepository.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thương hiệu thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thương hiệu thành công", toResponse(getBrandOrThrow(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> create(@Valid @RequestBody BrandRequest request) {
        Brand brand = new Brand();
        applyRequest(brand, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thương hiệu thành công", toResponse(brandRepository.save(brand))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        Brand brand = getBrandOrThrow(id);
        applyRequest(brand, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", toResponse(brandRepository.save(brand))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        brandRepository.delete(getBrandOrThrow(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa thương hiệu thành công"));
    }

    private Brand getBrandOrThrow(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu"));
    }

    private void applyRequest(Brand brand, BrandRequest request) {
        brand.setName(request.getName());
        brand.setSlug(request.getSlug());
        brand.setCountry(request.getCountry());
        brand.setActive(request.getIsActive() == null ? true : request.getIsActive());
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.getSlug(), brand.getCountry(), brand.isActive());
    }
}
