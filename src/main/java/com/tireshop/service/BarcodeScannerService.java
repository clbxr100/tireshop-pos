package com.tireshop.service;

import com.tireshop.dao.TireDataDao;
import com.tireshop.dao.ProductDao;
import com.tireshop.model.TireData;
import com.tireshop.model.Product;
import com.tireshop.util.DatabaseManager;
import com.tireshop.util.GTINUtil;
import com.tireshop.util.SettingsService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Barcode Scanner Service integrating with tire scraping functionality
 * Provides barcode lookup with fallback to online tire data sources
 */
public class BarcodeScannerService {
    
    private TireDataDao tireDataDao;
    private ProductDao productDao;
    private TireScrapingService tireScrapingService;
    private ExecutorService executorService;
    private SettingsService settingsService;
    
    // Common barcode patterns
    private static final Pattern UPC_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern EAN_PATTERN = Pattern.compile("^\\d{13}$");
    private static final Pattern CODE_128_PATTERN = Pattern.compile("^[\\d\\w-]{8,20}$");
    
    public BarcodeScannerService() {
        this.tireDataDao = new TireDataDao(DatabaseManager.getSessionFactory());
        this.productDao = new ProductDao(DatabaseManager.getSessionFactory());
        this.tireScrapingService = new TireScrapingService();
        this.executorService = Executors.newFixedThreadPool(2);
        this.settingsService = new SettingsService();
    }
    
    /**
     * Main barcode lookup method with intelligent fallback
     */
    public CompletableFuture<ScanResult> scanBarcode(String barcode) {
        System.out.println("🔍 Scanning barcode: " + barcode);
        
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Validate barcode format
            if (!isValidBarcode(barcode)) {
                return new ScanResult(false, "Invalid barcode format", null, null);
            }
            
            // Step 2: Check local tire database first (supports both barcode and GTIN)
            Optional<TireData> tireDataOpt = tireDataDao.findByBarcodeOrGtin(barcode);
            if (tireDataOpt.isPresent()) {
                TireData tireData = tireDataOpt.get();
                System.out.println("✅ Found tire in local database: " + tireData.getBrand() + " " + tireData.getName());
                return new ScanResult(true, "Found in local tire database", tireData, null);
            }
            
            // Step 3: Check existing product inventory
            Optional<Product> productOpt = productDao.findByBarcode(barcode);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                System.out.println("✅ Found product in inventory: " + product.getName());
                return new ScanResult(true, "Found in product inventory", null, product);
            }
            
            // Step 4: Try to lookup tire information online (if it looks like a tire barcode)
            if (isPotentialTireBarcode(barcode)) {
                // Check if API/web scraping is enabled
                if (settingsService.isTireApiLookupEnabled()) {
                    System.out.println("🌐 API lookups enabled - checking online sources...");
                    TireData onlineTireData = lookupTireOnline(barcode);
                    if (onlineTireData != null) {
                        // Save to database for future lookups
                        try {
                            tireDataDao.saveOrUpdateTireData(onlineTireData);
                            System.out.println("✅ Found tire online and saved to database: " + onlineTireData.getBrand() + " " + onlineTireData.getName());
                            return new ScanResult(true, "Found online and cached", onlineTireData, null);
                        } catch (Exception e) {
                            System.err.println("Error saving online tire data: " + e.getMessage());
                            return new ScanResult(true, "Found online (not cached)", onlineTireData, null);
                        }
                    }
                } else {
                    System.out.println("🚫 API lookups disabled - skipping online tire lookup");
                }
            }
            
            // Step 5: No results found
            System.out.println("❌ No information found for barcode: " + barcode);
            return new ScanResult(false, "No information found for this barcode", null, null);
            
        }, executorService);
    }
    
    /**
     * Validate barcode/GTIN format
     */
    private boolean isValidBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }
        
        barcode = barcode.trim();
        
        // Use GTINUtil for comprehensive GTIN validation
        if (GTINUtil.isValidGTIN(barcode)) {
            return true;
        }
        
        // Check other common barcode formats
        return UPC_PATTERN.matcher(barcode).matches() ||
               EAN_PATTERN.matcher(barcode).matches() ||
               CODE_128_PATTERN.matcher(barcode).matches();
    }
    
    /**
     * Determine if barcode might be for a tire product
     */
    private boolean isPotentialTireBarcode(String barcode) {
        // Basic heuristics for tire barcodes
        // This could be enhanced with known tire manufacturer prefixes
        
        // Length-based heuristics
        if (barcode.length() < 8 || barcode.length() > 20) {
            return false;
        }
        
        // Known tire manufacturer UPC prefixes (these would need to be researched)
        String[] tireManufacturerPrefixes = {
            "00071", "00380", "00031", // Example prefixes - would need real data
            "8801", "8802", "8803"    // Example EAN prefixes
        };
        
        for (String prefix : tireManufacturerPrefixes) {
            if (barcode.startsWith(prefix)) {
                return true;
            }
        }
        
        // If no specific prefix match, assume it could be a tire
        return true;
    }
    
    /**
     * Attempt to lookup tire information online
     */
    private TireData lookupTireOnline(String barcode) {
        System.out.println("🌐 Looking up tire information online for code: " + barcode);
        
        try {
            // Step 1: Determine if it's a GTIN and get type info
            GTINUtil.GTINType gtinType = GTINUtil.getGTINType(barcode);
            if (gtinType != null) {
                System.out.println("📋 Detected " + gtinType + " format GTIN");
            }
            
            // Step 2: Try live tire scraping to find this specific barcode/GTIN
            TireData liveScrapedData = performLiveTireScraping(barcode);
            if (liveScrapedData != null) {
                System.out.println("✅ Found tire via live scraping: " + liveScrapedData.getBrand() + " " + liveScrapedData.getName());
                return liveScrapedData;
            }
            
            // Step 3: Try barcode lookup APIs (simulated for now)
            TireData apiData = queryBarcodeApis(barcode);
            if (apiData != null) {
                System.out.println("✅ Found tire via barcode API");
                return apiData;
            }
            
            // Step 4: No mock data - only return real tire information
            System.out.println("❌ No real tire data found for code: " + barcode);
            
        } catch (Exception e) {
            System.err.println("Error during online tire lookup: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Perform live tire scraping for a specific barcode/GTIN
     */
    private TireData performLiveTireScraping(String code) {
        try {
            System.out.println("🔍 Attempting live tire scraping for: " + code);
            
            // First try to search existing scraped tire data for this specific code
            TireData existingTireData = searchScrapedTireData(code);
            if (existingTireData != null) {
                System.out.println("✅ Found tire in scraped data: " + existingTireData.getBrand() + " " + existingTireData.getName());
                return existingTireData;
            }
            
            // If it looks like a GTIN, try live web scraping from Discount Tire
            if (GTINUtil.isValidGTIN(code) || code.matches("\\d{8,14}")) {
                System.out.println("🌐 Performing live GTIN lookup on Discount Tire for: " + code);
                TireData liveTireData = tireScrapingService.lookupTireByGTIN(code);
                if (liveTireData != null) {
                    // Save the found data to our database for future lookups
                    try {
                        tireDataDao.saveOrUpdateTireData(liveTireData);
                        System.out.println("💾 Saved live tire data to database");
                    } catch (Exception saveError) {
                        System.err.println("⚠️ Could not save tire data: " + saveError.getMessage());
                    }
                    return liveTireData;
                }
            }
            
            // TODO: Add more tire retailer APIs:
            // 1. TireRack API search
            // 2. SimpleTire search  
            // 3. Tire manufacturer APIs (Michelin, Goodyear, etc.)
            // 4. Automotive parts databases
            
            System.out.println("❌ No results from live tire scraping");
            return null;
            
        } catch (Exception e) {
            System.err.println("Error in live tire scraping: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Search existing scraped tire data for a specific code
     */
    private TireData searchScrapedTireData(String code) {
        try {
            // First try exact GTIN match
            Optional<TireData> tireData = tireDataDao.findByGtin(code);
            if (tireData.isPresent()) {
                return tireData.get();
            }
            
            // Then try barcode match
            tireData = tireDataDao.findByBarcode(code);
            if (tireData.isPresent()) {
                return tireData.get();
            }
            
            // Try SKU match as fallback
            Optional<TireData> skuTireData = tireDataDao.findBySku(code);
            if (skuTireData.isPresent()) {
                return skuTireData.get();
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error searching scraped tire data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Query barcode lookup APIs
     */
    private TireData queryBarcodeApis(String barcode) {
        try {
            // Simulate API delay
            Thread.sleep(300);
            
            // Here you would integrate with:
            // 1. UPC Database API (https://api.upcitemdb.com)
            // 2. Barcode Spider API
            // 3. Product lookup services
            // 4. Tire manufacturer APIs
            
            // For now, return null to demonstrate the system works
            return null;
            
        } catch (Exception e) {
            System.err.println("Error querying barcode APIs: " + e.getMessage());
            return null;
        }
    }
    

    
    /**
     * Import tire data from scraped CSV files
     */
    public CompletableFuture<ImportResult> importScrapedTireData(String csvFilePath) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("📥 Importing scraped tire data from: " + csvFilePath);
            
            try {
                // Load tire data from CSV
                java.util.List<TireData> tireDataList = tireScrapingService.loadFromCsv(csvFilePath);
                
                if (tireDataList.isEmpty()) {
                    return new ImportResult(false, "No tire data found in CSV file", 0);
                }
                
                // Save to database
                tireDataDao.saveTireDataBatch(tireDataList);
                
                System.out.println("✅ Successfully imported " + tireDataList.size() + " tire records");
                return new ImportResult(true, "Import successful", tireDataList.size());
                
            } catch (Exception e) {
                System.err.println("Error importing tire data: " + e.getMessage());
                return new ImportResult(false, "Import failed: " + e.getMessage(), 0);
            }
        }, executorService);
    }
    
    /**
     * Convert tire data to product and add to inventory
     */
    public CompletableFuture<Boolean> addTireToInventory(TireData tireData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if product already exists
                Optional<Product> existingProduct = productDao.findBySku(tireData.getSku());
                if (existingProduct.isPresent()) {
                    System.out.println("Product with SKU " + tireData.getSku() + " already exists in inventory");
                    return false;
                }
                
                // Convert tire data to product
                Product product = tireData.toProduct();
                
                // Save to product inventory
                productDao.save(product);
                
                System.out.println("✅ Added tire to inventory: " + product.getName());
                return true;
                
            } catch (Exception e) {
                System.err.println("Error adding tire to inventory: " + e.getMessage());
                return false;
            }
        }, executorService);
    }
    
    /**
     * Update tire stock from external source (supports barcode or GTIN)
     */
    public CompletableFuture<Boolean> updateTireStock(String code, int newStockQty) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<TireData> tireDataOpt = tireDataDao.findByBarcodeOrGtin(code);
                if (tireDataOpt.isPresent()) {
                    TireData tireData = tireDataOpt.get();
                    tireData.setStockQty(newStockQty);
                    tireData.setAvailableQty(newStockQty);
                    tireDataDao.update(tireData);
                    
                    // Also update corresponding product if it exists
                    Optional<Product> productOpt = productDao.findByBarcode(tireData.getBarcode());
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        product.setQuantityInStock(newStockQty);
                        productDao.update(product);
                    }
                    
                    System.out.println("✅ Updated stock for " + tireData.getBrand() + " " + tireData.getName() + " to " + newStockQty);
                    return true;
                }
                return false;
            } catch (Exception e) {
                System.err.println("Error updating tire stock: " + e.getMessage());
                return false;
            }
        }, executorService);
    }
    
    public void shutdown() {
        if (tireScrapingService != null) {
            tireScrapingService.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    // Result classes
    public static class ScanResult {
        private final boolean success;
        private final String message;
        private final TireData tireData;
        private final Product product;
        
        public ScanResult(boolean success, String message, TireData tireData, Product product) {
            this.success = success;
            this.message = message;
            this.tireData = tireData;
            this.product = product;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public TireData getTireData() { return tireData; }
        public Product getProduct() { return product; }
        
        public boolean isTireData() { return tireData != null; }
        public boolean isProduct() { return product != null; }
    }
    
    public static class ImportResult {
        private final boolean success;
        private final String message;
        private final int recordsImported;
        
        public ImportResult(boolean success, String message, int recordsImported) {
            this.success = success;
            this.message = message;
            this.recordsImported = recordsImported;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getRecordsImported() { return recordsImported; }
    }
} 