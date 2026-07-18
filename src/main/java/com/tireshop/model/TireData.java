package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tire_data")
public class TireData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "brand")
    private String brand;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "size")
    private String size;
    
    @Column(name = "sku", unique = true)
    private String sku;
    
    @Column(name = "price")
    private String price;
    
    @Column(name = "cost_price")
    private String costPrice;
    
    @Column(name = "rating")
    private String rating;
    
    @Column(name = "warehouse")
    private String warehouse;
    
    @Column(name = "stock_qty")
    private Integer stockQty;
    
    @Column(name = "available_qty")
    private Integer availableQty;
    
    @Column(name = "reserved_qty")
    private Integer reservedQty;
    
    @Column(name = "reorder_point")
    private Integer reorderPoint;
    
    @Column(name = "load_index")
    private Integer loadIndex;
    
    @Column(name = "speed_rating")
    private String speedRating;
    
    @Column(name = "season")
    private String season;
    
    @Column(name = "supplier_code")
    private String supplierCode;
    
    @Column(name = "last_received")
    private String lastReceived;
    
    @Column(name = "barcode", unique = true)
    private String barcode;
    
    @Column(name = "gtin", unique = true)
    private String gtin;
    
    @Column(name = "source")
    private String source;
    
    @Column(name = "manufacturer")
    private String manufacturer;
    
    @Column(name = "tire_type")
    private String tireType;
    
    @Column(name = "pattern")
    private String pattern;
    
    @Column(name = "rim_diameter")
    private String rimDiameter;
    
    @Column(name = "section_width")
    private String sectionWidth;
    
    @Column(name = "aspect_ratio")
    private String aspectRatio;
    
    // Default constructor
    public TireData() {}
    
    // Constructor with essential fields
    public TireData(String brand, String name, String size, String sku, String price, String barcode) {
        this.brand = brand;
        this.name = name;
        this.size = size;
        this.sku = sku;
        this.price = price;
        this.barcode = barcode;
    }
    
    // Constructor with GTIN support
    public TireData(String brand, String name, String size, String sku, String price, String barcode, String gtin) {
        this.brand = brand;
        this.name = name;
        this.size = size;
        this.sku = sku;
        this.price = price;
        this.barcode = barcode;
        this.gtin = gtin;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getPrice() {
        return price;
    }
    
    public void setPrice(String price) {
        this.price = price;
    }
    
    public String getCostPrice() {
        return costPrice;
    }
    
    public void setCostPrice(String costPrice) {
        this.costPrice = costPrice;
    }
    
    public String getRating() {
        return rating;
    }
    
    public void setRating(String rating) {
        this.rating = rating;
    }
    
    public String getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
    
    public Integer getStockQty() {
        return stockQty;
    }
    
    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }
    
    public Integer getAvailableQty() {
        return availableQty;
    }
    
    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }
    
    public Integer getReservedQty() {
        return reservedQty;
    }
    
    public void setReservedQty(Integer reservedQty) {
        this.reservedQty = reservedQty;
    }
    
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
    
    public Integer getLoadIndex() {
        return loadIndex;
    }
    
    public void setLoadIndex(Integer loadIndex) {
        this.loadIndex = loadIndex;
    }
    
    public String getSpeedRating() {
        return speedRating;
    }
    
    public void setSpeedRating(String speedRating) {
        this.speedRating = speedRating;
    }
    
    public String getSeason() {
        return season;
    }
    
    public void setSeason(String season) {
        this.season = season;
    }
    
    public String getSupplierCode() {
        return supplierCode;
    }
    
    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }
    
    public String getLastReceived() {
        return lastReceived;
    }
    
    public void setLastReceived(String lastReceived) {
        this.lastReceived = lastReceived;
    }
    
    public String getBarcode() {
        return barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public String getGtin() {
        return gtin;
    }
    
    public void setGtin(String gtin) {
        this.gtin = gtin;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getTireType() {
        return tireType;
    }
    
    public void setTireType(String tireType) {
        this.tireType = tireType;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public String getRimDiameter() {
        return rimDiameter;
    }
    
    public void setRimDiameter(String rimDiameter) {
        this.rimDiameter = rimDiameter;
    }
    
    public String getSectionWidth() {
        return sectionWidth;
    }
    
    public void setSectionWidth(String sectionWidth) {
        this.sectionWidth = sectionWidth;
    }
    
    public String getAspectRatio() {
        return aspectRatio;
    }
    
    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
    
    /**
     * Convert to Product for inventory integration
     */
    public Product toProduct() {
        Product product = new Product();
        product.setName(this.brand + " " + this.name + " " + this.size);
        product.setSku(this.sku);
        product.setManufacturer(this.brand);
        product.setCategory("Tires");
        product.setTireType(this.season);
        product.setSize(this.size);
        
        try {
            if (this.price != null && !this.price.isEmpty()) {
                product.setSellingPrice(new BigDecimal(this.price));
            }
            if (this.costPrice != null && !this.costPrice.isEmpty()) {
                product.setPurchasePrice(new BigDecimal(this.costPrice));
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing price for SKU " + this.sku + ": " + e.getMessage());
        }
        
        if (this.stockQty != null) {
            product.setQuantityInStock(this.stockQty);
        }
        
        product.setBarcode(this.barcode);
        // Set supplier info in description or notes field since setSupplierInfo doesn't exist
        if (this.supplierCode != null) {
            String description = product.getDescription();
            if (description == null) description = "";
            product.setDescription(description + " [Supplier: " + this.supplierCode + "]");
        }
        
        return product;
    }
    
    @Override
    public String toString() {
        return "TireData{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", sku='" + sku + '\'' +
                ", price='" + price + '\'' +
                ", barcode='" + barcode + '\'' +
                ", gtin='" + gtin + '\'' +
                ", stockQty=" + stockQty +
                ", source='" + source + '\'' +
                '}';
    }
} 