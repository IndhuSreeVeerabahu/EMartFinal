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

@Controller
@RequestMapping("/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public String viewCart(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Cart cart = cartService.getCart(user.getId());
        int wishlistItemCount = wishlistService.getWishlistItemCount(user.getId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getCartItems());
        model.addAttribute("wishlistItemCount", wishlistItemCount);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity,
                           Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            cartService.addToCart(user.getId(), productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Product added to cart!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products";
    }

    @PostMapping("/update")
    public String updateCartItem(@RequestParam Long productId, @RequestParam Integer quantity,
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            cartService.updateCartItemQuantity(user.getId(), productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Cart updated!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long productId, Authentication authentication, 
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            cartService.removeFromCart(user.getId(), productId);
            redirectAttributes.addFlashAttribute("success", "Product removed from cart!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            cartService.clearCart(user.getId());
            redirectAttributes.addFlashAttribute("success", "Cart cleared!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }
}
