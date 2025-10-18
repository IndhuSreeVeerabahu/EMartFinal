package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.E_Commerce.validation.UpdateGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataInitializationService dataInitializationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Product> products = productService.getAllProducts();
        List<Order> orders = orderService.getAllOrders();
        List<User> users = userService.getAllUsers();
        
        // Calculate total revenue from completed orders
        double totalRevenue = orders.stream()
            .filter(o -> o.getPaymentStatus() == PaymentStatus.COMPLETED)
            .mapToDouble(o -> o.getTotalAmount().doubleValue())
            .sum();
        
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", orders.stream().limit(5).toList());
        
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        model.addAttribute("categories", ProductCategory.values());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", ProductCategory.values());
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute Product product, BindingResult result, 
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/product-form";
        }
        
        try {
            productService.createProduct(product);
            redirectAttributes.addFlashAttribute("success", "Product created successfully!");
            return "redirect:/admin/products";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/new";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", ProductCategory.values());
        return "admin/product-form";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id, @Valid @ModelAttribute Product product, 
                               BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/product-form";
        }
        
        try {
            product.setId(id);
            productService.updateProduct(product);
            redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
            return "redirect:/admin/products";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        try {
            System.out.println("AdminController: Loading orders page...");
            List<Order> orders = orderService.getAllOrders();
            System.out.println("AdminController: Found " + (orders != null ? orders.size() : 0) + " orders");
            
            // Calculate statistics safely
            long newOrders = 0;
            long processingOrders = 0;
            long shippedOrders = 0;
            long deliveredOrders = 0;
            
            if (orders != null && !orders.isEmpty()) {
                newOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equals("CREATED")).count();
                processingOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equals("PROCESSING")).count();
                shippedOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equals("SHIPPED")).count();
                deliveredOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus().name().equals("DELIVERED")).count();
            }

            model.addAttribute("orders", orders);
            model.addAttribute("orderStatuses", OrderStatus.values());
            model.addAttribute("paymentStatuses", PaymentStatus.values());
            model.addAttribute("newOrders", newOrders);
            model.addAttribute("processingOrders", processingOrders);
            model.addAttribute("shippedOrders", shippedOrders);
            model.addAttribute("deliveredOrders", deliveredOrders);

            System.out.println("AdminController: Successfully loaded orders page");
            return "admin/orders";
        } catch (Exception e) {
            System.err.println("AdminController: Error loading orders page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading orders: " + e.getMessage());
            model.addAttribute("orders", new ArrayList<>());
            model.addAttribute("newOrders", 0);
            model.addAttribute("processingOrders", 0);
            model.addAttribute("shippedOrders", 0);
            model.addAttribute("deliveredOrders", 0);
            return "admin/orders";
        }
    }


    @GetMapping("/orders/{id}")
    public String viewOrderDetail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, 
                                   @RequestParam(required = false) String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Updating order " + id + " status to: " + status);
            
            // Validate status parameter
            if (status == null || status.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Status is required");
                return "redirect:/admin/orders";
            }
            
            OrderStatus orderStatus;
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status: " + status);
                return "redirect:/admin/orders";
            }
            
            Order updatedOrder = orderService.updateOrderStatus(id, orderStatus);
            redirectAttributes.addFlashAttribute("success", "Order " + updatedOrder.getOrderNumber() + " status updated to " + orderStatus + "!");
            
        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to update order status: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/test-orders")
    public String testOrders(Model model) {
        try {
            System.out.println("AdminController: Testing orders endpoint...");
            List<Order> orders = orderService.getAllOrders();
            model.addAttribute("message", "Orders test successful! Found " + (orders != null ? orders.size() : 0) + " orders");
            model.addAttribute("orders", orders);
            return "admin/test-status";
        } catch (Exception e) {
            System.err.println("AdminController: Error in test orders: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error testing orders: " + e.getMessage());
            return "admin/test-status";
        }
    }

    @GetMapping("/test-status-update/{id}")
    public String testStatusUpdate(@PathVariable Long id, Model model) {
        try {
            Order order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            model.addAttribute("message", "Order found: " + order.getOrderNumber() + " with status: " + order.getStatus());
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        return "admin/test-status";
    }

    @PostMapping("/test-status-update/{id}")
    public String testStatusUpdatePost(@PathVariable Long id, 
                                      @RequestParam(required = false) String status,
                                      @RequestParam Map<String, String> allParams,
                                      RedirectAttributes redirectAttributes) {
        System.out.println("=== DEBUG INFO ===");
        System.out.println("Order ID: " + id);
        System.out.println("Status parameter: " + status);
        System.out.println("All parameters: " + allParams);
        System.out.println("==================");
        
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                Order updatedOrder = orderService.updateOrderStatus(id, orderStatus);
                redirectAttributes.addFlashAttribute("message", "Status updated successfully to: " + orderStatus);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "No status parameter received");
        }
        
        return "redirect:/admin/test-status-update/" + id;
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        List<Order> orders = orderService.getAllOrders();
        
        // Calculate revenue
        double totalRevenue = orders.stream()
            .filter(o -> o.getPaymentStatus() == PaymentStatus.COMPLETED)
            .mapToDouble(o -> o.getTotalAmount().doubleValue())
            .sum();
        
        // Order statistics
        long newOrders = orders.stream().filter(o -> o.getStatus().name().equals("CREATED")).count();
        long processingOrders = orders.stream().filter(o -> o.getStatus().name().equals("PROCESSING")).count();
        long shippedOrders = orders.stream().filter(o -> o.getStatus().name().equals("SHIPPED")).count();
        long deliveredOrders = orders.stream().filter(o -> o.getStatus().name().equals("DELIVERED")).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus().name().equals("CANCELLED")).count();
        
        model.addAttribute("orders", orders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("newOrders", newOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("shippedOrders", shippedOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        
        return "admin/reports";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }
    // Show Add User form
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    // View user details
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "admin/user-view"; // new template
    }

    // Show edit user form
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/user-form"; // reuse same form for add/edit
    }

    // Handle update user
    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @Validated(UpdateGroup.class) @ModelAttribute("user") User user,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }

        try {
            user.setId(id);
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("error", e.getMessage());
            return "admin/user-form";
        }
    }

    // Delete user
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }


    // Handle Add User form submission
    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }

        try {
            userService.registerUser(user); // service handles password encoding & defaults
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("error", e.getMessage());
            return "admin/user-form";
        }
    }

    // Initialize test data endpoint
    @PostMapping("/init-data")
    public String initializeTestData(RedirectAttributes redirectAttributes) {
        try {
            // Clear existing products and reinitialize
            List<Product> existingProducts = productService.getAllProducts();
            for (Product product : existingProducts) {
                productService.deleteProduct(product.getId());
            }
            
            // Reinitialize products
            dataInitializationService.initializeProducts();
            
            int productCount = productService.getAllProducts().size();
            redirectAttributes.addFlashAttribute("success", "Test data initialized successfully! " + 
                productCount + " products added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to initialize test data: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // Simple test endpoint to add products
    @GetMapping("/test-add-products")
    public String testAddProducts(RedirectAttributes redirectAttributes) {
        try {
            dataInitializationService.initializeProducts();
            redirectAttributes.addFlashAttribute("success", "Products added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding products: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

}
