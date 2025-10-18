package com.example.E_Commerce.repository;

import com.example.E_Commerce.model.OrderItem;
import com.example.E_Commerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrder(Order order);
    
    void deleteByOrder(Order order);
}
