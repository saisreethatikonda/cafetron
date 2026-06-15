package com.cafetron.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned by /api/admin/reports/status-breakdown
 *
 * overall_status values in DB (v2.0 multi-vendor schema):
 *   PENDING_VENDOR, VENDOR_ACCEPTED, VENDOR_DECLINED,
 *   READY_FOR_PICKUP, COLLECTED, CANCELLED
 */
@Data
@AllArgsConstructor
public class StatusCountDTO {
    private String status;
    private Long count;
}