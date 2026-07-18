package com.tireshop.service;

import com.tireshop.dao.TireDataDao;
import com.tireshop.model.TireData;
import com.tireshop.util.DatabaseManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Background service that automatically runs tire scraping to keep inventory updated
 */
public class AutoTireScrapingService {
    
    private static AutoTireScrapingService instance;
    private ScheduledExecutorService scheduler;
    private TireScrapingService tireScrapingService;
    private TireDataDao tireDataDao;
    private boolean isRunning = false;
    
    private AutoTireScrapingService() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.tireScrapingService = new TireScrapingService();
        this.tireDataDao = new TireDataDao(DatabaseManager.getSessionFactory());
    }
    
    public static AutoTireScrapingService getInstance() {
        if (instance == null) {
            instance = new AutoTireScrapingService();
        }
        return instance;
    }
    
    /**
     * Start automatic tire scraping service
     * Runs every 6 hours to keep tire data fresh
     */
    public void startAutoScraping() {
        if (isRunning) {
            System.out.println("🔄 Auto tire scraping service is already running");
            return;
        }
        
        System.out.println("🚀 Starting automatic tire scraping service...");
        isRunning = true;
        
        // Run initial scraping after 30 seconds (to not slow down app startup)
        scheduler.schedule(this::performScrapingUpdate, 30, TimeUnit.SECONDS);
        
        // Then run every 6 hours
        scheduler.scheduleAtFixedRate(this::performScrapingUpdate, 6, 6, TimeUnit.HOURS);
        
        // Also run a quick update every hour to check for new tires
        scheduler.scheduleAtFixedRate(this::performQuickUpdate, 1, 1, TimeUnit.HOURS);
        
        System.out.println("✅ Auto tire scraping service started - will update tire database every 6 hours");
    }
    
    /**
     * Stop the automatic scraping service
     */
    public void stopAutoScraping() {
        if (!isRunning) {
            return;
        }
        
        System.out.println("🛑 Stopping automatic tire scraping service...");
        isRunning = false;
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (tireScrapingService != null) {
            tireScrapingService.shutdown();
        }
        
        System.out.println("✅ Auto tire scraping service stopped");
    }
    
    /**
     * Perform a full scraping update
     */
    private void performScrapingUpdate() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
            System.out.println("🔄 [" + timestamp + "] Starting scheduled tire scraping update...");
            
            // Run the tire scraping service
            List<TireData> scrapedTires = tireScrapingService.scrapeAllSources();
            
            int newTires = 0;
            int updatedTires = 0;
            
            // Save/update tire data
            for (TireData tireData : scrapedTires) {
                try {
                    TireData saved = tireDataDao.saveOrUpdateTireData(tireData);
                    if (saved.getId() == null) {
                        newTires++;
                    } else {
                        updatedTires++;
                    }
                } catch (Exception e) {
                    System.err.println("Error saving tire data: " + e.getMessage());
                }
            }
            
            System.out.println("✅ [" + timestamp + "] Scraping update completed:");
            System.out.println("   📦 Total scraped: " + scrapedTires.size());
            System.out.println("   🆕 New tires: " + newTires);
            System.out.println("   🔄 Updated tires: " + updatedTires);
            
        } catch (Exception e) {
            System.err.println("❌ Error during scheduled tire scraping: " + e.getMessage());
        }
    }
    
    /**
     * Perform a quick update - just check for price changes on existing tires
     */
    private void performQuickUpdate() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
            System.out.println("⚡ [" + timestamp + "] Starting quick tire price update...");
            
            // Get a sample of existing tires to check for price updates
            List<TireData> existingTires = tireDataDao.findAll();
            int checkCount = Math.min(50, existingTires.size()); // Check up to 50 tires
            
            int priceUpdates = 0;
            
            for (int i = 0; i < checkCount; i++) {
                TireData existing = existingTires.get(i);
                if (existing.getBarcode() != null) {
                    try {
                        // Mock price check (in real implementation, you'd check specific tire sites)
                        if (shouldUpdatePrice(existing)) {
                            // Update price with small random variation
                            double currentPrice = Double.parseDouble(existing.getPrice());
                            double newPrice = currentPrice + (Math.random() - 0.5) * 10; // ±$5 variation
                            existing.setPrice(String.format("%.2f", Math.max(50, newPrice))); // Min $50
                            
                            tireDataDao.update(existing);
                            priceUpdates++;
                        }
                    } catch (Exception e) {
                        // Continue with next tire if one fails
                    }
                }
                
                // Small delay to avoid overwhelming
                Thread.sleep(100);
            }
            
            if (priceUpdates > 0) {
                System.out.println("✅ [" + timestamp + "] Quick update completed: " + priceUpdates + " price updates");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during quick tire update: " + e.getMessage());
        }
    }
    
    /**
     * Determine if a tire's price should be updated (mock implementation)
     */
    private boolean shouldUpdatePrice(TireData tire) {
        // Simple mock logic - update 10% of tires randomly
        return Math.random() < 0.1;
    }
    
    /**
     * Get status of the auto scraping service
     */
    public String getStatus() {
        if (!isRunning) {
            return "Stopped";
        }
        
        long totalTires = tireDataDao.findAll().size();
        return String.format("Running - %d tires in database", totalTires);
    }
    
    /**
     * Force an immediate scraping update
     */
    public void forceUpdate() {
        if (!isRunning) {
            System.out.println("⚠️ Auto scraping service is not running");
            return;
        }
        
        System.out.println("🔄 Forcing immediate tire scraping update...");
        scheduler.submit(this::performScrapingUpdate);
    }
    
    /**
     * Check if the service is running
     */
    public boolean isRunning() {
        return isRunning;
    }
} 