package com.tireshop.model.dto;

import java.math.BigDecimal;

public class SalesSummaryData {
    private int numberOfSales;
    private BigDecimal totalRevenue;

    public SalesSummaryData(int numberOfSales, BigDecimal totalRevenue) {
        this.numberOfSales = numberOfSales;
        this.totalRevenue = totalRevenue;
    }

    public int getNumberOfSales() {
        return numberOfSales;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
} 