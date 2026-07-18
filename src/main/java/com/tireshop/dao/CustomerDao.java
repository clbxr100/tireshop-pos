package com.tireshop.dao;

import com.tireshop.model.Customer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * Data Access Object for Customer entities
 */
public class CustomerDao extends HibernateDao<Customer, Long> {
    
    public CustomerDao(SessionFactory sessionFactory) {
        super(Customer.class, sessionFactory);
    }
    
    /**
     * Search customers by name or phone
     * @param searchTerm Search term
     * @return List of matching customers
     */
    public List<Customer> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            Query<Customer> query = session.createQuery(
                    "FROM Customer WHERE LOWER(firstName) LIKE :searchTerm OR " +
                    "LOWER(lastName) LIKE :searchTerm OR " +
                    "phone LIKE :searchTerm OR " +
                    "LOWER(email) LIKE :searchTerm", 
                    Customer.class);
            query.setParameter("searchTerm", searchPattern);
            return query.list();
        }
    }
    
    /**
     * Find all customers with an outstanding store charge balance
     * @return List of customers owing money, highest balance first
     */
    public List<Customer> findCustomersWithChargeBalance() {
        try (Session session = sessionFactory.openSession()) {
            Query<Customer> query = session.createQuery(
                    "FROM Customer WHERE chargeBalance > 0 ORDER BY chargeBalance DESC",
                    Customer.class);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Find customer by phone number
     * @param phone Phone number
     * @return List of matching customers (should be at most one)
     */
    public List<Customer> findByPhone(String phone) {
        try (Session session = sessionFactory.openSession()) {
            Query<Customer> query = session.createQuery(
                    "FROM Customer WHERE phone = :phone", 
                    Customer.class);
            query.setParameter("phone", phone);
            return query.list();
        }
    }
    
    /**
     * Find customer by email
     * @param email Email address
     * @return List of matching customers (should be at most one)
     */
    public List<Customer> findByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            Query<Customer> query = session.createQuery(
                    "FROM Customer WHERE email = :email", 
                    Customer.class);
            query.setParameter("email", email);
            return query.list();
        }
    }
    
    /**
     * Find customers with vehicles of a specific make
     * @param make Vehicle make
     * @return List of customers with matching vehicles
     */
    public List<Customer> findByVehicleMake(String make) {
        try (Session session = sessionFactory.openSession()) {
            Query<Customer> query = session.createQuery(
                    "SELECT DISTINCT c FROM Customer c JOIN c.vehicles v " +
                    "WHERE LOWER(v.make) = :make", 
                    Customer.class);
            query.setParameter("make", make.toLowerCase());
            return query.list();
        }
    }
} 