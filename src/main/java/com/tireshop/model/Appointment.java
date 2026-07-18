package com.tireshop.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a scheduled appointment
 */
@Entity
@Table(name = "appointments")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @ManyToOne
    private Customer customer;
    
    @ManyToOne
    private Vehicle vehicle;
    
    @ManyToOne
    private Technician technician;
    
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;
    
    /**
     * Default constructor
     */
    public Appointment() {
        this.status = AppointmentStatus.SCHEDULED;
    }
    
    /**
     * Parameterized constructor
     */
    public Appointment(String title, Customer customer, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.customer = customer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.SCHEDULED;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return title + " - " + (customer != null ? 
                customer.getFirstName() + " " + customer.getLastName() : "");
    }
} 