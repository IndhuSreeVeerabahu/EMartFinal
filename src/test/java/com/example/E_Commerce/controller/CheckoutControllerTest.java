package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
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
class CheckoutControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CheckoutController checkoutController;

    private MockMvc mockMvc;
    private User testUser;
    private Order testOrder;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(checkoutController).build();
        setupTestData();
    }

    @Test
    void testCheckout_WithValidCart() throws Exception {
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItems(1L)).thenReturn(cartItems);
        when(cartService.getCart(1L)).thenReturn(testCart);

        // When & Then
        mockMvc.perform(get("/checkout")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("cartItems", cartItems))
                .andExpect(model().attributeExists("cartTotal"));

        verify(cartService).getCartItems(1L);
        verify(cartService).getCart(1L);
    }

    @Test
    void testCheckout_EmptyCart() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(cartService.getCartItems(1L)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/checkout")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("error"));

        verify(cartService).getCartItems(1L);
    }

    @Test
    void testProcessCheckout_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.createOrder(1L, "Test Address", "Test Address")).thenReturn(testOrder);
        doNothing().when(cartService).clearCart(1L);

        // When & Then
        mockMvc.perform(post("/checkout/process")
                        .with(csrf())
                        .param("shippingAddress", "Test Address")
                        .param("billingAddress", "Test Address")
                        .param("paymentMethod", "CASHFREE")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/checkout/payment/*"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).createOrder(1L, "Test Address", "Test Address");
        verify(cartService).clearCart(1L);
    }

    @Test
    void testProcessCheckout_ValidationError() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/checkout/process")
                        .with(csrf())
                        .param("shippingAddress", "") // Invalid: empty address
                        .param("billingAddress", "Test Address")
                        .param("paymentMethod", "CASHFREE")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"));

        verify(orderService, never()).createOrder(anyLong(), anyString(), anyString());
    }

    @Test
    void testPaymentPage_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.createPaymentSession(testOrder)).thenReturn("test_session_123");
        when(paymentService.getCashfreeAppId()).thenReturn("test_app_id");
        when(paymentService.getCashfreeEnvironment()).thenReturn("SANDBOX");

        // When & Then
        mockMvc.perform(get("/checkout/payment/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("order", testOrder))
                .andExpect(model().attribute("paymentSessionId", "test_session_123"))
                .andExpect(model().attribute("cashfreeAppId", "test_app_id"))
                .andExpect(model().attribute("environment", "SANDBOX"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).createPaymentSession(testOrder);
    }

    @Test
    void testPaymentPage_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/checkout/payment/1")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).getOrderById(1L);
        verify(paymentService, never()).createPaymentSession(any(Order.class));
    }

    @Test
    void testPaymentPage_UnauthorizedAccess() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        when(authentication.getPrincipal()).thenReturn(otherUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/checkout/payment/1")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).getOrderById(1L);
        verify(paymentService, never()).createPaymentSession(any(Order.class));
    }

    @Test
    void testPaymentPage_PaymentServiceError() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.createPaymentSession(testOrder)).thenThrow(new RuntimeException("Payment service error"));
        when(paymentService.getCashfreeAppId()).thenReturn("test_app_id");

        // When & Then
        mockMvc.perform(get("/checkout/payment/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("order", testOrder))
                .andExpect(model().attribute("error", "Payment gateway initialization failed. Use test payment option."));

        verify(orderService).getOrderById(1L);
        verify(paymentService).createPaymentSession(testOrder);
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

        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setCreatedAt(LocalDateTime.now());

        // Setup test cart item
        Product testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setCart(testCart);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(1);
        // CartItem doesn't have setCreatedAt method

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(new BigDecimal("99.99"));
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        testOrder.setShippingAddress("Test Address");
        testOrder.setBillingAddress("Test Address");
        testOrder.setCreatedAt(LocalDateTime.now());
    }
}
