package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.OrderService;
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
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        setupTestData();
    }

    @Test
    void testViewOrders_WithAuthentication() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrdersByUser(1L, PageRequest.of(0, 10))).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/orders")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"));

        verify(orderService).getOrdersByUser(1L, any(Pageable.class));
    }

    @Test
    void testViewOrders_WithPagination() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(1, 5), 1);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrdersByUser(1L, PageRequest.of(1, 5))).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/orders")
                        .param("page", "1")
                        .param("size", "5")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"));

        verify(orderService).getOrdersByUser(1L, PageRequest.of(1, 5));
    }

    @Test
    void testViewOrderDetail_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/orders/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("order-detail"))
                .andExpect(model().attribute("order", testOrder));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testViewOrderDetail_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/orders/1")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testViewOrderDetail_UnauthorizedAccess() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        when(authentication.getPrincipal()).thenReturn(otherUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/orders/1")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        doNothing().when(orderService).cancelOrder(1L);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"))
                .andExpect(flash().attributeExists("success"));

        verify(orderService).getOrderById(1L);
        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testCancelOrder_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(orderService, never()).cancelOrder(anyLong());
    }

    @Test
    void testCancelOrder_UnauthorizedAccess() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        when(authentication.getPrincipal()).thenReturn(otherUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(orderService, never()).cancelOrder(anyLong());
    }

    @Test
    void testCancelOrder_OrderCannotBeCancelled() throws Exception {
        // Given
        testOrder.setStatus(OrderStatus.SHIPPED); // Cannot cancel shipped order
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        doThrow(new RuntimeException("Order cannot be cancelled")).when(orderService).cancelOrder(1L);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf())
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testTrackOrder_Success() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/orders/1/track")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("order-detail"))
                .andExpect(model().attribute("order", testOrder));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testTrackOrder_OrderNotFound() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/orders/1/track")
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"))
                .andExpect(flash().attributeExists("error"));

        verify(orderService).getOrderById(1L);
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
