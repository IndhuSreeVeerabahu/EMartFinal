package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        List<Product> featuredProducts = productService.getAvailableProducts();
        model.addAttribute("featuredProducts", featuredProducts);
        
        if (authentication != null) {
            User user = (User) authentication.getPrincipal();
            int cartItemCount = cartService.getCartItemCount(user.getId());
            int wishlistItemCount = wishlistService.getWishlistItemCount(user.getId());
            model.addAttribute("cartItemCount", cartItemCount);
            model.addAttribute("wishlistItemCount", wishlistItemCount);
        }
        
        return "index";
    }


    @GetMapping("/home")
    public String homePage(Model model, Authentication authentication) {
        return home(model, authentication);
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "12") int size,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) BigDecimal minPrice,
                          @RequestParam(required = false) BigDecimal maxPrice,
                          Model model, Authentication authentication) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search, pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            try {
                ProductCategory productCategory = ProductCategory.valueOf(category.toUpperCase());
                products = productService.getProductsByCategory(productCategory, pageable);
            } catch (IllegalArgumentException e) {
                products = productService.getAllProducts(pageable);
            }
        } else if (minPrice != null && maxPrice != null) {
            products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        
        model.addAttribute("products", products);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("categories", ProductCategory.values());
        
        if (authentication != null) {
            User user = (User) authentication.getPrincipal();
            int cartItemCount = cartService.getCartItemCount(user.getId());
            int wishlistItemCount = wishlistService.getWishlistItemCount(user.getId());
            model.addAttribute("cartItemCount", cartItemCount);
            model.addAttribute("wishlistItemCount", wishlistItemCount);
        }
        
        return "products";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                throw new IllegalArgumentException("Product not found with ID: " + id);
            }
            model.addAttribute("product", product);
            
            if (authentication != null) {
                User user = (User) authentication.getPrincipal();
                int cartItemCount = cartService.getCartItemCount(user.getId());
                int wishlistItemCount = wishlistService.getWishlistItemCount(user.getId());
                model.addAttribute("cartItemCount", cartItemCount);
                model.addAttribute("wishlistItemCount", wishlistItemCount);
            }
            
            return "product-detail";
        } catch (Exception e) {
            throw new IllegalArgumentException("Product not found with ID: " + id);
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute User user, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }
        
        // Additional validation
        if (user.getPassword() != null && user.getPassword().length() < 6) {
            result.rejectValue("password", "error.user", "Password must be at least 6 characters long");
            return "register";
        }
        
        if (user.getEmail() != null && !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            result.rejectValue("email", "error.user", "Please provide a valid email address");
            return "register";
        }
        
        try {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute User user, BindingResult result, 
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "profile";
        }
        
        try {
            User currentUser = (User) authentication.getPrincipal();
            user.setId(currentUser.getId());
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }

}
