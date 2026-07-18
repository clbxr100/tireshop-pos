package com.tireshop.model;

import java.math.BigDecimal;
import javax.persistence.*;

/**
 * Represents a service provided in the mechanic's shop
 */
@Entity
@Table(name = "services")
public class Service {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private String category; // e.g., "Tire Service", "Alignment", "Repair", etc.
    private BigDecimal price;
    private int estimatedDurationMinutes;
    
    // Constructors
    public Service() {
    }
    
    public Service(String name, String category, BigDecimal price, int estimatedDurationMinutes) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
    
    @Override
    public String toString() {
        return name + " - $" + price;
    }
} 