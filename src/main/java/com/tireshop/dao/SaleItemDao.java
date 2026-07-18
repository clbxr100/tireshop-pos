package com.tireshop.dao;

import com.tireshop.model.SaleItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for SaleItem entities
 */
public class SaleItemDao extends HibernateDao<SaleItem, Long> {
    
    public SaleItemDao(SessionFactory sessionFactory) {
        super(SaleItem.class, sessionFactory);
    }
    
    /**
     * Find items by sale ID
     * @param saleId Sale ID
     * @return List of items in the sale
     */
    public List<SaleItem> findBySaleId(Long saleId) {
        try (Session session = sessionFactory.openSession()) {
            Query<SaleItem> query = session.createQuery(
                    "FROM SaleItem WHERE sale.id = :saleId", 
                    SaleItem.class);
            query.setParameter("saleId", saleId);
            return query.list();
        }
    }
    
    /**
     * Find items by product ID
     * @param productId Product ID
     * @return List of sale items containing the product
     */
    public List<SaleItem> findByProductId(Long productId) {
        try (Session session = sessionFactory.openSession()) {
            Query<SaleItem> query = session.createQuery(
                    "FROM SaleItem WHERE product.id = :productId", 
                    SaleItem.class);
            query.setParameter("productId", productId);
            return query.list();
        }
    }
    
    /**
     * Find items by service ID
     * @param serviceId Service ID
     * @return List of sale items containing the service
     */
    public List<SaleItem> findByServiceId(Long serviceId) {
        try (Session session = sessionFactory.openSession()) {
            Query<SaleItem> query = session.createQuery(
                    "FROM SaleItem WHERE service.id = :serviceId", 
                    SaleItem.class);
            query.setParameter("serviceId", serviceId);
            return query.list();
        }
    }
    
    /**
     * Delete all items for a sale
     * @param saleId Sale ID
     * @return Count of items deleted
     */
    public int deleteBySaleId(Long saleId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Query<?> query = session.createQuery(
                    "DELETE FROM SaleItem WHERE sale.id = :saleId");
            query.setParameter("saleId", saleId);
            int result = query.executeUpdate();
            session.getTransaction().commit();
            return result;
        }
    }

    public List<SaleItem> findProductItemsBySaleDateRange(LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = fromDate.atStartOfDay();
            LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);

            // Fetches SaleItems of type PRODUCT where the associated Sale's timestamp is within the range AND the sale is paid
            Query<SaleItem> query = session.createQuery(
                "FROM SaleItem si WHERE si.itemType = :itemType AND si.sale.timestamp >= :startOfDay AND si.sale.timestamp <= :endOfDay AND si.sale.isPaid = true", 
                SaleItem.class);
            query.setParameter("itemType", "PRODUCT"); // Assuming itemType is stored as String "PRODUCT"
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // Return new list on error
        }
    }

    public List<SaleItem> findServiceItemsByTechnicianAndDateRange(Long technicianId, LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = fromDate.atStartOfDay();
            LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);

            Query<SaleItem> query = session.createQuery(
                "FROM SaleItem si WHERE si.itemType = :itemType AND si.technician.id = :technicianId AND si.sale.timestamp >= :startOfDay AND si.sale.timestamp <= :endOfDay ORDER BY si.sale.timestamp DESC", 
                SaleItem.class);
            query.setParameter("itemType", "SERVICE"); // Assuming itemType distinguishes services
            query.setParameter("technicianId", technicianId);
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
} 