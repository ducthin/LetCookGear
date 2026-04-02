package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.CategoryRequest;
import com.ducthin.LetCookGear.dto.CategoryResponse;
import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.entity.Category;
import com.ducthin.LetCookGear.repository.CategoryRepository;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll() {
        List<CategoryResponse> data = categoryRepository.findAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", toResponse(getCategoryOrThrow(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        Category category = new Category();
        applyRequest(category, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục thành công", toResponse(categoryRepository.save(category))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        applyRequest(category, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", toResponse(categoryRepository.save(category))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryRepository.delete(getCategoryOrThrow(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công"));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParent(request.getParentId() == null ? null : getCategoryOrThrow(request.getParentId()));
        category.setActive(request.getIsActive() == null ? true : request.getIsActive());
    }

    private CategoryResponse toResponse(Category category) {
        Long parentId = category.getParent() == null ? null : category.getParent().getId();
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), parentId, category.isActive());
    }
}
