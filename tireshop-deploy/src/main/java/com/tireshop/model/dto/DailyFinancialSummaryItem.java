package com.tireshop.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyFinancialSummaryItem {
    private LocalDate date;
    private BigDecimal totalSubtotal;
    private BigDecimal totalTax;
    private BigDecimal totalGrossRevenue;
    // private BigDecimal totalCostOfGoodsSold; // For profit margin - future
    // private BigDecimal totalProfit;          // For profit margin - future

    public DailyFinancialSummaryItem(LocalDate date, BigDecimal totalSubtotal, BigDecimal totalTax, BigDecimal totalGrossRevenue) {
        this.date = date;
        this.totalSubtotal = totalSubtotal;
        this.totalTax = totalTax;
        this.totalGrossRevenue = totalGrossRevenue;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public BigDecimal getTotalSubtotal() { return totalSubtotal; }
    public BigDecimal getTotalTax() { return totalTax; }
    public BigDecimal getTotalGrossRevenue() { return totalGrossRevenue; }
} 