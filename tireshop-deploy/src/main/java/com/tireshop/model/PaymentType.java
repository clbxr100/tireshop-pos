package com.tireshop.model;

/**
 * Enum representing different payment types supported by the system
 */
public enum PaymentType {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    CHECK("Check"),
    GIFT_CARD("Gift Card"),
    FINANCING("Financing"),
    STORE_CREDIT("Store Credit");
    
    private final String displayName;
    
    PaymentType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 