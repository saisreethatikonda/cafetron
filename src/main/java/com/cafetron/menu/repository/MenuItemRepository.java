package com.cafetron.menu.repository;

import com.cafetron.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByIsAvailableTrue();
    List<MenuItem> findByItemNameContainingIgnoreCase(String name);
    List<MenuItem> findByFoodType(String foodType);
    List<MenuItem> findByVendorId(Long vendorId);
}
