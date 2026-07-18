package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed sale/transaction
 */
@Entity
@Table(name = "sales")
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String invoiceNumber;
    private LocalDateTime timestamp;
    
    @ManyToOne
    private Customer customer;
    
    @ManyToOne
    private Vehicle vehicle;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SaleItem> items = new ArrayList<>();
    
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal creditCardFeeAmount;
    
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
    
    // Additional payment details
    private String cardLastFour;
    private String cardType;
    private String checkNumber;
    private String authorizationCode;
    
    private boolean isPaid;
    
    @Lob // For potentially long text
    private String notes;
    
    // Constructors
    public Sale() {
        this.timestamp = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.tax = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.isPaid = false;
        this.creditCardFeeAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getCreditCardFeeAmount() {
        return creditCardFeeAmount;
    }

    public void setCreditCardFeeAmount(BigDecimal creditCardFeeAmount) {
        this.creditCardFeeAmount = creditCardFeeAmount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }
    
    // For backward compatibility
    public String getPaymentMethod() {
        return paymentType != null ? paymentType.getDisplayName() : null;
    }

    public void setPaymentMethod(String paymentMethod) {
        if (paymentMethod != null) {
            for (PaymentType type : PaymentType.values()) {
                if (type.getDisplayName().equalsIgnoreCase(paymentMethod)) {
                    this.paymentType = type;
                    return;
                }
            }
        }
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

    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
    
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    // Helper methods
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
        recalculateAmounts();
    }
    
    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setSale(null);
        recalculateAmounts();
    }
    
    public void recalculateAmounts(BigDecimal taxRate, BigDecimal creditCardFeePercentage) {
        this.subtotal = BigDecimal.ZERO;
        for (SaleItem item : items) {
            this.subtotal = this.subtotal.add(item.getSubtotal());
        }
        
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) >= 0) {
            this.tax = this.subtotal.multiply(taxRate);
        } else {
            this.tax = BigDecimal.ZERO; 
        }

        BigDecimal subtotalPlusTax = this.subtotal.add(this.tax);
        this.creditCardFeeAmount = BigDecimal.ZERO; // Default to zero

        // Apply credit card fee only if payment type is CREDIT_CARD and fee percentage is positive
        // Note: This check on paymentType might be better handled in SalesService when paymentType is set.
        // For now, we make recalculateAmounts flexible.
        if (PaymentType.CREDIT_CARD.equals(this.paymentType) && 
            creditCardFeePercentage != null && 
            creditCardFeePercentage.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate fee on subtotal + tax
            this.creditCardFeeAmount = subtotalPlusTax.multiply(creditCardFeePercentage);
        }
        
        this.total = subtotalPlusTax.add(this.creditCardFeeAmount);
    }

    // Overloaded versions for existing calls
    public void recalculateAmounts(BigDecimal taxRate) {
        recalculateAmounts(taxRate, BigDecimal.ZERO); // Assume no CC fee if not specified
    }

    public void recalculateAmounts() {
        // This version might need access to SettingsService to get current rates,
        // or it should be called by a service that provides them.
        // For now, defaulting to 0 tax and 0 fee if no rates are passed.
        recalculateAmounts(BigDecimal.ZERO, BigDecimal.ZERO); 
    }
} 