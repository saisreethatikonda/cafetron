package com.cafetron.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Summarises vendor declines per day.
 * Useful for admin to see which vendors are declining frequently.
 * Returned by GET /api/admin/reports/vendor-declines?date=
 */
@Data
@AllArgsConstructor
public class VendorDeclineSummaryDTO {
    private Long vendorId;
    private String vendorName;
    private Long declineCount;
    private BigDecimal totalRefunded;
}