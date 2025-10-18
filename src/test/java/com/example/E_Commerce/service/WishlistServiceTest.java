package com.example.E_Commerce.service;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct;
    private Wishlist testWishlist;
    private WishlistItem testWishlistItem;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testGetOrCreateWishlist_ExistingWishlist() {
        // Given
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        // When
        Wishlist result = wishlistService.getOrCreateWishlist(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testWishlist, result);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void testGetOrCreateWishlist_NewWishlist() {
        // Given
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> {
            Wishlist wishlist = invocation.getArgument(0);
            wishlist.setId(1L);
            return wishlist;
        });

        // When
        Wishlist result = wishlistService.getOrCreateWishlist(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    void testAddToWishlist_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.existsByWishlistAndProduct(testWishlist, testProduct)).thenReturn(false);
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> {
            WishlistItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });

        // When
        Wishlist result = wishlistService.addToWishlist(1L, 1L);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(wishlistItemRepository).existsByWishlistAndProduct(testWishlist, testProduct);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void testAddToWishlist_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.addToWishlist(1L, 1L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void testAddToWishlist_ProductNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.addToWishlist(1L, 1L);
        });

        assertEquals("Product not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    void testAddToWishlist_ProductAlreadyExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.existsByWishlistAndProduct(testWishlist, testProduct)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.addToWishlist(1L, 1L);
        });

        assertEquals("Product is already in your wishlist", exception.getMessage());
        verify(wishlistItemRepository).existsByWishlistAndProduct(testWishlist, testProduct);
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void testRemoveFromWishlist_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.findByWishlistAndProduct(testWishlist, testProduct))
                .thenReturn(Optional.of(testWishlistItem));
        doNothing().when(wishlistItemRepository).delete(testWishlistItem);

        // When
        wishlistService.removeFromWishlist(1L, 1L);

        // Then
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistItemRepository).findByWishlistAndProduct(testWishlist, testProduct);
        verify(wishlistItemRepository).delete(testWishlistItem);
    }

    @Test
    void testRemoveFromWishlist_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.removeFromWishlist(1L, 1L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
    }

    @Test
    void testRemoveFromWishlist_WishlistNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.removeFromWishlist(1L, 1L);
        });

        assertEquals("Wishlist not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testRemoveFromWishlist_ProductNotInWishlist() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.findByWishlistAndProduct(testWishlist, testProduct))
                .thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.removeFromWishlist(1L, 1L);
        });

        assertEquals("Product not found in wishlist", exception.getMessage());
        verify(wishlistItemRepository).findByWishlistAndProduct(testWishlist, testProduct);
        verify(wishlistItemRepository, never()).delete(any(WishlistItem.class));
    }

    @Test
    void testGetWishlist_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        // When
        Wishlist result = wishlistService.getWishlist(1L);

        // Then
        assertNotNull(result);
        assertEquals(testWishlist, result);
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testGetWishlist_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.getWishlist(1L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetWishlist_WishlistNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.getWishlist(1L);
        });

        assertEquals("Wishlist not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testGetWishlistItems_Success() {
        // Given
        List<WishlistItem> wishlistItems = Arrays.asList(testWishlistItem);
        testWishlist.setWishlistItems(wishlistItems);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        // When
        List<WishlistItem> result = wishlistService.getWishlistItems(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testWishlistItem, result.get(0));
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testClearWishlist_Success() {
        // Given
        List<WishlistItem> wishlistItems = Arrays.asList(testWishlistItem);
        testWishlist.setWishlistItems(wishlistItems);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        doNothing().when(wishlistItemRepository).deleteAll(wishlistItems);

        // When
        wishlistService.clearWishlist(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistItemRepository).deleteAll(wishlistItems);
        assertTrue(testWishlist.getWishlistItems().isEmpty());
    }

    @Test
    void testClearWishlist_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.clearWishlist(1L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
    }

    @Test
    void testIsProductInWishlist_True() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.existsByWishlistAndProduct(testWishlist, testProduct)).thenReturn(true);

        // When
        boolean result = wishlistService.isProductInWishlist(1L, 1L);

        // Then
        assertTrue(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistItemRepository).existsByWishlistAndProduct(testWishlist, testProduct);
    }

    @Test
    void testIsProductInWishlist_False() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));
        when(wishlistItemRepository.existsByWishlistAndProduct(testWishlist, testProduct)).thenReturn(false);

        // When
        boolean result = wishlistService.isProductInWishlist(1L, 1L);

        // Then
        assertFalse(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistItemRepository).existsByWishlistAndProduct(testWishlist, testProduct);
    }

    @Test
    void testIsProductInWishlist_NoWishlist() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When
        boolean result = wishlistService.isProductInWishlist(1L, 1L);

        // Then
        assertFalse(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
        verify(wishlistItemRepository, never()).existsByWishlistAndProduct(any(), any());
    }

    @Test
    void testGetWishlistItemCount_Success() {
        // Given
        List<WishlistItem> wishlistItems = Arrays.asList(testWishlistItem, new WishlistItem());
        testWishlist.setWishlistItems(wishlistItems);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.of(testWishlist));

        // When
        int result = wishlistService.getWishlistItemCount(1L);

        // Then
        assertEquals(2, result);
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testGetWishlistItemCount_NoWishlist() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // When
        int result = wishlistService.getWishlistItemCount(1L);

        // Then
        assertEquals(0, result);
        verify(userRepository).findById(1L);
        verify(wishlistRepository).findByUser(testUser);
    }

    @Test
    void testGetWishlistItemCount_Exception() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        int result = wishlistService.getWishlistItemCount(1L);

        // Then
        assertEquals(0, result);
        verify(userRepository).findById(1L);
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

        // Setup test wishlist
        testWishlist = new Wishlist();
        testWishlist.setId(1L);
        testWishlist.setUser(testUser);
        testWishlist.setCreatedAt(LocalDateTime.now());

        // Setup test wishlist item
        testWishlistItem = new WishlistItem();
        testWishlistItem.setId(1L);
        testWishlistItem.setWishlist(testWishlist);
        testWishlistItem.setProduct(testProduct);
        testWishlistItem.setCreatedAt(LocalDateTime.now());
    }
}
