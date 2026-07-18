package com.tireshop;

import com.tireshop.util.DatabaseManager;
import com.tireshop.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Main entry point for the Tire Shop POS application
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            DatabaseManager.initialize();
            
            // Synchronize product prices in the database
            DatabaseManager.synchronizePrices();
            
            // For development/testing, load sample data
            // DatabaseManager.initializeSampleData(); // DISABLED - No mock data
            
            // Initialize UI
            MainView mainView = new MainView();
            mainView.initialize(primaryStage);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }
    
    @Override
    public void stop() {
        // Shutdown database connection
        DatabaseManager.shutdown();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 