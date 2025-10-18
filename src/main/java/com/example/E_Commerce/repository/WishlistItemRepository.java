package com.example.E_Commerce.repository;

import com.example.E_Commerce.model.Product;
import com.example.E_Commerce.model.Wishlist;
import com.example.E_Commerce.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    
    Optional<WishlistItem> findByWishlistAndProduct(Wishlist wishlist, Product product);
    
    boolean existsByWishlistAndProduct(Wishlist wishlist, Product product);
    
    void deleteByWishlistAndProduct(Wishlist wishlist, Product product);
}
