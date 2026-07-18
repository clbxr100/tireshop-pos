package com.tireshop.util;

import com.tireshop.model.Product;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing tire barcodes and extracting product information
 */
public class BarcodeParser {
    
    // Common tire manufacturer prefixes
    private static final Map<String, String> MANUFACTURER_PREFIXES = new HashMap<>();
    
    // Mastercraft tire model information for better product details
    private static final Map<String, String> MASTERCRAFT_MODELS = new HashMap<>();
    
    // Settings service instance
    private static final SettingsService settingsService = new SettingsService();
    
    static {
        MANUFACTURER_PREFIXES.put("1234", "Mastercraft");
        MANUFACTURER_PREFIXES.put("29142", "Mastercraft"); // Add new Mastercraft prefix
        MANUFACTURER_PREFIXES.put("2345", "Cooper");
        MANUFACTURER_PREFIXES.put("3456", "Goodyear");
        MANUFACTURER_PREFIXES.put("4567", "Michelin");
        MANUFACTURER_PREFIXES.put("5678", "Bridgestone");
        MANUFACTURER_PREFIXES.put("6789", "Firestone");
        MANUFACTURER_PREFIXES.put("7890", "Continental");
        MANUFACTURER_PREFIXES.put("8901", "Pirelli");
        MANUFACTURER_PREFIXES.put("9012", "Dunlop");
        // Add more as needed
    }
    
    static {
        // Mastercraft model information
        MASTERCRAFT_MODELS.put("28038", "Courser HSX 235/65R17 104T");
        MASTERCRAFT_MODELS.put("73825", "Courser AXT2 275/65R18");
        MASTERCRAFT_MODELS.put("28039", "Courser HSX 245/65R17 107T");
        MASTERCRAFT_MODELS.put("80333", "Courser CXT 265/70R17 115T");
        MASTERCRAFT_MODELS.put("90187", "Glacier MSR 225/60R17 99T");
        // Add more as needed
    }
    
    // Pattern for tire size in format 123/45R67
    private static final Pattern TIRE_SIZE_PATTERN = 
            Pattern.compile("(\\d{2,3})/(\\d{2})R(\\d{2})");
    
    /**
     * Parse a barcode and extract product information
     * 
     * @param barcode The tire barcode
     * @return A partially populated Product object, or null if the barcode cannot be parsed
     */
    public static Product parseBarcode(String barcode) {
        if (barcode == null || barcode.length() < 8) {
            return null;
        }
        
        // Clean up barcode - remove spaces and any non-digit characters
        barcode = barcode.replaceAll("[^0-9]", "");
        
        // Store original barcode for reference
        String originalBarcode = barcode;
        
        // First, try to look up product info from external APIs (if enabled)
        if (settingsService.isTireApiLookupEnabled()) {
            Product apiProduct = TireApiService.lookupTireInfo(barcode);
            if (apiProduct != null) {
                System.out.println("Found product information from API for barcode: " + barcode);
                return apiProduct;
            }

            // If API lookup fails, continue with local parsing
            System.out.println("No API data for barcode " + barcode + ", using local parsing");
            
            // Normalize UPC/EAN barcodes - some scanners add leading zeros
            if (barcode.length() == 12 && barcode.startsWith("0")) {
                // Store this as an alternate barcode to try later
                String alternateBarcode = barcode.substring(1); // Remove leading zero
                System.out.println("Barcode normalized from " + barcode + " to " + alternateBarcode + " (also checking both)");
                
                // Try the alternate barcode with API
                apiProduct = TireApiService.lookupTireInfo(alternateBarcode);
                if (apiProduct != null) {
                    System.out.println("Found product information from API for alternate barcode: " + alternateBarcode);
                    return apiProduct;
                }
            } else if (barcode.length() == 11) {
                // Add leading zero for standard UPC format
                String alternateBarcode = "0" + barcode;
                System.out.println("Adding leading zero to barcode: " + barcode + " -> " + alternateBarcode + " (checking both)");
                
                // Try the alternate barcode with API
                apiProduct = TireApiService.lookupTireInfo(alternateBarcode);
                if (apiProduct != null) {
                    System.out.println("Found product information from API for alternate barcode: " + alternateBarcode);
                    return apiProduct;
                }
            }
        } else {
            System.out.println("🚫 Tire API lookups disabled - skipping external API lookup for barcode: " + barcode);
        }
        
        // Continue with existing barcode parsing logic
        Product product = new Product();
        product.setBarcode(barcode);
        
        // Try to identify manufacturer from different parts of the barcode
        
        // Check first 5 digits for manufacturer prefix
        if (barcode.length() >= 5) {
            String prefix5 = barcode.substring(0, 5);
            if (MANUFACTURER_PREFIXES.containsKey(prefix5)) {
                product.setManufacturer(MANUFACTURER_PREFIXES.get(prefix5));
                product.setCategory("Tire");
            }
        }
        
        // Check first 4 digits if no match yet
        if (product.getManufacturer() == null && barcode.length() >= 4) {
            String prefix4 = barcode.substring(0, 4);
            if (MANUFACTURER_PREFIXES.containsKey(prefix4)) {
                product.setManufacturer(MANUFACTURER_PREFIXES.get(prefix4));
                product.setCategory("Tire");
            }
        }
        
        // If we have a 12-digit UPC with leading zero, also check with the leading zero removed
        if (product.getManufacturer() == null && barcode.length() == 12 && barcode.startsWith("0")) {
            String strippedBarcode = barcode.substring(1);
            if (strippedBarcode.length() >= 5) {
                String prefix5 = strippedBarcode.substring(0, 5);
                if (MANUFACTURER_PREFIXES.containsKey(prefix5)) {
                    product.setManufacturer(MANUFACTURER_PREFIXES.get(prefix5));
                    product.setCategory("Tire");
                    // Update barcode to the stripped version for consistency
                    product.setBarcode(strippedBarcode);
                }
            }
        }
        
        // If found a manufacturer, try to extract model number from middle part
        if (product.getManufacturer() != null && barcode.length() >= 10) {
            // For products with UPC/EAN format, use the middle digits as model number
            if (barcode.length() >= 12) {
                product.setModelNumber(barcode.substring(5, 10));
            } else {
                product.setModelNumber(barcode.substring(4, Math.min(barcode.length(), 10)));
            }
        }
        
        // Try to extract tire size from barcode or model number
        extractTireSize(barcode, product);
        
        // If we identified a manufacturer but no name/description, create a default one
        if (product.getManufacturer() != null && product.getName() == null) {
            String modelInfo = product.getModelNumber() != null ? " Model " + product.getModelNumber() : "";
            String productName = product.getManufacturer() + " Tire" + modelInfo;
            product.setName(productName);
            product.setDescription(productName + " - UPC: " + originalBarcode);
        }
        
        return product;
    }
    
    /**
     * Extract tire size information from the barcode
     */
    private static void extractTireSize(String barcode, Product product) {
        // First check if it's a known Mastercraft model
        if ("Mastercraft".equals(product.getManufacturer()) && product.getModelNumber() != null) {
            String modelInfo = MASTERCRAFT_MODELS.get(product.getModelNumber());
            if (modelInfo != null) {
                // Extract info from the model info
                String[] parts = modelInfo.split(" ");
                
                // Get the product line (Courser HSX, etc.)
                String productLine = parts.length >= 2 ? parts[0] + " " + parts[1] : "";
                
                // Get the size if available
                String size = null;
                for (String part : parts) {
                    if (part.contains("/") && part.contains("R")) {
                        size = part;
                        break;
                    }
                }
                
                // Build product name and description
                if (size != null) {
                    String name = "Mastercraft " + productLine + " " + size;
                    product.setName(name);
                    product.setSize(size);
                    
                    // Check for load rating
                    String loadRating = "";
                    for (String part : parts) {
                        if (part.matches("\\d{2,3}[A-Z]")) {
                            loadRating = " " + part;
                            break;
                        }
                    }
                    
                    product.setDescription(name + " - Premium All-Season Tire Model " + product.getModelNumber() + loadRating);
                    return;
                }
            }
        }
        
        // Fall back to regex pattern matching if we don't have specific model info
        Matcher matcher = TIRE_SIZE_PATTERN.matcher(barcode);
        if (matcher.find()) {
            String width = matcher.group(1);      // Width in millimeters
            String aspectRatio = matcher.group(2); // Aspect ratio as percentage
            String rimDiameter = matcher.group(3); // Rim diameter in inches
            
            String tireSize = width + "/" + aspectRatio + "R" + rimDiameter;
            String name = (product.getManufacturer() != null ? product.getManufacturer() + " " : "") +
                         "Tire " + tireSize;
            
            product.setName(name);
            product.setSize(tireSize);
            product.setDescription(name + " Tire - " + 
                     (product.getModelNumber() != null ? "Model " + product.getModelNumber() : "Standard Model"));
        }
    }
    
    /**
     * Enhance a product with additional information based on the barcode
     * 
     * @param product The product to enhance
     * @return The enhanced product
     */
    public static Product enhanceProductFromBarcode(Product product) {
        if (product.getBarcode() == null || product.getBarcode().isEmpty()) {
            return product;
        }
        
        // If product doesn't have a name or manufacturer yet, try to get them from the barcode
        if (product.getName() == null || product.getName().isEmpty() || 
            product.getManufacturer() == null || product.getManufacturer().isEmpty()) {
            
            Product parsedInfo = parseBarcode(product.getBarcode());
            if (parsedInfo != null) {
                // Only set values that aren't already set
                if ((product.getName() == null || product.getName().isEmpty()) && 
                     parsedInfo.getName() != null) {
                    product.setName(parsedInfo.getName());
                }
                
                if ((product.getManufacturer() == null || product.getManufacturer().isEmpty()) && 
                     parsedInfo.getManufacturer() != null) {
                    product.setManufacturer(parsedInfo.getManufacturer());
                }
                
                if ((product.getDescription() == null || product.getDescription().isEmpty()) && 
                     parsedInfo.getDescription() != null) {
                    product.setDescription(parsedInfo.getDescription());
                }
                
                if ((product.getModelNumber() == null || product.getModelNumber().isEmpty()) && 
                     parsedInfo.getModelNumber() != null) {
                    product.setModelNumber(parsedInfo.getModelNumber());
                }
                
                if ((product.getCategory() == null || product.getCategory().isEmpty()) && 
                     parsedInfo.getCategory() != null) {
                    product.setCategory(parsedInfo.getCategory());
                }
            }
        }
        
        return product;
    }
} 