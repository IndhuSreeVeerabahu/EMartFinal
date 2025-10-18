package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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
class HomeControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private UserService userService;

    @Mock
    private WishlistService wishlistService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
        setupTestData();
    }

    @Test
    void testHome_WithAuthentication() throws Exception {
        // Given
        List<Product> featuredProducts = Arrays.asList(testProduct);
        when(productService.getAvailableProducts()).thenReturn(featuredProducts);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItemCount(1L)).thenReturn(3);
        when(wishlistService.getWishlistItemCount(1L)).thenReturn(2);

        // When & Then
        mockMvc.perform(get("/")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("featuredProducts", featuredProducts))
                .andExpect(model().attribute("cartItemCount", 3))
                .andExpect(model().attribute("wishlistItemCount", 2));

        verify(productService).getAvailableProducts();
        verify(cartService).getCartItemCount(1L);
        verify(wishlistService).getWishlistItemCount(1L);
    }

    @Test
    void testHome_WithoutAuthentication() throws Exception {
        // Given
        List<Product> featuredProducts = Arrays.asList(testProduct);
        when(productService.getAvailableProducts()).thenReturn(featuredProducts);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("featuredProducts", featuredProducts))
                .andExpect(model().attributeDoesNotExist("cartItemCount"))
                .andExpect(model().attributeDoesNotExist("wishlistItemCount"));

        verify(productService).getAvailableProducts();
        verifyNoInteractions(cartService);
        verifyNoInteractions(wishlistService);
    }

    @Test
    void testHomePage() throws Exception {
        // Given
        List<Product> featuredProducts = Arrays.asList(testProduct);
        when(productService.getAvailableProducts()).thenReturn(featuredProducts);

        // When & Then
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("featuredProducts", featuredProducts));

        verify(productService).getAvailableProducts();
    }

    @Test
    void testProducts_DefaultParameters() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getAllProducts(pageable)).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("products", productPage))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("category", ""))
                .andExpect(model().attributeExists("categories"));

        verify(productService).getAllProducts(pageable);
    }

    @Test
    void testProducts_WithSearch() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.searchProducts("laptop", pageable)).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products")
                        .param("search", "laptop"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("products", productPage))
                .andExpect(model().attribute("search", "laptop"));

        verify(productService).searchProducts("laptop", pageable);
    }

    @Test
    void testProducts_WithCategory() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getProductsByCategory(ProductCategory.ELECTRONICS, pageable)).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products")
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("products", productPage))
                .andExpect(model().attribute("category", "ELECTRONICS"));

        verify(productService).getProductsByCategory(ProductCategory.ELECTRONICS, pageable);
    }

    @Test
    void testProducts_WithPriceRange() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getProductsByPriceRange(new BigDecimal("100"), new BigDecimal("500"), pageable))
                .thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products")
                        .param("minPrice", "100")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("products", productPage))
                .andExpect(model().attribute("minPrice", new BigDecimal("100")))
                .andExpect(model().attribute("maxPrice", new BigDecimal("500")));

        verify(productService).getProductsByPriceRange(new BigDecimal("100"), new BigDecimal("500"), pageable);
    }

    @Test
    void testProducts_WithPagination() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(1, 6);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.getAllProducts(pageable)).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products")
                        .param("page", "1")
                        .param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("products", productPage));

        verify(productService).getAllProducts(pageable);
    }

    @Test
    void testProductDetail_Success() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attribute("product", testProduct));

        verify(productService).getProductById(1L);
    }

    @Test
    void testProductDetail_WithAuthentication() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItemCount(1L)).thenReturn(2);
        when(wishlistService.getWishlistItemCount(1L)).thenReturn(1);

        // When & Then
        mockMvc.perform(get("/product/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attribute("product", testProduct))
                .andExpect(model().attribute("cartItemCount", 2))
                .andExpect(model().attribute("wishlistItemCount", 1));

        verify(productService).getProductById(1L);
        verify(cartService).getCartItemCount(1L);
        verify(wishlistService).getWishlistItemCount(1L);
    }

    @Test
    void testProductDetail_ProductNotFound() throws Exception {
        // Given
        when(productService.getProductById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/product/1"))
                .andExpect(status().isBadRequest());

        verify(productService).getProductById(1L);
    }

    @Test
    void testRegisterForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void testRegister_Success() throws Exception {
        // Given
        when(userService.registerUser(any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("firstName", "Test")
                        .param("lastName", "User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    void testRegister_ValidationError() throws Exception {
        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "")
                        .param("email", "invalid-email")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    void testRegister_ServiceException() throws Exception {
        // Given
        when(userService.registerUser(any(User.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("firstName", "Test")
                        .param("lastName", "User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("error"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    void testLoginForm_WithoutError() throws Exception {
        // When & Then
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void testLoginForm_WithError() throws Exception {
        // When & Then
        mockMvc.perform(get("/login")
                        .param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testProfile() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/profile")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", testUser));
    }

    @Test
    void testUpdateProfile_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .principal(authentication)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("phoneNumber", "9876543210"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).updateUser(any(User.class));
    }

    @Test
    void testUpdateProfile_ValidationError() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .principal(authentication)
                        .param("firstName", "")
                        .param("lastName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void testUpdateProfile_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(userService.updateUser(any(User.class)))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .principal(authentication)
                        .param("firstName", "Updated")
                        .param("lastName", "Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("error"));

        verify(userService).updateUser(any(User.class));
    }

    private void setupTestData() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Setup test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory(ProductCategory.ELECTRONICS);
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);
        testProduct.setCreatedAt(LocalDateTime.now());
    }
}
