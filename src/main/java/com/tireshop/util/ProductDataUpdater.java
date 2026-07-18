package com.tireshop.util;

import com.tireshop.dao.ProductDao;
import com.tireshop.model.Product;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class to update existing products with missing information
 */
public class ProductDataUpdater {
    private static final Logger LOGGER = Logger.getLogger(ProductDataUpdater.class.getName());
    
    /**
     * Update all products that have a barcode but are missing size information
     * @param productDao The product DAO
     * @return Number of products updated
     */
    public static int updateProductsWithMissingSize(ProductDao productDao) {
        LOGGER.info("Starting batch update of products with missing size information...");
        
        List<Product> allProducts = productDao.findAll();
        int updatedCount = 0;
        int checkedCount = 0;
        
        for (Product product : allProducts) {
            // Skip if product doesn't have a barcode
            if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
                continue;
            }
            
            // Skip if product already has size information
            if (product.getSize() != null && !product.getSize().isEmpty()) {
                continue;
            }
            
            // Skip non-tire products
            if (product.getCategory() == null || 
                (!product.getCategory().equalsIgnoreCase("Tire") && 
                 !product.getCategory().equalsIgnoreCase("Tires"))) {
                continue;
            }
            
            checkedCount++;
            LOGGER.info("Checking product: " + product.getName() + " (Barcode: " + product.getBarcode() + ")");
            
            try {
                // Look up the product information from TireRack service
                Product apiProduct = TireApiService.lookupTireInfo(product.getBarcode());
                
                if (apiProduct != null) {
                    boolean updated = false;
                    
                    // Update size if missing
                    if (apiProduct.getSize() != null && !apiProduct.getSize().isEmpty()) {
                        product.setSize(apiProduct.getSize());
                        updated = true;
                        LOGGER.info("  - Updated size to: " + apiProduct.getSize());
                    }
                    
                    // Update manufacturer if missing
                    if ((product.getManufacturer() == null || product.getManufacturer().isEmpty()) 
                        && apiProduct.getManufacturer() != null) {
                        product.setManufacturer(apiProduct.getManufacturer());
                        updated = true;
                        LOGGER.info("  - Updated manufacturer to: " + apiProduct.getManufacturer());
                    }
                    
                    // Update tire-specific attributes if missing
                    if (product.getTireType() == null && apiProduct.getTireType() != null) {
                        product.setTireType(apiProduct.getTireType());
                        updated = true;
                        LOGGER.info("  - Updated tire type to: " + apiProduct.getTireType());
                    }
                    
                    if (product.getSpeedRating() == null && apiProduct.getSpeedRating() != null) {
                        product.setSpeedRating(apiProduct.getSpeedRating());
                        updated = true;
                        LOGGER.info("  - Updated speed rating to: " + apiProduct.getSpeedRating());
                    }
                    
                    if (product.getLoadRating() == null && apiProduct.getLoadRating() != null) {
                        product.setLoadRating(apiProduct.getLoadRating());
                        updated = true;
                        LOGGER.info("  - Updated load rating to: " + apiProduct.getLoadRating());
                    }
                    
                    // Update UTQG ratings if missing
                    if (product.getUtqgTreadwear() == null && apiProduct.getUtqgTreadwear() != null) {
                        product.setUtqgTreadwear(apiProduct.getUtqgTreadwear());
                        product.setUtqgTraction(apiProduct.getUtqgTraction());
                        product.setUtqgTemperature(apiProduct.getUtqgTemperature());
                        updated = true;
                        LOGGER.info("  - Updated UTQG ratings");
                    }
                    
                    // Update description if it's generic or missing
                    if ((product.getDescription() == null || 
                         product.getDescription().isEmpty() || 
                         product.getDescription().contains("Standard Model")) 
                        && apiProduct.getDescription() != null) {
                        product.setDescription(apiProduct.getDescription());
                        updated = true;
                        LOGGER.info("  - Updated description");
                    }
                    
                    // Save the updated product
                    if (updated) {
                        productDao.update(product);
                        updatedCount++;
                        LOGGER.info("  ✓ Product updated successfully");
                    } else {
                        LOGGER.info("  - No updates needed");
                    }
                } else {
                    LOGGER.info("  - No data found in TireRack service");
                }
                
                // Add a small delay to avoid overwhelming the API
                Thread.sleep(500);
                
            } catch (Exception e) {
                LOGGER.warning("  ✗ Error updating product: " + e.getMessage());
            }
        }
        
        LOGGER.info("Batch update completed. Checked " + checkedCount + " products, updated " + updatedCount);
        return updatedCount;
    }
    
    /**
     * Find product by barcode helper method
     * @param barcode The barcode to search for
     * @param productDao The product DAO
     * @return The product if found, null otherwise
     */
    public static Product findProductByBarcode(String barcode, ProductDao productDao) {
        java.util.Optional<Product> productOpt = productDao.findByBarcode(barcode);
        return productOpt.isPresent() ? productOpt.get() : null;
    }
    
    /**
     * Update a single product with missing information
     * @param product The product to update
     * @param productDao The product DAO
     * @return true if product was updated
     */
    public static boolean updateSingleProduct(Product product, ProductDao productDao) {
        if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
            return false;
        }
        
        try {
            Product apiProduct = TireApiService.lookupTireInfo(product.getBarcode());
            
            if (apiProduct != null) {
                boolean updated = false;
                
                // Copy all relevant fields from API product
                if (apiProduct.getSize() != null && (product.getSize() == null || product.getSize().isEmpty())) {
                    product.setSize(apiProduct.getSize());
                    updated = true;
                }
                
                if (apiProduct.getManufacturer() != null && (product.getManufacturer() == null || product.getManufacturer().isEmpty())) {
                    product.setManufacturer(apiProduct.getManufacturer());
                    updated = true;
                }
                
                if (apiProduct.getTireType() != null && product.getTireType() == null) {
                    product.setTireType(apiProduct.getTireType());
                    updated = true;
                }
                
                if (apiProduct.getSpeedRating() != null && product.getSpeedRating() == null) {
                    product.setSpeedRating(apiProduct.getSpeedRating());
                    updated = true;
                }
                
                if (apiProduct.getLoadRating() != null && product.getLoadRating() == null) {
                    product.setLoadRating(apiProduct.getLoadRating());
                    updated = true;
                }
                
                if (apiProduct.getUtqgTreadwear() != null && product.getUtqgTreadwear() == null) {
                    product.setUtqgTreadwear(apiProduct.getUtqgTreadwear());
                    product.setUtqgTraction(apiProduct.getUtqgTraction());
                    product.setUtqgTemperature(apiProduct.getUtqgTemperature());
                    updated = true;
                }
                
                if (updated) {
                    productDao.update(product);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error updating product " + product.getName() + ": " + e.getMessage());
        }
        
        return false;
    }
} 