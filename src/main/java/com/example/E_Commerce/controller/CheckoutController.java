package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.json.JSONObject;

@Controller
@RequestMapping("/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public String checkoutForm(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Cart cart = cartService.getCart(user.getId());
        
        if (cart.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }
        
        model.addAttribute("cart", cart);
        model.addAttribute("order", new Order());
        model.addAttribute("cashfreeAppId", paymentService.getCashfreeAppId());
        return "checkout";
    }

    @PostMapping
    public String createOrder(@ModelAttribute Order order, Authentication authentication, 
                             RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Order createdOrder = orderService.createOrder(user.getId(), 
                order.getShippingAddress(), order.getBillingAddress());
            
            redirectAttributes.addFlashAttribute("order", createdOrder);
            return "redirect:/checkout/payment/" + createdOrder.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/payment/{orderId}")
    public String paymentPage(@PathVariable Long orderId, Model model, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(orderId);
            
            if (order == null) {
                logger.error("Order not found for ID: {}", orderId);
                return "redirect:/orders";
            }
            
            if (!order.getUser().getId().equals(user.getId())) {
                logger.error("Unauthorized access to order {} by user {}", orderId, user.getId());
                return "redirect:/orders";
            }
            
            // Create Cashfree payment session
            String paymentSessionId = paymentService.createPaymentSession(order);
            
            model.addAttribute("order", order);
            model.addAttribute("paymentSessionId", paymentSessionId);
            model.addAttribute("cashfreeAppId", paymentService.getCashfreeAppId());
            model.addAttribute("environment", paymentService.getCashfreeEnvironment());
            return "payment";
        } catch (Exception e) {
            logger.error("Error loading payment page for order {}: {}", orderId, e.getMessage(), e);
            try {
                User user = (User) authentication.getPrincipal();
                Order order = orderService.getOrderById(orderId);
                if (order != null && order.getUser().getId().equals(user.getId())) {
                    model.addAttribute("order", order);
                    model.addAttribute("cashfreeAppId", paymentService.getCashfreeAppId());
                    model.addAttribute("error", "Payment gateway initialization failed. Use test payment option.");
                    return "payment";
                }
            } catch (Exception ex) {
                logger.error("Error in fallback payment page: {}", ex.getMessage());
            }
            return "redirect:/orders";
        }
    }

    @PostMapping("/payment/verify")
    public String verifyPayment(@RequestParam String razorpayOrderId,
                               @RequestParam String razorpayPaymentId,
                               @RequestParam String razorpaySignature,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(Long.parseLong(razorpayOrderId));
            
            if (!order.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/orders";
            }
            
            boolean isValid = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId);
            
            if (isValid) {
                orderService.updateRazorpayPaymentId(order.getId(), razorpayPaymentId);
                orderService.updatePaymentStatus(order.getId(), PaymentStatus.COMPLETED);
                redirectAttributes.addFlashAttribute("success", "Payment successful!");
                return "redirect:/orders/" + order.getId();
            } else {
                orderService.updatePaymentStatus(order.getId(), PaymentStatus.FAILED);
                redirectAttributes.addFlashAttribute("error", "Payment verification failed");
                return "redirect:/checkout/payment/" + order.getId();
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed");
            return "redirect:/checkout";
        }
    }

    // Webhook endpoint for Cashfree payment notifications
    @PostMapping("/webhook/cashfree")
    public ResponseEntity<String> handleCashfreeWebhook(@RequestBody String payload, 
                                                       @RequestHeader("x-webhook-signature") String signature) {
        try {
            // Parse webhook payload
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("type");
            
            if ("PAYMENT_SUCCESS_WEBHOOK".equals(event)) {
                // Handle successful payment
                JSONObject data = webhookData.getJSONObject("data");
                String paymentId = data.getString("cf_payment_id");
                String orderId = data.getString("order_id");
                
                // Update order status
                Order order = orderService.getOrderById(Long.parseLong(orderId));
                if (order != null) {
                    orderService.updateRazorpayPaymentId(order.getId(), paymentId); // Reusing field
                    orderService.updatePaymentStatus(order.getId(), PaymentStatus.COMPLETED);
                }
            } else if ("PAYMENT_FAILED_WEBHOOK".equals(event)) {
                // Handle failed payment
                JSONObject data = webhookData.getJSONObject("data");
                String orderId = data.getString("order_id");
                
                Order order = orderService.getOrderById(Long.parseLong(orderId));
                if (order != null) {
                    orderService.updatePaymentStatus(order.getId(), PaymentStatus.FAILED);
                }
            }
            
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }
}
