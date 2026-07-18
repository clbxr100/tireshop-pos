package com.tireshop.dao;

import com.tireshop.model.Sale;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Sale entities
 */
public class SaleDao extends HibernateDao<Sale, Long> {
    
    public SaleDao(SessionFactory sessionFactory) {
        super(Sale.class, sessionFactory);
    }
    
    /**
     * Find sales by customer ID
     * @param customerId Customer ID
     * @return List of sales for the customer
     */
    public List<Sale> findByCustomerId(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                    "FROM Sale WHERE customer.id = :customerId", 
                    Sale.class);
            query.setParameter("customerId", customerId);
            return query.list();
        }
    }
    
    /**
     * Find sales by invoice number
     * @param invoiceNumber Invoice number
     * @return List of matching sales (should be at most one)
     */
    public List<Sale> findByInvoiceNumber(String invoiceNumber) {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                    "FROM Sale WHERE invoiceNumber = :invoiceNumber", 
                    Sale.class);
            query.setParameter("invoiceNumber", invoiceNumber);
            return query.list();
        }
    }
    
    /**
     * Find sales for a specific date
     * @param date The date to find sales for
     * @return List of sales on that date
     */
    public List<Sale> findByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                    "FROM Sale WHERE timestamp BETWEEN :startDate AND :endDate", 
                    Sale.class);
            query.setParameter("startDate", startOfDay);
            query.setParameter("endDate", endOfDay);
            return query.list();
        }
    }
    
    /**
     * Find sales by payment type
     * @param paymentType Payment type string
     * @return List of sales with that payment type
     */
    public List<Sale> findByPaymentType(String paymentType) {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                    "FROM Sale WHERE paymentType = :paymentType", 
                    Sale.class);
            query.setParameter("paymentType", paymentType);
            return query.list();
        }
    }
    
    /**
     * Find unpaid sales
     * @return List of unpaid sales
     */
    public List<Sale> findUnpaidSales() {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                    "FROM Sale WHERE isPaid = false", 
                    Sale.class);
            return query.list();
        }
    }

    public List<Sale> findSalesByDateRange(LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = fromDate.atStartOfDay();
            LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);

            Query<Sale> query = session.createQuery(
                    "FROM Sale s WHERE s.timestamp >= :startOfDay AND s.timestamp <= :endOfDay ORDER BY s.timestamp DESC", Sale.class);
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        }
    }

    public List<Sale> findByCustomerIdAndDateRange(Long customerId, LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = fromDate.atStartOfDay();
            LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);

            Query<Sale> query = session.createQuery(
                "FROM Sale s WHERE s.customer.id = :customerId AND s.timestamp >= :startOfDay AND s.timestamp <= :endOfDay ORDER BY s.timestamp DESC", 
                Sale.class);
            query.setParameter("customerId", customerId);
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        }
    }

    public List<Sale> findPaidSalesInDateRange(LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startRange = fromDate.atStartOfDay();
            LocalDateTime endRange = toDate.atTime(LocalTime.MAX);

            Query<Sale> query = session.createQuery(
                "FROM Sale s WHERE s.isPaid = true AND s.timestamp >= :startRange AND s.timestamp <= :endRange ORDER BY s.timestamp ASC", 
                Sale.class);
            query.setParameter("startRange", startRange);
            query.setParameter("endRange", endRange);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
} 