package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.OrderService;
import com.example.E_Commerce.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;
    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        setupTestData();
    }

    @Test
    void testPaymentSuccess_WithValidParameters() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.verifyPayment("ORD-123456", "payment_123")).thenReturn(true);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.COMPLETED);
        doNothing().when(orderService).updateRazorpayPaymentId(1L, "payment_123");

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .param("order_id", "ORD-123456")
                        .param("cf_payment_id", "payment_123")
                        .param("payment_status", "SUCCESS")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).verifyPayment("ORD-123456", "payment_123");
        verify(orderService).updatePaymentStatus(1L, PaymentStatus.COMPLETED);
        verify(orderService).updateRazorpayPaymentId(1L, "payment_123");
    }

    @Test
    void testPaymentSuccess_WithAlternativeParameterNames() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.verifyPayment("ORD-123456", "payment_123")).thenReturn(true);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.COMPLETED);
        doNothing().when(orderService).updateRazorpayPaymentId(1L, "payment_123");

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .param("orderId", "ORD-123456")
                        .param("paymentId", "payment_123")
                        .param("status", "SUCCESS")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).verifyPayment("ORD-123456", "payment_123");
    }

    @Test
    void testPaymentSuccess_PaymentVerificationFailed() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.verifyPayment("ORD-123456", "payment_123")).thenReturn(false);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .param("order_id", "ORD-123456")
                        .param("cf_payment_id", "payment_123")
                        .param("payment_status", "SUCCESS")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).verifyPayment("ORD-123456", "payment_123");
        verify(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);
    }

    @Test
    void testPaymentSuccess_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .param("order_id", "ORD-123456")
                        .param("cf_payment_id", "payment_123")
                        .param("payment_status", "SUCCESS")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(paymentService, never()).verifyPayment(anyString(), anyString());
    }

    @Test
    void testPaymentSuccess_UnauthorizedAccess() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        when(authentication.getPrincipal()).thenReturn(otherUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .param("order_id", "ORD-123456")
                        .param("cf_payment_id", "payment_123")
                        .param("payment_status", "SUCCESS")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(paymentService, never()).verifyPayment(anyString(), anyString());
    }

    @Test
    void testPaymentSuccess_MissingParameters() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/payment/success")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService, never()).getOrderById(anyLong());
        verify(paymentService, never()).verifyPayment(anyString(), anyString());
    }

    @Test
    void testPaymentFailure() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);

        // When & Then
        mockMvc.perform(get("/payment/failure")
                        .param("order_id", "ORD-123456")
                        .param("error", "Payment failed")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);
    }

    @Test
    void testTestPayment_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.simulateTestPayment("ORD-123456", true)).thenReturn(true);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.COMPLETED);

        // When & Then
        mockMvc.perform(post("/payment/test/1")
                        .with(csrf())
                        .param("success", "true")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).simulateTestPayment("ORD-123456", true);
        verify(orderService).updatePaymentStatus(1L, PaymentStatus.COMPLETED);
    }

    @Test
    void testTestPayment_Failure() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(paymentService.simulateTestPayment("ORD-123456", false)).thenReturn(false);
        doNothing().when(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);

        // When & Then
        mockMvc.perform(post("/payment/test/1")
                        .with(csrf())
                        .param("success", "false")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(paymentService).simulateTestPayment("ORD-123456", false);
        verify(orderService).updatePaymentStatus(1L, PaymentStatus.FAILED);
    }

    @Test
    void testTestPayment_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/payment/test/1")
                        .with(csrf())
                        .param("success", "true")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(paymentService, never()).simulateTestPayment(anyString(), anyBoolean());
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
