package com.example.E_Commerce.service;

import com.example.E_Commerce.model.Product;
import com.example.E_Commerce.model.ProductCategory;
import com.example.E_Commerce.model.User;
import com.example.E_Commerce.model.Role;
import com.example.E_Commerce.repository.ProductRepository;
import com.example.E_Commerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Temporarily disabled CommandLineRunner to fix startup hanging
    // @Override
    // public void run(String... args) throws Exception {
    //     initializeUsers();
    //     initializeProducts();
    // }

    private void initializeUsers() {
        if (userRepository.count() == 0) {
            // Create admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@ecommerce.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);

            // Create sample customer
            User customer = new User();
            customer.setUsername("customer");
            customer.setEmail("customer@ecommerce.com");
            customer.setPassword(passwordEncoder.encode("customer123"));
            customer.setFirstName("John");
            customer.setLastName("Doe");
            customer.setPhoneNumber("+1234567890");
            customer.setRole(Role.CUSTOMER);
            customer.setEnabled(true);
            userRepository.save(customer);
        }
    }

    public void initializeProducts() {
        try {
            System.out.println("Initializing products... Current count: " + productRepository.count());
            List<Product> sampleProducts = Arrays.asList(
                // Electronics - All under ₹500
                createProduct("Wireless Earbuds", "Bluetooth wireless earbuds with noise cancellation", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=400", 
                    50, ProductCategory.ELECTRONICS),
                
                createProduct("Phone Case", "Protective phone case with stand", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400", 
                    25, ProductCategory.ELECTRONICS),
                
                createProduct("USB Cable", "Fast charging USB-C cable 2m", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400", 
                    40, ProductCategory.ELECTRONICS),
                
                createProduct("Power Bank", "10000mAh portable power bank", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", 
                    30, ProductCategory.ELECTRONICS),

                createProduct("Screen Protector", "Tempered glass screen protector", 
                    new BigDecimal("99.00"), "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400", 
                    35, ProductCategory.ELECTRONICS),

                createProduct("Bluetooth Speaker", "Portable Bluetooth speaker", 
                    new BigDecimal("449.00"), "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400", 
                    20, ProductCategory.ELECTRONICS),

                createProduct("Phone Stand", "Adjustable phone stand with wireless charging", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=400", 
                    60, ProductCategory.ELECTRONICS),

                createProduct("Car Charger", "Dual port car charger with fast charging", 
                    new BigDecimal("179.00"), "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400", 
                    45, ProductCategory.ELECTRONICS),

                // Clothing - All under ₹500
                createProduct("Cotton T-Shirt", "Comfortable cotton t-shirt", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400", 
                    100, ProductCategory.CLOTHING),
                
                createProduct("Denim Jeans", "Classic blue denim jeans", 
                    new BigDecimal("499.00"), "https://images.unsplash.com/photo-1542272604-787c3835535d?w=400", 
                    50, ProductCategory.CLOTHING),
                
                createProduct("Hoodie", "Warm and cozy hoodie", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=400", 
                    30, ProductCategory.CLOTHING),
                
                createProduct("Sneakers", "Comfortable running sneakers", 
                    new BigDecimal("449.00"), "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=400", 
                    25, ProductCategory.CLOTHING),

                createProduct("Baseball Cap", "Stylish baseball cap", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=400", 
                    40, ProductCategory.CLOTHING),

                createProduct("Socks Pack", "Pack of 6 cotton socks", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=400", 
                    60, ProductCategory.CLOTHING),

                // Books - All under ₹500
                createProduct("Programming Book", "Learn Java programming", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400", 
                    20, ProductCategory.BOOKS),
                
                createProduct("Novel", "Bestselling fiction novel", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400", 
                    30, ProductCategory.BOOKS),
                
                createProduct("Cookbook", "Delicious recipes cookbook", 
                    new BigDecimal("349.00"), "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400", 
                    25, ProductCategory.BOOKS),
                
                createProduct("Self-Help Book", "Personal development guide", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400", 
                    35, ProductCategory.BOOKS),

                createProduct("Children's Book", "Fun storybook for kids", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400", 
                    40, ProductCategory.BOOKS),

                // Home & Garden - All under ₹500
                createProduct("Plant Pot", "Ceramic plant pot with drainage", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400", 
                    50, ProductCategory.HOME_AND_GARDEN),
                
                createProduct("Garden Tools", "Essential gardening tool set", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400", 
                    20, ProductCategory.HOME_AND_GARDEN),
                
                createProduct("LED Bulb", "Energy efficient LED bulb", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400", 
                    100, ProductCategory.HOME_AND_GARDEN),
                
                createProduct("Candles Set", "Aromatherapy candle set", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400", 
                    30, ProductCategory.HOME_AND_GARDEN),

                // Sports - All under ₹500
                createProduct("Yoga Mat", "Non-slip yoga mat", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400", 
                    25, ProductCategory.SPORTS),
                
                createProduct("Resistance Bands", "Set of resistance bands", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400", 
                    40, ProductCategory.SPORTS),
                
                createProduct("Water Bottle", "Insulated sports water bottle", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400", 
                    60, ProductCategory.SPORTS),
                
                createProduct("Jump Rope", "Adjustable jump rope", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400", 
                    35, ProductCategory.SPORTS),

                // Beauty - All under ₹500
                createProduct("Face Cream", "Moisturizing face cream", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400", 
                    50, ProductCategory.BEAUTY),
                
                createProduct("Lip Balm", "Natural lip balm set", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400", 
                    80, ProductCategory.BEAUTY),
                
                createProduct("Shampoo", "Organic hair shampoo", 
                    new BigDecimal("349.00"), "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400", 
                    40, ProductCategory.BEAUTY),
                
                createProduct("Face Mask", "Hydrating face mask pack", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400", 
                    30, ProductCategory.BEAUTY),

                // Toys - All under ₹500
                createProduct("Building Blocks", "Colorful building blocks set", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400", 
                    45, ProductCategory.TOYS),
                
                createProduct("Board Game", "Classic family board game", 
                    new BigDecimal("399.00"), "https://images.unsplash.com/photo-1606092195730-5d7b9af1efc5?w=400", 
                    25, ProductCategory.TOYS),
                
                createProduct("Puzzle Set", "1000-piece jigsaw puzzle", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1606092195730-5d7b9af1efc5?w=400", 
                    40, ProductCategory.TOYS),
                
                createProduct("Action Figure", "Collectible superhero action figure", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400", 
                    50, ProductCategory.TOYS),

                // Food - All under ₹500
                createProduct("Organic Honey", "Pure organic honey 500g", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400", 
                    100, ProductCategory.FOOD),
                
                createProduct("Coffee Beans", "Premium roasted coffee beans", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=400", 
                    80, ProductCategory.FOOD),
                
                createProduct("Chocolate Box", "Luxury assorted chocolate box", 
                    new BigDecimal("349.00"), "https://images.unsplash.com/photo-1511381939415-e44015466834?w=400", 
                    60, ProductCategory.FOOD),
                
                createProduct("Tea Collection", "Premium tea collection (20 bags)", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=400", 
                    70, ProductCategory.FOOD),

                // Health - All under ₹500
                createProduct("Multivitamin", "Daily multivitamin supplement", 
                    new BigDecimal("299.00"), "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400", 
                    90, ProductCategory.HEALTH),
                
                createProduct("Fish Oil", "Omega-3 fish oil capsules", 
                    new BigDecimal("249.00"), "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400", 
                    65, ProductCategory.HEALTH),
                
                createProduct("Vitamin D", "High-potency vitamin D3", 
                    new BigDecimal("199.00"), "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400", 
                    75, ProductCategory.HEALTH),
                
                createProduct("Thermometer", "Digital medical thermometer", 
                    new BigDecimal("149.00"), "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400", 
                    35, ProductCategory.HEALTH)
            );

            productRepository.saveAll(sampleProducts);
            System.out.println("Products saved successfully! Total products: " + productRepository.count());
        } catch (Exception e) {
            System.err.println("Error initializing products: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Product createProduct(String name, String description, BigDecimal price, 
                                String imageUrl, Integer stock, ProductCategory category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setStockQuantity(stock);
        product.setCategory(category);
        product.setActive(true);
        return product;
    }
}