package com.cafetron.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by GET /api/admin/config
 * Shows current window state and cutoff time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpsStatusDTO {
    private boolean windowOpen;
    private String cutoffTime;    // "HH:mm"
    private boolean cutoffPassed;
}