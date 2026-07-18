package com.tireshop.service;

import com.tireshop.dao.GenericDao;
import com.tireshop.model.Appointment;
import com.tireshop.model.AppointmentStatus;
import com.tireshop.model.Customer;
import com.tireshop.model.Technician;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for appointment operations
 */
public class AppointmentService {
    
    private final GenericDao<Appointment, Long> appointmentDao;
    private final GenericDao<Customer, Long> customerDao;
    private final GenericDao<Technician, Long> technicianDao;
    
    /**
     * Constructor
     */
    public AppointmentService(
            GenericDao<Appointment, Long> appointmentDao,
            GenericDao<Customer, Long> customerDao,
            GenericDao<Technician, Long> technicianDao) {
        this.appointmentDao = appointmentDao;
        this.customerDao = customerDao;
        this.technicianDao = technicianDao;
    }
    
    /**
     * Create a new appointment
     * @param title Appointment title
     * @param customerId Customer ID
     * @param startTime Start time
     * @param endTime End time
     * @return The created appointment
     */
    public Appointment createAppointment(String title, Long customerId,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        Appointment appointment = new Appointment();
        appointment.setTitle(title);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        
        // Set customer if provided
        if (customerId != null) {
            Optional<Customer> customer = customerDao.findById(customerId);
            customer.ifPresent(appointment::setCustomer);
        }
        
        return appointmentDao.save(appointment);
    }
    
    /**
     * Get all appointments
     * @return List of all appointments
     */
    public List<Appointment> getAllAppointments() {
        return appointmentDao.findAll();
    }
    
    /**
     * Get appointments for a specific date
     * @param date The date
     * @return List of appointments
     */
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        // Need to cast to AppointmentDao for this specific method
        if (appointmentDao instanceof com.tireshop.dao.AppointmentDao) {
            return ((com.tireshop.dao.AppointmentDao) appointmentDao).findByDate(date);
        }
        
        // Fallback if the DAO is not of the expected type
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        return appointmentDao.findAll().stream()
            .filter(a -> a.getStartTime().isAfter(startOfDay) && a.getStartTime().isBefore(endOfDay))
            .collect(Collectors.toList());
    }
    
    /**
     * Get an appointment by ID
     * @param id Appointment ID
     * @return Optional containing the appointment if found
     */
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentDao.findById(id);
    }
    
    /**
     * Update appointment status
     * @param id Appointment ID
     * @param status New status
     * @return Optional containing the updated appointment if found
     */
    public Optional<Appointment> updateStatus(Long id, AppointmentStatus status) {
        Optional<Appointment> optionalAppointment = appointmentDao.findById(id);
        
        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            appointment.setStatus(status);
            return Optional.of(appointmentDao.update(appointment));
        }
        
        return Optional.empty();
    }
    
    /**
     * Assign a technician to an appointment
     * @param appointmentId Appointment ID
     * @param technicianId Technician ID
     * @return Optional containing the updated appointment if found
     */
    public Optional<Appointment> assignTechnician(Long appointmentId, Long technicianId) {
        Optional<Appointment> optionalAppointment = appointmentDao.findById(appointmentId);
        Optional<Technician> optionalTechnician = technicianDao.findById(technicianId);
        
        if (optionalAppointment.isPresent() && optionalTechnician.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            appointment.setTechnician(optionalTechnician.get());
            return Optional.of(appointmentDao.update(appointment));
        }
        
        return Optional.empty();
    }
    
    /**
     * Cancel an appointment
     * @param id Appointment ID
     * @return true if the appointment was canceled
     */
    public boolean cancelAppointment(Long id) {
        Optional<Appointment> optionalAppointment = appointmentDao.findById(id);
        
        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentDao.update(appointment);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a time slot is available
     * @param startTime Start time
     * @param endTime End time
     * @param technicianId Technician ID (optional)
     * @return true if the time slot is available
     */
    public boolean isTimeSlotAvailable(LocalDateTime startTime, LocalDateTime endTime, Long technicianId) {
        List<Appointment> appointments;
        
        if (technicianId != null) {
            // Check for technician availability
            if (appointmentDao instanceof com.tireshop.dao.AppointmentDao) {
                appointments = ((com.tireshop.dao.AppointmentDao) appointmentDao).findByTechnicianId(technicianId);
            } else {
                appointments = appointmentDao.findAll().stream()
                    .filter(a -> a.getTechnician() != null && a.getTechnician().getId().equals(technicianId))
                    .collect(Collectors.toList());
            }
        } else {
            // Check general availability
            LocalDate date = startTime.toLocalDate();
            appointments = getAppointmentsByDate(date);
        }
        
        // Check for overlap with existing appointments
        for (Appointment appointment : appointments) {
            // Skip cancelled appointments
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            
            // Check for overlap
            if (!(endTime.isBefore(appointment.getStartTime()) || 
                  startTime.isAfter(appointment.getEndTime()))) {
                return false;
            }
        }
        
        return true;
    }
} 