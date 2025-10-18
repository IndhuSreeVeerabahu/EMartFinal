package com.example.E_Commerce.service;

import com.example.E_Commerce.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private User testUser;

    @BeforeEach
    void setUp() {
        setupTestData();
        setupPaymentServiceProperties();
    }

    @Test
    void testCreatePaymentSession_Success() throws Exception {
        // Given
        String mockResponse = "{\"payment_session_id\":\"test_session_123\"}";
        JsonNode mockJsonNode = mock(JsonNode.class);
        JsonNode mockSessionIdNode = mock(JsonNode.class);
        
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("payment_session_id")).thenReturn(mockSessionIdNode);
        when(mockSessionIdNode.asText()).thenReturn("test_session_123");

        // When
        String result = paymentService.createPaymentSession(testOrder);

        // Then
        assertNotNull(result);
        assertEquals("test_session_123", result);
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testCreatePaymentSession_ApiError() {
        // Given
        ResponseEntity<String> errorResponse = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPaymentSession(testOrder);
        });

        assertTrue(exception.getMessage().contains("Failed to create payment session"));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testCreatePaymentSession_Exception() {
        // Given
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPaymentSession(testOrder);
        });

        assertTrue(exception.getMessage().contains("Failed to create payment session"));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testVerifyPayment_Success() throws Exception {
        // Given
        String mockResponse = "{\"data\":[{\"cf_payment_id\":\"payment_123\",\"payment_status\":\"SUCCESS\"}]}";
        JsonNode mockJsonNode = mock(JsonNode.class);
        JsonNode mockDataNode = mock(JsonNode.class);
        JsonNode mockPaymentNode = mock(JsonNode.class);
        JsonNode mockPaymentIdNode = mock(JsonNode.class);
        JsonNode mockStatusNode = mock(JsonNode.class);
        
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("data")).thenReturn(mockDataNode);
        when(mockDataNode.isArray()).thenReturn(true);
        when(mockDataNode.get(0)).thenReturn(mockPaymentNode);
        when(mockPaymentNode.get("cf_payment_id")).thenReturn(mockPaymentIdNode);
        when(mockPaymentIdNode.asText()).thenReturn("payment_123");
        when(mockPaymentNode.get("payment_status")).thenReturn(mockStatusNode);
        when(mockStatusNode.asText()).thenReturn("SUCCESS");

        // When
        boolean result = paymentService.verifyPayment("order_123", "payment_123");

        // Then
        assertTrue(result);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testVerifyPayment_Failed() throws Exception {
        // Given
        String mockResponse = "{\"data\":[{\"cf_payment_id\":\"payment_123\",\"payment_status\":\"FAILED\"}]}";
        JsonNode mockJsonNode = mock(JsonNode.class);
        JsonNode mockDataNode = mock(JsonNode.class);
        JsonNode mockPaymentNode = mock(JsonNode.class);
        JsonNode mockPaymentIdNode = mock(JsonNode.class);
        JsonNode mockStatusNode = mock(JsonNode.class);
        
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("data")).thenReturn(mockDataNode);
        when(mockDataNode.isArray()).thenReturn(true);
        when(mockDataNode.get(0)).thenReturn(mockPaymentNode);
        when(mockPaymentNode.get("cf_payment_id")).thenReturn(mockPaymentIdNode);
        when(mockPaymentIdNode.asText()).thenReturn("payment_123");
        when(mockPaymentNode.get("payment_status")).thenReturn(mockStatusNode);
        when(mockStatusNode.asText()).thenReturn("FAILED");

        // When
        boolean result = paymentService.verifyPayment("order_123", "payment_123");

        // Then
        assertFalse(result);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testVerifyPayment_Exception() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = paymentService.verifyPayment("order_123", "payment_123");

        // Then
        assertFalse(result);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testGetPaymentDetails_Success() throws Exception {
        // Given
        String mockResponse = "{\"data\":{\"payment_id\":\"payment_123\",\"amount\":10000}}";
        JsonNode mockJsonNode = mock(JsonNode.class);
        JsonNode mockDataNode = mock(JsonNode.class);
        Map<String, Object> mockPaymentData = new HashMap<>();
        mockPaymentData.put("payment_id", "payment_123");
        mockPaymentData.put("amount", 10000);
        
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponseEntity);
        when(objectMapper.readTree(mockResponse)).thenReturn(mockJsonNode);
        when(mockJsonNode.get("data")).thenReturn(mockDataNode);
        when(objectMapper.convertValue(mockDataNode, Map.class)).thenReturn(mockPaymentData);

        // When
        Map<String, Object> result = paymentService.getPaymentDetails("payment_123");

        // Then
        assertNotNull(result);
        assertEquals("payment_123", result.get("payment_id"));
        assertEquals(10000, result.get("amount"));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testGetPaymentDetails_ApiError() {
        // Given
        ResponseEntity<String> errorResponse = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(errorResponse);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.getPaymentDetails("payment_123");
        });

        assertTrue(exception.getMessage().contains("Failed to fetch payment details"));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testIsPaymentSuccessful_Success() {
        // Given
        Map<String, Object> payment = new HashMap<>();
        payment.put("payment_status", "SUCCESS");

        // When
        boolean result = paymentService.isPaymentSuccessful(payment);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPaymentSuccessful_Completed() {
        // Given
        Map<String, Object> payment = new HashMap<>();
        payment.put("payment_status", "COMPLETED");

        // When
        boolean result = paymentService.isPaymentSuccessful(payment);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsPaymentSuccessful_Failed() {
        // Given
        Map<String, Object> payment = new HashMap<>();
        payment.put("payment_status", "FAILED");

        // When
        boolean result = paymentService.isPaymentSuccessful(payment);

        // Then
        assertFalse(result);
    }

    @Test
    void testSimulateTestPayment_Success() {
        // When
        boolean result = paymentService.simulateTestPayment("order_123", true);

        // Then
        assertTrue(result);
    }

    @Test
    void testSimulateTestPayment_Failed() {
        // When
        boolean result = paymentService.simulateTestPayment("order_123", false);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetCashfreeAppId() {
        // When
        String result = paymentService.getCashfreeAppId();

        // Then
        assertEquals("TEST108283821957fe1153788f32479528382801", result);
    }

    @Test
    void testGetCashfreeEnvironment() {
        // When
        String result = paymentService.getCashfreeEnvironment();

        // Then
        assertEquals("SANDBOX", result);
    }

    private void setupTestData() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhoneNumber("1234567890");
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

    private void setupPaymentServiceProperties() {
        // Set up the properties using ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "cashfreeAppId", "TEST108283821957fe1153788f32479528382801");
        ReflectionTestUtils.setField(paymentService, "cashfreeSecretKey", "cfsk_ma_test_dddc2fa7d13c09a0a5f0c9c88f26f678_f1e97d01");
        ReflectionTestUtils.setField(paymentService, "cashfreeEnvironment", "SANDBOX");
    }
}
