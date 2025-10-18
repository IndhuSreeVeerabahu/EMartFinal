package com.example.E_Commerce.service;

import com.example.E_Commerce.model.Order;
import com.example.E_Commerce.model.Product;
import com.example.E_Commerce.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    public byte[] exportOrdersToExcel(List<Order> orders) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Orders Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Order Number", "Customer Name", "Customer Email", "Total Amount", 
            "Status", "Payment Status", "Order Date", "Items Count"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(order.getOrderNumber());
            row.createCell(1).setCellValue(order.getUser().getFullName());
            row.createCell(2).setCellValue(order.getUser().getEmail());
            row.createCell(3).setCellValue(order.getTotalAmount().doubleValue());
            row.createCell(4).setCellValue(order.getStatus().toString());
            row.createCell(5).setCellValue(order.getPaymentStatus().toString());
            row.createCell(6).setCellValue(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            row.createCell(7).setCellValue(order.getOrderItems().size());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    public byte[] exportProductsToExcel(List<Product> products) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Product Name", "Description", "Price", "Stock", "Category", "Created Date"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Product product : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getName());
            row.createCell(1).setCellValue(product.getDescription());
            row.createCell(2).setCellValue(product.getPrice().doubleValue());
            row.createCell(3).setCellValue(product.getStockQuantity());
            row.createCell(4).setCellValue(product.getCategory().toString());
            row.createCell(5).setCellValue(product.getCreatedAt() != null ? 
                product.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    public byte[] exportUsersToExcel(List<User> users) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Full Name", "Email", "Phone", "Role", "Registration Date", "Active"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getFullName());
            row.createCell(1).setCellValue(user.getEmail());
            row.createCell(2).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
            row.createCell(3).setCellValue(user.getRole().toString());
            row.createCell(4).setCellValue(user.getCreatedAt() != null ? 
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A");
            row.createCell(5).setCellValue(user.isEnabled() ? "Yes" : "No");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    public String generateOrderInvoiceHTML(Order order) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Invoice - ").append(order.getOrderNumber()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".invoice-details { margin-bottom: 30px; }");
        html.append(".customer-info, .order-info { display: inline-block; width: 48%; vertical-align: top; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".total { text-align: right; font-weight: bold; }");
        html.append("</style>");
        html.append("</head><body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>E-Commerce Store</h1>");
        html.append("<h2>Invoice</h2>");
        html.append("</div>");

        // Invoice details
        html.append("<div class='invoice-details'>");
        html.append("<div class='customer-info'>");
        html.append("<h3>Bill To:</h3>");
        html.append("<p><strong>").append(order.getUser().getFullName()).append("</strong></p>");
        html.append("<p>").append(order.getUser().getEmail()).append("</p>");
        html.append("<p>").append(order.getBillingAddress()).append("</p>");
        html.append("</div>");

        html.append("<div class='order-info'>");
        html.append("<h3>Order Details:</h3>");
        html.append("<p><strong>Invoice #:</strong> ").append(order.getOrderNumber()).append("</p>");
        html.append("<p><strong>Order Date:</strong> ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>");
        html.append("<p><strong>Status:</strong> ").append(order.getStatus()).append("</p>");
        html.append("<p><strong>Payment Status:</strong> ").append(order.getPaymentStatus()).append("</p>");
        html.append("</div>");
        html.append("</div>");

        // Items table
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Item</th><th>Description</th><th>Quantity</th><th>Unit Price</th><th>Total</th>");
        html.append("</tr></thead><tbody>");

        for (var item : order.getOrderItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getProduct().getName()).append("</td>");
            html.append("<td>").append(item.getProduct().getDescription()).append("</td>");
            html.append("<td>").append(item.getQuantity()).append("</td>");
            html.append("<td>₹").append(String.format("%.2f", item.getUnitPrice())).append("</td>");
            html.append("<td>₹").append(String.format("%.2f", item.getSubtotal())).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        // Total
        html.append("<div class='total'>");
        html.append("<p><strong>Total Amount: ₹").append(String.format("%.2f", order.getTotalAmount())).append("</strong></p>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }
}
