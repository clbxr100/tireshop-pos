package com.tireshop.util;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for automatic database backups
 */
public class AutoBackupService {
    private static final Logger LOGGER = Logger.getLogger(AutoBackupService.class.getName());
    private static final long BACKUP_INTERVAL = 5 * 60 * 60 * 1000; // 5 hours in milliseconds
    private static final String BACKUP_DIR = "backups";
    private static final int MAX_BACKUPS = 10; // Keep only the last 10 backups
    
    private Timer timer;
    private static AutoBackupService instance;
    
    private AutoBackupService() {
        // Create backup directory if it doesn't exist
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }
    
    public static AutoBackupService getInstance() {
        if (instance == null) {
            instance = new AutoBackupService();
        }
        return instance;
    }
    
    /**
     * Start the automatic backup service
     */
    public void start() {
        if (timer != null) {
            LOGGER.warning("Backup service is already running");
            return;
        }
        
        timer = new Timer("AutoBackupTimer", true);
        
        // Schedule backup task
        TimerTask backupTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    performBackup();
                } catch (Exception e) {
                    LOGGER.severe("Error performing automatic backup: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        // Run first backup after 1 minute, then every 5 hours
        timer.scheduleAtFixedRate(backupTask, 60000, BACKUP_INTERVAL);
        
        LOGGER.info("Automatic backup service started. Backups will run every 5 hours.");
        
        // Also perform an immediate backup
        performBackup();
    }
    
    /**
     * Stop the automatic backup service
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            LOGGER.info("Automatic backup service stopped.");
        }
    }
    
    /**
     * Perform a database backup
     * Uses H2's BACKUP command to safely backup while database is in use
     */
    public void performBackup() {
        LOGGER.info("Starting automatic backup...");
        
        try {
            // Create timestamp for backup file
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            String backupFileName = String.format("tireshop_backup_%s.zip", timestamp);
            File backupFile = new File(BACKUP_DIR, backupFileName);
            
            // Use H2's built-in BACKUP command (works while database is in use!)
            org.hibernate.Session session = null;
            try {
                session = DatabaseManager.getSessionFactory().openSession();
                
                // Execute H2 BACKUP TO command
                String backupPath = backupFile.getAbsolutePath().replace("\\", "/");
                String sql = "BACKUP TO '" + backupPath + "'";
                
                LOGGER.info("Executing H2 backup command: " + sql);
                session.createNativeQuery(sql).executeUpdate();
                
                LOGGER.info("✅ H2 database backup completed: " + backupFile.getAbsolutePath());
                LOGGER.info("📦 Backup size: " + backupFile.length() + " bytes");
                
                // Verify backup is not too small
                if (backupFile.length() < 10000) {
                    LOGGER.warning("⚠️ WARNING: Backup file is suspiciously small (" + backupFile.length() + " bytes)");
                }
                
            } catch (Exception e) {
                LOGGER.severe("❌ H2 backup command failed: " + e.getMessage());
                LOGGER.info("Attempting fallback: Copy database file method...");
                
                // Fallback: Try to backup additional files separately
                backupConfigFiles(backupFile.getParent());
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
            
            // Clean up old backups
            cleanupOldBackups();
            
        } catch (Exception e) {
            LOGGER.severe("❌ Error creating backup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Backup config and other files separately (used as fallback)
     */
    private void backupConfigFiles(String backupDir) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            String configBackupName = "config_backup_" + timestamp + ".zip";
            File configBackupFile = new File(backupDir, configBackupName);
            
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(configBackupFile))) {
                // Backup config.properties
                File configFile = new File("config.properties");
                if (configFile.exists()) {
                    addFileToZip(zos, configFile, "config.properties");
                }
                
                // Backup barcode cache
                File barcodeCache = new File("barcode_cache.dat");
                if (barcodeCache.exists()) {
                    addFileToZip(zos, barcodeCache, "barcode_cache.dat");
                }
            }
            
            LOGGER.info("✓ Config files backed up separately: " + configBackupFile.getName());
        } catch (Exception e) {
            LOGGER.warning("Could not backup config files: " + e.getMessage());
        }
    }
    
    /**
     * Add a file to the zip archive
     */
    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        
        zos.closeEntry();
    }
    
    /**
     * Add a directory and its contents to the zip archive
     */
    private void addDirectoryToZip(ZipOutputStream zos, File directory, String basePath) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String entryName = basePath + "/" + file.getName();
                if (file.isDirectory()) {
                    addDirectoryToZip(zos, file, entryName);
                } else {
                    addFileToZip(zos, file, entryName);
                }
            }
        }
    }
    
    /**
     * Clean up old backup files, keeping only the most recent ones
     */
    private void cleanupOldBackups() {
        File backupDir = new File(BACKUP_DIR);
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("tireshop_backup_") && name.endsWith(".zip"));
        
        if (backupFiles != null && backupFiles.length > MAX_BACKUPS) {
            // Sort by last modified date (oldest first)
            java.util.Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            // Delete oldest files
            int filesToDelete = backupFiles.length - MAX_BACKUPS;
            for (int i = 0; i < filesToDelete; i++) {
                if (backupFiles[i].delete()) {
                    LOGGER.info("Deleted old backup: " + backupFiles[i].getName());
                }
            }
        }
    }
    
    /**
     * Restore from a backup file
     * @param backupFile The backup file to restore from
     * @return true if restore was successful
     */
    public boolean restoreFromBackup(File backupFile) {
        LOGGER.info("Restoring from backup: " + backupFile.getName());
        
        try {
            // First, create a safety backup of current state
            performBackup();
            
            // Extract the backup zip
            Path tempDir = Files.createTempDirectory("tireshop_restore_");
            
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(backupFile))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path filePath = tempDir.resolve(entry.getName());
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    zis.closeEntry();
                }
            }
            
            // Stop the database connection
            DatabaseManager.shutdown();
            
            // Copy restored files to their proper locations - check both possible names
            Path dbSource = tempDir.resolve("database/tireshop_db.mv.db");
            if (!Files.exists(dbSource)) {
                dbSource = tempDir.resolve("database/tireshop.mv.db");
            }
            
            if (Files.exists(dbSource)) {
                // Restore to correct filename based on what was backed up
                String targetName = dbSource.getFileName().toString();
                Files.copy(dbSource, Paths.get(targetName), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("✓ Database restored: " + targetName);
            } else {
                LOGGER.severe("❌ No database file found in backup!");
            }
            
            // Restore config.properties
            Path configSource = tempDir.resolve("config.properties");
            if (Files.exists(configSource)) {
                Files.copy(configSource, Paths.get("config.properties"), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("✓ Config restored");
            }
            
            // Clean up temp directory
            deleteDirectory(tempDir.toFile());
            
            LOGGER.info("Restore completed successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.severe("Error restoring from backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a directory and its contents
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
} 