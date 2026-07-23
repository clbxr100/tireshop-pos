package com.tireshop.dao;

import com.tireshop.model.SalePayment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for SalePayment entities
 * Supports split payments where a sale has multiple payment records
 */
public class SalePaymentDao extends HibernateDao<SalePayment, Long> {

    public SalePaymentDao(SessionFactory sessionFactory) {
        super(SalePayment.class, sessionFactory);
    }

    /**
     * Find all payment records for a sale
     * @param saleId Sale ID
     * @return List of payments (empty if the sale has no recorded payments)
     */
    public List<SalePayment> findBySaleId(Long saleId) {
        try (Session session = sessionFactory.openSession()) {
            Query<SalePayment> query = session.createQuery(
                    "FROM SalePayment WHERE sale.id = :saleId ORDER BY paymentTimestamp",
                    SalePayment.class);
            query.setParameter("saleId", saleId);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find all payment records for paid, non-voided sales within a date range
     * (inclusive). Used for payment-type breakdowns in reports - voided sales
     * are excluded so refunds don't inflate the totals.
     */
    public List<SalePayment> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startOfDay = fromDate.atStartOfDay();
            LocalDateTime endOfDay = toDate.atTime(LocalTime.MAX);

            Query<SalePayment> query = session.createQuery(
                    "FROM SalePayment sp WHERE sp.sale.timestamp >= :startOfDay "
                    + "AND sp.sale.timestamp <= :endOfDay AND sp.sale.isPaid = true "
                    + "AND sp.sale.isVoided = false",
                    SalePayment.class);
            query.setParameter("startOfDay", startOfDay);
            query.setParameter("endOfDay", endOfDay);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find all store charge payment records for a customer (charges made against
     * their charge account on paid sales), newest first
     */
    public List<SalePayment> findStoreChargesByCustomer(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<SalePayment> query = session.createQuery(
                    "FROM SalePayment sp WHERE sp.sale.customer.id = :customerId "
                    + "AND sp.paymentType = :type ORDER BY sp.paymentTimestamp DESC",
                    SalePayment.class);
            query.setParameter("customerId", customerId);
            query.setParameter("type", com.tireshop.model.PaymentType.STORE_CHARGE);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Delete all payment records for a sale
     * @param saleId Sale ID
     * @return Count of records deleted
     */
    public int deleteBySaleId(Long saleId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Query<?> query = session.createQuery(
                    "DELETE FROM SalePayment WHERE sale.id = :saleId");
            query.setParameter("saleId", saleId);
            int result = query.executeUpdate();
            session.getTransaction().commit();
            return result;
        }
    }
}
