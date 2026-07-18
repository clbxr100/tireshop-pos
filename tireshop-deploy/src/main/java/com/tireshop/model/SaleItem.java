package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Represents an individual line item in a sale (either a product or service)
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Sale sale;
    
    private String itemType; // "PRODUCT", "SERVICE", or "CUSTOM"
    
    @ManyToOne
    private Product product;
    
    @ManyToOne
    private Service service;
    
    @ManyToOne
    private Technician technician; // For service items
    
    private String customItemName; // For custom items
    
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal subtotal;
    
    // Constructors
    public SaleItem() {
        this.quantity = 1;
        this.discount = BigDecimal.ZERO;
    }
    
    public SaleItem(Product product, int quantity) {
        this.itemType = "PRODUCT";
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getSellingPrice();
        this.discount = BigDecimal.ZERO;
        calculateSubtotal();
    }
    
    public SaleItem(Service service, Technician technician) {
        this.itemType = "SERVICE";
        this.service = service;
        this.technician = technician;
        this.quantity = 1;
        this.unitPrice = service.getPrice();
        this.discount = BigDecimal.ZERO;
        calculateSubtotal();
    }
    
    /**
     * Constructor for custom items
     */
    public SaleItem(String customItemName, BigDecimal unitPrice, int quantity) {
        this.itemType = "CUSTOM";
        this.customItemName = customItemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.discount = BigDecimal.ZERO;
        calculateSubtotal();
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

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.itemType = "PRODUCT";
            this.unitPrice = product.getSellingPrice();
            calculateSubtotal();
        }
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
        if (service != null) {
            this.itemType = "SERVICE";
            this.unitPrice = service.getPrice();
            calculateSubtotal();
        }
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }

    public String getCustomItemName() {
        return customItemName;
    }

    public void setCustomItemName(String customItemName) {
        this.customItemName = customItemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    /**
     * Get the name of this item regardless of type
     */
    public String getItemName() {
        if ("PRODUCT".equals(itemType)) {
            return product != null ? product.getName() : "Unknown Product";
        } else if ("SERVICE".equals(itemType)) {
            return service != null ? service.getName() : "Unknown Service";
        } else if ("CUSTOM".equals(itemType)) {
            return customItemName != null ? customItemName : "Custom Item";
        }
        return "Unknown Item";
    }
    
    // Helper methods
    private void calculateSubtotal() {
        if (unitPrice == null) {
            this.subtotal = BigDecimal.ZERO;
            return;
        }
        
        BigDecimal grossAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.subtotal = grossAmount.subtract(discount);
    }
    
    @Override
    public String toString() {
        return getItemName() + " x " + quantity + " = $" + subtotal;
    }
} 