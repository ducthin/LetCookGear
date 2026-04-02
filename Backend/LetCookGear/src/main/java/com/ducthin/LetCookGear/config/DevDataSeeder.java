package com.ducthin.LetCookGear.config;

import com.ducthin.LetCookGear.entity.Brand;
import com.ducthin.LetCookGear.entity.Category;
import com.ducthin.LetCookGear.entity.Inventory;
import com.ducthin.LetCookGear.entity.Product;
import com.ducthin.LetCookGear.entity.ProductVariant;
import com.ducthin.LetCookGear.entity.Role;
import com.ducthin.LetCookGear.entity.User;
import com.ducthin.LetCookGear.entity.enums.ProductStatus;
import com.ducthin.LetCookGear.entity.enums.UserStatus;
import com.ducthin.LetCookGear.repository.BrandRepository;
import com.ducthin.LetCookGear.repository.CategoryRepository;
import com.ducthin.LetCookGear.repository.InventoryRepository;
import com.ducthin.LetCookGear.repository.ProductRepository;
import com.ducthin.LetCookGear.repository.ProductVariantRepository;
import com.ducthin.LetCookGear.repository.RoleRepository;
import com.ducthin.LetCookGear.repository.UserRepository;
import java.math.BigDecimal;
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

        User adminUser = userRepository.findByEmail(adminEmail).orElseGet(User::new);
        adminUser.setEmail(adminEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUser.setFullName(adminFullName);
        adminUser.setPhone(adminPhone);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Set.of(adminRole, customerRole));
        userRepository.save(adminUser);

        Category laptopCategory = categoryRepository.findAll().stream()
                .filter(c -> "laptop".equals(c.getSlug()))
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName("Laptop");
                    category.setSlug("laptop");
                    category.setActive(true);
                    return categoryRepository.save(category);
                });

        Brand asusBrand = brandRepository.findAll().stream()
                .filter(b -> "asus".equals(b.getSlug()))
                .findFirst()
                .orElseGet(() -> {
                    Brand brand = new Brand();
                    brand.setName("ASUS");
                    brand.setSlug("asus");
                    brand.setCountry("Taiwan");
                    brand.setActive(true);
                    return brandRepository.save(brand);
                });

        Product product = productRepository.findBySlug("asus-tuf-gaming-a15").orElseGet(() -> {
            Product p = new Product();
            p.setName("ASUS TUF Gaming A15");
            p.setSlug("asus-tuf-gaming-a15");
            p.setShortDescription("Laptop gaming tầm trung cho dev và giải trí.");
            p.setDescription("Ryzen 7, RAM 16GB, SSD 512GB, RTX 4050.");
            p.setWarrantyMonths(24);
            p.setStatus(ProductStatus.ACTIVE);
            p.setCategory(laptopCategory);
            p.setBrand(asusBrand);
            return productRepository.save(p);
        });

        ProductVariant variant = productVariantRepository.findBySku("ASUS-A15-R7-16-512-4050")
                .orElseGet(() -> {
                    ProductVariant v = new ProductVariant();
                    v.setProduct(product);
                    v.setSku("ASUS-A15-R7-16-512-4050");
                    v.setVariantName("R7 / 16GB / 512GB / RTX4050");
                    v.setPrice(new BigDecimal("28990000"));
                    v.setCompareAtPrice(new BigDecimal("30990000"));
                    v.setWeight(new BigDecimal("2.200"));
                    v.setStatus(ProductStatus.ACTIVE);
                    return productVariantRepository.save(v);
                });

        if (!inventoryRepository.existsByVariantId(variant.getId())) {
            Inventory inventory = new Inventory();
            inventory.setVariant(variant);
            inventory.setQuantityAvailable(20);
            inventory.setQuantityReserved(0);
            inventory.setReorderLevel(5);
            inventoryRepository.save(inventory);
        }
    }
}
