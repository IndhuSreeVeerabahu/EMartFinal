package com.example.E_Commerce.service;

import com.example.E_Commerce.model.*;
import com.example.E_Commerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public Wishlist getOrCreateWishlist(User user) {
        Optional<Wishlist> existingWishlist = wishlistRepository.findByUser(user);
        if (existingWishlist.isPresent()) {
            return existingWishlist.get();
        }
        
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        return wishlistRepository.save(wishlist);
    }

    public Wishlist addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Wishlist wishlist = getOrCreateWishlist(user);

        // Check if product is already in wishlist
        if (wishlistItemRepository.existsByWishlistAndProduct(wishlist, product)) {
            throw new RuntimeException("Product is already in your wishlist");
        }

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setWishlist(wishlist);
        wishlistItem.setProduct(product);
        
        wishlistItemRepository.save(wishlistItem);
        wishlist.addWishlistItem(wishlistItem);
        
        return wishlist;
    }

    public void removeFromWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        WishlistItem wishlistItem = wishlistItemRepository.findByWishlistAndProduct(wishlist, product)
                .orElseThrow(() -> new RuntimeException("Product not found in wishlist"));

        wishlistItemRepository.delete(wishlistItem);
        wishlist.removeWishlistItem(wishlistItem);
    }

    public Wishlist getWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return wishlistRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));
    }

    public List<WishlistItem> getWishlistItems(Long userId) {
        try {
            Wishlist wishlist = getWishlist(userId);
            return wishlist.getWishlistItems();
        } catch (RuntimeException e) {
            // Return empty list if wishlist doesn't exist yet
            return List.of();
        }
    }

    public void clearWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        wishlistItemRepository.deleteAll(wishlist.getWishlistItems());
        wishlist.getWishlistItems().clear();
    }

    public boolean isProductInWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<Wishlist> wishlist = wishlistRepository.findByUser(user);
        if (wishlist.isEmpty()) {
            return false;
        }

        return wishlistItemRepository.existsByWishlistAndProduct(wishlist.get(), product);
    }

    public int getWishlistItemCount(Long userId) {
        try {
            Wishlist wishlist = getWishlist(userId);
            return wishlist.getTotalItems();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
