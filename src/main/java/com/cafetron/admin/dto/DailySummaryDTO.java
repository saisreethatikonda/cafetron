package com.cafetron.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailySummaryDTO {

    private LocalDate date;
    private Long orderCount;
    private BigDecimal revenue;
    private Long itemsSold;

    // Used by JPQL query — date is injected by service afterward
    public DailySummaryDTO(Long orderCount, BigDecimal revenue, Long itemsSold) {
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.itemsSold = itemsSold;
    }

    // Used by service for zero-result fallback
    public DailySummaryDTO(LocalDate date, Long orderCount,
                           BigDecimal revenue, Long itemsSold) {
        this.date = date;
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.itemsSold = itemsSold;
    }
}