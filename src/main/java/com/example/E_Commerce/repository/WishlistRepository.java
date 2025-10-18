package com.example.E_Commerce.repository;

import com.example.E_Commerce.model.User;
import com.example.E_Commerce.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    Optional<Wishlist> findByUser(User user);
    
    Optional<Wishlist> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
