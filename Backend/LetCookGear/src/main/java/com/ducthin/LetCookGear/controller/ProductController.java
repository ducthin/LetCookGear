package com.ducthin.LetCookGear.controller;

import com.ducthin.LetCookGear.dto.ApiResponse;
import com.ducthin.LetCookGear.dto.ProductRequest;
import com.ducthin.LetCookGear.dto.ProductResponse;
import com.ducthin.LetCookGear.dto.ProductVariantSummaryResponse;
import com.ducthin.LetCookGear.entity.Brand;
import com.ducthin.LetCookGear.entity.Category;
import com.ducthin.LetCookGear.entity.Product;
import com.ducthin.LetCookGear.entity.ProductVariant;
import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import com.ducthin.LetCookGear.repository.BrandRepository;
import com.ducthin.LetCookGear.repository.CategoryRepository;
import com.ducthin.LetCookGear.repository.ProductRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    public ResponseEntity<ApiResponse<List<ProductResponse>>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) List<String> brandSlug,
            @RequestParam(required = false) List<String> cpu,
            @RequestParam(required = false) List<Integer> ram,
            @RequestParam(required = false) List<Integer> storage,
            @RequestParam(required = false) List<String> gpu,
            @RequestParam(required = false) List<Integer> refreshRate,
            @RequestParam(required = false) List<String> panelType,
            @RequestParam(required = false) List<String> connectionType,
            @RequestParam(required = false) List<String> switchType,
            @RequestParam(required = false) List<BigDecimal> sizeInch) {

        VariantFilter filter = new VariantFilter(
                normalizeStrings(cpu),
                toSet(ram),
                toSet(storage),
                normalizeStrings(gpu),
                toSet(refreshRate),
                normalizeStrings(panelType),
                normalizeStrings(connectionType),
                normalizeStrings(switchType),
                toSet(sizeInch));

        List<ProductResponse> data = productRepository.findAllWithDetails().stream()
                .filter(product -> matchesKeyword(product, q))
                .filter(product -> matchesCategory(product, categorySlug))
                .filter(product -> matchesBrand(product, brandSlug))
                .filter(product -> matchesVariantFilters(product, filter))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", data));
    }

        @GetMapping("/facets")
        public ResponseEntity<ApiResponse<List<FacetGroupResponse>>> getFacets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) List<String> brandSlug,
            @RequestParam(required = false) List<String> cpu,
            @RequestParam(required = false) List<Integer> ram,
            @RequestParam(required = false) List<Integer> storage,
            @RequestParam(required = false) List<String> gpu,
            @RequestParam(required = false) List<Integer> refreshRate,
            @RequestParam(required = false) List<String> panelType,
            @RequestParam(required = false) List<String> connectionType,
            @RequestParam(required = false) List<String> switchType,
            @RequestParam(required = false) List<BigDecimal> sizeInch) {

        VariantFilter activeFilter = new VariantFilter(
            normalizeStrings(cpu),
            toSet(ram),
            toSet(storage),
            normalizeStrings(gpu),
            toSet(refreshRate),
            normalizeStrings(panelType),
            normalizeStrings(connectionType),
            normalizeStrings(switchType),
            toSet(sizeInch));

        List<Product> baseProducts = productRepository.findAllWithDetails().stream()
            .filter(product -> matchesKeyword(product, q))
            .filter(product -> matchesCategory(product, categorySlug))
            .filter(product -> matchesBrand(product, brandSlug))
            .toList();

        List<FacetGroupDef> facetDefs = getFacetDefinitions(categorySlug);
        List<FacetGroupResponse> groups = facetDefs.stream()
            .map(groupDef -> toFacetGroupResponse(groupDef, baseProducts, activeFilter))
            .filter(group -> !group.options().isEmpty())
            .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy bộ đếm bộ lọc thành công", groups));
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
                    .map(v -> {
                        VariantSpecs specs = extractSpecs(v);
                        return new ProductVariantSummaryResponse(
                                v.getId(),
                                v.getSku(),
                                v.getVariantName(),
                                v.getPrice(),
                                v.getCompareAtPrice(),
                                specs.cpuModel(),
                                specs.ramGb(),
                                specs.storageGb(),
                                specs.gpuModel(),
                                specs.refreshRateHz(),
                                specs.panelType(),
                                specs.connectionType(),
                                specs.switchType(),
                                specs.sizeInch(),
                                v.getStatus());
                    })
                    .toList());
    }

    private boolean matchesKeyword(Product product, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String keyword = normalize(q);
        if (normalize(product.getName()).contains(keyword)
                || normalize(product.getSlug()).contains(keyword)
                || normalize(product.getBrand().getName()).contains(keyword)
                || normalize(product.getCategory().getName()).contains(keyword)
                || normalize(product.getShortDescription()).contains(keyword)
                || normalize(product.getDescription()).contains(keyword)) {
            return true;
        }

        return product.getVariants().stream().anyMatch(variant -> variantContainsKeyword(variant, keyword));
    }

    private boolean variantContainsKeyword(ProductVariant variant, String keyword) {
        VariantSpecs specs = extractSpecs(variant);
        return normalize(variant.getVariantName()).contains(keyword)
                || normalize(variant.getSku()).contains(keyword)
            || normalize(specs.cpuModel()).contains(keyword)
            || normalize(specs.gpuModel()).contains(keyword)
            || normalize(specs.panelType()).contains(keyword)
            || normalize(specs.connectionType()).contains(keyword)
            || normalize(specs.switchType()).contains(keyword)
            || normalize(specs.ramGb()).contains(keyword)
            || normalize(specs.storageGb()).contains(keyword)
            || normalize(specs.refreshRateHz()).contains(keyword)
            || normalize(specs.sizeInch()).contains(keyword);
    }

    private boolean matchesCategory(Product product, String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) {
            return true;
        }
        return normalize(product.getCategory().getSlug()).equals(normalize(categorySlug));
    }

    private boolean matchesBrand(Product product, List<String> brandSlug) {
        if (brandSlug == null || brandSlug.isEmpty()) {
            return true;
        }

        String currentBrand = normalize(product.getBrand().getSlug());
        return brandSlug.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalize)
                .anyMatch(currentBrand::equals);
    }

    private boolean matchesVariantFilters(Product product, VariantFilter filter) {
        if (filter.isEmpty()) {
            return true;
        }
        return product.getVariants().stream().anyMatch(variant -> filter.matches(variant));
    }

    private FacetGroupResponse toFacetGroupResponse(FacetGroupDef groupDef, List<Product> baseProducts, VariantFilter activeFilter) {
        VariantFilter filterWithoutSelf = activeFilter.withoutGroup(groupDef.key());
        List<Product> candidates = baseProducts.stream()
                .filter(product -> matchesVariantFilters(product, filterWithoutSelf))
                .toList();

        List<FacetOptionResponse> options = new ArrayList<>();
        for (FacetOptionDef optionDef : groupDef.options()) {
            long count = candidates.stream()
                    .filter(product -> product.getVariants().stream()
                            .map(ProductController::extractSpecs)
                            .anyMatch(optionDef.matcher()))
                    .count();

            if (count > 0 || activeFilter.isSelected(groupDef.key(), optionDef.valueKey())) {
                options.add(new FacetOptionResponse(optionDef.valueKey(), optionDef.label(), count));
            }
        }

        return new FacetGroupResponse(groupDef.key(), groupDef.title(), options);
    }

    private List<FacetGroupDef> getFacetDefinitions(String categorySlug) {
        String slug = normalize(categorySlug);

        if (slug.contains("laptop")) {
            return LAPTOP_FACETS;
        }
        if (slug.contains("pc")) {
            return PC_FACETS;
        }
        if (slug.contains("monitor") || slug.contains("man-hinh")) {
            return MONITOR_FACETS;
        }
        if (slug.contains("gear") || slug.contains("keyboard") || slug.contains("mouse") || slug.contains("headset")) {
            return GEAR_FACETS;
        }

        return Collections.emptyList();
    }

    private Set<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(this::normalize)
                .collect(java.util.stream.Collectors.toSet());
    }

    private <T> Set<T> toSet(List<T> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        return values.stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
    }

    private String normalize(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private record VariantFilter(
            Set<String> cpu,
            Set<Integer> ram,
            Set<Integer> storage,
            Set<String> gpu,
            Set<Integer> refreshRate,
            Set<String> panelType,
            Set<String> connectionType,
            Set<String> switchType,
            Set<BigDecimal> sizeInch) {

        boolean isEmpty() {
            return cpu.isEmpty()
                    && ram.isEmpty()
                    && storage.isEmpty()
                    && gpu.isEmpty()
                    && refreshRate.isEmpty()
                    && panelType.isEmpty()
                    && connectionType.isEmpty()
                    && switchType.isEmpty()
                    && sizeInch.isEmpty();
        }

        VariantFilter withoutGroup(String groupKey) {
            return switch (groupKey) {
                case "cpu" -> new VariantFilter(Collections.emptySet(), ram, storage, gpu, refreshRate, panelType, connectionType, switchType, sizeInch);
                case "gpu" -> new VariantFilter(cpu, ram, storage, Collections.emptySet(), refreshRate, panelType, connectionType, switchType, sizeInch);
                case "ram" -> new VariantFilter(cpu, Collections.emptySet(), storage, gpu, refreshRate, panelType, connectionType, switchType, sizeInch);
                case "storage" -> new VariantFilter(cpu, ram, Collections.emptySet(), gpu, refreshRate, panelType, connectionType, switchType, sizeInch);
                case "refresh", "refreshRate" -> new VariantFilter(cpu, ram, storage, gpu, Collections.emptySet(), panelType, connectionType, switchType, sizeInch);
                case "panelType" -> new VariantFilter(cpu, ram, storage, gpu, refreshRate, Collections.emptySet(), connectionType, switchType, sizeInch);
                case "connection" -> new VariantFilter(cpu, ram, storage, gpu, refreshRate, panelType, Collections.emptySet(), switchType, sizeInch);
                case "switchType" -> new VariantFilter(cpu, ram, storage, gpu, refreshRate, panelType, connectionType, Collections.emptySet(), sizeInch);
                case "size" -> new VariantFilter(cpu, ram, storage, gpu, refreshRate, panelType, connectionType, switchType, Collections.emptySet());
                default -> this;
            };
        }

        boolean isSelected(String groupKey, String valueKey) {
            String key = valueKey == null ? "" : valueKey.trim().toLowerCase(Locale.ROOT);
            return switch (groupKey) {
                case "cpu" -> cpu.contains(key);
                case "gpu" -> gpu.contains(key);
                case "ram" -> tryParseInt(key).map(ram::contains).orElse(false);
                case "storage" -> tryParseInt(key).map(storage::contains).orElse(false);
                case "refresh", "refreshRate" -> tryParseInt(key).map(refreshRate::contains).orElse(false);
                case "panelType" -> panelType.contains(key);
                case "connection" -> connectionType.contains(key);
                case "switchType" -> switchType.contains(key);
                case "size" -> tryParseBigDecimal(key).map(value -> sizeInch.stream().anyMatch(s -> s.compareTo(value) == 0)).orElse(false);
                default -> false;
            };
        }

        boolean matches(ProductVariant variant) {
            VariantSpecs specs = extractSpecs(variant);
            return matchString(cpu, specs.cpuModel())
                && matchNumber(ram, specs.ramGb())
                && matchNumber(storage, specs.storageGb())
                && matchString(gpu, specs.gpuModel())
                && matchNumber(refreshRate, specs.refreshRateHz())
                && matchString(panelType, specs.panelType())
                && matchString(connectionType, specs.connectionType())
                && matchString(switchType, specs.switchType())
                && matchDecimal(sizeInch, specs.sizeInch());
        }

        private boolean matchString(Set<String> selected, String value) {
            if (selected.isEmpty()) {
                return true;
            }
            if (value == null || value.isBlank()) {
                return false;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return selected.stream().anyMatch(normalized::contains);
        }

        private boolean matchNumber(Set<Integer> selected, Integer value) {
            if (selected.isEmpty()) {
                return true;
            }
            return value != null && selected.contains(value);
        }

        private boolean matchDecimal(Set<BigDecimal> selected, BigDecimal value) {
            if (selected.isEmpty()) {
                return true;
            }
            if (value == null) {
                return false;
            }
            return selected.stream().anyMatch(s -> s.compareTo(value) == 0);
        }
    }

    private static final Pattern RAM_PATTERN = Pattern.compile("(\\d{1,2})\\s*GB");
    private static final Pattern STORAGE_PATTERN = Pattern.compile("(\\d)\\s*TB|(\\d{3,4})\\s*GB");
    private static final Pattern REFRESH_RATE_PATTERN = Pattern.compile("(\\d{2,3})\\s*HZ");
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d{2}(?:\\.\\d)?)\\s*INCH");

        private static final List<FacetGroupDef> LAPTOP_FACETS = List.of(
            new FacetGroupDef("cpu", "CPU", List.of(
                new FacetOptionDef("core i5", "Intel i5", specs -> contains(specs.cpuModel(), "core i5")),
                new FacetOptionDef("core i7", "Intel i7", specs -> contains(specs.cpuModel(), "core i7")),
                new FacetOptionDef("core i9", "Intel i9", specs -> contains(specs.cpuModel(), "core i9")),
                new FacetOptionDef("ryzen 5", "Ryzen 5", specs -> contains(specs.cpuModel(), "ryzen 5")),
                new FacetOptionDef("ryzen 7", "Ryzen 7", specs -> contains(specs.cpuModel(), "ryzen 7")),
                new FacetOptionDef("ryzen 9", "Ryzen 9", specs -> contains(specs.cpuModel(), "ryzen 9")))),
            new FacetGroupDef("gpu", "Card đồ họa", List.of(
                new FacetOptionDef("rtx 4050", "RTX 4050", specs -> contains(specs.gpuModel(), "rtx 4050")),
                new FacetOptionDef("rtx 4060", "RTX 4060", specs -> contains(specs.gpuModel(), "rtx 4060")),
                new FacetOptionDef("rtx 4070", "RTX 4070", specs -> contains(specs.gpuModel(), "rtx 4070")),
                new FacetOptionDef("rtx 4080", "RTX 4080", specs -> contains(specs.gpuModel(), "rtx 4080")),
                new FacetOptionDef("rtx 4090", "RTX 4090", specs -> contains(specs.gpuModel(), "rtx 4090")))),
            new FacetGroupDef("ram", "RAM", List.of(
                new FacetOptionDef("16", "16GB", specs -> equalsNumber(specs.ramGb(), 16)),
                new FacetOptionDef("24", "24GB", specs -> equalsNumber(specs.ramGb(), 24)),
                new FacetOptionDef("32", "32GB", specs -> equalsNumber(specs.ramGb(), 32)))),
            new FacetGroupDef("refreshRate", "Màn hình", List.of(
                new FacetOptionDef("144", "144Hz", specs -> equalsNumber(specs.refreshRateHz(), 144)),
                new FacetOptionDef("165", "165Hz", specs -> equalsNumber(specs.refreshRateHz(), 165)),
                new FacetOptionDef("240", "240Hz", specs -> equalsNumber(specs.refreshRateHz(), 240)))));

        private static final List<FacetGroupDef> PC_FACETS = List.of(
            new FacetGroupDef("cpu", "CPU", LAPTOP_FACETS.get(0).options()),
            new FacetGroupDef("gpu", "GPU", List.of(
                new FacetOptionDef("rtx 4060", "RTX 4060", specs -> contains(specs.gpuModel(), "rtx 4060")),
                new FacetOptionDef("rtx 4070", "RTX 4070", specs -> contains(specs.gpuModel(), "rtx 4070")),
                new FacetOptionDef("rtx 4080", "RTX 4080+", specs -> contains(specs.gpuModel(), "rtx 4080") || contains(specs.gpuModel(), "rtx 4090")))),
            new FacetGroupDef("ram", "RAM", List.of(
                new FacetOptionDef("16", "16GB", specs -> equalsNumber(specs.ramGb(), 16)),
                new FacetOptionDef("32", "32GB", specs -> equalsNumber(specs.ramGb(), 32)),
                new FacetOptionDef("64", "64GB", specs -> equalsNumber(specs.ramGb(), 64)))),
            new FacetGroupDef("storage", "Lưu trữ", List.of(
                new FacetOptionDef("512", "512GB", specs -> equalsNumber(specs.storageGb(), 512)),
                new FacetOptionDef("1024", "1TB", specs -> equalsNumber(specs.storageGb(), 1024)),
                new FacetOptionDef("2048", "2TB", specs -> equalsNumber(specs.storageGb(), 2048)))));

        private static final List<FacetGroupDef> MONITOR_FACETS = List.of(
            new FacetGroupDef("size", "Kích thước", List.of(
                new FacetOptionDef("24", "24 inch", specs -> equalsDecimal(specs.sizeInch(), 24)),
                new FacetOptionDef("27", "27 inch", specs -> equalsDecimal(specs.sizeInch(), 27)),
                new FacetOptionDef("32", "32 inch", specs -> equalsDecimal(specs.sizeInch(), 32)))),
            new FacetGroupDef("refresh", "Tần số quét", List.of(
                new FacetOptionDef("144", "144Hz", specs -> equalsNumber(specs.refreshRateHz(), 144)),
                new FacetOptionDef("165", "165Hz", specs -> equalsNumber(specs.refreshRateHz(), 165)),
                new FacetOptionDef("180", "180Hz+", specs -> specs.refreshRateHz() != null && specs.refreshRateHz() >= 180))),
            new FacetGroupDef("panelType", "Tấm nền", List.of(
                new FacetOptionDef("ips", "IPS", specs -> contains(specs.panelType(), "ips")),
                new FacetOptionDef("oled", "OLED", specs -> contains(specs.panelType(), "oled")),
                new FacetOptionDef("va", "VA", specs -> contains(specs.panelType(), "va")))));

        private static final List<FacetGroupDef> GEAR_FACETS = List.of(
            new FacetGroupDef("connection", "Kết nối", List.of(
                new FacetOptionDef("wireless", "Wireless", specs -> contains(specs.connectionType(), "wireless")),
                new FacetOptionDef("wired", "Wired", specs -> contains(specs.connectionType(), "wired")))),
            new FacetGroupDef("switchType", "Switch", List.of(
                new FacetOptionDef("optical", "Optical", specs -> contains(specs.switchType(), "optical")),
                new FacetOptionDef("linear", "Linear", specs -> contains(specs.switchType(), "linear")),
                new FacetOptionDef("tactile", "Tactile", specs -> contains(specs.switchType(), "tactile")))));

        private static boolean contains(String source, String expected) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(expected);
        }

        private static boolean equalsNumber(Integer source, int value) {
        return source != null && source == value;
        }

        private static boolean equalsDecimal(BigDecimal source, int value) {
        return source != null && source.compareTo(BigDecimal.valueOf(value)) == 0;
        }

        private static Optional<Integer> tryParseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        }

        private static Optional<BigDecimal> tryParseBigDecimal(String value) {
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        }

    private static VariantSpecs extractSpecs(ProductVariant variant) {
        String normalized = normalizeForParse(variant.getVariantName() + " " + variant.getSku());

        String cpu = detectCpu(normalized).orElse(null);
        Integer ram = detectRamGb(normalized).orElse(null);
        Integer storage = detectStorageGb(normalized).orElse(null);
        String gpu = detectGpu(normalized).orElse(null);
        Integer refreshRate = detectRefreshRate(normalized).orElse(null);
        String panel = detectPanelType(normalized).orElse(null);
        String connection = detectConnectionType(normalized).orElse(null);
        String switchType = detectSwitchType(normalized).orElse(null);
        BigDecimal sizeInch = detectSizeInch(normalized).orElse(null);

        return new VariantSpecs(cpu, ram, storage, gpu, refreshRate, panel, connection, switchType, sizeInch);
    }

    private static String normalizeForParse(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private static Optional<String> detectCpu(String text) {
        if (text.contains("R9") || text.contains("RYZEN 9")) {
            return Optional.of("Ryzen 9");
        }
        if (text.contains("R7") || text.contains("RYZEN 7")) {
            return Optional.of("Ryzen 7");
        }
        if (text.contains("I9") || text.contains("CORE I9")) {
            return Optional.of("Core i9");
        }
        if (text.contains("I7") || text.contains("CORE I7")) {
            return Optional.of("Core i7");
        }
        if (text.contains("I5") || text.contains("CORE I5")) {
            return Optional.of("Core i5");
        }
        return Optional.empty();
    }

    private static Optional<Integer> detectRamGb(String text) {
        Matcher matcher = RAM_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static Optional<Integer> detectStorageGb(String text) {
        Matcher matcher = STORAGE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }

        if (matcher.group(1) != null) {
            return Optional.of(Integer.parseInt(matcher.group(1)) * 1024);
        }
        return Optional.of(Integer.parseInt(matcher.group(2)));
    }

    private static Optional<String> detectGpu(String text) {
        if (text.contains("RTX 4070") || text.contains("-4070")) {
            return Optional.of("RTX 4070");
        }
        if (text.contains("RTX 4060") || text.contains("-4060")) {
            return Optional.of("RTX 4060");
        }
        if (text.contains("RTX 4050") || text.contains("-4050")) {
            return Optional.of("RTX 4050");
        }
        return Optional.empty();
    }

    private static Optional<Integer> detectRefreshRate(String text) {
        Matcher matcher = REFRESH_RATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static Optional<String> detectPanelType(String text) {
        if (text.contains("IPS")) {
            return Optional.of("IPS");
        }
        if (text.contains("OLED")) {
            return Optional.of("OLED");
        }
        if (text.contains("VA")) {
            return Optional.of("VA");
        }
        return Optional.empty();
    }

    private static Optional<String> detectConnectionType(String text) {
        if (text.contains("WIRELESS") || text.contains("LIGHTSPEED") || text.contains("2.4G")) {
            return Optional.of("Wireless");
        }
        if (text.contains("WIRED") || text.contains("USB") || text.contains("DISPLAYPORT")) {
            return Optional.of("Wired");
        }
        return Optional.empty();
    }

    private static Optional<String> detectSwitchType(String text) {
        if (text.contains("OPTICAL")) {
            return Optional.of("Optical");
        }
        if (text.contains("LINEAR")) {
            return Optional.of("Linear");
        }
        if (text.contains("TACTILE")) {
            return Optional.of("Tactile");
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> detectSizeInch(String text) {
        Matcher matcher = SIZE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(new BigDecimal(matcher.group(1)));
        }

        if (text.contains("27")) {
            return Optional.of(new BigDecimal("27.0"));
        }
        if (text.contains("16")) {
            return Optional.of(new BigDecimal("16.0"));
        }
        if (text.contains("15")) {
            return Optional.of(new BigDecimal("15.6"));
        }
        return Optional.empty();
    }

    private record VariantSpecs(
            String cpuModel,
            Integer ramGb,
            Integer storageGb,
            String gpuModel,
            Integer refreshRateHz,
            String panelType,
            String connectionType,
            String switchType,
            BigDecimal sizeInch) {
    }

    private record FacetOptionDef(String valueKey, String label, Predicate<VariantSpecs> matcher) {
    }

    private record FacetGroupDef(String key, String title, List<FacetOptionDef> options) {
    }

    private record FacetOptionResponse(String valueKey, String label, long count) {
    }

    private record FacetGroupResponse(String key, String title, List<FacetOptionResponse> options) {
    }
}
