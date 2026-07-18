package com.tireshop;

import com.tireshop.util.AutoBackupService;
import com.tireshop.util.DatabaseManager;
import com.tireshop.service.AutoTireScrapingService;
import com.tireshop.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Main entry point for the Tire Shop POS application
 */
public class Main extends Application {
    
    private MainView mainView; // Added field to store MainView instance

    @Override
    public void start(Stage primaryStage) {
        try {
            // If the app was just updated, confirm the boot so the update
            // script knows the new version started successfully (else it rolls back)
            boolean justUpdated = com.tireshop.service.UpdateService.confirmSuccessfulBootAfterUpdate();
            if (justUpdated) {
                System.out.println("✅ Update applied successfully - running the new version");
            }

            // Initialize database
            DatabaseManager.initialize();
            
            // Synchronize product prices in the database
            DatabaseManager.synchronizePrices();
            
            // For development/testing, load sample data
            // DatabaseManager.initializeSampleData(); // DISABLED - No mock data
            
            // Start connection resilience monitoring
            com.tireshop.util.ConnectionResilience.getInstance();
            System.out.println("🔗 Database connection monitoring started - automatic reconnection enabled");
            
            // Start automatic backup service
            AutoBackupService.getInstance().start();
            
            // Start automatic tire scraping service
            AutoTireScrapingService.getInstance().startAutoScraping();
            System.out.println("🏎️ Tire scraping service initialized - tire data will be automatically updated");
            
            // Initialize UI
            this.mainView = new MainView(); // Store the instance
            this.mainView.initialize(primaryStage);
            
            // Set up connection status callback
            com.tireshop.util.ConnectionResilience.getInstance().setConnectionCallback(
                new com.tireshop.util.ConnectionResilience.ConnectionLostCallback() {
                    @Override
                    public void onConnectionLost() {
                        javafx.application.Platform.runLater(() -> {
                            mainView.showConnectionWarning(true);
                        });
                    }
                    
                    @Override
                    public void onConnectionRestored() {
                        javafx.application.Platform.runLater(() -> {
                            mainView.showConnectionWarning(false);
                        });
                    }
                });
            
            primaryStage.show();

            // Check for app updates in the background (if configured)
            checkForUpdatesInBackground();

        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }
    
    /**
     * Background update check on startup - only notifies; installation is done
     * from Admin -> General Settings -> Updates.
     */
    private void checkForUpdatesInBackground() {
        new Thread(() -> {
            try {
                com.tireshop.util.SettingsService settings = com.tireshop.util.SettingsService.getInstance();
                boolean autoCheck = Boolean.parseBoolean(settings.getSetting(
                        com.tireshop.service.UpdateService.KEY_AUTO_CHECK, "true"));
                if (!autoCheck) {
                    return;
                }

                com.tireshop.service.UpdateService updateService = new com.tireshop.service.UpdateService(settings);
                if (!updateService.isConfigured() || !updateService.isRunningFromJar()) {
                    return;
                }

                com.tireshop.service.UpdateService.UpdateInfo info = updateService.checkForUpdate();
                if (info != null) {
                    Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Update Available");
                        alert.setHeaderText("Version " + info.version + " is available");
                        alert.setContentText("Go to Admin → General Settings → Updates to install it.");
                        alert.show();
                    });
                }
            } catch (Exception e) {
                // Update check is best-effort - never disturb the user over it
                System.out.println("Update check skipped: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void stop() {
        // Shutdown application resources
        if (this.mainView != null) {
            this.mainView.shutdownResources(); // Call shutdown on MainView
        }
        // Stop automatic backup service
        AutoBackupService.getInstance().stop();
        // Stop automatic tire scraping service
        AutoTireScrapingService.getInstance().stopAutoScraping();
        // Shutdown database connection
        DatabaseManager.shutdown();
        System.out.println("Application stopped and resources released."); // Added log
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 