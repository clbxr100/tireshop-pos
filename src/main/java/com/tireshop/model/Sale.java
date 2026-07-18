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
    
    @Column(name = "createdDate")
    private LocalDateTime createdDate; // When invoice was created
    
    @Column(name = "paidDate")
    private LocalDateTime paidDate; // When payment was received
    
    private LocalDateTime timestamp; // For backward compatibility - uses paidDate if paid, createdDate if not
    
    @ManyToOne(fetch = FetchType.EAGER)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    private Vehicle vehicle;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SaleItem> items = new ArrayList<>();
    
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal creditCardFeeAmount;

    @Column(name = "discountAmount")
    private BigDecimal discountAmount; // Sale-level discount (e.g. military discount)

    @Column(name = "discountReason")
    private String discountReason; // e.g. "Military (10%)"
    
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

    @Column(name = "poNumber")
    private String poNumber;

    // Void-related fields
    @Column(name = "isVoided", nullable = false, columnDefinition = "boolean default false")
    private boolean isVoided = false;
    
    @Column(name = "voidReason")
    private String voidReason;
    
    @Column(name = "voidTimestamp")
    private LocalDateTime voidTimestamp;
    
    // Partial return tracking
    @Column(name = "hasPartialReturn", nullable = false, columnDefinition = "boolean default false")
    private boolean hasPartialReturn = false;
    
    // Constructors
    public Sale() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.timestamp = now; // Initial timestamp is creation date
        this.subtotal = BigDecimal.ZERO;
        this.tax = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.isPaid = false;
        this.creditCardFeeAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDateTime paidDate) {
        this.paidDate = paidDate;
        // Update timestamp for backward compatibility
        if (paidDate != null) {
            this.timestamp = paidDate;
        }
    }

    public LocalDateTime getTimestamp() {
        // For backward compatibility, return paidDate if paid, createdDate otherwise
        return (paidDate != null) ? paidDate : (createdDate != null ? createdDate : timestamp);
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

    public BigDecimal getDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
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

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }
    
    public boolean isVoided() {
        return isVoided;
    }

    public void setVoided(boolean voided) {
        isVoided = voided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public LocalDateTime getVoidTimestamp() {
        return voidTimestamp;
    }

    public void setVoidTimestamp(LocalDateTime voidTimestamp) {
        this.voidTimestamp = voidTimestamp;
    }
    
    public boolean hasPartialReturn() {
        return hasPartialReturn;
    }

    public void setHasPartialReturn(boolean hasPartialReturn) {
        this.hasPartialReturn = hasPartialReturn;
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
        BigDecimal taxableSubtotal = BigDecimal.ZERO;
        
        for (SaleItem item : items) {
            // Guard against NULL subtotals from older database rows
            BigDecimal itemSubtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
            this.subtotal = this.subtotal.add(itemSubtotal);
            // Only add to taxable subtotal if the item is taxable
            if (item.isTaxable()) {
                taxableSubtotal = taxableSubtotal.add(itemSubtotal);
            }
        }
        
        // Apply sale-level discount (e.g. military) - never more than the subtotal
        BigDecimal discount = getDiscountAmount();
        if (discount.compareTo(this.subtotal) > 0) {
            discount = this.subtotal;
        }
        BigDecimal discountedSubtotal = this.subtotal.subtract(discount);
        // The discount also reduces the taxable base (floor at zero)
        BigDecimal discountedTaxable = taxableSubtotal.subtract(discount);
        if (discountedTaxable.compareTo(BigDecimal.ZERO) < 0) {
            discountedTaxable = BigDecimal.ZERO;
        }

        // Check if customer is tax exempt
        boolean isTaxExempt = (customer != null && customer.isTaxExempt());

        if (!isTaxExempt && taxRate != null && taxRate.compareTo(BigDecimal.ZERO) >= 0) {
            // Apply tax only to discounted taxable amount
            this.tax = discountedTaxable.multiply(taxRate);
        } else {
            this.tax = BigDecimal.ZERO;
        }

        BigDecimal subtotalPlusTax = discountedSubtotal.add(this.tax);
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice #").append(invoiceNumber);
        
        if (customer != null) {
            sb.append(" - ").append(customer.getFullName());
        }
        
        if (timestamp != null) {
            sb.append(" (").append(timestamp.toLocalDate()).append(")");
        }
        
        if (total != null) {
            sb.append(" - $").append(String.format("%.2f", total));
        }
        
        return sb.toString();
    }
} 