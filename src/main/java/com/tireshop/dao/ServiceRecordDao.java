package com.tireshop.dao;

import com.tireshop.model.ServiceRecord;
import com.tireshop.model.Vehicle;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for ServiceRecord operations
 */
public class ServiceRecordDao extends HibernateDao<ServiceRecord, Long> {
    
    public ServiceRecordDao(SessionFactory sessionFactory) {
        super(ServiceRecord.class, sessionFactory);
    }
    
    /**
     * Find all service records for a specific vehicle
     * @param vehicleId The vehicle ID
     * @return List of service records
     */
    public List<ServiceRecord> findByVehicleId(Long vehicleId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.vehicle.id = :vehicleId ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("vehicleId", vehicleId);
            return query.list();
        }
    }
    
    /**
     * Find service records by date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of service records
     */
    public List<ServiceRecord> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.serviceDate BETWEEN :startDate AND :endDate ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        }
    }
    
    /**
     * Find service records by technician
     * @param technicianId The technician ID
     * @return List of service records
     */
    public List<ServiceRecord> findByTechnicianId(Long technicianId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.technician.id = :technicianId ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("technicianId", technicianId);
            return query.list();
        }
    }
    
    /**
     * Find service records by type
     * @param serviceType The service type
     * @return List of service records
     */
    public List<ServiceRecord> findByServiceType(String serviceType) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.serviceType = :serviceType ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("serviceType", serviceType);
            return query.list();
        }
    }
    
    /**
     * Find the last service record for a vehicle
     * @param vehicleId The vehicle ID
     * @return The most recent service record or null
     */
    public ServiceRecord findLastServiceForVehicle(Long vehicleId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.vehicle.id = :vehicleId ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("vehicleId", vehicleId);
            query.setMaxResults(1);
            return query.uniqueResult();
        }
    }
    
    /**
     * Find vehicles due for service
     * @param currentDate Current date to check against
     * @return List of service records with upcoming service dates
     */
    public List<ServiceRecord> findVehiclesDueForService(LocalDateTime currentDate) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE sr.nextServiceDate <= :currentDate " +
                "AND sr.id IN (SELECT MAX(sr2.id) FROM ServiceRecord sr2 GROUP BY sr2.vehicle.id)", 
                ServiceRecord.class
            );
            query.setParameter("currentDate", currentDate);
            return query.list();
        }
    }
    
    /**
     * Search service records by various criteria
     * @param searchTerm Search term to match against multiple fields
     * @return List of matching service records
     */
    public List<ServiceRecord> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            Query<ServiceRecord> query = session.createQuery(
                "FROM ServiceRecord sr WHERE " +
                "LOWER(sr.description) LIKE :term OR " +
                "LOWER(sr.notes) LIKE :term OR " +
                "LOWER(sr.serviceType) LIKE :term OR " +
                "LOWER(sr.vehicle.make) LIKE :term OR " +
                "LOWER(sr.vehicle.model) LIKE :term OR " +
                "LOWER(sr.vehicle.licensePlate) LIKE :term " +
                "ORDER BY sr.serviceDate DESC", 
                ServiceRecord.class
            );
            query.setParameter("term", "%" + searchTerm.toLowerCase() + "%");
            return query.list();
        }
    }
} 