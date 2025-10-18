package com.example.E_Commerce.integration;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
import com.example.E_Commerce.service.*;
import com.example.E_Commerce.config.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestEntityManager
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class OrderWorkflowIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        
        // Clear entity manager cache
        entityManager.clear();
        
        setupTestData();
    }

    @Test
    void testCompleteOrderWorkflow() {
        // Step 1: Create a user
        User savedUser = userRepository.save(testUser);
        assertNotNull(savedUser.getId());

        // Step 2: Create a product
        Product savedProduct = productRepository.save(testProduct);
        assertNotNull(savedProduct.getId());
        assertEquals(10, savedProduct.getStockQuantity());

        // Step 3: Create a cart for the user
        testCart.setUser(savedUser);
        Cart savedCart = cartRepository.save(testCart);
        assertNotNull(savedCart.getId());

        // Step 4: Add product to cart
        testCartItem.setCart(savedCart);
        testCartItem.setProduct(savedProduct);
        CartItem savedCartItem = cartItemRepository.save(testCartItem);
        assertNotNull(savedCartItem.getId());

        // Step 5: Create an order
        Order order = new Order();
        order.setUser(savedUser);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress("123 Test Street, Test City");
        order.setBillingAddress("123 Test Street, Test City");
        order.setCreatedAt(LocalDateTime.now());

        // Step 6: Create order items
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(savedProduct);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(savedProduct.getPrice());
        orderItem.setSubtotal(savedProduct.getPrice().multiply(BigDecimal.valueOf(2)));

        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order); // Set the bidirectional relationship

        // Step 7: Save the order
        Order savedOrder = orderRepository.save(order);
        entityManager.flush(); // Ensure the order and order items are persisted
        assertNotNull(savedOrder.getId());
        assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
        assertEquals(PaymentStatus.PENDING, savedOrder.getPaymentStatus());

        // Step 8: Update order status to confirmed
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderRepository.save(savedOrder);
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());

        // Step 9: Update payment status
        updatedOrder.setPaymentStatus(PaymentStatus.COMPLETED);
        Order paidOrder = orderRepository.save(updatedOrder);
        assertEquals(PaymentStatus.COMPLETED, paidOrder.getPaymentStatus());

        // Step 10: Update order to processing
        paidOrder.setStatus(OrderStatus.PROCESSING);
        Order processingOrder = orderRepository.save(paidOrder);
        assertEquals(OrderStatus.PROCESSING, processingOrder.getStatus());

        // Step 11: Update order to shipped
        processingOrder.setStatus(OrderStatus.SHIPPED);
        Order shippedOrder = orderRepository.save(processingOrder);
        assertEquals(OrderStatus.SHIPPED, shippedOrder.getStatus());

        // Step 12: Update order to delivered
        shippedOrder.setStatus(OrderStatus.DELIVERED);
        Order deliveredOrder = orderRepository.save(shippedOrder);
        assertEquals(OrderStatus.DELIVERED, deliveredOrder.getStatus());

        // Verify final state
        assertNotNull(deliveredOrder.getId());
        assertEquals(savedUser.getId(), deliveredOrder.getUser().getId());
        assertEquals(1, deliveredOrder.getOrderItems().size());
        assertEquals(savedProduct.getId(), deliveredOrder.getOrderItems().get(0).getProduct().getId());
    }

    @Test
    void testOrderCancellationWorkflow() {
        // Step 1: Create and save entities
        User savedUser = userRepository.save(testUser);
        Product savedProduct = productRepository.save(testProduct);
        
        testCart.setUser(savedUser);
        Cart savedCart = cartRepository.save(testCart);
        
        testCartItem.setCart(savedCart);
        testCartItem.setProduct(savedProduct);
        cartItemRepository.save(testCartItem);

        // Step 2: Create an order
        Order order = new Order();
        order.setUser(savedUser);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress("123 Test Street, Test City");
        order.setBillingAddress("123 Test Street, Test City");
        order.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(savedProduct);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(savedProduct.getPrice());
        orderItem.setSubtotal(savedProduct.getPrice().multiply(BigDecimal.valueOf(2)));

        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order); // Set the bidirectional relationship
        Order savedOrder = orderRepository.save(order);
        entityManager.flush(); // Ensure the order and order items are persisted

        // Step 3: Cancel the order
        savedOrder.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(savedOrder);

        // Verify cancellation
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        assertNotNull(cancelledOrder.getId());
    }

    @Test
    void testOrderReturnWorkflow() {
        // Step 1: Create and save entities
        User savedUser = userRepository.save(testUser);
        Product savedProduct = productRepository.save(testProduct);
        
        testCart.setUser(savedUser);
        Cart savedCart = cartRepository.save(testCart);
        
        testCartItem.setCart(savedCart);
        testCartItem.setProduct(savedProduct);
        cartItemRepository.save(testCartItem);

        // Step 2: Create a delivered order
        Order order = new Order();
        order.setUser(savedUser);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setShippingAddress("123 Test Street, Test City");
        order.setBillingAddress("123 Test Street, Test City");
        order.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(savedProduct);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(savedProduct.getPrice());
        orderItem.setSubtotal(savedProduct.getPrice().multiply(BigDecimal.valueOf(2)));

        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order); // Set the bidirectional relationship
        Order savedOrder = orderRepository.save(order);
        entityManager.flush(); // Ensure the order and order items are persisted

        // Step 3: Process return
        savedOrder.setStatus(OrderStatus.RETURNED);
        Order returnedOrder = orderRepository.save(savedOrder);

        // Verify return
        assertEquals(OrderStatus.RETURNED, returnedOrder.getStatus());
        assertNotNull(returnedOrder.getId());
    }

    @Test
    void testMultipleOrdersForUser() {
        // Step 1: Create user and product
        User savedUser = userRepository.save(testUser);
        Product savedProduct = productRepository.save(testProduct);

        // Step 2: Create multiple orders for the same user
        for (int i = 0; i < 3; i++) {
            Order order = new Order();
            order.setUser(savedUser);
            order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + i);
            order.setTotalAmount(new BigDecimal("99.99"));
            order.setStatus(OrderStatus.CREATED);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setShippingAddress("123 Test Street, Test City");
            order.setBillingAddress("123 Test Street, Test City");
            order.setCreatedAt(LocalDateTime.now());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(savedProduct);
            orderItem.setQuantity(1);
            orderItem.setUnitPrice(savedProduct.getPrice());
            orderItem.setSubtotal(savedProduct.getPrice());

            order.getOrderItems().add(orderItem);
            orderItem.setOrder(order); // Set the bidirectional relationship
            orderRepository.save(order);
            entityManager.flush(); // Ensure the order and order items are persisted
        }

        // Step 3: Verify all orders are created for the user
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(savedUser);
        assertEquals(3, userOrders.size());

        // Verify all orders belong to the same user
        for (Order order : userOrders) {
            assertEquals(savedUser.getId(), order.getUser().getId());
        }
    }

    @Test
    void testOrderStatusHistory() {
        // Step 1: Create and save entities
        User savedUser = userRepository.save(testUser);
        Product savedProduct = productRepository.save(testProduct);

        // Step 2: Create an order
        Order order = new Order();
        order.setUser(savedUser);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress("123 Test Street, Test City");
        order.setBillingAddress("123 Test Street, Test City");
        order.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(savedProduct);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(savedProduct.getPrice());
        orderItem.setSubtotal(savedProduct.getPrice());

        order.getOrderItems().add(orderItem);
        orderItem.setOrder(order); // Set the bidirectional relationship
        Order savedOrder = orderRepository.save(order);
        entityManager.flush(); // Ensure the order and order items are persisted

        // Step 3: Track status changes
        OrderStatus[] statuses = {
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
        };

        for (OrderStatus status : statuses) {
            savedOrder.setStatus(status);
            savedOrder = orderRepository.save(savedOrder);
            assertEquals(status, savedOrder.getStatus());
        }

        // Verify final status
        assertEquals(OrderStatus.DELIVERED, savedOrder.getStatus());
    }

    private void setupTestData() {
        // Setup test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);

        // Setup test product
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setCategory(ProductCategory.ELECTRONICS);
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);

        // Setup test cart
        testCart = new Cart();

        // Setup test cart item
        testCartItem = new CartItem();
        testCartItem.setQuantity(2);
    }
}
