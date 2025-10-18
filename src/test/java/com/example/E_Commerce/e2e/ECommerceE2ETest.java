package com.example.E_Commerce.e2e;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
import com.example.E_Commerce.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.hamcrest.Matchers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class ECommerceE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User testUser;
    private User adminUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        
        // Clear existing data before setting up test data
        try {
            cartItemRepository.deleteAll();
            cartRepository.deleteAll();
            orderRepository.deleteAll();
            productRepository.deleteAll();
            userRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors - tables might not exist yet
        }
        
        setupTestData();
    }

    @Test
    void testUserRegistrationAndLogin() throws Exception {
        // Test user registration
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "password123")
                        .param("firstName", "New")
                        .param("lastName", "User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success=true"));

        // Test login
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testProductBrowsing() throws Exception {
        // Test product listing
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("products"));

        // Test product detail view
        mockMvc.perform(get("/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testAddToCart() throws Exception {
        // Test adding product to cart
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", testProduct.getId().toString())
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        // Test cart view
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("cart"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testCheckoutProcess() throws Exception {
        // First add item to cart
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", testProduct.getId().toString())
                        .param("quantity", "1"));

        // Test checkout page
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeExists("cart"));

        // Test order placement
        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .param("shippingAddress", "123 Test Street, Test City")
                        .param("billingAddress", "123 Test Street, Test City"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/checkout/payment/*"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testOrderHistory() throws Exception {
        // Test orders page
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminDashboard() throws Exception {
        // Test admin dashboard
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalProducts"))
                .andExpect(model().attributeExists("totalOrders"))
                .andExpect(model().attributeExists("totalUsers"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminProductManagement() throws Exception {
        // Test product management page
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(model().attributeExists("products"));

        // Test new product form
        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeExists("product"));

        // Test product creation
        mockMvc.perform(post("/admin/products")
                        .with(csrf())
                        .param("name", "New Test Product")
                        .param("description", "New Test Description")
                        .param("price", "149.99")
                        .param("category", "ELECTRONICS")
                        .param("stockQuantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminOrderManagement() throws Exception {
        // Test order management page
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attributeExists("orders"));

        // Test order detail view - we need to create an order first
        Order testOrder = orderRepository.findAll().stream().findFirst().orElse(null);
        if (testOrder != null) {
            mockMvc.perform(get("/admin/orders/" + testOrder.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/order-detail"))
                    .andExpect(model().attributeExists("order"));

            // Test order status update
            mockMvc.perform(post("/admin/orders/" + testOrder.getId() + "/status")
                            .with(csrf())
                            .param("status", "CONFIRMED"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/orders"))
                    .andExpect(flash().attributeExists("success"));
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminUserManagement() throws Exception {
        // Test user management page
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));

        // Test new user form
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attributeExists("user"));

        // Test user creation
        mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .param("username", "newadmin")
                        .param("email", "newadmin@example.com")
                        .param("password", "password123")
                        .param("firstName", "New")
                        .param("lastName", "Admin")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminReports() throws Exception {
        // Test reports page
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("totalRevenue"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testExportFunctionality() throws Exception {
        // Test Excel export
        mockMvc.perform(get("/admin/export/orders/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                        Matchers.containsString("attachment")));

        // Test PDF export
        mockMvc.perform(get("/admin/export/orders/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                        Matchers.containsString("attachment")));

        // Test product Excel export
        mockMvc.perform(get("/admin/export/products/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                        Matchers.containsString("attachment")));

        // Test user Excel export
        mockMvc.perform(get("/admin/export/users/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", 
                        Matchers.containsString("attachment")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testProfileManagement() throws Exception {
        // Test profile page
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));

        // Test profile update
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("email", "updated@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void testSecurityAccess() throws Exception {
        // Test unauthorized access to admin pages
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Test unauthorized access to customer pages
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void testCompleteUserWorkflow() throws Exception {
        // Step 1: Browse products
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());

        // Step 2: View product details
        mockMvc.perform(get("/products/" + testProduct.getId()))
                .andExpect(status().isOk());

        // Step 3: Add to cart
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", testProduct.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection());

        // Step 4: View cart
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk());

        // Step 5: Proceed to checkout
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk());

        // Step 6: Place order
        mockMvc.perform(post("/checkout")
                        .with(csrf())
                        .param("shippingAddress", "123 Test Street, Test City")
                        .param("billingAddress", "123 Test Street, Test City"))
                .andExpect(status().is3xxRedirection());

        // Step 7: View order history
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }

    private void setupTestData() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory(ProductCategory.ELECTRONICS);
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);
        testProduct = productRepository.save(testProduct);

        // Create test cart
        Cart cart = new Cart();
        cart.setUser(testUser);
        cart = cartRepository.save(cart);

        // Create test cart item
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(testProduct);
        cartItem.setQuantity(1);
        cartItemRepository.save(cartItem);

        // Create test order
        Order order = new Order();
        order.setUser(testUser);
        order.setOrderNumber("ORD-123456");
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress("123 Test Street, Test City");
        order.setBillingAddress("123 Test Street, Test City");
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
