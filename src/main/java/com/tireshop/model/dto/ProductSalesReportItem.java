package com.tireshop.model.dto;

import java.math.BigDecimal;

public class ProductSalesReportItem {
    private String productName;
    private String productCategory;
    private int quantitySold;
    private BigDecimal totalRevenue;

    public ProductSalesReportItem(String productName, String productCategory, int quantitySold, BigDecimal totalRevenue) {
        this.productName = productName;
        this.productCategory = productCategory;
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    // Setters if needed, but typically DTOs for reports are read-only after creation
} 