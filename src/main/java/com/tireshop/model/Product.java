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
    
    // New tire-specific fields
    private String tireType; // All-Season, Winter, Summer, All-Terrain, etc.
    private String speedRating; // Speed rating (e.g., H, V, W, Y)
    private String loadRating; // Load rating (e.g., 91, 94, etc.)
    private String treadPattern; // Name of the tread pattern
    private Integer treadDepth; // Tread depth in 32nds of an inch
    private String construction; // Tire construction (e.g., Radial)
    private String imageUrl; // URL to tire image
    private Integer utqgTreadwear; // UTQG Treadwear rating
    private String utqgTraction; // UTQG Traction rating (AA, A, B, C)
    private String utqgTemperature; // UTQG Temperature rating (A, B, C)
    private Boolean runFlat; // Whether it's a run-flat tire
    private String seasonality; // Primary season usage
    private String warranty; // Warranty information
    
    // Supplier integration fields
    @Column(length = 500)
    private String supplierSkuMapping; // JSON string storing supplier:sku mappings
    private String primarySupplierSku; // Main supplier SKU for quick lookup
    private String primarySupplier; // Main supplier name
    
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
    
    // New getters and setters for tire-specific fields
    public String getTireType() {
        return tireType;
    }

    public void setTireType(String tireType) {
        this.tireType = tireType;
    }

    public String getSpeedRating() {
        return speedRating;
    }

    public void setSpeedRating(String speedRating) {
        this.speedRating = speedRating;
    }

    public String getLoadRating() {
        return loadRating;
    }

    public void setLoadRating(String loadRating) {
        this.loadRating = loadRating;
    }

    public String getTreadPattern() {
        return treadPattern;
    }

    public void setTreadPattern(String treadPattern) {
        this.treadPattern = treadPattern;
    }

    public Integer getTreadDepth() {
        return treadDepth;
    }

    public void setTreadDepth(Integer treadDepth) {
        this.treadDepth = treadDepth;
    }

    public String getConstruction() {
        return construction;
    }

    public void setConstruction(String construction) {
        this.construction = construction;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getUtqgTreadwear() {
        return utqgTreadwear;
    }

    public void setUtqgTreadwear(Integer utqgTreadwear) {
        this.utqgTreadwear = utqgTreadwear;
    }

    public String getUtqgTraction() {
        return utqgTraction;
    }

    public void setUtqgTraction(String utqgTraction) {
        this.utqgTraction = utqgTraction;
    }

    public String getUtqgTemperature() {
        return utqgTemperature;
    }

    public void setUtqgTemperature(String utqgTemperature) {
        this.utqgTemperature = utqgTemperature;
    }

    public Boolean getRunFlat() {
        return runFlat;
    }

    public void setRunFlat(Boolean runFlat) {
        this.runFlat = runFlat;
    }

    public String getSeasonality() {
        return seasonality;
    }

    public void setSeasonality(String seasonality) {
        this.seasonality = seasonality;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
    
    // Supplier integration getters and setters
    public String getSupplierSkuMapping() {
        return supplierSkuMapping;
    }

    public void setSupplierSkuMapping(String supplierSkuMapping) {
        this.supplierSkuMapping = supplierSkuMapping;
    }

    public String getPrimarySupplierSku() {
        return primarySupplierSku;
    }

    public void setPrimarySupplierSku(String primarySupplierSku) {
        this.primarySupplierSku = primarySupplierSku;
    }

    public String getPrimarySupplier() {
        return primarySupplier;
    }

    public void setPrimarySupplier(String primarySupplier) {
        this.primarySupplier = primarySupplier;
    }
    
    @Override
    public String toString() {
        return name + " (" + category + ") - $" + sellingPrice;
    }
} 