package com.example.E_Commerce.service;

import com.example.E_Commerce.model.Order;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PDFService {

    @Autowired
    private ExportService exportService;

    public byte[] generateOrderInvoicePDF(Order order) throws IOException {
        String html = exportService.generateOrderInvoiceHTML(order);
        return convertHtmlToPdf(html);
    }

    public byte[] generateOrdersReportPDF(String htmlContent) throws IOException {
        return convertHtmlToPdf(htmlContent);
    }

    private byte[] convertHtmlToPdf(String html) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        return outputStream.toByteArray();
    }
}
