package com.cafetron.order.repository;

import com.cafetron.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
       List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
       Order findByOrderIdAndUserId(Long orderId, Long userId);
 }