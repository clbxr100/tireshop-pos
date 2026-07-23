package com.tireshop.dao;

import com.tireshop.model.ChargeAccountPayment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for ChargeAccountPayment entities (store charge account payoffs)
 */
public class ChargeAccountPaymentDao extends HibernateDao<ChargeAccountPayment, Long> {

    public ChargeAccountPaymentDao(SessionFactory sessionFactory) {
        super(ChargeAccountPayment.class, sessionFactory);
    }

    /**
     * Find all account payments for a customer, newest first
     */
    public List<ChargeAccountPayment> findByCustomerId(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            Query<ChargeAccountPayment> query = session.createQuery(
                    "FROM ChargeAccountPayment WHERE customer.id = :customerId ORDER BY paymentTimestamp DESC",
                    ChargeAccountPayment.class);
            query.setParameter("customerId", customerId);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
