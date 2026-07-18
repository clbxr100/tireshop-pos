package com.tireshop.util;

import com.tireshop.model.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Properties;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connection and session factory
 */
public class DatabaseManager {
    
    private static SessionFactory sessionFactory;
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    
    /**
     * Get the Hibernate SessionFactory
     * @return SessionFactory instance
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                
                // Hibernate settings
                Properties settings = new Properties();
                
                // Using H2 database for development
                settings.put(Environment.DRIVER, "org.h2.Driver");
                settings.put(Environment.URL, "jdbc:h2:./tireshopdb;DB_CLOSE_DELAY=-1");
                settings.put(Environment.USER, "sa");
                settings.put(Environment.PASS, "");
                settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                
                // Echo all executed SQL to stdout
                settings.put(Environment.SHOW_SQL, "true");
                
                // Drop and re-create the database schema on startup (for development)
                // In production, use "update" instead
                settings.put(Environment.HBM2DDL_AUTO, "create-drop");
                
                // Optional optimization settings
                settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
                settings.put(Environment.USE_SQL_COMMENTS, "true");
                settings.put(Environment.FORMAT_SQL, "true");
                settings.put(Environment.STATEMENT_BATCH_SIZE, "100");
                
                configuration.setProperties(settings);
                
                // Register entity classes
                configuration.addAnnotatedClass(Product.class);
                configuration.addAnnotatedClass(Customer.class);
                configuration.addAnnotatedClass(Vehicle.class);
                configuration.addAnnotatedClass(Service.class);
                configuration.addAnnotatedClass(Technician.class);
                configuration.addAnnotatedClass(Sale.class);
                configuration.addAnnotatedClass(SaleItem.class);
                configuration.addAnnotatedClass(Appointment.class);
                
                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties())
                        .build();
                
                sessionFactory = configuration.buildSessionFactory(serviceRegistry);
                
                System.out.println("Database connection established successfully.");
            } catch (Exception e) {
                System.err.println("Initial SessionFactory creation failed: " + e);
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }
    
    /**
     * Close the session factory
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("Database connection closed.");
        }
    }
    
    /**
     * Initialize the database connection
     */
    public static void initialize() {
        // Initialize the session factory
        getSessionFactory();
        System.out.println("Database initialized successfully.");
    }
    
    /**
     * Initialize the database with sample data (for development/testing)
     */
    public static void initializeSampleData() {
        System.out.println("Initializing sample data...");
        DatabaseInitializer.initialize(getSessionFactory());
    }
    
    /**
     * Synchronize price and sellingPrice for all products
     */
    public static void synchronizePrices() {
        try {
            Session session = getSessionFactory().openSession();
            Transaction transaction = session.beginTransaction();
            
            // Read the SQL script
            InputStream inputStream = DatabaseManager.class.getResourceAsStream("/db/sync-prices.sql");
            if (inputStream != null) {
                String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Split and execute each statement
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    if (!statement.trim().isEmpty()) {
                        session.createNativeQuery(statement.trim()).executeUpdate();
                    }
                }
                
                transaction.commit();
                session.close();
                LOGGER.info("Price synchronization completed successfully");
            } else {
                LOGGER.warning("Could not find sync-prices.sql script");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error synchronizing prices", e);
        }
    }
} 