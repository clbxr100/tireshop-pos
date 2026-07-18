package com.tireshop.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TechnicianPerformanceReportItem {
    private String serviceName;
    private LocalDateTime serviceDate;
    private String saleInvoiceNumber; // To link back to the sale
    private BigDecimal servicePrice; // Price of this specific service instance
    // Could add customer name, vehicle later if needed

    public TechnicianPerformanceReportItem(String serviceName, LocalDateTime serviceDate, String saleInvoiceNumber, BigDecimal servicePrice) {
        this.serviceName = serviceName;
        this.serviceDate = serviceDate;
        this.saleInvoiceNumber = saleInvoiceNumber;
        this.servicePrice = servicePrice;
    }

    // Getters
    public String getServiceName() {
        return serviceName;
    }

    public LocalDateTime getServiceDate() {
        return serviceDate;
    }

    public String getSaleInvoiceNumber() {
        return saleInvoiceNumber;
    }

    public BigDecimal getServicePrice() {
        return servicePrice;
    }
} 