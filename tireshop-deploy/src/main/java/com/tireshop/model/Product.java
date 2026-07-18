package com.tireshop.model;

import java.math.BigDecimal;
import javax.persistence.*;

/**
 * Represents any product in the shop inventory (tires, rims, etc.)
 */
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private String category; // "Tire", "Rim", etc.
    private String manufacturer;
    private String modelNumber;
    @Column(name = "barcode", nullable = true, unique = true)
    private String barcode;
    private String size; // For storing tire size like "205/55R16"
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int quantityInStock;
    private String location; // Storage location in the shop
    private Integer reorderLevel; // New field for reorder level
    private BigDecimal price;
    private String sku;
    
    // Constructors
    public Product() {
        this.quantityInStock = 0;
        this.reorderLevel = 5;
        this.price = BigDecimal.ZERO;
        this.sellingPrice = BigDecimal.ZERO;
    }
    
    public Product(String name, String category, BigDecimal sellingPrice) {
        this.name = name;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.price = sellingPrice;
        this.quantityInStock = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
        // Keep price in sync with sellingPrice
        this.price = sellingPrice;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        // Keep sellingPrice in sync with price
        this.sellingPrice = price;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
    
    @Override
    public String toString() {
        return name + " (" + category + ") - $" + sellingPrice;
    }
} 