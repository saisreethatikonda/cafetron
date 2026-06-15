package com.cafetron.order.dto;

public record OrderDetailItemResponse(

        Long menuItemId,
        String itemName,
        Integer quantity,
        java.math.BigDecimal unitPrice,
        String vendorStatus
) {}

