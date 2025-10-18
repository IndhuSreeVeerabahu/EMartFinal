package com.example.E_Commerce.controller;

import com.example.E_Commerce.model.User;
import com.example.E_Commerce.model.WishlistItem;
import com.example.E_Commerce.service.CartService;
import com.example.E_Commerce.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private CartService cartService;

    @GetMapping
    public String viewWishlist(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<WishlistItem> wishlistItems = wishlistService.getWishlistItems(user.getId());
        int wishlistItemCount = wishlistService.getWishlistItemCount(user.getId());
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistItemCount", wishlistItemCount);
        return "wishlist";
    }

    @PostMapping("/add")
    public String addToWishlist(@RequestParam Long productId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            wishlistService.addToWishlist(user.getId(), productId);
            redirectAttributes.addFlashAttribute("success", "Product added to wishlist!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/product/" + productId;
    }

    @PostMapping("/remove")
    public String removeFromWishlist(@RequestParam Long productId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            wishlistService.removeFromWishlist(user.getId(), productId);
            redirectAttributes.addFlashAttribute("success", "Product removed from wishlist!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wishlist";
    }

    @PostMapping("/clear")
    public String clearWishlist(Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            wishlistService.clearWishlist(user.getId());
            redirectAttributes.addFlashAttribute("success", "Wishlist cleared!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wishlist";
    }

    @PostMapping("/move-to-cart")
    public String moveToCart(@RequestParam Long productId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            // Remove from wishlist
            wishlistService.removeFromWishlist(user.getId(), productId);
            // Add to cart
            cartService.addToCart(user.getId(), productId, 1);
            redirectAttributes.addFlashAttribute("success", "Product moved to cart!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wishlist";
    }

    @GetMapping("/check/{productId}")
    @ResponseBody
    public boolean isProductInWishlist(@PathVariable Long productId,
                                      Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return wishlistService.isProductInWishlist(user.getId(), productId);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
