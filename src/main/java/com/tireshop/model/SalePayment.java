package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single payment transaction for a sale
 * Supports split payments where multiple payment methods can be used
 */
@Entity
@Table(name = "sale_payments")
public class SalePayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp;
    
    // Card-specific fields
    private String cardLastFour;
    private String cardType;
    private String authorizationCode;
    
    // Check-specific fields
    private String checkNumber;
    
    // Notes for this specific payment
    private String notes;
    
    // Constructors
    public SalePayment() {
        this.paymentTimestamp = LocalDateTime.now();
    }
    
    public SalePayment(Sale sale, PaymentType paymentType, BigDecimal amount) {
        this.sale = sale;
        this.paymentType = paymentType;
        this.amount = amount;
        this.paymentTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentTimestamp() {
        return paymentTimestamp;
    }

    public void setPaymentTimestamp(LocalDateTime paymentTimestamp) {
        this.paymentTimestamp = paymentTimestamp;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getPaymentSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(paymentType.getDisplayName()).append(": $").append(String.format("%.2f", amount));
        
        if (paymentType == PaymentType.CREDIT_CARD || paymentType == PaymentType.DEBIT_CARD) {
            if (cardType != null && !cardType.isEmpty()) {
                sb.append(" (").append(cardType);
                if (cardLastFour != null && !cardLastFour.isEmpty() && !cardLastFour.equals("XXXX")) {
                    sb.append(" ****").append(cardLastFour);
                }
                sb.append(")");
            }
        } else if (paymentType == PaymentType.CHECK && checkNumber != null && !checkNumber.isEmpty()) {
            sb.append(" (Check #").append(checkNumber).append(")");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getPaymentSummary();
    }
}

