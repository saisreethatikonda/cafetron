package com.cafetron.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Summarises vendor declines per day.
 * Useful for admin to see which vendors are declining frequently.
 * Returned by GET /api/admin/reports/vendor-declines?date=
 */
@Data
public class VendorDeclineSummaryDTO {
    private Long vendorId;
    private String vendorName;
    private Long declineCount;
    private BigDecimal totalRefunded;

    public VendorDeclineSummaryDTO(Long vendorId,
                                   String vendorName,
                                   Long declineCount,
                                   BigDecimal totalRefunded) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.declineCount = declineCount;
        this.totalRefunded = totalRefunded;
    }

    public VendorDeclineSummaryDTO(Long vendorId,
                                   String vendorName,
                                   Long declineCount,
                                   Number totalRefunded) {
        this(vendorId,
                vendorName,
                declineCount,
                totalRefunded == null ? BigDecimal.ZERO : BigDecimal.valueOf(totalRefunded.doubleValue()));
    }

    public VendorDeclineSummaryDTO(Long vendorId,
                                   String vendorName,
                                   Long declineCount,
                                   int totalRefunded) {
        this(vendorId, vendorName, declineCount, BigDecimal.valueOf(totalRefunded));
    }
}