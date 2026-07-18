package com.tireshop.service;

import com.tireshop.model.Product;
import com.tireshop.util.TireApiService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for tire-specific operations and advanced features
 */
public class TireService {
    private static final Logger LOGGER = Logger.getLogger(TireService.class.getName());
    
    private final InventoryService inventoryService;
    private final TireApiService tireApiService;
    
    // API key for tire image service (replace with actual key)
    private static final String TIRE_IMAGE_API_KEY = "your_api_key_here";
    
    public TireService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.tireApiService = new TireApiService();
    }
    
    /**
     * Get all tires sorted by a specific criteria
     * @param sortBy The field to sort by
     * @param ascending Whether to sort in ascending order
     * @return Sorted list of tires
     */
    public List<Product> getSortedTires(String sortBy, boolean ascending) {
        List<Product> tires = inventoryService.getProductsByCategory("Tire");
        
        Comparator<Product> comparator;
        switch (sortBy.toLowerCase()) {
            case "price":
                comparator = Comparator.comparing(Product::getPrice);
                break;
            case "name":
                comparator = Comparator.comparing(Product::getName);
                break;
            case "manufacturer":
                comparator = Comparator.comparing(Product::getManufacturer);
                break;
            case "size":
                comparator = Comparator.comparing(Product::getSize);
                break;
            case "treadwear":
                comparator = Comparator.comparing(p -> p.getUtqgTreadwear() != null ? p.getUtqgTreadwear() : 0);
                break;
            case "type":
                comparator = Comparator.comparing(p -> p.getTireType() != null ? p.getTireType() : "");
                break;
            case "speed_rating":
                comparator = Comparator.comparing(p -> p.getSpeedRating() != null ? p.getSpeedRating() : "");
                break;
            default:
                comparator = Comparator.comparing(Product::getName);
                break;
        }
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return tires.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    /**
     * Filter tires by various criteria
     * @param filters Map of filter criteria
     * @return Filtered list of tires
     */
    public List<Product> filterTires(Map<String, Object> filters) {
        List<Product> tires = inventoryService.getProductsByCategory("Tire");
        
        return tires.stream()
                .filter(tire -> matchesFilters(tire, filters))
                .collect(Collectors.toList());
    }
    
    private boolean matchesFilters(Product tire, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            boolean matches;
            String key = filter.getKey().toLowerCase();
            Object value = filter.getValue();
            
            switch (key) {
                case "manufacturer":
                    matches = tire.getManufacturer() != null && 
                        tire.getManufacturer().equalsIgnoreCase((String) value);
                    break;
                case "type":
                    matches = tire.getTireType() != null && 
                        tire.getTireType().equalsIgnoreCase((String) value);
                    break;
                case "size":
                    matches = tire.getSize() != null && 
                        tire.getSize().equals(value);
                    break;
                case "speed_rating":
                    matches = tire.getSpeedRating() != null && 
                        tire.getSpeedRating().equalsIgnoreCase((String) value);
                    break;
                case "min_price":
                    matches = tire.getPrice().compareTo((BigDecimal) value) >= 0;
                    break;
                case "max_price":
                    matches = tire.getPrice().compareTo((BigDecimal) value) <= 0;
                    break;
                case "in_stock":
                    matches = tire.getQuantityInStock() > 0;
                    break;
                case "run_flat":
                    matches = tire.getRunFlat() != null && 
                        tire.getRunFlat().equals(value);
                    break;
                case "seasonality":
                    matches = tire.getSeasonality() != null && 
                        tire.getSeasonality().equalsIgnoreCase((String) value);
                    break;
                default:
                    matches = true;
                    break;
            }
            
            if (!matches) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get tire image URL from external API
     * @param tire The tire product
     * @return URL to tire image or null if not found
     */
    public String getTireImageUrl(Product tire) {
        try {
            // Format the search query
            String query = String.format("%s %s %s", 
                tire.getManufacturer(),
                tire.getName(),
                tire.getSize());
            
            // Call tire image API (example implementation)
            String apiUrl = String.format("https://api.tireimages.com/search?key=%s&q=%s",
                TIRE_IMAGE_API_KEY,
                java.net.URLEncoder.encode(query, "UTF-8"));
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(response.toString());
                
                // Extract image URL from response (adjust based on actual API response format)
                if (json.containsKey("images") && json.get("images") instanceof List) {
                    List<?> images = (List<?>) json.get("images");
                    if (!images.isEmpty() && images.get(0) instanceof JSONObject) {
                        JSONObject firstImage = (JSONObject) images.get(0);
                        return (String) firstImage.get("url");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fetching tire image", e);
        }
        
        // Return a default image URL if no image found
        return "/images/default-tire.png";
    }
    
    /**
     * Compare multiple tires
     * @param tireIds List of tire IDs to compare
     * @return Map of comparison data
     */
    public Map<String, List<Object>> compareTires(List<Long> tireIds) {
        List<Product> tires = tireIds.stream()
                .map(id -> inventoryService.getProductById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        Map<String, List<Object>> comparison = new HashMap<>();
        
        // Compare basic information
        comparison.put("name", tires.stream().map(Product::getName).collect(Collectors.toList()));
        comparison.put("manufacturer", tires.stream().map(Product::getManufacturer).collect(Collectors.toList()));
        comparison.put("price", tires.stream().map(Product::getPrice).collect(Collectors.toList()));
        comparison.put("size", tires.stream().map(Product::getSize).collect(Collectors.toList()));
        
        // Compare tire-specific attributes
        comparison.put("type", tires.stream().map(Product::getTireType).collect(Collectors.toList()));
        comparison.put("speed_rating", tires.stream().map(Product::getSpeedRating).collect(Collectors.toList()));
        comparison.put("load_rating", tires.stream().map(Product::getLoadRating).collect(Collectors.toList()));
        comparison.put("tread_depth", tires.stream().map(Product::getTreadDepth).collect(Collectors.toList()));
        comparison.put("utqg_treadwear", tires.stream().map(Product::getUtqgTreadwear).collect(Collectors.toList()));
        comparison.put("utqg_traction", tires.stream().map(Product::getUtqgTraction).collect(Collectors.toList()));
        comparison.put("utqg_temperature", tires.stream().map(Product::getUtqgTemperature).collect(Collectors.toList()));
        comparison.put("run_flat", tires.stream().map(Product::getRunFlat).collect(Collectors.toList()));
        comparison.put("warranty", tires.stream().map(Product::getWarranty).collect(Collectors.toList()));
        
        return comparison;
    }
    
    /**
     * Get recommended tires based on various factors
     * @param filters Customer preferences and vehicle requirements
     * @return List of recommended tires
     */
    public List<Product> getRecommendedTires(Map<String, Object> filters) {
        // Get all tires matching basic requirements
        List<Product> matchingTires = filterTires(filters);
        
        // Sort by a composite score based on various factors
        return matchingTires.stream()
                .sorted(Comparator.comparing(tire -> calculateTireScore(tire, filters)))
                .collect(Collectors.toList());
    }
    
    private double calculateTireScore(Product tire, Map<String, Object> preferences) {
        double score = 0.0;
        
        // Base score from UTQG ratings
        if (tire.getUtqgTreadwear() != null) {
            score += tire.getUtqgTreadwear() * 0.3; // 30% weight for treadwear
        }
        
        // Add points for matching preferences
        if (preferences.containsKey("preferred_brand") && 
            tire.getManufacturer().equalsIgnoreCase((String) preferences.get("preferred_brand"))) {
            score += 100;
        }
        
        if (preferences.containsKey("preferred_type") && 
            tire.getTireType().equalsIgnoreCase((String) preferences.get("preferred_type"))) {
            score += 100;
        }
        
        // Adjust score based on price range
        if (preferences.containsKey("target_price")) {
            BigDecimal targetPrice = (BigDecimal) preferences.get("target_price");
            BigDecimal priceDiff = tire.getPrice().subtract(targetPrice).abs();
            score -= priceDiff.doubleValue() * 0.5; // Penalty for price difference
        }
        
        return score;
    }
} 