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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testGetOrCreateCart_ExistingCart() {
        // Given
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        // When
        Cart result = cartService.getOrCreateCart(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testCart, result);
        verify(cartRepository).findByUser(testUser);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testGetOrCreateCart_NewCart() {
        // Given
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setId(1L);
            return cart;
        });

        // When
        Cart result = cartService.getOrCreateCart(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        verify(cartRepository).findByUser(testUser);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testAddToCart_Success_NewItem() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(1L);
            return item;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.addToCart(1L, 1L, 2);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).findByCartAndProduct(testCart, testProduct);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(testCart);
    }

    @Test
    void testAddToCart_Success_ExistingItem() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.addToCart(1L, 1L, 2);

        // Then
        assertNotNull(result);
        assertEquals(3, testCartItem.getQuantity()); // Original 1 + new 2
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).findByCartAndProduct(testCart, testProduct);
        verify(cartItemRepository).save(testCartItem);
        verify(cartRepository).save(testCart);
    }

    @Test
    void testAddToCart_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(1L, 1L, 2);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verifyNoInteractions(productRepository);
        verifyNoInteractions(cartRepository);
    }

    @Test
    void testAddToCart_ProductNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(1L, 1L, 2);
        });

        assertEquals("Product not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verifyNoInteractions(cartRepository);
    }

    @Test
    void testAddToCart_ProductNotActive() {
        // Given
        testProduct.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(1L, 1L, 2);
        });

        assertEquals("Product not available or insufficient stock", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    void testAddToCart_InsufficientStock() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(1L, 1L, 15); // Requesting more than available stock (10)
        });

        assertEquals("Product not available or insufficient stock", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    void testUpdateCartItemQuantity_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.updateCartItemQuantity(1L, 1L, 5);

        // Then
        assertNotNull(result);
        assertEquals(5, testCartItem.getQuantity());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).findByCartAndProduct(testCart, testProduct);
        verify(cartItemRepository).save(testCartItem);
        verify(cartRepository).save(testCart);
    }

    @Test
    void testUpdateCartItemQuantity_RemoveItem() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(testCartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.updateCartItemQuantity(1L, 1L, 0);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).findByCartAndProduct(testCart, testProduct);
        verify(cartItemRepository).delete(testCartItem);
        verify(cartRepository).save(testCart);
    }

    @Test
    void testUpdateCartItemQuantity_CartItemNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.updateCartItemQuantity(1L, 1L, 5);
        });

        assertEquals("Cart item not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).findByCartAndProduct(testCart, testProduct);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testRemoveFromCart_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.removeFromCart(1L, 1L);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartRepository).save(testCart);
    }

    @Test
    void testClearCart_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        Cart result = cartService.clearCart(1L);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        assertTrue(result.getCartItems().isEmpty());
        verify(userRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
        verify(cartItemRepository).deleteAll(testCart.getCartItems());
        verify(cartRepository).save(testCart);
    }

    @Test
    void testGetCart_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        // When
        Cart result = cartService.getCart(1L);

        // Then
        assertNotNull(result);
        assertEquals(testCart, result);
        verify(userRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
    }

    @Test
    void testGetCartItems_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        // When
        List<CartItem> result = cartService.getCartItems(1L);

        // Then
        assertNotNull(result);
        assertEquals(testCart.getCartItems(), result);
        verify(userRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
    }

    @Test
    void testGetCartItemCount_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        // When
        int result = cartService.getCartItemCount(1L);

        // Then
        assertEquals(testCart.getTotalItems(), result);
        verify(userRepository).findById(1L);
        verify(cartRepository).findByUser(testUser);
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

        // Setup test cart item
        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(1);

        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setCartItems(Arrays.asList(testCartItem));
        testCart.setTotalAmount(new BigDecimal("99.99"));
    }
}
