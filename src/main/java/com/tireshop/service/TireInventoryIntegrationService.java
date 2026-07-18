package com.tireshop.service;

import com.tireshop.dao.TireDataDao;
import com.tireshop.dao.ProductDao;
import com.tireshop.model.TireData;
import com.tireshop.model.Product;
import com.tireshop.util.DatabaseManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Integration service between tire scraping system and POS inventory
 * Handles importing scraped tire data into the main product inventory
 */
public class TireInventoryIntegrationService {
    
    private TireDataDao tireDataDao;
    private ProductDao productDao;
    private TireScrapingService tireScrapingService;
    private BarcodeScannerService barcodeScannerService;
    private InventoryService inventoryService;
    
    public TireInventoryIntegrationService(InventoryService inventoryService) {
        this.tireDataDao = new TireDataDao(DatabaseManager.getSessionFactory());
        this.productDao = new ProductDao(DatabaseManager.getSessionFactory());
        this.tireScrapingService = new TireScrapingService();
        this.barcodeScannerService = new BarcodeScannerService();
        this.inventoryService = inventoryService;
    }
    
    /**
     * Run tire scraping and integrate results into inventory
     */
    public IntegrationResult scrapeAndIntegrateTires() {
        System.out.println("🔄 Starting tire scraping and integration process...");
        
        try {
            // Step 1: Scrape tire data from all sources
            List<TireData> scrapedTires = tireScrapingService.scrapeAllSources();
            
            if (scrapedTires.isEmpty()) {
                return new IntegrationResult(false, "No tire data scraped", 0, 0, 0);
            }
            
            // Step 2: Save scraped data to tire database
            System.out.println("💾 Saving scraped tire data to database...");
            int savedCount = 0;
            for (TireData tireData : scrapedTires) {
                try {
                    tireDataDao.saveOrUpdateTireData(tireData);
                    savedCount++;
                } catch (Exception e) {
                    System.err.println("Error saving tire data: " + e.getMessage());
                }
            }
            
            // Step 3: Integrate with main product inventory
            System.out.println("🔗 Integrating with main product inventory...");
            int addedToInventory = 0;
            int updatedInInventory = 0;
            
            for (TireData tireData : scrapedTires) {
                try {
                    IntegrationAction action = integrateWithInventory(tireData);
                    if (action == IntegrationAction.ADDED) {
                        addedToInventory++;
                    } else if (action == IntegrationAction.UPDATED) {
                        updatedInInventory++;
                    }
                } catch (Exception e) {
                    System.err.println("Error integrating tire to inventory: " + e.getMessage());
                }
            }
            
            String message = String.format(
                "Scraped %d tires, saved %d to tire DB, added %d to inventory, updated %d in inventory",
                scrapedTires.size(), savedCount, addedToInventory, updatedInInventory
            );
            
            System.out.println("✅ " + message);
            return new IntegrationResult(true, message, scrapedTires.size(), addedToInventory, updatedInInventory);
            
        } catch (Exception e) {
            String errorMessage = "Integration failed: " + e.getMessage();
            System.err.println("❌ " + errorMessage);
            return new IntegrationResult(false, errorMessage, 0, 0, 0);
        }
    }
    
    /**
     * Integrate a single tire data entry with the main inventory
     */
    private IntegrationAction integrateWithInventory(TireData tireData) {
        // Check if product already exists by SKU or barcode
        Optional<Product> existingProduct = findExistingProduct(tireData);
        
        if (existingProduct.isPresent()) {
            // Update existing product
            Product product = existingProduct.get();
            updateProductFromTireData(product, tireData);
            inventoryService.updateProduct(product);
            return IntegrationAction.UPDATED;
        } else {
            // Create new product
            Product newProduct = convertTireDataToProduct(tireData);
            inventoryService.addProduct(newProduct);
            return IntegrationAction.ADDED;
        }
    }
    
    /**
     * Find existing product by SKU or barcode
     */
    private Optional<Product> findExistingProduct(TireData tireData) {
        // First try to find by SKU
        if (tireData.getSku() != null) {
            Optional<Product> productBySku = productDao.findBySku(tireData.getSku());
            if (productBySku.isPresent()) {
                return productBySku;
            }
        }
        
        // Then try to find by barcode
        if (tireData.getBarcode() != null) {
            Optional<Product> productByBarcode = productDao.findByBarcode(tireData.getBarcode());
            if (productByBarcode.isPresent()) {
                return productByBarcode;
            }
        }
        
        // Finally try to find by name similarity (for tires with same brand, model, size)
        String searchName = tireData.getBrand() + " " + tireData.getName() + " " + tireData.getSize();
        List<Product> similarProducts = inventoryService.searchProducts(searchName);
        
        for (Product product : similarProducts) {
            if (isSimilarTire(product, tireData)) {
                return Optional.of(product);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if a product is similar to tire data (same tire essentially)
     */
    private boolean isSimilarTire(Product product, TireData tireData) {
        if (product.getCategory() == null || !product.getCategory().toLowerCase().contains("tire")) {
            return false;
        }
        
        String productName = product.getName().toLowerCase();
        String tireBrand = tireData.getBrand() != null ? tireData.getBrand().toLowerCase() : "";
        String tireName = tireData.getName() != null ? tireData.getName().toLowerCase() : "";
        String tireSize = tireData.getSize() != null ? tireData.getSize().toLowerCase() : "";
        
        return productName.contains(tireBrand) && 
               productName.contains(tireName) && 
               productName.contains(tireSize);
    }
    
    /**
     * Convert tire data to product
     */
    private Product convertTireDataToProduct(TireData tireData) {
        Product product = new Product();
        
        // Basic product information
        product.setName(buildProductName(tireData));
        product.setSku(tireData.getSku());
        product.setBarcode(tireData.getBarcode());
        product.setCategory("Tires");
        product.setManufacturer(tireData.getBrand());
        
        // Tire-specific information
        product.setSize(tireData.getSize());
        product.setTireType(tireData.getSeason());
        
        // Pricing
        if (tireData.getPrice() != null && !tireData.getPrice().isEmpty()) {
            try {
                product.setSellingPrice(new BigDecimal(tireData.getPrice()));
            } catch (NumberFormatException e) {
                System.err.println("Invalid price format for SKU " + tireData.getSku() + ": " + tireData.getPrice());
            }
        }
        
        if (tireData.getCostPrice() != null && !tireData.getCostPrice().isEmpty()) {
            try {
                product.setPurchasePrice(new BigDecimal(tireData.getCostPrice()));
            } catch (NumberFormatException e) {
                System.err.println("Invalid cost price format for SKU " + tireData.getSku() + ": " + tireData.getCostPrice());
            }
        }
        
        // Inventory information
        if (tireData.getStockQty() != null) {
            product.setQuantityInStock(tireData.getStockQty());
        } else {
            product.setQuantityInStock(0);
        }
        
        if (tireData.getReorderPoint() != null) {
            product.setReorderLevel(tireData.getReorderPoint());
        } else {
            product.setReorderLevel(5); // Default threshold
        }
        
        // Additional information in description
        StringBuilder description = new StringBuilder();
        description.append("Tire Size: ").append(tireData.getSize()).append("\n");
        
        if (tireData.getLoadIndex() != null) {
            description.append("Load Index: ").append(tireData.getLoadIndex()).append("\n");
        }
        
        if (tireData.getSpeedRating() != null) {
            description.append("Speed Rating: ").append(tireData.getSpeedRating()).append("\n");
        }
        
        if (tireData.getSeason() != null) {
            description.append("Season: ").append(tireData.getSeason()).append("\n");
        }
        
        if (tireData.getRating() != null) {
            description.append("Rating: ").append(tireData.getRating()).append("\n");
        }
        
        if (tireData.getSource() != null) {
            description.append("Data Source: ").append(tireData.getSource()).append("\n");
        }
        
        if (tireData.getSupplierCode() != null) {
            description.append("Supplier: ").append(tireData.getSupplierCode()).append("\n");
        }
        
        description.append("Last Updated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
        
        product.setDescription(description.toString());
        
        return product;
    }
    
    /**
     * Build a product name from tire data
     */
    private String buildProductName(TireData tireData) {
        StringBuilder name = new StringBuilder();
        
        if (tireData.getBrand() != null) {
            name.append(tireData.getBrand()).append(" ");
        }
        
        if (tireData.getName() != null) {
            name.append(tireData.getName()).append(" ");
        }
        
        if (tireData.getSize() != null) {
            name.append(tireData.getSize());
        }
        
        // Add season if available
        if (tireData.getSeason() != null && !tireData.getSeason().equals("All-Season")) {
            name.append(" (").append(tireData.getSeason()).append(")");
        }
        
        return name.toString().trim();
    }
    
    /**
     * Update existing product with tire data
     */
    private void updateProductFromTireData(Product product, TireData tireData) {
        // Update pricing if newer data is available
        if (tireData.getPrice() != null && !tireData.getPrice().isEmpty()) {
            try {
                BigDecimal newPrice = new BigDecimal(tireData.getPrice());
                if (product.getSellingPrice() == null || newPrice.compareTo(product.getSellingPrice()) != 0) {
                    product.setSellingPrice(newPrice);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid price format: " + tireData.getPrice());
            }
        }
        
        // Update cost price
        if (tireData.getCostPrice() != null && !tireData.getCostPrice().isEmpty()) {
            try {
                BigDecimal newCostPrice = new BigDecimal(tireData.getCostPrice());
                if (product.getPurchasePrice() == null || newCostPrice.compareTo(product.getPurchasePrice()) != 0) {
                    product.setPurchasePrice(newCostPrice);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid cost price format: " + tireData.getCostPrice());
            }
        }
        
        // Update stock information if available
        if (tireData.getStockQty() != null && tireData.getStockQty() > 0) {
            product.setQuantityInStock(tireData.getStockQty());
        }
        
        // Update description with latest information
        String currentDescription = product.getDescription();
        if (currentDescription == null) currentDescription = "";
        
        String updateInfo = "\n[Updated from " + tireData.getSource() + " on " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) + "]";
        
        if (!currentDescription.contains(updateInfo)) {
            product.setDescription(currentDescription + updateInfo);
        }
    }
    
    /**
     * Import tire data from Python CSV output
     */
    public IntegrationResult importFromPythonCsv(String csvFilePath) {
        System.out.println("📥 Importing tire data from Python CSV: " + csvFilePath);
        
        try {
            // This would read the CSV files generated by your Python scraper
            java.io.File csvFile = new java.io.File(csvFilePath);
            if (!csvFile.exists()) {
                return new IntegrationResult(false, "CSV file not found: " + csvFilePath, 0, 0, 0);
            }
            
            // Load tire data from CSV
            List<TireData> tireDataList = loadTireDataFromCsv(csvFilePath);
            
            if (tireDataList.isEmpty()) {
                return new IntegrationResult(false, "No tire data found in CSV", 0, 0, 0);
            }
            
            // Process each tire data entry
            int addedToInventory = 0;
            int updatedInInventory = 0;
            
            for (TireData tireData : tireDataList) {
                try {
                    // Save to tire database
                    tireDataDao.saveOrUpdateTireData(tireData);
                    
                    // Integrate with main inventory
                    IntegrationAction action = integrateWithInventory(tireData);
                    if (action == IntegrationAction.ADDED) {
                        addedToInventory++;
                    } else if (action == IntegrationAction.UPDATED) {
                        updatedInInventory++;
                    }
                } catch (Exception e) {
                    System.err.println("Error processing tire data: " + e.getMessage());
                }
            }
            
            String message = String.format(
                "Imported %d tires from CSV, added %d to inventory, updated %d",
                tireDataList.size(), addedToInventory, updatedInInventory
            );
            
            System.out.println("✅ " + message);
            return new IntegrationResult(true, message, tireDataList.size(), addedToInventory, updatedInInventory);
            
        } catch (Exception e) {
            String errorMessage = "CSV import failed: " + e.getMessage();
            System.err.println("❌ " + errorMessage);
            return new IntegrationResult(false, errorMessage, 0, 0, 0);
        }
    }
    
    /**
     * Load tire data from CSV file (compatible with Python output format)
     */
    private List<TireData> loadTireDataFromCsv(String csvFilePath) {
        List<TireData> tireDataList = new java.util.ArrayList<>();
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(csvFilePath))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 16) {
                    try {
                        TireData tireData = new TireData();
                        tireData.setBrand(cleanCsvValue(parts[0]));
                        tireData.setName(cleanCsvValue(parts[1]));
                        tireData.setSize(cleanCsvValue(parts[2]));
                        tireData.setSku(cleanCsvValue(parts[3]));
                        tireData.setPrice(cleanCsvValue(parts[4]));
                        tireData.setCostPrice(cleanCsvValue(parts[5]));
                        tireData.setRating(cleanCsvValue(parts[6]));
                        tireData.setWarehouse(cleanCsvValue(parts[7]));
                        
                        if (!parts[8].trim().isEmpty()) {
                            tireData.setStockQty(Integer.parseInt(parts[8].trim()));
                        }
                        if (!parts[9].trim().isEmpty()) {
                            tireData.setAvailableQty(Integer.parseInt(parts[9].trim()));
                        }
                        if (!parts[10].trim().isEmpty()) {
                            tireData.setReorderPoint(Integer.parseInt(parts[10].trim()));
                        }
                        if (!parts[11].trim().isEmpty()) {
                            tireData.setLoadIndex(Integer.parseInt(parts[11].trim()));
                        }
                        
                        tireData.setSpeedRating(cleanCsvValue(parts[12]));
                        tireData.setSeason(cleanCsvValue(parts[13]));
                        tireData.setSupplierCode(cleanCsvValue(parts[14]));
                        
                        if (parts.length > 15) {
                            tireData.setBarcode(cleanCsvValue(parts[15]));
                        }
                        if (parts.length > 16) {
                            tireData.setSource(cleanCsvValue(parts[16]));
                        }
                        
                        tireDataList.add(tireData);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing CSV line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        
        return tireDataList;
    }
    
    /**
     * Clean CSV values (remove quotes, trim whitespace)
     */
    private String cleanCsvValue(String value) {
        if (value == null) return null;
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.isEmpty() ? null : value;
    }
    
    /**
     * Get tire data that hasn't been integrated into inventory yet
     */
    public List<TireData> getUnintegratedTireData() {
        List<TireData> allTireData = tireDataDao.findAll();
        return allTireData.stream()
            .filter(tireData -> !isIntegratedToInventory(tireData))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if tire data has been integrated into main inventory
     */
    private boolean isIntegratedToInventory(TireData tireData) {
        Optional<Product> product = findExistingProduct(tireData);
        return product.isPresent();
    }
    
    public void shutdown() {
        if (tireScrapingService != null) {
            tireScrapingService.shutdown();
        }
        if (barcodeScannerService != null) {
            barcodeScannerService.shutdown();
        }
    }
    
    // Enums and result classes
    private enum IntegrationAction {
        ADDED, UPDATED, SKIPPED
    }
    
    public static class IntegrationResult {
        private final boolean success;
        private final String message;
        private final int totalScraped;
        private final int addedToInventory;
        private final int updatedInInventory;
        
        public IntegrationResult(boolean success, String message, int totalScraped, int addedToInventory, int updatedInInventory) {
            this.success = success;
            this.message = message;
            this.totalScraped = totalScraped;
            this.addedToInventory = addedToInventory;
            this.updatedInInventory = updatedInInventory;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getTotalScraped() { return totalScraped; }
        public int getAddedToInventory() { return addedToInventory; }
        public int getUpdatedInInventory() { return updatedInInventory; }
    }
} 