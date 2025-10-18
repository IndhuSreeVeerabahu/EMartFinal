package com.example.E_Commerce.service;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(testCart);
        when(cartService.clearCart(1L)).thenReturn(testCart); // clearCart returns Cart, not void
        doNothing().when(productService).updateStock(anyLong(), anyInt());
        
        // Mock the save method to return the order that was passed to it
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L); // Set an ID for the returned order
            return order;
        });

        // When
        Order result = orderService.createOrder(1L, "Shipping Address", "Billing Address");

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals("Shipping Address", result.getShippingAddress());
        assertEquals("Billing Address", result.getBillingAddress());
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());

        verify(userRepository).findById(1L);
        verify(cartService).getCart(1L);
        verify(orderRepository).save(any(Order.class));
        verify(cartService).clearCart(1L);
        verify(productService).updateStock(anyLong(), anyInt());
    }

    @Test
    void testCreateOrder_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(1L, "Shipping Address", "Billing Address");
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verifyNoInteractions(cartService);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void testCreateOrder_EmptyCart() {
        // Given
        Cart emptyCart = new Cart();
        emptyCart.setCartItems(Arrays.asList());
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(emptyCart);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(1L, "Shipping Address", "Billing Address");
        });

        assertEquals("Cart is empty", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(cartService).getCart(1L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void testUpdateOrderStatus_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testUpdateOrderStatus_OrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);
        });

        assertEquals("Order not found", exception.getMessage());
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testUpdatePaymentStatus_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = orderService.updatePaymentStatus(1L, PaymentStatus.COMPLETED);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testGetOrderById_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.getOrderById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(1L);
        });

        assertEquals("Order not found", exception.getMessage());
        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderByOrderNumber_Success() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-123456")).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.getOrderByOrderNumber("ORD-123456");

        // Then
        assertNotNull(result);
        assertEquals("ORD-123456", result.getOrderNumber());
        verify(orderRepository).findByOrderNumber("ORD-123456");
    }

    @Test
    void testGetOrdersByUser_Success() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(orders);

        // When
        List<Order> result = orderService.getOrdersByUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(userRepository).findById(1L);
        verify(orderRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void testGetOrdersByUser_Paginated() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser, pageable)).thenReturn(orderPage);

        // When
        Page<Order> result = orderService.getOrdersByUser(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testOrder, result.getContent().get(0));
        verify(userRepository).findById(1L);
        verify(orderRepository).findByUser(testUser, pageable);
    }

    @Test
    void testGetAllOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // When
        List<Order> result = orderService.getAllOrders();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderRepository).findAll();
    }

    @Test
    void testGetOrdersByStatus() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByStatus(OrderStatus.CREATED)).thenReturn(orders);

        // When
        List<Order> result = orderService.getOrdersByStatus(OrderStatus.CREATED);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder, result.get(0));
        verify(orderRepository).findByStatus(OrderStatus.CREATED);
    }

    @Test
    void testCancelOrder_Success() {
        // Given
        testOrder.setStatus(OrderStatus.CONFIRMED);
        
        // Create order items for the test order
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(2);
        testOrder.setOrderItems(Arrays.asList(orderItem));
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(productService).restoreStock(anyLong(), anyInt());

        // When
        orderService.cancelOrder(1L);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
        verify(productService).restoreStock(testProduct.getId(), 2);
    }

    @Test
    void testCancelOrder_DeliveredOrder() {
        // Given
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(1L);
        });

        assertEquals("Cannot cancel delivered order", exception.getMessage());
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testFindByRazorpayOrderId_Success() {
        // Given
        when(orderRepository.findByRazorpayOrderId("razorpay_123")).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.findByRazorpayOrderId("razorpay_123");

        // Then
        assertNotNull(result);
        assertEquals(testOrder, result);
        verify(orderRepository).findByRazorpayOrderId("razorpay_123");
    }

    @Test
    void testUpdateRazorpayOrderId() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderService.updateRazorpayOrderId(1L, "razorpay_123");

        // Then
        assertEquals("razorpay_123", testOrder.getRazorpayOrderId());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testUpdateRazorpayPaymentId() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderService.updateRazorpayPaymentId(1L, "payment_123");

        // Then
        assertEquals("payment_123", testOrder.getRazorpayPaymentId());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
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

        // Setup test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(10);

        // Setup test cart item
        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        // Setup test cart
        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setCartItems(Arrays.asList(testCartItem));

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(new BigDecimal("199.98"));
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        testOrder.setShippingAddress("Test Address");
        testOrder.setBillingAddress("Test Address");
        testOrder.setCreatedAt(LocalDateTime.now());
    }
}
