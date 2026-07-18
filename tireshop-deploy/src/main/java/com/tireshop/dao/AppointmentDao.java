package com.tireshop.dao;

import com.tireshop.model.Appointment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Data Access Object for Appointment entities
 */
public class AppointmentDao extends HibernateDao<Appointment, Long> {
    
    public AppointmentDao(SessionFactory sessionFactory) {
        super(Appointment.class, sessionFactory);
    }
    
    /**
     * Find appointments for a specific date
     * @param date The date to find appointments for
     * @return List of appointments on the specified date
     */
    public List<Appointment> findByDate(LocalDate date) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            Query<Appointment> query = session.createQuery(
                    "FROM Appointment WHERE startTime >= :startOfDay AND startTime <= :endOfDay", 
                    Appointment.class);
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        }
    }
    
    /**
     * Find appointments between two dates
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of appointments in the date range
     */
    public List<Appointment> findBetweenDates(LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfFromDate = fromDate.atStartOfDay();
            LocalDateTime endOfToDate = toDate.atTime(LocalTime.MAX);
            
            Query<Appointment> query = session.createQuery(
                    "FROM Appointment WHERE startTime >= :startOfFromDate AND startTime <= :endOfToDate", 
                    Appointment.class);
            query.setParameter("startOfFromDate", startOfFromDate);
            query.setParameter("endOfToDate", endOfToDate);
            return query.list();
        }
    }
    
    /**
     * Find appointments for a specific customer
     * @param customerId Customer ID
     * @return List of appointments for the specified customer
     */
    public List<Appointment> findByCustomerId(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Appointment> query = session.createQuery(
                    "FROM Appointment WHERE customer.id = :customerId", 
                    Appointment.class);
            query.setParameter("customerId", customerId);
            return query.list();
        }
    }
    
    /**
     * Find appointments for a specific vehicle
     * @param vehicleId Vehicle ID
     * @return List of appointments for the specified vehicle
     */
    public List<Appointment> findByVehicleId(Long vehicleId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Appointment> query = session.createQuery(
                    "FROM Appointment WHERE vehicle.id = :vehicleId", 
                    Appointment.class);
            query.setParameter("vehicleId", vehicleId);
            return query.list();
        }
    }
    
    /**
     * Find appointments for a specific technician
     * @param technicianId Technician ID
     * @return List of appointments for the specified technician
     */
    public List<Appointment> findByTechnicianId(Long technicianId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Appointment> query = session.createQuery(
                    "FROM Appointment WHERE technician.id = :technicianId", 
                    Appointment.class);
            query.setParameter("technicianId", technicianId);
            return query.list();
        }
    }
} 