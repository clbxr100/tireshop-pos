package com.tireshop.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer of the tire shop
 */
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private boolean taxExempt = false;

    @Column(name = "chargeBalance")
    private java.math.BigDecimal chargeBalance = java.math.BigDecimal.ZERO; // Store charge account balance
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();
    
    // Constructors
    public Customer() {
    }
    
    public Customer(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }
    
    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
        vehicle.setCustomer(this);
    }
    
    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
        vehicle.setCustomer(null);
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isTaxExempt() {
        return taxExempt;
    }

    public void setTaxExempt(boolean taxExempt) {
        this.taxExempt = taxExempt;
    }

    public java.math.BigDecimal getChargeBalance() {
        return chargeBalance != null ? chargeBalance : java.math.BigDecimal.ZERO;
    }

    public void setChargeBalance(java.math.BigDecimal chargeBalance) {
        this.chargeBalance = chargeBalance != null ? chargeBalance : java.math.BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return getFullName() + " (" + phone + ")";
    }
} 