package com.cafetron.vendor.repository;

import com.cafetron.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByIsActiveTrue();
    Optional<Vendor> findByEmail(String email);
    boolean existsByEmail(String email);
}

