package com.example.E_Commerce.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,50}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-()]{10,15}$"
    );
    
    private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\s\\-.,()]{1,255}$"
    );

    public boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public boolean isValidUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return true; // Phone number is optional
        }
        return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    public boolean isValidProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return false;
        }
        return PRODUCT_NAME_PATTERN.matcher(productName.trim()).matches();
    }

    public boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        // Password must be at least 6 characters, contain at least one letter and one number
        return password.length() >= 6 && 
               password.matches(".*[a-zA-Z].*") && 
               password.matches(".*[0-9].*");
    }

    public String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        // Remove potentially dangerous characters
        return input.trim()
                   .replaceAll("[<>\"'&]", "")
                   .replaceAll("\\s+", " ");
    }

    public boolean containsScriptTags(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return input.toLowerCase().contains("<script") || 
               input.toLowerCase().contains("javascript:") ||
               input.toLowerCase().contains("onload=") ||
               input.toLowerCase().contains("onerror=");
    }

    public boolean isValidPrice(String priceStr) {
        if (!StringUtils.hasText(priceStr)) {
            return false;
        }
        
        try {
            double price = Double.parseDouble(priceStr);
            return price > 0 && price <= 999999.99;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isValidQuantity(String quantityStr) {
        if (!StringUtils.hasText(quantityStr)) {
            return false;
        }
        
        try {
            int quantity = Integer.parseInt(quantityStr);
            return quantity > 0 && quantity <= 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
