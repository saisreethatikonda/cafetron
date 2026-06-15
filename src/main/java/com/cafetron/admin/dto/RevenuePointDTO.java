package com.cafetron.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RevenuePointDTO {

    private LocalDate date;
    private BigDecimal revenue;

    // Used by JPQL — date injected by service
    public RevenuePointDTO(BigDecimal revenue) {
        this.revenue = revenue;
    }

    // Used for direct construction
    public RevenuePointDTO(LocalDate date, BigDecimal revenue) {
        this.date = date;
        this.revenue = revenue;
    }
}