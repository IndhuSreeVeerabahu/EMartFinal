package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.ExportService;
import com.example.E_Commerce.service.OrderService;
import com.example.E_Commerce.service.PDFService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @Mock
    private PDFService pdfService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ExportController exportController;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testExportOrdersToExcel() throws IOException {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        byte[] mockExcelData = "mock excel data".getBytes();
        when(orderService.getAllOrders()).thenReturn(orders);
        when(exportService.exportOrdersToExcel(orders)).thenReturn(mockExcelData);

        // When
        ResponseEntity<byte[]> response = exportController.exportOrdersToExcel();

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertArrayEquals(mockExcelData, response.getBody());

        verify(orderService).getAllOrders();
        verify(exportService).exportOrdersToExcel(orders);
    }

    @Test
    void testExportOrdersToPDF() throws IOException {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        byte[] mockPdfData = "mock pdf data".getBytes();
        when(orderService.getAllOrders()).thenReturn(orders);
        when(pdfService.generateOrdersReportPDF(anyString())).thenReturn(mockPdfData);

        // When
        ResponseEntity<byte[]> response = exportController.exportOrdersToPDF();

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertArrayEquals(mockPdfData, response.getBody());

        verify(orderService).getAllOrders();
        verify(pdfService).generateOrdersReportPDF(anyString());
    }

    @Test
    void testExportOrdersToExcel_NoOrders() throws IOException {
        // Given
        List<Order> emptyOrders = Arrays.asList();
        when(orderService.getAllOrders()).thenReturn(emptyOrders);

        // When
        ResponseEntity<byte[]> response = exportController.exportOrdersToExcel();

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(orderService).getAllOrders();
        verify(exportService).exportOrdersToExcel(emptyOrders);
    }

    @Test
    void testExportOrdersToExcel_ExportServiceError() throws IOException {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderService.getAllOrders()).thenReturn(orders);
        when(exportService.exportOrdersToExcel(orders)).thenThrow(new RuntimeException("Export failed"));

        // When
        ResponseEntity<byte[]> response = exportController.exportOrdersToExcel();

        // Then
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertTrue(new String(response.getBody()).contains("Error exporting orders"));

        verify(orderService).getAllOrders();
        verify(exportService).exportOrdersToExcel(orders);
    }

    @Test
    void testExportOrderInvoiceToPDF() throws IOException {
        // Given
        byte[] mockPdfData = "mock pdf data".getBytes();
        when(orderService.getOrderById(1L)).thenReturn(testOrder);
        when(pdfService.generateOrderInvoicePDF(testOrder)).thenReturn(mockPdfData);

        // When
        ResponseEntity<byte[]> response = exportController.exportOrderInvoiceToPDF(1L);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertArrayEquals(mockPdfData, response.getBody());

        verify(orderService).getOrderById(1L);
        verify(pdfService).generateOrderInvoicePDF(testOrder);
    }

    @Test
    void testExportOrderInvoiceToPDF_OrderNotFound() throws IOException {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(null);

        // When
        ResponseEntity<byte[]> response = exportController.exportOrderInvoiceToPDF(1L);

        // Then
        assertNotNull(response);
        assertEquals(404, response.getStatusCode().value());
        assertEquals("Order not found", new String(response.getBody()));

        verify(orderService).getOrderById(1L);
        verify(pdfService, never()).generateOrderInvoicePDF(any(Order.class));
    }

    private void setupTestData() {
        // Setup test user
        User testUser = new User();
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