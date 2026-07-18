package com.tireshop.service;

import com.tireshop.model.Product;
import com.tireshop.util.SettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for integrating with tire supplier catalogs for real-time pricing and availability
 */
public class SupplierCatalogService {
    
    private static final Logger LOGGER = Logger.getLogger(SupplierCatalogService.class.getName());
    
    private final SettingsService settingsService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Cache for supplier data with TTL
    private final Map<String, CachedSupplierData> supplierDataCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 15; // Cache data for 15 minutes
    
    // Supported suppliers
    public enum Supplier {
        NTD("National Tire Distributors", "ntd"),
        MEYER("Meyer Tire", "meyer"),
        ATD("American Tire Distributors", "atd"),
        TIRE_HUB("TireHub", "tirehub");
        
        private final String displayName;
        private final String configKey;
        
        Supplier(String displayName, String configKey) {
            this.displayName = displayName;
            this.configKey = configKey;
        }
        
        public String getDisplayName() { return displayName; }
        public String getConfigKey() { return configKey; }
    }
    
    public SupplierCatalogService(SettingsService settingsService) {
        this.settingsService = settingsService;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Check availability and pricing across all configured suppliers
     */
    public List<SupplierInventory> checkAllSuppliers(String productSku) {
        List<SupplierInventory> results = new ArrayList<>();
        
        for (Supplier supplier : Supplier.values()) {
            if (isSupplierConfigured(supplier)) {
                try {
                    SupplierInventory inventory = checkSupplierInventory(supplier, productSku);
                    if (inventory != null) {
                        results.add(inventory);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error checking " + supplier.getDisplayName() + ": " + e.getMessage());
                }
            }
        }
        
        // Sort by best price
        results.sort(Comparator.comparing(SupplierInventory::getPrice));
        return results;
    }
    
    /**
     * Check inventory for a specific supplier
     */
    public SupplierInventory checkSupplierInventory(Supplier supplier, String productSku) {
        // Check cache first
        String cacheKey = supplier.getConfigKey() + ":" + productSku;
        CachedSupplierData cached = supplierDataCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            return cached.getInventory();
        }
        
        // Make API call based on supplier
        SupplierInventory inventory = null;
        
        switch (supplier) {
            case NTD:
                inventory = checkNTDInventory(productSku);
                break;
            case MEYER:
                inventory = checkMeyerInventory(productSku);
                break;
            case ATD:
                inventory = checkATDInventory(productSku);
                break;
            case TIRE_HUB:
                inventory = checkTireHubInventory(productSku);
                break;
        }
        
        // Cache the result
        if (inventory != null) {
            supplierDataCache.put(cacheKey, new CachedSupplierData(inventory));
        }
        
        return inventory;
    }
    
    /**
     * National Tire Distributors API integration
     */
    private SupplierInventory checkNTDInventory(String productSku) {
        try {
            String apiUrl = settingsService.getSupplierApiUrl(Supplier.NTD);
            String apiKey = settingsService.getSupplierApiKey(Supplier.NTD);
            
            if (apiUrl == null || apiKey == null) {
                return null;
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/inventory/" + productSku))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                NTDResponse ntdResponse = objectMapper.readValue(response.body(), NTDResponse.class);
                return ntdResponse.toSupplierInventory(Supplier.NTD);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking NTD inventory", e);
        }
        
        return null;
    }
    
    /**
     * Meyer Tire API integration
     */
    private SupplierInventory checkMeyerInventory(String productSku) {
        // Similar implementation for Meyer Tire
        // This would be customized based on their specific API format
        return null; // Placeholder
    }
    
    /**
     * American Tire Distributors API integration
     */
    private SupplierInventory checkATDInventory(String productSku) {
        // Similar implementation for ATD
        // This would be customized based on their specific API format
        return null; // Placeholder
    }
    
    /**
     * TireHub API integration
     */
    private SupplierInventory checkTireHubInventory(String productSku) {
        // Similar implementation for TireHub
        // This would be customized based on their specific API format
        return null; // Placeholder
    }
    
    /**
     * Place an order with a supplier
     */
    public SupplierOrder placeOrder(Supplier supplier, List<OrderItem> items, String poNumber) {
        // Implementation would depend on each supplier's ordering API
        LOGGER.info("Placing order with " + supplier.getDisplayName() + " - PO: " + poNumber);
        
        // This would make the actual API call to place the order
        // For now, returning a mock response
        SupplierOrder order = new SupplierOrder();
        order.setSupplier(supplier);
        order.setPoNumber(poNumber);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(items);
        order.setStatus("SUBMITTED");
        
        return order;
    }
    
    /**
     * Check if a supplier is configured
     */
    private boolean isSupplierConfigured(Supplier supplier) {
        String apiUrl = settingsService.getSupplierApiUrl(supplier);
        String apiKey = settingsService.getSupplierApiKey(supplier);
        return apiUrl != null && !apiUrl.isEmpty() && 
               apiKey != null && !apiKey.isEmpty() && 
               !apiKey.equals("YOUR_API_KEY_HERE");
    }
    
    // Data Transfer Objects
    
    public static class SupplierInventory {
        private Supplier supplier;
        private String sku;
        private String description;
        private int quantityAvailable;
        private BigDecimal price;
        private String warehouse;
        private boolean inStock;
        private Integer estimatedDeliveryDays;
        
        // Getters and setters
        public Supplier getSupplier() { return supplier; }
        public void setSupplier(Supplier supplier) { this.supplier = supplier; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getQuantityAvailable() { return quantityAvailable; }
        public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public String getWarehouse() { return warehouse; }
        public void setWarehouse(String warehouse) { this.warehouse = warehouse; }
        
        public boolean isInStock() { return inStock; }
        public void setInStock(boolean inStock) { this.inStock = inStock; }
        
        public Integer getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
        public void setEstimatedDeliveryDays(Integer estimatedDeliveryDays) { this.estimatedDeliveryDays = estimatedDeliveryDays; }
    }
    
    public static class OrderItem {
        private String sku;
        private int quantity;
        private BigDecimal unitPrice;
        
        public OrderItem(String sku, int quantity, BigDecimal unitPrice) {
            this.sku = sku;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
        
        // Getters and setters
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
    }
    
    public static class SupplierOrder {
        private Supplier supplier;
        private String poNumber;
        private LocalDateTime orderDate;
        private List<OrderItem> items;
        private String status;
        private String confirmationNumber;
        
        // Getters and setters
        public Supplier getSupplier() { return supplier; }
        public void setSupplier(Supplier supplier) { this.supplier = supplier; }
        
        public String getPoNumber() { return poNumber; }
        public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
        
        public LocalDateTime getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
        
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getConfirmationNumber() { return confirmationNumber; }
        public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }
    }
    
    // Cache helper class
    private static class CachedSupplierData {
        private final SupplierInventory inventory;
        private final LocalDateTime cacheTime;
        
        public CachedSupplierData(SupplierInventory inventory) {
            this.inventory = inventory;
            this.cacheTime = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(cacheTime.plusMinutes(CACHE_TTL_MINUTES));
        }
        
        public SupplierInventory getInventory() {
            return inventory;
        }
    }
    
    // Response DTOs for different supplier APIs
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NTDResponse {
        public String itemNumber;
        public String description;
        public int onHand;
        public BigDecimal dealerPrice;
        public String location;
        
        public SupplierInventory toSupplierInventory(Supplier supplier) {
            SupplierInventory inv = new SupplierInventory();
            inv.setSupplier(supplier);
            inv.setSku(itemNumber);
            inv.setDescription(description);
            inv.setQuantityAvailable(onHand);
            inv.setPrice(dealerPrice);
            inv.setWarehouse(location);
            inv.setInStock(onHand > 0);
            inv.setEstimatedDeliveryDays(1); // NTD typically next day
            return inv;
        }
    }
} 