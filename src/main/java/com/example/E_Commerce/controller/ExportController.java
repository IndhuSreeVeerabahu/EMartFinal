package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.Order;
import com.example.E_Commerce.model.Product;
import com.example.E_Commerce.model.User;
import com.example.E_Commerce.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/export")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @GetMapping("/orders/excel")
    public ResponseEntity<byte[]> exportOrdersToExcel() {
        try {
            var orders = orderService.getAllOrders();
            byte[] excelData = exportService.exportOrdersToExcel(orders);
            
            String filename = "orders_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/excel")
    public ResponseEntity<byte[]> exportProductsToExcel() {
        try {
            var products = productService.getAllProducts();
            byte[] excelData = exportService.exportProductsToExcel(products);
            
            String filename = "products_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/excel")
    public ResponseEntity<byte[]> exportUsersToExcel() {
        try {
            var users = userService.getAllUsers();
            byte[] excelData = exportService.exportUsersToExcel(users);
            
            String filename = "users_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/orders/pdf")
    public ResponseEntity<byte[]> exportOrdersToPDF() {
        try {
            var orders = orderService.getAllOrders();
            String htmlContent = generateOrdersReportHTML(orders);
            byte[] pdfData = pdfService.generateOrdersReportPDF(htmlContent);
            
            String filename = "orders_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/order/{id}/invoice/pdf")
    public ResponseEntity<byte[]> exportOrderInvoiceToPDF(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);
            byte[] pdfData = pdfService.generateOrderInvoicePDF(order);
            
            String filename = "invoice_" + order.getOrderNumber() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateOrdersReportHTML(java.util.List<Order> orders) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Orders Report</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".summary { margin-top: 30px; }");
        html.append("</style>");
        html.append("</head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>E-Commerce Store</h1>");
        html.append("<h2>Orders Report</h2>");
        html.append("<p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>");
        html.append("</div>");

        // Orders table
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Order Number</th><th>Customer</th><th>Amount</th><th>Status</th><th>Payment</th><th>Date</th>");
        html.append("</tr></thead><tbody>");

        double totalRevenue = 0;
        for (Order order : orders) {
            html.append("<tr>");
            html.append("<td>").append(order.getOrderNumber()).append("</td>");
            html.append("<td>").append(order.getUser().getFullName()).append("</td>");
            html.append("<td>₹").append(String.format("%.2f", order.getTotalAmount())).append("</td>");
            html.append("<td>").append(order.getStatus()).append("</td>");
            html.append("<td>").append(order.getPaymentStatus()).append("</td>");
            html.append("<td>").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</td>");
            html.append("</tr>");
            
            if (order.getPaymentStatus().name().equals("COMPLETED")) {
                totalRevenue += order.getTotalAmount().doubleValue();
            }
        }

        html.append("</tbody></table>");

        // Summary
        html.append("<div class='summary'>");
        html.append("<h3>Summary</h3>");
        html.append("<p><strong>Total Orders:</strong> ").append(orders.size()).append("</p>");
        html.append("<p><strong>Total Revenue:</strong> ₹").append(String.format("%.2f", totalRevenue)).append("</p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }
}
