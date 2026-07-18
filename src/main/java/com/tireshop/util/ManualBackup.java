package com.tireshop.util;

/**
 * Manual backup utility that can be run from command line
 */
public class ManualBackup {
    
    public static void main(String[] args) {
        System.out.println("Starting manual backup...");
        
        try {
            // Use the existing backup service
            AutoBackupService backupService = AutoBackupService.getInstance();
            backupService.performBackup();
            
            System.out.println("\nManual backup completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error performing manual backup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 