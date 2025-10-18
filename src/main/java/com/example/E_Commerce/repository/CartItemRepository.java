package com.example.E_Commerce.repository;

import com.example.E_Commerce.model.CartItem;
import com.example.E_Commerce.model.Cart;
import com.example.E_Commerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    
    void deleteByCartAndProduct(Cart cart, Product product);
    
    void deleteByCart(Cart cart);
}
