package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private DataInitializationService dataInitializationService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDashboard() throws Exception {
        // Given
        List<Product> products = Arrays.asList(createTestProduct());
        List<Order> orders = Arrays.asList(createTestOrder());
        List<User> users = Arrays.asList(createTestUser());

        when(productService.getAllProducts()).thenReturn(products);
        when(orderService.getAllOrders()).thenReturn(orders);
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalProducts", 1))
                .andExpect(model().attribute("totalOrders", 1))
                .andExpect(model().attribute("totalUsers", 1))
                .andExpect(model().attributeExists("recentOrders"));

        verify(productService).getAllProducts();
        verify(orderService).getAllOrders();
        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testProducts() throws Exception {
        // Given
        List<Product> products = Arrays.asList(createTestProduct());
        when(productService.getAllProducts()).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(model().attribute("products", products))
                .andExpect(model().attributeExists("categories"));

        verify(productService).getAllProducts();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testNewProductForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct_Success() throws Exception {
        // Given
        Product product = createTestProduct();
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        // When & Then
        mockMvc.perform(post("/admin/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", product.getName())
                        .param("description", product.getDescription())
                        .param("price", product.getPrice().toString())
                        .param("category", product.getCategory().toString())
                        .param("stockQuantity", product.getStockQuantity().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateProduct_ValidationError() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "") // Invalid: empty name
                        .param("description", "Test description")
                        .param("price", "10.00")
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"));

        verify(productService, never()).createProduct(any(Product.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateProduct_Success() throws Exception {
        // Given
        Product product = createTestProduct();
        product.setId(1L);
        when(productService.updateProduct(any(Product.class))).thenReturn(product);

        // When & Then
        mockMvc.perform(post("/admin/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", product.getName())
                        .param("description", product.getDescription())
                        .param("price", product.getPrice().toString())
                        .param("category", product.getCategory().toString())
                        .param("stockQuantity", product.getStockQuantity().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).updateProduct(any(Product.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteProduct() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(1L);

        // When & Then
        mockMvc.perform(post("/admin/products/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        verify(productService).deleteProduct(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testOrders() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(createTestOrder());
        when(orderService.getAllOrders()).thenReturn(orders);

        // When & Then
        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/orders"))
                .andExpect(model().attribute("orders", orders))
                .andExpect(model().attributeExists("orderStatuses"))
                .andExpect(model().attributeExists("paymentStatuses"));

        verify(orderService).getAllOrders();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testViewOrderDetail() throws Exception {
        // Given
        Order order = createTestOrder();
        order.setId(1L);
        when(orderService.getOrderById(1L)).thenReturn(order);

        // When & Then
        mockMvc.perform(get("/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order-detail"))
                .andExpect(model().attribute("order", order));

        verify(orderService).getOrderById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateOrderStatus_Success() throws Exception {
        // Given
        Order order = createTestOrder();
        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/admin/orders/1/status")
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).updateOrderStatus(1L, OrderStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateOrderStatus_Error() throws Exception {
        // Given
        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                .thenThrow(new RuntimeException("Order not found"));

        // When & Then
        mockMvc.perform(post("/admin/orders/1/status")
                        .with(csrf())
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).updateOrderStatus(1L, OrderStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUsers() throws Exception {
        // Given
        List<User> users = Arrays.asList(createTestUser());
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("users", users));

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testReports() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(createTestOrder());
        when(orderService.getAllOrders()).thenReturn(orders);

        // When & Then
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attribute("orders", orders))
                .andExpect(model().attributeExists("totalRevenue"));

        verify(orderService).getAllOrders();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testInitializeTestData() throws Exception {
        // Given
        List<Product> existingProducts = Arrays.asList(createTestProduct());
        when(productService.getAllProducts()).thenReturn(existingProducts);
        doNothing().when(productService).deleteProduct(anyLong());
        doNothing().when(dataInitializationService).initializeProducts();

        // When & Then
        mockMvc.perform(post("/admin/init-data")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("success"));

        verify(productService, times(2)).getAllProducts(); // Called twice: once to get existing products, once to get count
        verify(dataInitializationService).initializeProducts();
    }

    // Helper methods to create test data
    private Product createTestProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setCategory(ProductCategory.ELECTRONICS);
        product.setStockQuantity(10);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        return product;
    }

    private Order createTestOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123456");
        order.setUser(createTestUser());
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress("Test Address");
        order.setBillingAddress("Test Address");
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
