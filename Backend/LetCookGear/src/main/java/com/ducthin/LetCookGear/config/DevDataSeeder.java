package com.ducthin.LetCookGear.config;

import com.ducthin.LetCookGear.entity.Brand;
import com.ducthin.LetCookGear.entity.Cart;
import com.ducthin.LetCookGear.entity.CartItem;
import com.ducthin.LetCookGear.entity.Category;
import com.ducthin.LetCookGear.entity.CustomerOrder;
import com.ducthin.LetCookGear.entity.Inventory;
import com.ducthin.LetCookGear.entity.OrderItem;
import com.ducthin.LetCookGear.entity.Payment;
import com.ducthin.LetCookGear.entity.Product;
import com.ducthin.LetCookGear.entity.ProductVariant;
import com.ducthin.LetCookGear.entity.Role;
import com.ducthin.LetCookGear.entity.Shipment;
import com.ducthin.LetCookGear.entity.User;
import com.ducthin.LetCookGear.entity.Address;
import com.ducthin.LetCookGear.entity.enums.CartStatus;
import com.ducthin.LetCookGear.entity.enums.OrderStatus;
import com.ducthin.LetCookGear.entity.enums.PaymentMethod;
import com.ducthin.LetCookGear.entity.enums.PaymentStatus;
import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import com.ducthin.LetCookGear.entity.enums.ShipmentStatus;
import com.ducthin.LetCookGear.entity.enums.UserStatus;
import com.ducthin.LetCookGear.repository.AddressRepository;
import com.ducthin.LetCookGear.repository.BrandRepository;
import com.ducthin.LetCookGear.repository.CartItemRepository;
import com.ducthin.LetCookGear.repository.CartRepository;
import com.ducthin.LetCookGear.repository.CategoryRepository;
import com.ducthin.LetCookGear.repository.CustomerOrderRepository;
import com.ducthin.LetCookGear.repository.InventoryRepository;
import com.ducthin.LetCookGear.repository.OrderItemRepository;
import com.ducthin.LetCookGear.repository.PaymentRepository;
import com.ducthin.LetCookGear.repository.ProductRepository;
import com.ducthin.LetCookGear.repository.ProductVariantRepository;
import com.ducthin.LetCookGear.repository.RoleRepository;
import com.ducthin.LetCookGear.repository.ShipmentRepository;
import com.ducthin.LetCookGear.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.run-on-startup:false}")
    private boolean runOnStartup;

    @Value("${seed.admin.email:admin@letcookgear.local}")
    private String adminEmail;

    @Value("${seed.admin.password:}")
    private String adminPassword;

    @Value("${seed.admin.full-name:System Admin}")
    private String adminFullName;

    @Value("${seed.admin.phone:0900000000}")
    private String adminPhone;

    @Override
    public void run(String... args) {
        if (runOnStartup) {
            seedData();
        }
    }

    @Transactional
    public synchronized void seedData() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalArgumentException("SEED_ADMIN_PASSWORD is required for seeding admin account");
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            return roleRepository.save(role);
        });

        Role customerRole = roleRepository.findByName("CUSTOMER").orElseGet(() -> {
            Role role = new Role();
            role.setName("CUSTOMER");
            return roleRepository.save(role);
        });

        User adminUser = upsertUser(
                adminEmail,
                adminPassword,
                adminFullName,
                adminPhone,
                Set.of(adminRole, customerRole));

        List<User> customers = seedCustomers(customerRole);
        Map<String, Category> categories = seedCategories();
        Map<String, Brand> brands = seedBrands();
        List<ProductVariant> variants = seedCatalog(categories, brands);
        seedAddresses(customers);
        seedActiveCarts(customers, variants);
        seedOrders(customers, variants);

        // Keep admin as a customer too for quick cart/order testing in one account.
        if (!customers.stream().anyMatch(c -> c.getEmail().equalsIgnoreCase(adminUser.getEmail()))) {
            customers.add(adminUser);
        }
    }

    private User upsertUser(String email, String plainPassword, String fullName, String phone, Set<Role> roles) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private List<User> seedCustomers(Role customerRole) {
        List<User> users = new ArrayList<>();
        users.add(upsertUser("test1@letcookgear.local", "Password@123", "Nguyen Van An", "0901111001", Set.of(customerRole)));
        users.add(upsertUser("test2@letcookgear.local", "Password@123", "Tran Thi Bich", "0901111002", Set.of(customerRole)));
        users.add(upsertUser("test3@letcookgear.local", "Password@123", "Le Quoc Cuong", "0901111003", Set.of(customerRole)));
        users.add(upsertUser("test4@letcookgear.local", "Password@123", "Pham Minh Duc", "0901111004", Set.of(customerRole)));
        users.add(upsertUser("test5@letcookgear.local", "Password@123", "Do Thanh Ha", "0901111005", Set.of(customerRole)));
        return users;
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> bySlug = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            bySlug.put(category.getSlug(), category);
        }

        ensureCategory(bySlug, "laptop", "Laptop", null);
        ensureCategory(bySlug, "pc", "PC", null);
        ensureCategory(bySlug, "monitor", "Màn hình", null);
        ensureCategory(bySlug, "gaming-gear", "Gaming Gear", null);
        ensureCategory(bySlug, "cpu", "CPU", "pc");
        ensureCategory(bySlug, "gpu", "VGA", "pc");
        ensureCategory(bySlug, "keyboard", "Bàn phím", "gaming-gear");
        ensureCategory(bySlug, "mouse", "Chuột", "gaming-gear");
        ensureCategory(bySlug, "headset", "Tai nghe", "gaming-gear");
        ensureCategory(bySlug, "chair", "Ghế gaming", null);

        return bySlug;
    }

    private void ensureCategory(Map<String, Category> bySlug, String slug, String name, String parentSlug) {
        Category category = bySlug.getOrDefault(slug, new Category());
        category.setSlug(slug);
        category.setName(name);
        category.setActive(true);
        category.setParent(parentSlug == null ? null : bySlug.get(parentSlug));
        Category saved = categoryRepository.save(category);
        bySlug.put(slug, saved);
    }

    private Map<String, Brand> seedBrands() {
        Map<String, Brand> bySlug = new HashMap<>();
        for (Brand brand : brandRepository.findAll()) {
            bySlug.put(brand.getSlug(), brand);
        }

        ensureBrand(bySlug, "asus", "ASUS", "Taiwan");
        ensureBrand(bySlug, "msi", "MSI", "Taiwan");
        ensureBrand(bySlug, "lenovo", "Lenovo", "China");
        ensureBrand(bySlug, "acer", "Acer", "Taiwan");
        ensureBrand(bySlug, "gigabyte", "Gigabyte", "Taiwan");
        ensureBrand(bySlug, "dell", "Dell", "USA");
        ensureBrand(bySlug, "hp", "HP", "USA");
        ensureBrand(bySlug, "logitech", "Logitech", "Switzerland");
        ensureBrand(bySlug, "corsair", "Corsair", "USA");
        ensureBrand(bySlug, "razer", "Razer", "Singapore");
        return bySlug;
    }

    private void ensureBrand(Map<String, Brand> bySlug, String slug, String name, String country) {
        Brand brand = bySlug.getOrDefault(slug, new Brand());
        brand.setSlug(slug);
        brand.setName(name);
        brand.setCountry(country);
        brand.setActive(true);
        Brand saved = brandRepository.save(brand);
        bySlug.put(slug, saved);
    }

    private List<ProductVariant> seedCatalog(Map<String, Category> categories, Map<String, Brand> brands) {
        List<ProductVariant> variants = new ArrayList<>();
        variants.add(seedProductWithVariant(
                categories.get("laptop"),
                brands.get("asus"),
                "ASUS ROG Strix G16",
                "asus-rog-strix-g16",
                "Gaming laptop i7 RTX 4060, màn 240Hz.",
                "Thiết kế gaming mạnh, tản nhiệt tốt, phù hợp stream và thi đấu.",
                24,
                "I7 / 16GB / 1TB / RTX 4060",
                "ASUS-G16-I7-16-1TB-4060",
                36990000,
                38990000,
                2.35,
                42));
        variants.add(seedProductWithVariant(categories.get("laptop"), brands.get("msi"), "MSI Cyborg 15", "msi-cyborg-15",
                "Laptop gaming i5 RTX 4050 giá tốt.", "Cân bằng giữa giá và hiệu năng, phù hợp game thủ mới.", 24,
                "I5 / 16GB / 512GB / RTX 4050", "MSI-CYBORG15-I5-16-512-4050", 31990000, 33990000, 2.18, 36));
        variants.add(seedProductWithVariant(categories.get("laptop"), brands.get("lenovo"), "Lenovo Legion 5 15APH", "lenovo-legion-5-15aph",
                "Ryzen 7 RTX 4060, bàn phím full-size.", "Dòng Legion 5 nổi bật với hiệu năng ổn định và build chắc chắn.", 24,
                "R7 / 16GB / 1TB / RTX 4060", "LENOVO-LEGION5-R7-16-1TB-4060", 35990000, 37990000, 2.4, 28));
        variants.add(seedProductWithVariant(categories.get("laptop"), brands.get("acer"), "Acer Nitro V15", "acer-nitro-v15",
                "Laptop gaming i7 RTX 4050.", "Tối ưu gaming FHD 144Hz, phù hợp nhu cầu học tập và giải trí.", 24,
                "I7 / 16GB / 512GB / RTX 4050", "ACER-NITROV15-I7-16-512-4050", 29990000, 31990000, 2.25, 40));
        variants.add(seedProductWithVariant(categories.get("laptop"), brands.get("gigabyte"), "Gigabyte A16", "gigabyte-a16",
                "Ryzen AI 9, RTX 4060, màn 165Hz.", "Mẫu A16 thế hệ mới cho creator và game thủ.", 24,
                "R9 / 16GB / 1TB / RTX 4060", "GIGA-A16-R9-16-1TB-4060", 38990000, 41990000, 2.2, 18));

        variants.add(seedProductWithVariant(categories.get("pc"), brands.get("msi"), "PC Gaming MSI Core i5", "pc-gaming-msi-core-i5",
                "PC gaming RTX 4060 build sẵn.", "Case mid tower, tản khí, nâng cấp dễ dàng.", 36,
                "I5 13400F / 16GB / 1TB / RTX 4060", "PC-MSI-I5-16-1TB-4060", 26990000, 28990000, 8.5, 30));
        variants.add(seedProductWithVariant(categories.get("pc"), brands.get("asus"), "PC Gaming ASUS TUF", "pc-gaming-asus-tuf",
                "PC RTX 4070 cho game AAA.", "Tối ưu hiệu năng 2K, phù hợp stream và render cơ bản.", 36,
                "I7 14700F / 32GB / 1TB / RTX 4070", "PC-ASUS-I7-32-1TB-4070", 39990000, 42990000, 9.2, 16));

        variants.add(seedProductWithVariant(categories.get("monitor"), brands.get("dell"), "Dell G2724D", "dell-g2724d",
                "Màn hình 27 inch 2K 165Hz.", "Tấm nền IPS, độ trễ thấp, màu sắc cân bằng.", 24,
                "27 inch / 2K / 165Hz", "MON-DELL-G2724D-27-2K-165", 7290000, 7990000, 5.6, 55));
        variants.add(seedProductWithVariant(categories.get("monitor"), brands.get("asus"), "ASUS TUF VG27AQ3A", "asus-tuf-vg27aq3a",
                "Màn hình gaming 27 inch 180Hz.", "Mượt trong FPS, hỗ trợ Adaptive Sync.", 24,
                "27 inch / 2K / 180Hz", "MON-ASUS-VG27AQ3A-27-2K-180", 7990000, 8590000, 5.4, 32));

        variants.add(seedProductWithVariant(categories.get("keyboard"), brands.get("corsair"), "Corsair K70 RGB Pro", "corsair-k70-rgb-pro",
                "Bàn phím cơ switch linear.", "Layout full-size, keycap PBT, polling 8000Hz.", 24,
                "Full-size / Linear / RGB", "KB-CORSAIR-K70-RGB-PRO", 3590000, 3990000, 1.3, 70));
        variants.add(seedProductWithVariant(categories.get("keyboard"), brands.get("razer"), "Razer Huntsman V3", "razer-huntsman-v3",
                "Bàn phím optical cho esports.", "Rapid trigger, phản hồi cực nhanh.", 24,
                "TKL / Optical / RGB", "KB-RAZER-HUNTSMAN-V3-TKL", 4290000, 4690000, 1.1, 45));

        variants.add(seedProductWithVariant(categories.get("mouse"), brands.get("logitech"), "Logitech G Pro X Superlight 2", "logitech-gpro-x-superlight-2",
                "Chuột không dây siêu nhẹ.", "Sensor cao cấp, pin dài, phù hợp FPS competitive.", 24,
                "Wireless / 60g / HERO 2", "M-LOGI-GPROX-SUPERLIGHT2", 2990000, 3290000, 0.09, 95));
        variants.add(seedProductWithVariant(categories.get("mouse"), brands.get("razer"), "Razer Viper V3 Pro", "razer-viper-v3-pro",
                "Chuột flagship 8K.", "Thiết kế công thái học, tracking chính xác cao.", 24,
                "Wireless / 54g / 8K", "M-RAZER-VIPERV3-PRO", 3990000, 4290000, 0.08, 48));

        variants.add(seedProductWithVariant(categories.get("headset"), brands.get("logitech"), "Logitech G Pro X 2 Lightspeed", "logitech-gpro-x2-lightspeed",
                "Headset wireless cho game thủ.", "Driver graphene, mic rời, pin lâu.", 24,
                "Wireless / 50mm / DTS", "HS-LOGI-GPRO-X2", 4990000, 5490000, 0.35, 24));
        variants.add(seedProductWithVariant(categories.get("headset"), brands.get("corsair"), "Corsair HS80 Max", "corsair-hs80-max",
                "Headset không dây cao cấp.", "Âm trường tốt, đeo êm, phù hợp stream dài.", 24,
                "Wireless / Dolby / RGB", "HS-CORSAIR-HS80-MAX", 10000, 20000, 0.34, 22));

        variants.add(seedProductWithVariant(categories.get("chair"), brands.get("asus"), "ASUS ROG Destrier Ergo", "asus-rog-destrier-ergo",
                "Ghế công thái học cao cấp.", "Hỗ trợ lưng tốt, khung chắc chắn, phù hợp ngồi lâu.", 36,
                "Ergonomic / Mesh", "CH-ASUS-ROG-DESTRIER", 16990000, 17990000, 22.0, 8));

        return variants;
    }

    private ProductVariant seedProductWithVariant(
            Category category,
            Brand brand,
            String name,
            String slug,
            String shortDescription,
            String description,
            int warrantyMonths,
            String variantName,
            String sku,
            double price,
            double compareAtPrice,
            double weight,
            int inventoryQty) {

        Product product = productRepository.findBySlug(slug).orElseGet(Product::new);
        product.setCategory(category);
        product.setBrand(brand);
        product.setName(name);
        product.setSlug(slug);
        product.setShortDescription(shortDescription);
        product.setDescription(description);
        product.setWarrantyMonths(warrantyMonths);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        ProductVariant variant = productVariantRepository.findBySku(sku).orElseGet(ProductVariant::new);
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setVariantName(variantName);
        variant.setPrice(decimal(price));
        variant.setCompareAtPrice(decimal(compareAtPrice));
        variant.setWeight(decimal(weight));
        variant.setStatus(ProductStatus.ACTIVE);
        variant = productVariantRepository.save(variant);

        Inventory inventory = inventoryRepository.findByVariantId(variant.getId()).orElseGet(Inventory::new);
        inventory.setVariant(variant);
        inventory.setQuantityAvailable(inventoryQty);
        inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() == null ? 0 : inventory.getQuantityReserved()));
        inventory.setReorderLevel(5);
        inventoryRepository.save(inventory);

        return variant;
    }

    private void seedAddresses(List<User> users) {
        Set<Long> existingUserAddressIds = new HashSet<>();
        for (Address address : addressRepository.findAll()) {
            if (address.getUser() != null && address.getUser().getId() != null) {
                existingUserAddressIds.add(address.getUser().getId());
            }
        }

        int idx = 1;
        for (User user : users) {
            if (existingUserAddressIds.contains(user.getId())) {
                idx++;
                continue;
            }

            Address address = new Address();
            address.setUser(user);
            address.setReceiverName(user.getFullName());
            address.setPhone(user.getPhone());
            address.setProvince("Ho Chi Minh");
            address.setDistrict("Quan " + ((idx % 12) + 1));
            address.setWard("Phuong " + ((idx % 20) + 1));
            address.setDetail("So " + (10 + idx) + ", Duong Test Seed");
            address.setDefault(true);
            addressRepository.save(address);
            idx++;
        }
    }

    private void seedActiveCarts(List<User> users, List<ProductVariant> variants) {
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE).orElseGet(Cart::new);
            cart.setUser(user);
            cart.setStatus(CartStatus.ACTIVE);
            cart = cartRepository.save(cart);

            ProductVariant first = variants.get((i * 2) % variants.size());
            ProductVariant second = variants.get((i * 2 + 1) % variants.size());

            upsertCartItem(cart, first, 1 + (i % 2));
            upsertCartItem(cart, second, 1);
        }
    }

    private void upsertCartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId()).orElseGet(CartItem::new);
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(variant.getPrice());
        cartItemRepository.save(item);
    }

    private void seedOrders(List<User> users, List<ProductVariant> variants) {
        OrderStatus[] orderStatuses = {
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        };

        PaymentMethod[] paymentMethods = {
            PaymentMethod.COD,
            PaymentMethod.BANK_TRANSFER,
            PaymentMethod.VNPAY,
            PaymentMethod.MOMO,
            PaymentMethod.PAYOS
        };

        for (int i = 1; i <= 18; i++) {
            String orderCode = String.format("TEST-ORD-%04d", i);
            if (customerOrderRepository.existsByOrderCode(orderCode)) {
                continue;
            }

            User user = users.get(i % users.size());
            ProductVariant variant = variants.get(i % variants.size());
            int quantity = (i % 3) + 1;

            BigDecimal unitPrice = variant.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal shippingFee = decimal(150000);
            BigDecimal discountAmount = (i % 4 == 0) ? decimal(250000) : BigDecimal.ZERO;
            BigDecimal finalAmount = lineTotal.add(shippingFee).subtract(discountAmount);

            OrderStatus orderStatus = orderStatuses[i % orderStatuses.length];
            PaymentStatus paymentStatus = switch (orderStatus) {
                case DELIVERED, SHIPPED, PROCESSING, CONFIRMED -> PaymentStatus.PAID;
                case CANCELLED -> PaymentStatus.FAILED;
                default -> PaymentStatus.UNPAID;
            };

            CustomerOrder order = new CustomerOrder();
            order.setUser(user);
            order.setOrderCode(orderCode);
            order.setTotalAmount(lineTotal);
            order.setShippingFee(shippingFee);
            order.setDiscountAmount(discountAmount);
            order.setFinalAmount(finalAmount);
            order.setStatus(orderStatus);
            order.setPaymentStatus(paymentStatus);
            order.setPlacedAt(LocalDateTime.now().minusDays(i));
            order = customerOrderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(variant);
            orderItem.setProductSnapshotName(variant.getProduct().getName());
            orderItem.setSkuSnapshot(variant.getSku());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(quantity);
            orderItem.setLineTotal(lineTotal);
            orderItemRepository.save(orderItem);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setMethod(paymentMethods[i % paymentMethods.length]);
            payment.setTransactionRef("PAY-" + orderCode);
            payment.setAmount(finalAmount);
            payment.setStatus(paymentStatus == PaymentStatus.UNPAID ? PaymentStatus.PENDING : paymentStatus);
            payment.setPaidAt(paymentStatus == PaymentStatus.PAID ? LocalDateTime.now().minusDays(i).plusHours(2) : null);
            paymentRepository.save(payment);

            if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED) {
                Shipment shipment = new Shipment();
                shipment.setOrder(order);
                shipment.setCarrier("GHN");
                shipment.setTrackingCode("TRK" + String.format("%08d", i));
                shipment.setShippingStatus(orderStatus == OrderStatus.DELIVERED
                        ? ShipmentStatus.DELIVERED
                        : ShipmentStatus.IN_TRANSIT);
                shipment.setShippedAt(LocalDateTime.now().minusDays(i).plusHours(5));
                shipment.setDeliveredAt(orderStatus == OrderStatus.DELIVERED
                        ? LocalDateTime.now().minusDays(i - 1).plusHours(10)
                        : null);
                shipmentRepository.save(shipment);
            }
        }
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2);
    }
}
