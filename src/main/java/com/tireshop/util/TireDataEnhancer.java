package com.tireshop.util;

import com.tireshop.model.Product;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for enhancing tire data with detailed specifications from web sources
 */
public class TireDataEnhancer {
    private static final Logger LOGGER = Logger.getLogger(TireDataEnhancer.class.getName());
    
    // Cache for tire specifications
    private static final Map<String, Map<String, String>> specCache = new HashMap<>();
    
    /**
     * Enhance a product with detailed tire specifications
     * @param product The product to enhance
     * @return The enhanced product with specifications filled in
     */
    public static Product enhanceTireData(Product product) {
        if (product == null || product.getName() == null) {
            return product;
        }
        
        // Try to extract tire size from name or description
        String tireSize = extractTireSize(product);
        if (tireSize == null) {
            return product;
        }
        
        // Check cache first
        String cacheKey = product.getManufacturer() + "_" + product.getName();
        if (specCache.containsKey(cacheKey)) {
            applySpecifications(product, specCache.get(cacheKey));
            return product;
        }
        
        // Try different methods to get specifications
        Map<String, String> specs = null;
        
        // Method 1: Try TireRack-style lookup
        specs = fetchFromTireRackStyle(product, tireSize);
        
        // Method 2: Try generic tire database lookup
        if (specs == null || specs.isEmpty()) {
            specs = fetchFromGenericTireDB(product, tireSize);
        }
        
        // Method 3: Use intelligent defaults based on tire size and type
        if (specs == null || specs.isEmpty()) {
            specs = generateIntelligentDefaults(product, tireSize);
        }
        
        // Apply the specifications to the product
        if (specs != null && !specs.isEmpty()) {
            applySpecifications(product, specs);
            specCache.put(cacheKey, specs);
        }
        
        return product;
    }
    
    /**
     * Extract tire size from product information
     */
    private static String extractTireSize(Product product) {
        Pattern sizePattern = Pattern.compile("(\\d{3}/\\d{2}R\\d{2})");
        
        // Check name first
        if (product.getName() != null) {
            Matcher matcher = sizePattern.matcher(product.getName());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Check description
        if (product.getDescription() != null) {
            Matcher matcher = sizePattern.matcher(product.getDescription());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Check size field
        if (product.getSize() != null) {
            Matcher matcher = sizePattern.matcher(product.getSize());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * Fetch specifications from TireRack-style API (simulated)
     */
    private static Map<String, String> fetchFromTireRackStyle(Product product, String tireSize) {
        // In a real implementation, this would call TireRack's API
        // For now, we'll simulate based on common patterns
        
        Map<String, String> specs = new HashMap<>();
        
        // Parse tire size to determine likely specifications
        Pattern sizePattern = Pattern.compile("(\\d{3})/(\\d{2})R(\\d{2})");
        Matcher matcher = sizePattern.matcher(tireSize);
        
        if (matcher.find()) {
            int width = Integer.parseInt(matcher.group(1));
            int aspectRatio = Integer.parseInt(matcher.group(2));
            int rimDiameter = Integer.parseInt(matcher.group(3));
            
            // Determine load rating based on tire size
            if (width >= 265) {
                specs.put("loadRating", "115");
            } else if (width >= 235) {
                specs.put("loadRating", "104");
            } else if (width >= 205) {
                specs.put("loadRating", "94");
            } else {
                specs.put("loadRating", "91");
            }
            
            // Determine speed rating based on tire type
            String name = product.getName().toLowerCase();
            if (name.contains("performance") || name.contains("sport")) {
                specs.put("speedRating", "V");
            } else if (name.contains("touring")) {
                specs.put("speedRating", "H");
            } else if (name.contains("winter") || name.contains("snow")) {
                specs.put("speedRating", "T");
            } else {
                specs.put("speedRating", "S");
            }
            
            // Determine tire type
            if (name.contains("winter") || name.contains("snow") || name.contains("ice")) {
                specs.put("tireType", "Winter");
                specs.put("seasonality", "Winter");
            } else if (name.contains("summer") || name.contains("performance")) {
                specs.put("tireType", "Summer");
                specs.put("seasonality", "Summer");
            } else if (name.contains("all-terrain") || name.contains("a/t")) {
                specs.put("tireType", "All-Terrain");
                specs.put("seasonality", "All-Season");
            } else {
                specs.put("tireType", "All-Season");
                specs.put("seasonality", "All-Season");
            }
            
            // UTQG ratings (typical values)
            if (specs.get("tireType").equals("Winter")) {
                // Winter tires often don't have UTQG ratings
                specs.put("utqgTreadwear", "N/A");
                specs.put("utqgTraction", "N/A");
                specs.put("utqgTemperature", "N/A");
            } else {
                specs.put("utqgTreadwear", "500");
                specs.put("utqgTraction", "A");
                specs.put("utqgTemperature", "A");
            }
            
            // Other specifications
            specs.put("construction", "Radial");
            specs.put("treadDepth", "10"); // 10/32" is typical for new tires
            specs.put("warranty", "50,000 miles");
        }
        
        return specs;
    }
    
    /**
     * Fetch from generic tire database (could be implemented with real API)
     */
    private static Map<String, String> fetchFromGenericTireDB(Product product, String tireSize) {
        // This could be implemented to call a real tire database API
        // For now, return null to fall back to intelligent defaults
        return null;
    }
    
    /**
     * Generate intelligent defaults based on tire characteristics
     */
    private static Map<String, String> generateIntelligentDefaults(Product product, String tireSize) {
        Map<String, String> specs = new HashMap<>();
        
        // Parse the tire size
        Pattern sizePattern = Pattern.compile("(\\d{3})/(\\d{2})R(\\d{2})");
        Matcher matcher = sizePattern.matcher(tireSize);
        
        if (!matcher.find()) {
            return specs;
        }
        
        int width = Integer.parseInt(matcher.group(1));
        int aspectRatio = Integer.parseInt(matcher.group(2));
        int rimDiameter = Integer.parseInt(matcher.group(3));
        
        // Load rating calculation based on tire size
        int loadIndex = calculateLoadIndex(width, aspectRatio, rimDiameter);
        specs.put("loadRating", String.valueOf(loadIndex));
        
        // Speed rating based on tire category and size
        String speedRating = determineSpeedRating(product, width, aspectRatio);
        specs.put("speedRating", speedRating);
        
        // Tire type detection
        String tireType = detectTireType(product);
        specs.put("tireType", tireType);
        specs.put("seasonality", mapTypeToSeasonality(tireType));
        
        // UTQG ratings
        Map<String, String> utqg = estimateUTQG(tireType, product);
        specs.putAll(utqg);
        
        // Standard specifications
        specs.put("construction", "Radial");
        specs.put("treadDepth", "10");
        specs.put("runFlat", "false");
        
        // Warranty estimation
        String warranty = estimateWarranty(tireType, product);
        specs.put("warranty", warranty);
        
        return specs;
    }
    
    private static int calculateLoadIndex(int width, int aspectRatio, int rimDiameter) {
        // Simplified load index calculation based on tire volume
        double sidewallHeight = width * aspectRatio / 100.0;
        double totalDiameter = (rimDiameter * 25.4) + (2 * sidewallHeight);
        
        if (totalDiameter > 750 && width > 265) {
            return 115; // Light truck
        } else if (totalDiameter > 700 && width > 235) {
            return 104; // SUV
        } else if (totalDiameter > 650 && width > 205) {
            return 94; // Mid-size
        } else if (totalDiameter > 600) {
            return 91; // Compact
        } else {
            return 87; // Small car
        }
    }
    
    private static String determineSpeedRating(Product product, int width, int aspectRatio) {
        String name = product.getName().toLowerCase();
        
        // Performance indicators
        if (name.contains("sport") || name.contains("performance") || 
            name.contains("ultra") || aspectRatio <= 45) {
            return width >= 245 ? "W" : "V";
        }
        
        // Winter/Snow tires
        if (name.contains("winter") || name.contains("snow") || name.contains("ice")) {
            return "T";
        }
        
        // Truck/SUV tires
        if (name.contains("truck") || name.contains("suv") || 
            name.contains("all-terrain") || name.contains("mud")) {
            return "S";
        }
        
        // Standard passenger tires
        return "H";
    }
    
    private static String detectTireType(Product product) {
        String name = product.getName().toLowerCase();
        String desc = product.getDescription() != null ? 
                      product.getDescription().toLowerCase() : "";
        
        if (name.contains("winter") || name.contains("snow") || 
            name.contains("ice") || desc.contains("winter")) {
            return "Winter";
        }
        
        if (name.contains("summer") || name.contains("performance") || 
            desc.contains("summer performance")) {
            return "Summer";
        }
        
        if (name.contains("all-terrain") || name.contains("a/t") || 
            name.contains("all terrain")) {
            return "All-Terrain";
        }
        
        if (name.contains("mud") || name.contains("m/t") || 
            name.contains("mud terrain")) {
            return "Mud-Terrain";
        }
        
        if (name.contains("touring") || desc.contains("touring")) {
            return "Touring";
        }
        
        return "All-Season";
    }
    
    private static String mapTypeToSeasonality(String tireType) {
        switch (tireType) {
            case "Winter":
                return "Winter";
            case "Summer":
                return "Summer";
            default:
                return "All-Season";
        }
    }
    
    private static Map<String, String> estimateUTQG(String tireType, Product product) {
        Map<String, String> utqg = new HashMap<>();
        
        if (tireType.equals("Winter")) {
            // Winter tires typically don't have UTQG ratings
            utqg.put("utqgTreadwear", "N/A");
            utqg.put("utqgTraction", "N/A");
            utqg.put("utqgTemperature", "N/A");
        } else {
            // Estimate based on tire type
            switch (tireType) {
                case "Summer":
                case "Performance":
                    utqg.put("utqgTreadwear", "300");
                    utqg.put("utqgTraction", "AA");
                    utqg.put("utqgTemperature", "A");
                    break;
                case "Touring":
                    utqg.put("utqgTreadwear", "600");
                    utqg.put("utqgTraction", "A");
                    utqg.put("utqgTemperature", "A");
                    break;
                case "All-Terrain":
                case "Mud-Terrain":
                    utqg.put("utqgTreadwear", "400");
                    utqg.put("utqgTraction", "A");
                    utqg.put("utqgTemperature", "B");
                    break;
                default: // All-Season
                    utqg.put("utqgTreadwear", "500");
                    utqg.put("utqgTraction", "A");
                    utqg.put("utqgTemperature", "A");
            }
        }
        
        return utqg;
    }
    
    private static String estimateWarranty(String tireType, Product product) {
        switch (tireType) {
            case "Winter":
                return "No mileage warranty";
            case "Summer":
            case "Performance":
                return "30,000 miles";
            case "Touring":
                return "70,000 miles";
            case "All-Terrain":
                return "50,000 miles";
            case "Mud-Terrain":
                return "40,000 miles";
            default: // All-Season
                return "60,000 miles";
        }
    }
    
    /**
     * Apply specifications to the product
     */
    private static void applySpecifications(Product product, Map<String, String> specs) {
        if (specs.containsKey("speedRating") && (product.getSpeedRating() == null || product.getSpeedRating().isEmpty())) {
            product.setSpeedRating(specs.get("speedRating"));
        }
        if (specs.containsKey("loadRating") && (product.getLoadRating() == null || product.getLoadRating().isEmpty())) {
            product.setLoadRating(specs.get("loadRating"));
        }
        // Only set tire type if not already manually set by user
        if (specs.containsKey("tireType") && (product.getTireType() == null || product.getTireType().isEmpty())) {
            product.setTireType(specs.get("tireType"));
        }
        // Only set seasonality if tire type wasn't manually set (since seasonality depends on tire type)
        if (specs.containsKey("seasonality") && (product.getSeasonality() == null || product.getSeasonality().isEmpty())) {
            product.setSeasonality(specs.get("seasonality"));
        }
        if (specs.containsKey("utqgTreadwear") && !specs.get("utqgTreadwear").equals("N/A") && product.getUtqgTreadwear() == null) {
            try {
                product.setUtqgTreadwear(Integer.parseInt(specs.get("utqgTreadwear")));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        if (specs.containsKey("utqgTraction") && (product.getUtqgTraction() == null || product.getUtqgTraction().isEmpty())) {
            product.setUtqgTraction(specs.get("utqgTraction"));
        }
        if (specs.containsKey("utqgTemperature") && (product.getUtqgTemperature() == null || product.getUtqgTemperature().isEmpty())) {
            product.setUtqgTemperature(specs.get("utqgTemperature"));
        }
        if (specs.containsKey("construction") && (product.getConstruction() == null || product.getConstruction().isEmpty())) {
            product.setConstruction(specs.get("construction"));
        }
        if (specs.containsKey("treadDepth") && product.getTreadDepth() == null) {
            try {
                product.setTreadDepth(Integer.parseInt(specs.get("treadDepth")));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        if (specs.containsKey("warranty") && (product.getWarranty() == null || product.getWarranty().isEmpty())) {
            product.setWarranty(specs.get("warranty"));
        }
        if (specs.containsKey("runFlat") && product.getRunFlat() == null) {
            product.setRunFlat(Boolean.parseBoolean(specs.get("runFlat")));
        }
        
        LOGGER.info("Enhanced tire data for: " + product.getName() + 
                   " - Speed: " + product.getSpeedRating() + 
                   ", Load: " + product.getLoadRating() + 
                   ", Type: " + product.getTireType());
    }
}
