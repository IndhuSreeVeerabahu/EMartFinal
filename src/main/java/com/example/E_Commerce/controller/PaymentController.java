package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.Order;
import com.example.E_Commerce.model.PaymentStatus;
import com.example.E_Commerce.model.User;
import com.example.E_Commerce.service.OrderService;
import com.example.E_Commerce.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/payment")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId, Model model, Authentication authentication) {
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
            
            String paymentSessionId = paymentService.createPaymentSession(order);
            logger.info("Payment session created: {}", paymentSessionId);
            
            model.addAttribute("order", order);
            model.addAttribute("paymentSessionId", paymentSessionId);
            model.addAttribute("cashfreeAppId", paymentService.getCashfreeAppId());
            model.addAttribute("environment", paymentService.getCashfreeEnvironment());
            
            logger.info("Payment page attributes set - Order ID: {}, Session ID: {}", order.getId(), paymentSessionId);
            
            return "payment";
        } catch (Exception e) {
            logger.error("Error loading payment page for order {}: {}", orderId, e.getMessage(), e);
            return "redirect:/orders";
        }
    }

    @PostMapping("/verify")
    public String verifyPayment(@RequestParam String orderId, 
                              @RequestParam String paymentId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(Long.parseLong(orderId));
            
            if (!order.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/orders";
            }
            
            // Verify payment with Cashfree
            boolean isPaymentValid = paymentService.verifyPayment(orderId, paymentId);
            
            if (isPaymentValid) {
                orderService.updatePaymentStatus(order.getId(), PaymentStatus.COMPLETED);
                orderService.updateRazorpayPaymentId(order.getId(), paymentId); // Reusing field for Cashfree payment ID
                redirectAttributes.addFlashAttribute("success", "Payment successful! Order confirmed.");
                return "redirect:/orders/" + order.getId();
            } else {
                orderService.updatePaymentStatus(order.getId(), PaymentStatus.FAILED);
                redirectAttributes.addFlashAttribute("error", "Payment verification failed. Please try again.");
                return "redirect:/checkout/payment/" + order.getId();
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment verification failed");
            return "redirect:/orders";
        }
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam(required = false) String order_id,
                               @RequestParam(required = false) String cf_payment_id,
                               @RequestParam(required = false) String payment_status,
                               @RequestParam(required = false) String orderId,
                               @RequestParam(required = false) String paymentId,
                               @RequestParam(required = false) String status,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            // Handle different parameter names from Cashfree
            String actualOrderId = order_id != null ? order_id : orderId;
            String actualPaymentId = cf_payment_id != null ? cf_payment_id : paymentId;
            String actualStatus = payment_status != null ? payment_status : status;
            
            logger.info("Payment success callback - Order: {}, Payment: {}, Status: {}", actualOrderId, actualPaymentId, actualStatus);
            logger.info("All parameters - order_id: {}, cf_payment_id: {}, payment_status: {}, orderId: {}, paymentId: {}, status: {}", 
                       order_id, cf_payment_id, payment_status, orderId, paymentId, status);
            
            if (actualOrderId != null && actualPaymentId != null) {
                User user = (User) authentication.getPrincipal();
                Order order = orderService.getOrderById(Long.parseLong(actualOrderId));
                
                if (order == null) {
                    logger.error("Order not found for ID: {}", actualOrderId);
                    redirectAttributes.addFlashAttribute("error", "Order not found");
                    return "redirect:/orders";
                }
                
                if (order.getUser().getId().equals(user.getId())) {
                    // Verify payment with Cashfree API
                    logger.info("Verifying payment with Cashfree API");
                    boolean isPaymentValid = paymentService.verifyPayment(actualOrderId, actualPaymentId);
                    
                    if (isPaymentValid) {
                        logger.info("Payment verified successfully with Cashfree");
                        orderService.updatePaymentStatus(order.getId(), PaymentStatus.COMPLETED);
                        orderService.updateRazorpayPaymentId(order.getId(), actualPaymentId);
                        redirectAttributes.addFlashAttribute("success", "Payment successful! Order confirmed.");
                        return "redirect:/orders/" + order.getId();
                    } else {
                        logger.warn("Payment verification failed for order: {}, payment: {}", actualOrderId, actualPaymentId);
                        orderService.updatePaymentStatus(order.getId(), PaymentStatus.FAILED);
                        redirectAttributes.addFlashAttribute("error", "Payment verification failed. Please contact support.");
                        return "redirect:/orders";
                    }
                } else {
                    logger.error("Unauthorized access to order {} by user {}", actualOrderId, user.getId());
                    redirectAttributes.addFlashAttribute("error", "Unauthorized access to order");
                    return "redirect:/orders";
                }
            }
            
            // If we reach here, payment might be successful but parameters are missing
            // This can happen when Cashfree redirects without proper parameters
            logger.warn("Payment callback with missing parameters - order_id: {}, cf_payment_id: {}", actualOrderId, actualPaymentId);
            logger.info("This might be a successful payment with missing callback parameters");
            
            redirectAttributes.addFlashAttribute("success", "Payment completed! Please check your orders for confirmation.");
            return "redirect:/orders";
            
        } catch (Exception e) {
            logger.error("Payment success processing failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Payment processing failed");
            return "redirect:/orders";
        }
    }

    @GetMapping("/test/{orderId}")
    public String testPayment(@PathVariable Long orderId, Model model, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUser().getId().equals(user.getId())) {
                return "redirect:/orders";
            }
            
            model.addAttribute("order", order);
            return "test-payment";
        } catch (Exception e) {
            return "redirect:/orders";
        }
    }

    @PostMapping("/test/success/{orderId}")
    public String testPaymentSuccess(@PathVariable Long orderId, Authentication authentication, 
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/orders";
            }
            
            // Simulate successful payment
            orderService.updateRazorpayPaymentId(orderId, "test_cashfree_payment_" + System.currentTimeMillis());
            orderService.updatePaymentStatus(orderId, PaymentStatus.COMPLETED);
            
            redirectAttributes.addFlashAttribute("success", "Test payment successful! Order confirmed.");
            return "redirect:/orders/" + orderId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Test payment failed");
            return "redirect:/orders";
        }
    }

    @PostMapping("/test/fail/{orderId}")
    public String testPaymentFail(@PathVariable Long orderId, Authentication authentication, 
                                RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/orders";
            }
            
            orderService.updatePaymentStatus(orderId, PaymentStatus.FAILED);
            
            redirectAttributes.addFlashAttribute("error", "Test payment failed. Please try again.");
            return "redirect:/checkout/payment/" + orderId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Test payment simulation failed");
            return "redirect:/orders";
        }
    }

    @PostMapping("/webhook")
    public String handleWebhook(@RequestBody String payload, 
                              @RequestHeader(value = "x-webhook-signature", required = false) String signature) {
        try {
            logger.info("Received Cashfree webhook: {}", payload);
            
            // Verify webhook signature (for production)
            if (signature != null && !paymentService.verifyWebhookSignature(payload, signature)) {
                logger.error("Invalid webhook signature");
                return "ERROR";
            }
            
            // Parse webhook payload and update order status
            // In production, you would parse the JSON and update order status accordingly
            
            return "OK";
        } catch (Exception e) {
            logger.error("Webhook processing failed: {}", e.getMessage());
            return "ERROR";
        }
    }
}