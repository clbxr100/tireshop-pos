package com.tireshop.dao;

import com.tireshop.model.Technician;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for Technician entities
 */
public class TechnicianDao extends HibernateDao<Technician, Long> {
    
    public TechnicianDao(SessionFactory sessionFactory) {
        super(Technician.class, sessionFactory);
    }
    
    /**
     * Find technicians by specialization
     * @param specialization Technician specialization
     * @return List of technicians with the specified specialization
     */
    public List<Technician> findBySpecialization(String specialization) {
        try (Session session = sessionFactory.openSession()) {
            Query<Technician> query = session.createQuery(
                    "FROM Technician WHERE specialization = :specialization", 
                    Technician.class);
            query.setParameter("specialization", specialization);
            return query.list();
        }
    }
    
    /**
     * Search technicians by name
     * @param searchTerm Search term
     * @return List of matching technicians
     */
    public List<Technician> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            Query<Technician> query = session.createQuery(
                    "FROM Technician WHERE LOWER(firstName) LIKE :searchTerm OR " +
                    "LOWER(lastName) LIKE :searchTerm", 
                    Technician.class);
            query.setParameter("searchTerm", searchPattern);
            return query.list();
        }
    }
} 