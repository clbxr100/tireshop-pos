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
     * Find a sale by ID with all related data loaded (customer, vehicle, items)
     * Use this when you need full sale details (e.g., for receipts, detail views)
     */
    public Sale findByIdWithDetails(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                "SELECT DISTINCT s FROM Sale s " +
                "LEFT JOIN FETCH s.customer " +
                "LEFT JOIN FETCH s.vehicle " +
                "LEFT JOIN FETCH s.items " +
                "WHERE s.id = :id",
                Sale.class);
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    /**
     * Find all sales with customer info loaded (for table display)
     * Only loads customer - not items or vehicle for better performance
     */
    public List<Sale> findAllWithCustomer() {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                "SELECT DISTINCT s FROM Sale s " +
                "LEFT JOIN FETCH s.customer " +
                "ORDER BY s.timestamp DESC",
                Sale.class);
            return query.list();
        }
    }

    /**
     * Get total count of sales
     * @return Total number of sales
     */
    public int getTotalCount() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(s) FROM Sale s",
                Long.class);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        }
    }

    /**
     * Find sales with pagination (with customer info loaded)
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return List of sales for the requested page
     */
    public List<Sale> findPaginated(int page, int pageSize) {
        try (Session session = sessionFactory.openSession()) {
            Query<Sale> query = session.createQuery(
                "SELECT DISTINCT s FROM Sale s " +
                "LEFT JOIN FETCH s.customer " +
                "ORDER BY s.timestamp DESC",
                Sale.class);
            query.setFirstResult(page * pageSize);
            query.setMaxResults(pageSize);
            return query.list();
        }
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
    
    /**
     * Get the highest invoice number to generate next sequential number
     * Only considers new format (INV-###) to avoid issues with old format dates
     * @return The highest invoice number found, or 0 if none exist
     */
    public synchronized int getMaxInvoiceNumber() {
        try (Session session = sessionFactory.openSession()) {
            // Get ALL invoice numbers that match new format (INV-### where ### is only digits)
            // DO NOT use ORDER BY on strings - it causes "INV-999" > "INV-1000" bug
            // Instead, fetch all and find max in Java
            Query<String> query = session.createQuery(
                "SELECT s.invoiceNumber FROM Sale s WHERE s.invoiceNumber LIKE 'INV-%'",
                String.class);
            List<String> invoiceNumbers = query.list();

            int maxNumber = 0;
            for (String invoiceNumber : invoiceNumbers) {
                if (invoiceNumber != null && invoiceNumber.startsWith("INV-")) {
                    try {
                        // Extract number from formats like INV-001 or INV-123
                        String numberPart = invoiceNumber.substring(4);

                        // Only process if it's all digits (new format)
                        // Skip old format like INV-20251011-abc123
                        if (numberPart.matches("\\d+")) {
                            int num = Integer.parseInt(numberPart);
                            if (num > maxNumber) {
                                maxNumber = num;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid invoice numbers
                    }
                }
            }
            return maxNumber;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
} 