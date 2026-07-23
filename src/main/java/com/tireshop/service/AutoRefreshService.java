package com.tireshop.service;

import com.tireshop.view.MainView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service that automatically refreshes table data at regular intervals
 * to keep data synchronized across multiple machines
 */
public class AutoRefreshService {
    
    private static AutoRefreshService instance;
    private ScheduledExecutorService scheduler;
    private MainView mainView;
    private boolean isRunning = false;
    
    // Refresh interval in seconds (10 seconds for faster updates)
    private static final int REFRESH_INTERVAL = 10;
    
    private AutoRefreshService() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public static AutoRefreshService getInstance() {
        if (instance == null) {
            instance = new AutoRefreshService();
        }
        return instance;
    }
    
    /**
     * Set the MainView reference for refreshing
     */
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }
    
    /**
     * Start the auto-refresh service
     */
    public void start() {
        if (isRunning) {
            System.out.println("🔄 Auto-refresh service is already running");
            return;
        }
        
        if (mainView == null) {
            System.out.println("⚠️ MainView not set - cannot start auto-refresh");
            return;
        }
        
        System.out.println("🚀 Starting auto-refresh service (every " + REFRESH_INTERVAL + " seconds)...");
        isRunning = true;
        
        // Start refreshing after 5 seconds (quick first refresh)
        scheduler.scheduleAtFixedRate(this::performRefresh, 5, REFRESH_INTERVAL, TimeUnit.SECONDS);
        
        System.out.println("✅ Auto-refresh service started - pages will auto-update every " + REFRESH_INTERVAL + " seconds");
    }
    
    /**
     * Stop the auto-refresh service
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        System.out.println("🛑 Stopping auto-refresh service...");
        isRunning = false;
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("✅ Auto-refresh service stopped");
    }
    
    /**
     * Perform the refresh operation
     */
    private void performRefresh() {
        try {
            if (mainView != null) {
                // Run refresh on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Never refresh while a dialog/popup is open - it would wipe
                        // whatever the user is working on (payment, new sale, edits...)
                        if (mainView.isDialogOpen()) {
                            return;
                        }
                        mainView.refreshCurrentTab();  // Only refresh active tab for better performance
                        // Silent refresh - no console spam
                    } catch (Exception e) {
                        System.err.println("❌ Error during auto-refresh: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Error in auto-refresh service: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the service is running
     */
    public boolean isRunning() {
        return isRunning;
    }
}
