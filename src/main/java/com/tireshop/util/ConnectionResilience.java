package com.tireshop.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.JDBCConnectionException;

import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles database connection resilience and automatic reconnection
 */
public class ConnectionResilience {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectionResilience.class.getName());
    private static ConnectionResilience instance;
    private static boolean connectionHealthy = true;
    private static long lastSuccessfulConnection = System.currentTimeMillis();
    private static int consecutiveFailures = 0;
    
    private ScheduledExecutorService healthCheckExecutor;
    private ConnectionLostCallback callback;
    
    private ConnectionResilience() {
        startHealthMonitoring();
    }
    
    public static ConnectionResilience getInstance() {
        if (instance == null) {
            instance = new ConnectionResilience();
        }
        return instance;
    }
    
    /**
     * Set callback for when connection is lost/restored
     */
    public void setConnectionCallback(ConnectionLostCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Check if database connection is healthy
     */
    public static boolean isConnectionHealthy() {
        return connectionHealthy;
    }
    
    /**
     * Get time since last successful connection
     */
    public static long getTimeSinceLastConnection() {
        return System.currentTimeMillis() - lastSuccessfulConnection;
    }
    
    /**
     * Execute a database operation with automatic retry
     */
    public static <T> T executeWithRetry(DatabaseOperation<T> operation, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                T result = operation.execute();
                
                // Operation succeeded - mark connection as healthy
                if (!connectionHealthy) {
                    connectionHealthy = true;
                    consecutiveFailures = 0;
                    LOGGER.info("✅ Database connection restored!");
                }
                lastSuccessfulConnection = System.currentTimeMillis();
                
                return result;
                
            } catch (JDBCConnectionException | org.hibernate.exception.GenericJDBCException e) {
                lastException = e;
                attempts++;
                consecutiveFailures++;
                
                if (!connectionHealthy) {
                    LOGGER.warning("⚠️ Database still unreachable (attempt " + attempts + "/" + maxRetries + ")");
                } else {
                    connectionHealthy = false;
                    LOGGER.severe("❌ Database connection lost! Attempting to reconnect...");
                }
                
                if (attempts < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s, 8s...
                    long waitTime = (long) Math.pow(2, attempts) * 1000;
                    try {
                        Thread.sleep(Math.min(waitTime, 10000)); // Max 10 seconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                // Other exceptions (not connection-related) - don't retry
                throw new RuntimeException(e);
            }
        }
        
        // All retries failed
        LOGGER.severe("❌ Database operation failed after " + maxRetries + " attempts");
        throw new RuntimeException("Database unavailable after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Start background health monitoring
     */
    private void startHealthMonitoring() {
        healthCheckExecutor = Executors.newScheduledThreadPool(1);
        
        healthCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                checkConnectionHealth();
            } catch (Exception e) {
                LOGGER.warning("Health check error: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // Check every 30 seconds
        
        LOGGER.info("✓ Connection health monitoring started");
    }
    
    /**
     * Check database connection health
     */
    private void checkConnectionHealth() {
        try {
            Session session = DatabaseManager.getSessionFactory().openSession();
            try {
                // Simple test query
                session.createNativeQuery("SELECT 1").getSingleResult();
                
                // Connection is healthy
                if (!connectionHealthy) {
                    connectionHealthy = true;
                    consecutiveFailures = 0;
                    LOGGER.info("✅ Database connection restored!");
                    if (callback != null) {
                        callback.onConnectionRestored();
                    }
                }
                lastSuccessfulConnection = System.currentTimeMillis();
                
            } finally {
                session.close();
            }
        } catch (Exception e) {
            consecutiveFailures++;
            
            if (connectionHealthy) {
                connectionHealthy = false;
                LOGGER.severe("❌ Database connection lost! Will attempt automatic reconnection...");
                if (callback != null) {
                    callback.onConnectionLost();
                }
            }
            
            // Log degraded state
            if (consecutiveFailures % 10 == 0) {
                LOGGER.warning("⚠️ Database unreachable for " + (consecutiveFailures * 30) + " seconds");
            }
        }
    }
    
    /**
     * Stop health monitoring
     */
    public void stop() {
        if (healthCheckExecutor != null && !healthCheckExecutor.isShutdown()) {
            healthCheckExecutor.shutdown();
        }
    }
    
    /**
     * Interface for database operations
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Callback interface for connection state changes
     */
    public interface ConnectionLostCallback {
        void onConnectionLost();
        void onConnectionRestored();
    }
}



