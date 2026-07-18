package com.tireshop.model;

import javax.persistence.*;

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
    private Customer customer;
    
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
    
    @Override
    public String toString() {
        return modelYear + " " + make + " " + model + " (" + licensePlate + ")";
    }
} 