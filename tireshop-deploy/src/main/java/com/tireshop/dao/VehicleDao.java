package com.tireshop.dao;

import com.tireshop.model.Vehicle;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for Vehicle entities
 */
public class VehicleDao extends HibernateDao<Vehicle, Long> {
    
    public VehicleDao(SessionFactory sessionFactory) {
        super(Vehicle.class, sessionFactory);
    }
    
    /**
     * Find vehicles for a specific customer
     * @param customerId Customer ID
     * @return List of vehicles belonging to the customer
     */
    public List<Vehicle> findByCustomerId(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle WHERE customer.id = :customerId", 
                    Vehicle.class);
            query.setParameter("customerId", customerId);
            return query.list();
        }
    }
    
    /**
     * Search vehicles by make, model, or license plate
     * @param searchTerm Search term
     * @return List of matching vehicles
     */
    public List<Vehicle> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle WHERE LOWER(make) LIKE :searchTerm OR " +
                    "LOWER(model) LIKE :searchTerm OR " +
                    "LOWER(licensePlate) LIKE :searchTerm OR " +
                    "LOWER(vin) LIKE :searchTerm", 
                    Vehicle.class);
            query.setParameter("searchTerm", searchPattern);
            return query.list();
        }
    }
    
    /**
     * Find vehicles by make
     * @param make Vehicle make
     * @return List of vehicles with the specified make
     */
    public List<Vehicle> findByMake(String make) {
        try (Session session = sessionFactory.openSession()) {
            Query<Vehicle> query = session.createQuery(
                    "FROM Vehicle WHERE LOWER(make) = :make", 
                    Vehicle.class);
            query.setParameter("make", make.toLowerCase());
            return query.list();
        }
    }
} 