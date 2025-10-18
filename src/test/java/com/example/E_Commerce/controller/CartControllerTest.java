package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.CartService;
import com.example.E_Commerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;
    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
        setupTestData();
    }

    @Test
    void testViewCart_WithAuthentication() throws Exception {
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItems(1L)).thenReturn(cartItems);
        when(cartService.getCart(1L)).thenReturn(testCart);

        // When & Then
        mockMvc.perform(get("/cart")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("cartItems", cartItems))
                .andExpect(model().attributeExists("cartTotal"));

        verify(cartService).getCartItems(1L);
        verify(cartService).getCart(1L);
    }

    @Test
    void testAddToCart_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(productService.getProductById(1L)).thenReturn(testProduct);
        doNothing().when(cartService).addToCart(1L, 1L, 2);

        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "2")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("success"));

        verify(cartService).addToCart(1L, 1L, 2);
    }

    @Test
    void testAddToCart_ProductNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(productService.getProductById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "2")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("error"));

        verify(cartService, never()).addToCart(anyLong(), anyLong(), anyInt());
    }

    @Test
    void testUpdateCartItem_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(cartService).updateCartItemQuantity(1L, 1L, 3);

        // When & Then
        mockMvc.perform(post("/cart/update")
                        .with(csrf())
                        .param("cartItemId", "1")
                        .param("quantity", "3")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("success"));

        verify(cartService).updateCartItemQuantity(1L, 1L, 3);
    }

    @Test
    void testRemoveFromCart_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(cartService).removeFromCart(1L, 1L);

        // When & Then
        mockMvc.perform(post("/cart/remove")
                        .with(csrf())
                        .param("cartItemId", "1")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("success"));

        verify(cartService).removeFromCart(1L, 1L);
    }

    @Test
    void testClearCart_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(cartService).clearCart(1L);

        // When & Then
        mockMvc.perform(post("/cart/clear")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("success"));

        verify(cartService).clearCart(1L);
    }

    @Test
    void testGetCartItemCount_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItemCount(1L)).thenReturn(5);

        // When & Then
        mockMvc.perform(get("/cart/count")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(cartService).getCartItemCount(1L);
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

        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setCreatedAt(LocalDateTime.now());

        // Setup test cart item
        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setCart(testCart);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);
        // CartItem doesn't have setCreatedAt method
    }
}
