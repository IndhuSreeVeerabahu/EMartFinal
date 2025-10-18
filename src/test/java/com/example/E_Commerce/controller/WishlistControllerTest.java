package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.CartService;
import com.example.E_Commerce.service.WishlistService;
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
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private CartService cartService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WishlistController wishlistController;

    private MockMvc mockMvc;
    private User testUser;
    private Product testProduct;
    private WishlistItem testWishlistItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(wishlistController).build();
        setupTestData();
    }

    @Test
    void testViewWishlist_Success() throws Exception {
        // Given
        List<WishlistItem> wishlistItems = Arrays.asList(testWishlistItem);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.getWishlistItems(1L)).thenReturn(wishlistItems);
        when(wishlistService.getWishlistItemCount(1L)).thenReturn(1);

        // When & Then
        mockMvc.perform(get("/wishlist")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("wishlist"))
                .andExpect(model().attribute("wishlistItems", wishlistItems))
                .andExpect(model().attribute("wishlistItemCount", 1));

        verify(wishlistService).getWishlistItems(1L);
        verify(wishlistService).getWishlistItemCount(1L);
    }

    @Test
    void testAddToWishlist_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.addToWishlist(1L, 1L)).thenReturn(new Wishlist());

        // When & Then
        mockMvc.perform(post("/wishlist/add")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/1"))
                .andExpect(flash().attributeExists("success"));

        verify(wishlistService).addToWishlist(1L, 1L);
    }

    @Test
    void testAddToWishlist_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.addToWishlist(1L, 1L))
                .thenThrow(new RuntimeException("Product is already in your wishlist"));

        // When & Then
        mockMvc.perform(post("/wishlist/add")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/1"))
                .andExpect(flash().attributeExists("error"));

        verify(wishlistService).addToWishlist(1L, 1L);
    }

    @Test
    void testRemoveFromWishlist_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(wishlistService).removeFromWishlist(1L, 1L);

        // When & Then
        mockMvc.perform(post("/wishlist/remove")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("success"));

        verify(wishlistService).removeFromWishlist(1L, 1L);
    }

    @Test
    void testRemoveFromWishlist_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doThrow(new RuntimeException("Product not found in wishlist"))
                .when(wishlistService).removeFromWishlist(1L, 1L);

        // When & Then
        mockMvc.perform(post("/wishlist/remove")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("error"));

        verify(wishlistService).removeFromWishlist(1L, 1L);
    }

    @Test
    void testClearWishlist_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(wishlistService).clearWishlist(1L);

        // When & Then
        mockMvc.perform(post("/wishlist/clear")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("success"));

        verify(wishlistService).clearWishlist(1L);
    }

    @Test
    void testClearWishlist_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doThrow(new RuntimeException("Wishlist not found"))
                .when(wishlistService).clearWishlist(1L);

        // When & Then
        mockMvc.perform(post("/wishlist/clear")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("error"));

        verify(wishlistService).clearWishlist(1L);
    }

    @Test
    void testMoveToCart_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doNothing().when(wishlistService).removeFromWishlist(1L, 1L);
        when(cartService.addToCart(1L, 1L, 1)).thenReturn(new Cart());

        // When & Then
        mockMvc.perform(post("/wishlist/move-to-cart")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("success"));

        verify(wishlistService).removeFromWishlist(1L, 1L);
        verify(cartService).addToCart(1L, 1L, 1);
    }

    @Test
    void testMoveToCart_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        doThrow(new RuntimeException("Product not found in wishlist"))
                .when(wishlistService).removeFromWishlist(1L, 1L);

        // When & Then
        mockMvc.perform(post("/wishlist/move-to-cart")
                        .with(csrf())
                        .principal(authentication)
                        .param("productId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wishlist"))
                .andExpect(flash().attributeExists("error"));

        verify(wishlistService).removeFromWishlist(1L, 1L);
        verify(cartService, never()).addToCart(anyLong(), anyLong(), anyInt());
    }

    @Test
    void testIsProductInWishlist_True() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.isProductInWishlist(1L, 1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/wishlist/check/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(wishlistService).isProductInWishlist(1L, 1L);
    }

    @Test
    void testIsProductInWishlist_False() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.isProductInWishlist(1L, 1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/wishlist/check/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(wishlistService).isProductInWishlist(1L, 1L);
    }

    @Test
    void testIsProductInWishlist_ServiceException() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(wishlistService.isProductInWishlist(1L, 1L))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/wishlist/check/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(wishlistService).isProductInWishlist(1L, 1L);
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

        // Setup test wishlist item
        Wishlist testWishlist = new Wishlist();
        testWishlist.setId(1L);
        testWishlist.setUser(testUser);
        testWishlist.setCreatedAt(LocalDateTime.now());

        testWishlistItem = new WishlistItem();
        testWishlistItem.setId(1L);
        testWishlistItem.setWishlist(testWishlist);
        testWishlistItem.setProduct(testProduct);
        testWishlistItem.setCreatedAt(LocalDateTime.now());
    }
}
