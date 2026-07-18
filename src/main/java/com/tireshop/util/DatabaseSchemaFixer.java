package com.tireshop.util;

import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utility to fix database schema issues
 */
public class DatabaseSchemaFixer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseSchemaFixer.class.getName());
    
    /**
     * Fix the sales table schema by adding missing columns
     */
    public static void fixSalesTableSchema() {
        Session session = null;
        Transaction transaction = null;
        
        try {
            session = DatabaseManager.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
            // Check if columns exist and add them if they don't
            try {
                session.createNativeQuery("SELECT isVoided FROM sales LIMIT 1").getResultList();
                LOGGER.info("Column 'isVoided' already exists");
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Adding column 'isVoided' to sales table");
                session.createNativeQuery("ALTER TABLE sales ADD COLUMN isVoided BOOLEAN DEFAULT FALSE").executeUpdate();
                session.createNativeQuery("UPDATE sales SET isVoided = FALSE WHERE isVoided IS NULL").executeUpdate();
            }
            
            try {
                session.createNativeQuery("SELECT voidReason FROM sales LIMIT 1").getResultList();
                LOGGER.info("Column 'voidReason' already exists");
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Adding column 'voidReason' to sales table");
                session.createNativeQuery("ALTER TABLE sales ADD COLUMN voidReason VARCHAR(255)").executeUpdate();
            }
            
            try {
                session.createNativeQuery("SELECT voidTimestamp FROM sales LIMIT 1").getResultList();
                LOGGER.info("Column 'voidTimestamp' already exists");
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Adding column 'voidTimestamp' to sales table");
                session.createNativeQuery("ALTER TABLE sales ADD COLUMN voidTimestamp TIMESTAMP").executeUpdate();
            }
            
            transaction.commit();
            LOGGER.info("Sales table schema fixed successfully");
            
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.log(Level.SEVERE, "Error fixing sales table schema", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
} 