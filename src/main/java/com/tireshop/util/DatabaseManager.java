package com.tireshop.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Properties;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

/**
 * Manages database connections and Hibernate SessionFactory
 */
public class DatabaseManager {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static SessionFactory sessionFactory;
    
    static {
        try {
            Properties dbProps = loadDatabaseProperties();
            
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            
            // Override with properties from database.properties
            String dbType = dbProps.getProperty("database.type", "h2");
            String dbUrl = dbProps.getProperty("database.url", "jdbc:h2:./tireshop_db");
            String dbUsername = dbProps.getProperty("database.username", "sa");
            String dbPassword = dbProps.getProperty("database.password", "");
            
            // Set the appropriate dialect based on database type
            switch (dbType.toLowerCase()) {
                case "postgresql":
                    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                    configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
                    break;
                case "mysql":
                    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
                    configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
                    break;
                case "h2":
                default:
                    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                    configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
                    break;
            }
            
            // Set connection properties
            configuration.setProperty("hibernate.connection.url", dbUrl);
            configuration.setProperty("hibernate.connection.username", dbUsername);
            configuration.setProperty("hibernate.connection.password", dbPassword);
            
            // Set pool size
            String poolSize = dbProps.getProperty("database.pool.size", "10");
            configuration.setProperty("hibernate.connection.pool_size", poolSize);
            
            // Set show_sql
            String showSql = dbProps.getProperty("database.show_sql", "false");
            configuration.setProperty("hibernate.show_sql", showSql);
            
            LOGGER.info("Connecting to database: " + dbType + " at " + dbUrl);
            
            sessionFactory = configuration.buildSessionFactory();
            
            LOGGER.info("Database connection established successfully");
            
        } catch (Exception e) {
            LOGGER.severe("Failed to create SessionFactory: " + e.getMessage());
            throw new RuntimeException("Failed to create SessionFactory", e);
        }
    }
    
    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        
        // First, try to load from local file (for client configurations)
        File localConfig = new File("database.properties");
        if (localConfig.exists()) {
            try (java.io.FileInputStream input = new java.io.FileInputStream(localConfig)) {
                props.load(input);
                LOGGER.info("Loaded database properties from local database.properties file");
                return props;
            } catch (IOException e) {
                LOGGER.warning("Error loading local database.properties: " + e.getMessage());
            }
        }
        
        // If no local file, load from resources
        try (InputStream input = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Loaded database properties from resources");
            } else {
                LOGGER.warning("database.properties not found, using defaults");
            }
        } catch (IOException e) {
            LOGGER.warning("Error loading database.properties: " + e.getMessage());
        }
        return props;
    }
    
    /**
     * Get the SessionFactory instance
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    /**
     * Shutdown the SessionFactory
     */
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    
    /**
     * Initialize the database connection
     */
    public static void initialize() {
        if (sessionFactory == null) {
            throw new IllegalStateException("SessionFactory not initialized");
        }
        
        // Fix schema issues before proceeding
        fixSchemaIssues();
        
        // Synchronize prices between Product and Service tables
        synchronizePrices();
        
        // Initialize sample data if needed
        // initializeSampleData(); // DISABLED - No mock data
        
        LOGGER.info("Database initialized successfully.");
    }
    
    /**
     * Fix known schema issues
     */
    private static void fixSchemaIssues() {
        Session session = null;
        Transaction transaction = null;
        
        try {
            // Use a temporary connection to fix schema
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();
            
            // Check and fix sales table schema
            try {
                session.createNativeQuery("SELECT isVoided FROM sales LIMIT 1").getResultList();
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Fixing sales table schema - adding void-related columns");
                try {
                    session.createNativeQuery("ALTER TABLE sales ADD COLUMN isVoided BOOLEAN DEFAULT FALSE").executeUpdate();
                } catch (Exception ex) {
                    LOGGER.warning("Could not add isVoided column: " + ex.getMessage());
                }
                try {
                    session.createNativeQuery("ALTER TABLE sales ADD COLUMN voidReason VARCHAR(255)").executeUpdate();
                } catch (Exception ex) {
                    LOGGER.warning("Could not add voidReason column: " + ex.getMessage());
                }
                try {
                    session.createNativeQuery("ALTER TABLE sales ADD COLUMN voidTimestamp TIMESTAMP").executeUpdate();
                } catch (Exception ex) {
                    LOGGER.warning("Could not add voidTimestamp column: " + ex.getMessage());
                }
                // Update existing rows
                try {
                    session.createNativeQuery("UPDATE sales SET isVoided = FALSE WHERE isVoided IS NULL").executeUpdate();
                } catch (Exception ex) {
                    LOGGER.warning("Could not update isVoided values: " + ex.getMessage());
                }
            }
            
            // Check and fix customers table schema - add taxExempt column
            try {
                session.createNativeQuery("SELECT taxExempt FROM customers LIMIT 1").getResultList();
                LOGGER.info("Column 'taxExempt' already exists in customers table");
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Fixing customers table schema - adding taxExempt column");
                try {
                    session.createNativeQuery("ALTER TABLE customers ADD COLUMN taxExempt BOOLEAN DEFAULT FALSE").executeUpdate();
                    // Update existing rows
                    session.createNativeQuery("UPDATE customers SET taxExempt = FALSE WHERE taxExempt IS NULL").executeUpdate();
                    LOGGER.info("Successfully added taxExempt column to customers table");
                } catch (Exception ex) {
                    LOGGER.warning("Could not add taxExempt column to customers table: " + ex.getMessage());
                }
            }
            
            // Check and fix services table schema - add taxable column
            try {
                session.createNativeQuery("SELECT taxable FROM services LIMIT 1").getResultList();
                LOGGER.info("Column 'taxable' already exists in services table");
            } catch (Exception e) {
                // Column doesn't exist, add it
                LOGGER.info("Fixing services table schema - adding taxable column");
                try {
                    session.createNativeQuery("ALTER TABLE services ADD COLUMN taxable BOOLEAN DEFAULT TRUE").executeUpdate();
                    // Update existing rows
                    session.createNativeQuery("UPDATE services SET taxable = TRUE WHERE taxable IS NULL").executeUpdate();
                    LOGGER.info("Successfully added taxable column to services table");
                } catch (Exception ex) {
                    LOGGER.warning("Could not add taxable column to services table: " + ex.getMessage());
                }
            }
            
            transaction.commit();
            LOGGER.info("Schema fixes applied successfully");
            
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            LOGGER.log(Level.WARNING, "Error fixing schema issues", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Initialize the database with sample data (for development/testing)
     */
    public static void initializeSampleData() {
        LOGGER.info("Initializing sample data...");
        try {
            DatabaseInitializer.initialize(getSessionFactory());
            LOGGER.info("Sample data initialized successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize sample data", e);
            throw e;
        }
    }
    
    /**
     * Synchronize product prices in the database
     */
    public static void synchronizePrices() {
        Session session = null;
        Transaction transaction = null;
        try {
            session = getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
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
                LOGGER.info("Price synchronization completed successfully");
            } else {
                LOGGER.warning("Could not find sync-prices.sql script");
            }
        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception re) {
                    LOGGER.log(Level.WARNING, "Error rolling back transaction", re);
                }
            }
            LOGGER.log(Level.SEVERE, "Error synchronizing prices", e);
            throw new RuntimeException("Failed to synchronize prices", e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing session", e);
                }
            }
        }
    }
} 