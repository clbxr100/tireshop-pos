package com.tireshop.dao;

import com.tireshop.model.Service;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for Service entities
 */
public class ServiceDao extends HibernateDao<Service, Long> {
    
    public ServiceDao(SessionFactory sessionFactory) {
        super(Service.class, sessionFactory);
    }
    
    /**
     * Find services by category
     * @param category Service category
     * @return List of services in the specified category
     */
    public List<Service> findByCategory(String category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Service> query = session.createQuery(
                    "FROM Service WHERE category = :category", 
                    Service.class);
            query.setParameter("category", category);
            return query.list();
        }
    }
    
    /**
     * Search services by name or description
     * @param searchTerm Search term
     * @return List of matching services
     */
    public List<Service> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            Query<Service> query = session.createQuery(
                    "FROM Service WHERE LOWER(name) LIKE :searchTerm OR " +
                    "LOWER(description) LIKE :searchTerm OR " +
                    "LOWER(category) LIKE :searchTerm", 
                    Service.class);
            query.setParameter("searchTerm", searchPattern);
            return query.list();
        }
    }
} 