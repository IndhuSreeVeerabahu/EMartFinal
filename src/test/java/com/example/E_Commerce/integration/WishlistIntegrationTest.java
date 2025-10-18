package com.example.E_Commerce.integration;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
import com.example.E_Commerce.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class WishlistIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testWishlistWorkflow_AddRemoveItems() {
        // Given - User and products are already set up

        // When - Add first product to wishlist
        Wishlist wishlist = wishlistService.addToWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify wishlist was created and item was added
        assertNotNull(wishlist);
        assertNotNull(wishlist.getId());
        assertEquals(testUser, wishlist.getUser());
        assertEquals(1, wishlist.getTotalItems());

        // When - Add second product to wishlist
        wishlist = wishlistService.addToWishlist(testUser.getId(), testProduct2.getId());

        // Then - Verify second item was added
        assertEquals(2, wishlist.getTotalItems());

        // When - Get wishlist items
        List<WishlistItem> wishlistItems = wishlistService.getWishlistItems(testUser.getId());

        // Then - Verify both items are in wishlist
        assertEquals(2, wishlistItems.size());
        assertTrue(wishlistItems.stream().anyMatch(item -> item.getProduct().equals(testProduct1)));
        assertTrue(wishlistItems.stream().anyMatch(item -> item.getProduct().equals(testProduct2)));

        // When - Remove first product from wishlist
        wishlistService.removeFromWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify first item was removed
        wishlistItems = wishlistService.getWishlistItems(testUser.getId());
        assertEquals(1, wishlistItems.size());
        assertTrue(wishlistItems.stream().anyMatch(item -> item.getProduct().equals(testProduct2)));
        assertFalse(wishlistItems.stream().anyMatch(item -> item.getProduct().equals(testProduct1)));

        // When - Clear wishlist
        wishlistService.clearWishlist(testUser.getId());

        // Then - Verify wishlist is empty
        wishlistItems = wishlistService.getWishlistItems(testUser.getId());
        assertEquals(0, wishlistItems.size());
    }

    @Test
    void testWishlistWorkflow_DuplicatePrevention() {
        // Given - User and product are already set up

        // When - Add product to wishlist first time
        wishlistService.addToWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify item was added
        assertTrue(wishlistService.isProductInWishlist(testUser.getId(), testProduct1.getId()));

        // When - Try to add same product again
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            wishlistService.addToWishlist(testUser.getId(), testProduct1.getId());
        });

        // Then - Verify duplicate prevention
        assertEquals("Product is already in your wishlist", exception.getMessage());
        assertEquals(1, wishlistService.getWishlistItemCount(testUser.getId()));
    }

    @Test
    void testWishlistWorkflow_GetOrCreateWishlist() {
        // Given - User is set up but no wishlist exists

        // When - Get or create wishlist
        Wishlist wishlist = wishlistService.getOrCreateWishlist(testUser);

        // Then - Verify wishlist was created
        assertNotNull(wishlist);
        assertNotNull(wishlist.getId());
        assertEquals(testUser, wishlist.getUser());
        assertEquals(0, wishlist.getTotalItems());

        // When - Get or create wishlist again
        Wishlist sameWishlist = wishlistService.getOrCreateWishlist(testUser);

        // Then - Verify same wishlist is returned
        assertEquals(wishlist.getId(), sameWishlist.getId());
    }

    @Test
    void testWishlistWorkflow_ItemCountTracking() {
        // Given - User and products are set up

        // When - Initially no items
        int initialCount = wishlistService.getWishlistItemCount(testUser.getId());

        // Then - Verify count is 0
        assertEquals(0, initialCount);

        // When - Add first product
        wishlistService.addToWishlist(testUser.getId(), testProduct1.getId());
        int countAfterFirst = wishlistService.getWishlistItemCount(testUser.getId());

        // Then - Verify count is 1
        assertEquals(1, countAfterFirst);

        // When - Add second product
        wishlistService.addToWishlist(testUser.getId(), testProduct2.getId());
        int countAfterSecond = wishlistService.getWishlistItemCount(testUser.getId());

        // Then - Verify count is 2
        assertEquals(2, countAfterSecond);

        // When - Remove one product
        wishlistService.removeFromWishlist(testUser.getId(), testProduct1.getId());
        int countAfterRemove = wishlistService.getWishlistItemCount(testUser.getId());

        // Then - Verify count is 1
        assertEquals(1, countAfterRemove);
    }

    @Test
    void testWishlistWorkflow_ProductInWishlistCheck() {
        // Given - User and products are set up

        // When - Initially check if product is in wishlist
        boolean initiallyInWishlist = wishlistService.isProductInWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify product is not in wishlist
        assertFalse(initiallyInWishlist);

        // When - Add product to wishlist
        wishlistService.addToWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify product is now in wishlist
        assertTrue(wishlistService.isProductInWishlist(testUser.getId(), testProduct1.getId()));

        // When - Remove product from wishlist
        wishlistService.removeFromWishlist(testUser.getId(), testProduct1.getId());

        // Then - Verify product is no longer in wishlist
        assertFalse(wishlistService.isProductInWishlist(testUser.getId(), testProduct1.getId()));
    }

    @Test
    void testWishlistWorkflow_ErrorHandling() {
        // Given - User and products are set up

        // When - Try to add non-existent product
        RuntimeException exception1 = assertThrows(RuntimeException.class, () -> {
            wishlistService.addToWishlist(testUser.getId(), 999L);
        });

        // Then - Verify error handling
        assertEquals("Product not found", exception1.getMessage());

        // When - Try to remove non-existent product
        RuntimeException exception2 = assertThrows(RuntimeException.class, () -> {
            wishlistService.removeFromWishlist(testUser.getId(), testProduct1.getId());
        });

        // Then - Verify error handling
        assertEquals("Wishlist not found", exception2.getMessage());

        // When - Try to get wishlist for non-existent user
        RuntimeException exception3 = assertThrows(RuntimeException.class, () -> {
            wishlistService.getWishlist(999L);
        });

        // Then - Verify error handling
        assertEquals("User not found", exception3.getMessage());
    }

    private void setupTestData() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("1234567890");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = entityManager.persistAndFlush(testUser);

        // Create test products
        testProduct1 = new Product();
        testProduct1.setName("Test Product 1");
        testProduct1.setDescription("Test Description 1");
        testProduct1.setPrice(new BigDecimal("99.99"));
        testProduct1.setCategory(ProductCategory.ELECTRONICS);
        testProduct1.setStockQuantity(10);
        testProduct1.setActive(true);
        testProduct1.setCreatedAt(LocalDateTime.now());
        testProduct1 = entityManager.persistAndFlush(testProduct1);

        testProduct2 = new Product();
        testProduct2.setName("Test Product 2");
        testProduct2.setDescription("Test Description 2");
        testProduct2.setPrice(new BigDecimal("149.99"));
        testProduct2.setCategory(ProductCategory.CLOTHING);
        testProduct2.setStockQuantity(5);
        testProduct2.setActive(true);
        testProduct2.setCreatedAt(LocalDateTime.now());
        testProduct2 = entityManager.persistAndFlush(testProduct2);

        entityManager.clear();
    }
}
