package com.tireshop.util;

import com.tireshop.dao.ProductDao;
import com.tireshop.model.Product;
import java.util.List;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Standalone batch update tool for updating product information
 */
public class BatchUpdateTool {
    private static final Logger LOGGER = Logger.getLogger(BatchUpdateTool.class.getName());
    
    static {
        // Configure console logging
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(consoleHandler);
        rootLogger.setUseParentHandlers(false);
    }
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   Tire Shop Batch Update Tool");
        System.out.println("===========================================");
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        ProductDao productDao = new ProductDao(DatabaseManager.getSessionFactory());
        
        while (true) {
            System.out.println("Select an option:");
            System.out.println("1. Update all products with missing size information");
            System.out.println("2. Update a specific product by barcode");
            System.out.println("3. Show products with missing information");
            System.out.println("4. Test TireRack service connection");
            System.out.println("5. Exit");
            System.out.print("\nEnter your choice (1-5): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    updateAllProducts(productDao);
                    break;
                    
                case "2":
                    updateSpecificProduct(scanner, productDao);
                    break;
                    
                case "3":
                    showProductsWithMissingInfo(productDao);
                    break;
                    
                case "4":
                    testTireRackService(scanner);
                    break;
                    
                case "5":
                    System.out.println("\nExiting batch update tool...");
                    scanner.close();
                    System.exit(0);
                    break;
                    
                default:
                    System.out.println("\nInvalid choice. Please try again.\n");
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            System.out.println();
        }
    }
    
    private static void updateAllProducts(ProductDao productDao) {
        System.out.println("\n--- Updating All Products ---");
        System.out.println("This will check all tire products with barcodes and update missing information.");
        System.out.println("This may take several minutes depending on the number of products.");
        System.out.print("\nProceed? (y/n): ");
        
        Scanner scanner = new Scanner(System.in);
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (confirm.equals("y") || confirm.equals("yes")) {
            System.out.println("\nStarting batch update...\n");
            
            int updatedCount = ProductDataUpdater.updateProductsWithMissingSize(productDao);
            
            System.out.println("\n✓ Batch update completed!");
            System.out.println("  Total products updated: " + updatedCount);
        } else {
            System.out.println("\nBatch update cancelled.");
        }
    }
    
    private static void updateSpecificProduct(Scanner scanner, ProductDao productDao) {
        System.out.println("\n--- Update Specific Product ---");
        System.out.print("Enter product barcode: ");
        String barcode = scanner.nextLine().trim();
        
        if (barcode.isEmpty()) {
            System.out.println("Barcode cannot be empty.");
            return;
        }
        
        java.util.Optional<Product> productOpt = productDao.findByBarcode(barcode);
        
        if (!productOpt.isPresent()) {
            System.out.println("\n✗ Product not found with barcode: " + barcode);
            return;
        }
        
        Product product = productOpt.get();
        
        System.out.println("\nFound product: " + product.getName());
        System.out.println("Current information:");
        System.out.println("  - Manufacturer: " + (product.getManufacturer() != null ? product.getManufacturer() : "Not set"));
        System.out.println("  - Size: " + (product.getSize() != null ? product.getSize() : "Not set"));
        System.out.println("  - Tire Type: " + (product.getTireType() != null ? product.getTireType() : "Not set"));
        System.out.println("  - Speed Rating: " + (product.getSpeedRating() != null ? product.getSpeedRating() : "Not set"));
        System.out.println("  - Load Rating: " + (product.getLoadRating() != null ? product.getLoadRating() : "Not set"));
        
        System.out.print("\nUpdate this product? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (confirm.equals("y") || confirm.equals("yes")) {
            System.out.println("\nUpdating product...");
            
            boolean updated = ProductDataUpdater.updateSingleProduct(product, productDao);
            
            if (updated) {
                System.out.println("\n✓ Product updated successfully!");
                
                // Reload and show updated information
                productOpt = productDao.findByBarcode(barcode);
                if (productOpt.isPresent()) {
                    product = productOpt.get();
                }
                System.out.println("\nUpdated information:");
                System.out.println("  - Manufacturer: " + (product.getManufacturer() != null ? product.getManufacturer() : "Not set"));
                System.out.println("  - Size: " + (product.getSize() != null ? product.getSize() : "Not set"));
                System.out.println("  - Tire Type: " + (product.getTireType() != null ? product.getTireType() : "Not set"));
                System.out.println("  - Speed Rating: " + (product.getSpeedRating() != null ? product.getSpeedRating() : "Not set"));
                System.out.println("  - Load Rating: " + (product.getLoadRating() != null ? product.getLoadRating() : "Not set"));
            } else {
                System.out.println("\n✗ No updates were made. Product may already have complete information or API lookup failed.");
            }
        } else {
            System.out.println("\nUpdate cancelled.");
        }
    }
    
    private static void showProductsWithMissingInfo(ProductDao productDao) {
        System.out.println("\n--- Products with Missing Information ---");
        
        int count = 0;
        for (Product product : productDao.findAll()) {
            // Check if it's a tire product
            if (product.getCategory() == null || 
                (!product.getCategory().equalsIgnoreCase("Tire") && 
                 !product.getCategory().equalsIgnoreCase("Tires"))) {
                continue;
            }
            
            // Check for missing information
            boolean hasMissingInfo = false;
            StringBuilder missing = new StringBuilder();
            
            if (product.getSize() == null || product.getSize().isEmpty()) {
                missing.append("size, ");
                hasMissingInfo = true;
            }
            
            if (product.getManufacturer() == null || product.getManufacturer().isEmpty()) {
                missing.append("manufacturer, ");
                hasMissingInfo = true;
            }
            
            if (product.getTireType() == null) {
                missing.append("tire type, ");
                hasMissingInfo = true;
            }
            
            if (product.getSpeedRating() == null) {
                missing.append("speed rating, ");
                hasMissingInfo = true;
            }
            
            if (product.getLoadRating() == null) {
                missing.append("load rating, ");
                hasMissingInfo = true;
            }
            
            if (hasMissingInfo) {
                count++;
                String missingStr = missing.toString();
                if (missingStr.endsWith(", ")) {
                    missingStr = missingStr.substring(0, missingStr.length() - 2);
                }
                
                System.out.println("\n" + count + ". " + product.getName());
                System.out.println("   Barcode: " + (product.getBarcode() != null ? product.getBarcode() : "No barcode"));
                System.out.println("   Missing: " + missingStr);
            }
        }
        
        if (count == 0) {
            System.out.println("\n✓ All tire products have complete information!");
        } else {
            System.out.println("\n\nTotal products with missing information: " + count);
        }
    }
    
    private static void testTireRackService(Scanner scanner) {
        System.out.println("\n--- Test TireRack Service ---");
        System.out.print("Enter a barcode to test: ");
        String barcode = scanner.nextLine().trim();
        
        if (barcode.isEmpty()) {
            System.out.println("Barcode cannot be empty.");
            return;
        }
        
        System.out.println("\nTesting TireRack service with barcode: " + barcode);
        System.out.println("Please wait...\n");
        
        try {
            Product result = TireApiService.lookupTireInfo(barcode);
            
            if (result != null) {
                System.out.println("✓ TireRack service returned data:");
                System.out.println("  - Name: " + (result.getName() != null ? result.getName() : "Not provided"));
                System.out.println("  - Manufacturer: " + (result.getManufacturer() != null ? result.getManufacturer() : "Not provided"));
                System.out.println("  - Size: " + (result.getSize() != null ? result.getSize() : "Not provided"));
                System.out.println("  - Tire Type: " + (result.getTireType() != null ? result.getTireType() : "Not provided"));
                System.out.println("  - Speed Rating: " + (result.getSpeedRating() != null ? result.getSpeedRating() : "Not provided"));
                System.out.println("  - Load Rating: " + (result.getLoadRating() != null ? result.getLoadRating() : "Not provided"));
                System.out.println("  - Description: " + (result.getDescription() != null ? result.getDescription() : "Not provided"));
            } else {
                System.out.println("✗ No data returned from TireRack service.");
                System.out.println("  This could mean:");
                System.out.println("  - The barcode is not recognized");
                System.out.println("  - The TireRack service is not running");
                System.out.println("  - There was a network error");
            }
        } catch (Exception e) {
            System.out.println("✗ Error testing TireRack service: " + e.getMessage());
            System.out.println("  Make sure the TireRack service is running on port 3001.");
        }
    }
} 