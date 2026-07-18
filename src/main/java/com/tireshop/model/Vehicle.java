package com.tireshop.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a customer's vehicle
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String make;
    private String model;
    private int modelYear;
    private String licensePlate;
    private String vin;
    private String color;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    // TPMS sensor data
    private String tpmsSensorLF; // Left Front sensor ID
    private String tpmsSensorRF; // Right Front sensor ID
    private String tpmsSensorLR; // Left Rear sensor ID
    private String tpmsSensorRR; // Right Rear sensor ID
    private String tpmsSensorSpare; // Spare tire sensor ID
    private String tpmsProtocol; // TPMS protocol type for this vehicle
    private LocalDateTime lastTpmsService; // Last TPMS service date
    
    // Constructors
    public Vehicle() {
    }
    
    public Vehicle(String make, String model, int modelYear, String licensePlate) {
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.licensePlate = licensePlate;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getModelYear() {
        return modelYear;
    }

    public void setModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    // TPMS getters and setters
    public String getTpmsSensorLF() {
        return tpmsSensorLF;
    }

    public void setTpmsSensorLF(String tpmsSensorLF) {
        this.tpmsSensorLF = tpmsSensorLF;
    }

    public String getTpmsSensorRF() {
        return tpmsSensorRF;
    }

    public void setTpmsSensorRF(String tpmsSensorRF) {
        this.tpmsSensorRF = tpmsSensorRF;
    }

    public String getTpmsSensorLR() {
        return tpmsSensorLR;
    }

    public void setTpmsSensorLR(String tpmsSensorLR) {
        this.tpmsSensorLR = tpmsSensorLR;
    }

    public String getTpmsSensorRR() {
        return tpmsSensorRR;
    }

    public void setTpmsSensorRR(String tpmsSensorRR) {
        this.tpmsSensorRR = tpmsSensorRR;
    }

    public String getTpmsSensorSpare() {
        return tpmsSensorSpare;
    }

    public void setTpmsSensorSpare(String tpmsSensorSpare) {
        this.tpmsSensorSpare = tpmsSensorSpare;
    }

    public String getTpmsProtocol() {
        return tpmsProtocol;
    }

    public void setTpmsProtocol(String tpmsProtocol) {
        this.tpmsProtocol = tpmsProtocol;
    }

    public LocalDateTime getLastTpmsService() {
        return lastTpmsService;
    }

    public void setLastTpmsService(LocalDateTime lastTpmsService) {
        this.lastTpmsService = lastTpmsService;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (modelYear > 0) {
            sb.append(modelYear).append(" ");
        }
        
        if (make != null && !make.trim().isEmpty()) {
            sb.append(make).append(" ");
        }
        
        if (model != null && !model.trim().isEmpty()) {
            sb.append(model).append(" ");
        }
        
        if (licensePlate != null && !licensePlate.trim().isEmpty()) {
            sb.append("(").append(licensePlate).append(")");
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? "Vehicle (No details)" : result;
    }
} 