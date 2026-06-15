package com.cafetron.pickup.repository;

import com.cafetron.pickup.VendorOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorOrderStatusRepository extends JpaRepository<VendorOrderStatus, Long> {
    List<VendorOrderStatus> findByOrderItem_Order_Id(Long orderId);
}

