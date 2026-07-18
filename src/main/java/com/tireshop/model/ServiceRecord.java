package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service record for a vehicle
 */
@Entity
@Table(name = "service_records")
public class ServiceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;
    
    @Column(name = "service_date", nullable = false)
    private LocalDateTime serviceDate;
    
    @Column(name = "mileage")
    private Integer mileage;
    
    @Column(name = "service_type", nullable = false)
    private String serviceType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;
    
    @ElementCollection
    @CollectionTable(name = "service_record_items", joinColumns = @JoinColumn(name = "service_record_id"))
    private List<ServiceItem> serviceItems = new ArrayList<>();
    
    @Column(name = "total_cost")
    private BigDecimal totalCost;
    
    @Column(name = "next_service_date")
    private LocalDateTime nextServiceDate;
    
    @Column(name = "next_service_mileage")
    private Integer nextServiceMileage;
    
    @Column(name = "warranty_info")
    private String warrantyInfo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Embeddable
    public static class ServiceItem {
        @Column(name = "item_type")
        private String itemType; // TIRE, PART, SERVICE
        
        @Column(name = "item_name")
        private String itemName;
        
        @Column(name = "item_description")
        private String itemDescription;
        
        @Column(name = "quantity")
        private Integer quantity;
        
        @Column(name = "unit_price")
        private BigDecimal unitPrice;
        
        @Column(name = "part_number")
        private String partNumber;
        
        @Column(name = "tire_position")
        private String tirePosition; // FL, FR, RL, RR for tire services
        
        // Constructors
        public ServiceItem() {}
        
        public ServiceItem(String itemType, String itemName, Integer quantity, BigDecimal unitPrice) {
            this.itemType = itemType;
            this.itemName = itemName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
        
        // Getters and Setters
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public String getPartNumber() { return partNumber; }
        public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
        
        public String getTirePosition() { return tirePosition; }
        public void setTirePosition(String tirePosition) { this.tirePosition = tirePosition; }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public ServiceRecord() {}
    
    public ServiceRecord(Vehicle vehicle, LocalDateTime serviceDate, String serviceType) {
        this.vehicle = vehicle;
        this.serviceDate = serviceDate;
        this.serviceType = serviceType;
    }
    
    // Helper methods
    public void addServiceItem(ServiceItem item) {
        serviceItems.add(item);
        recalculateTotalCost();
    }
    
    public void removeServiceItem(ServiceItem item) {
        serviceItems.remove(item);
        recalculateTotalCost();
    }
    
    private void recalculateTotalCost() {
        totalCost = serviceItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }
    
    public LocalDateTime getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDateTime serviceDate) { this.serviceDate = serviceDate; }
    
    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Technician getTechnician() { return technician; }
    public void setTechnician(Technician technician) { this.technician = technician; }
    
    public List<ServiceItem> getServiceItems() { return serviceItems; }
    public void setServiceItems(List<ServiceItem> serviceItems) { 
        this.serviceItems = serviceItems;
        recalculateTotalCost();
    }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public LocalDateTime getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(LocalDateTime nextServiceDate) { this.nextServiceDate = nextServiceDate; }
    
    public Integer getNextServiceMileage() { return nextServiceMileage; }
    public void setNextServiceMileage(Integer nextServiceMileage) { this.nextServiceMileage = nextServiceMileage; }
    
    public String getWarrantyInfo() { return warrantyInfo; }
    public void setWarrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 