package com.cafetron.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned by /api/admin/reports/top-items
 * Ranked by total quantity sold across all orders.
 */
@Data
@AllArgsConstructor
public class TopItemDTO {
    private Long menuItemId;
    private String name;
    private Long qtySold;
}