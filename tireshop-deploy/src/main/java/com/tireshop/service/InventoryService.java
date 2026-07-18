package com.tireshop.service;

import com.tireshop.dao.ProductDao;
import com.tireshop.model.Product;
import com.tireshop.util.SettingsService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for inventory management operations
 */
public class InventoryService {
    
    private final ProductDao productDao;
    private final SettingsService settingsService;
    
    public InventoryService(ProductDao productDao, SettingsService settingsService) {
        this.productDao = productDao;
        this.settingsService = settingsService;
    }
    
    /**
     * Add a new product to inventory
     * @param product Product to add
     * @return Added product with ID
     */
    public Product addProduct(Product product) {
        return productDao.save(product);
    }
    
    /**
     * Update an existing product
     * @param product Product to update
     * @return Updated product
     */
    public Product updateProduct(Product product) {
        // Ensure price and sellingPrice are in sync
        if (product.getPrice() != null && product.getSellingPrice() == null) {
            product.setSellingPrice(product.getPrice());
        } else if (product.getSellingPrice() != null && product.getPrice() == null) {
            product.setPrice(product.getSellingPrice());
        }
        return productDao.update(product);
    }
    
    /**
     * Get a product by ID
     * @param id Product ID
     * @return Optional containing product if found
     */
    public Optional<Product> getProductById(Long id) {
        return productDao.findById(id);
    }
    
    /**
     * Get all products
     * @return List of all products
     */
    public List<Product> getAllProducts() {
        return productDao.findAll();
    }
    
    /**
     * Get products by category
     * @param category Product category
     * @return List of products in the specified category
     */
    public List<Product> getProductsByCategory(String category) {
        return productDao.findByCategory(category);
    }
    
    /**
     * Search for products
     * @param searchTerm Search term
     * @return List of matching products
     */
    public List<Product> searchProducts(String searchTerm) {
        return productDao.search(searchTerm);
    }
    
    /**
     * Find product by barcode
     * @param barcode Product barcode
     * @return First product matching the barcode or empty Optional
     */
    public Optional<Product> findProductByBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return Optional.empty();
        }
        
        // Clean up barcode - remove spaces and any non-digit characters
        barcode = barcode.replaceAll("[^0-9]", "");
        System.out.println("Looking up barcode (original): " + barcode);
        
        // First try with the exact barcode
        List<Product> products = productDao.findByBarcode(barcode);
        if (!products.isEmpty()) {
            System.out.println("Found product with exact barcode: " + barcode);
            return Optional.of(products.get(0));
        }
        
        // Try different variations of the barcode
        
        // Remove ALL leading zeros for a standardized check
        String trimmedBarcode = barcode.replaceAll("^0+", "");
        if (!trimmedBarcode.equals(barcode)) {
            System.out.println("Trying without leading zeros: " + trimmedBarcode);
            products = productDao.findByBarcode(trimmedBarcode);
            if (!products.isEmpty()) {
                System.out.println("Found product after removing all leading zeros: " + trimmedBarcode);
                return Optional.of(products.get(0));
            }
        }
        
        // Try with a single leading zero (standard UPC/EAN format is 12 digits)
        if (trimmedBarcode.length() == 11) {
            String paddedBarcode = "0" + trimmedBarcode;
            System.out.println("Trying with single leading zero: " + paddedBarcode);
            products = productDao.findByBarcode(paddedBarcode);
            if (!products.isEmpty()) {
                System.out.println("Found product with single leading zero: " + paddedBarcode);
                return Optional.of(products.get(0));
            }
        }
        
        // Also check specific barcodes we know about (like the Mastercraft one)
        if (trimmedBarcode.equals("29142738251")) {
            System.out.println("Special case: Known Mastercraft barcode. Trying specific format...");
            products = productDao.findByBarcode("029142738251");
            if (!products.isEmpty()) {
                return Optional.of(products.get(0));
            }
        }
        
        // If not found in the database, try to get from external API
        System.out.println("Product not found in database. Checking external API...");
        Product apiProduct = com.tireshop.util.TireApiService.lookupTireInfo(barcode);
        
        if (apiProduct != null) {
            System.out.println("Found product from API: " + apiProduct.getName());
            
            // Save the product to the database for future lookups
            Product savedProduct = addProduct(apiProduct);
            System.out.println("Added new product from API to database: " + savedProduct.getName());
            
            return Optional.of(savedProduct);
        }
        
        // Try variations with the API
        if (!trimmedBarcode.equals(barcode)) {
            apiProduct = com.tireshop.util.TireApiService.lookupTireInfo(trimmedBarcode);
            if (apiProduct != null) {
                System.out.println("Found product from API using trimmed barcode: " + apiProduct.getName());
                Product savedProduct = addProduct(apiProduct);
                return Optional.of(savedProduct);
            }
        }
        
        if (trimmedBarcode.length() == 11) {
            String paddedBarcode = "0" + trimmedBarcode;
            apiProduct = com.tireshop.util.TireApiService.lookupTireInfo(paddedBarcode);
            if (apiProduct != null) {
                System.out.println("Found product from API using padded barcode: " + apiProduct.getName());
                Product savedProduct = addProduct(apiProduct);
                return Optional.of(savedProduct);
            }
        }
        
        System.out.println("No product found for barcode: " + barcode + " or variations");
        return Optional.empty();
    }
    
    /**
     * Add inventory to a product
     * @param productId Product ID
     * @param quantity Quantity to add
     * @return Updated product or empty Optional if product not found
     */
    public Optional<Product> addInventory(Long productId, int quantity) {
        Optional<Product> optionalProduct = productDao.findById(productId);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setQuantityInStock(product.getQuantityInStock() + quantity);
            return Optional.of(productDao.update(product));
        }
        return Optional.empty();
    }
    
    /**
     * Remove inventory from a product
     * @param productId Product ID
     * @param quantity Quantity to remove
     * @return Updated product or empty Optional if product not found or insufficient inventory
     */
    public Optional<Product> removeInventory(Long productId, int quantity) {
        Optional<Product> optionalProduct = productDao.findById(productId);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            int newQuantity = product.getQuantityInStock() - quantity;
            if (newQuantity < 0) {
                return Optional.empty(); // Not enough inventory
            }
            product.setQuantityInStock(newQuantity);
            return Optional.of(productDao.update(product));
        }
        return Optional.empty();
    }
    
    /**
     * Adjust product price
     * @param productId Product ID
     * @param newPrice New selling price
     * @return Updated product or empty Optional if product not found
     */
    public Optional<Product> adjustPrice(Long productId, BigDecimal newPrice) {
        Optional<Product> optionalProduct = productDao.findById(productId);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setSellingPrice(newPrice);
            return Optional.of(productDao.update(product));
        }
        return Optional.empty();
    }
    
    /**
     * Get products with low inventory
     * @param threshold Inventory threshold
     * @return List of products with quantity below threshold
     */
    public List<Product> getLowInventory(int threshold) {
        return productDao.findLowInventory(threshold);
    }
    
    /**
     * Delete a product
     * @param productId Product ID
     * @return true if deleted successfully
     */
    public boolean deleteProduct(Long productId) {
        return productDao.deleteById(productId);
    }

    public List<Product> getLowStockProducts() {
        System.out.println("[InventoryService] getLowStockProducts called.");
        int globalThreshold = settingsService.getGlobalLowStockThreshold();
        System.out.println("[InventoryService] Global low stock threshold: " + globalThreshold);
        List<Product> lowStock = productDao.findAll().stream()
                .filter(p -> {
                    Integer productReorderLevel = p.getReorderLevel();
                    int effectiveThreshold = (productReorderLevel != null && productReorderLevel > 0) ? 
                                             productReorderLevel : globalThreshold;
                    boolean isLow = p.getQuantityInStock() <= effectiveThreshold;
                    if (isLow) {
                        System.out.println("[InventoryService] Low stock: " + p.getName() + " (Qty: " + p.getQuantityInStock() + ", Threshold: " + effectiveThreshold + ")");
                    }
                    return isLow;
                })
                .collect(Collectors.toList());
        System.out.println("[InventoryService] Found " + lowStock.size() + " low stock products.");
        return lowStock;
    }
} 