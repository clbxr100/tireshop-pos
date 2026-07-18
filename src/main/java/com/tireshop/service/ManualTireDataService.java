package com.tireshop.service;

import com.tireshop.dao.TireDataDao;
import com.tireshop.model.TireData;
import com.tireshop.util.DatabaseManager;
import com.tireshop.util.GTINUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing tire data manually
 * This provides a practical solution for tire data management
 */
public class ManualTireDataService {
    
    private TireDataDao tireDataDao;
    
    public ManualTireDataService() {
        this.tireDataDao = new TireDataDao(DatabaseManager.getSessionFactory());
    }
    
    /**
     * Initialize the database with common tire data
     */
    public void initializeCommonTires() {
        System.out.println("🔧 Initializing common tire database...");
        
        List<TireData> commonTires = createCommonTireData();
        
        try {
            int added = 0;
            for (TireData tire : commonTires) {
                // Check if tire already exists (by GTIN or SKU)
                Optional<TireData> existing = tireDataDao.findByGtin(tire.getGtin());
                if (!existing.isPresent()) {
                    tireDataDao.saveOrUpdateTireData(tire);
                    added++;
                }
            }
            
            System.out.println("✅ Added " + added + " new tire records to database");
            System.out.println("📊 Total tire records: " + tireDataDao.findAll().size());
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing tire database: " + e.getMessage());
        }
    }
    
    /**
     * Add a single tire manually
     */
    public boolean addTire(String brand, String name, String size, String price, String gtin) {
        try {
            TireData tire = new TireData();
            tire.setBrand(brand);
            tire.setName(name);
            tire.setSize(size);
            tire.setPrice(price);
            tire.setGtin(gtin);
            
            // Generate SKU
            String sku = brand.substring(0, Math.min(3, brand.length())).toUpperCase() + 
                        "-" + name.replaceAll("[^A-Za-z0-9]", "").toUpperCase().substring(0, Math.min(6, name.length())) + 
                        "-" + Math.abs((brand + name + size).hashCode()) % 1000;
            tire.setSku(sku);
            
            // Generate UPC barcode if not provided
            if (tire.getBarcode() == null) {
                tire.setBarcode(generateUPCBarcode());
            }
            
            tire.setSource("Manual-Entry");
            tire.setStockQty(50); // Default stock
            tire.setAvailableQty(50);
            tire.setSeason("All-Season"); // Default
            tire.setSpeedRating("H"); // Default
            tire.setLoadIndex(95); // Default
            
            tireDataDao.saveOrUpdateTireData(tire);
            System.out.println("✅ Added tire: " + brand + " " + name + " (" + size + ")");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error adding tire: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create common tire data including the user's Michelin tire
     */
    private List<TireData> createCommonTireData() {
        List<TireData> tires = new ArrayList<>();
        
        // Add the tire from the user's screenshot with EXACT specifications
        TireData michelinDefender = new TireData();
        michelinDefender.setBrand("Michelin");
        michelinDefender.setName("Defender LTX M/S2");
        michelinDefender.setSize("255/70R16 115T XL OWL"); // Exact size from user
        michelinDefender.setPrice("242.00");
        michelinDefender.setSku("MICH-DEFEND-LTX-255-70R16");
        michelinDefender.setGtin("086699105813"); // From user's screenshot
        michelinDefender.setBarcode("086699105813");
        michelinDefender.setSource("Real-Tire-Data");
        michelinDefender.setStockQty(25);
        michelinDefender.setAvailableQty(25);
        michelinDefender.setSeason("All-Season");
        michelinDefender.setSpeedRating("T"); // From user specification
        michelinDefender.setLoadIndex(115); // From user specification
        michelinDefender.setSupplierCode("MICH-001");
        michelinDefender.setWarehouse("Main Warehouse");
        michelinDefender.setReorderPoint(10);
        tires.add(michelinDefender);
        
        // Add other real tire sizes for the same model (Defender LTX M/S2)
        addTireVariation(tires, "Michelin", "Defender LTX M/S2", "225/75R16 104T", "189.99");
        addTireVariation(tires, "Michelin", "Defender LTX M/S2", "235/70R16 106T", "199.99");
        addTireVariation(tires, "Michelin", "Defender LTX M/S2", "265/70R17 115T", "219.99");
        addTireVariation(tires, "Michelin", "Defender LTX M/S2", "275/65R18 116T", "249.99");
        
        // Add other popular Michelin tires
        addTireVariation(tires, "Michelin", "Pilot Sport 4S", "225/45R17", "299.99");
        addTireVariation(tires, "Michelin", "Pilot Sport 4S", "245/40R18", "349.99");
        addTireVariation(tires, "Michelin", "CrossClimate2", "225/60R17", "179.99");
        addTireVariation(tires, "Michelin", "Primacy MXM4", "235/50R18", "249.99");
        
        // Add popular Goodyear tires
        addTireVariation(tires, "Goodyear", "Eagle F1 Asymmetric 3", "225/45R17", "279.99");
        addTireVariation(tires, "Goodyear", "Assurance WeatherReady", "225/60R17", "169.99");
        addTireVariation(tires, "Goodyear", "Wrangler TrailRunner AT", "265/70R17", "189.99");
        
        // Add popular Bridgestone tires  
        addTireVariation(tires, "Bridgestone", "Potenza RE-71R", "225/45R17", "259.99");
        addTireVariation(tires, "Bridgestone", "Turanza QuietTrack", "225/60R17", "179.99");
        addTireVariation(tires, "Bridgestone", "Blizzak WS90", "225/60R17", "199.99");
        
        // Add Continental tires
        addTireVariation(tires, "Continental", "ExtremeContact DWS06", "225/45R17", "229.99");
        addTireVariation(tires, "Continental", "TrueContact Tour", "225/60R17", "159.99");
        
        return tires;
    }
    
    /**
     * Helper method to create tire variations
     */
    private void addTireVariation(List<TireData> tires, String brand, String name, String size, String price) {
        TireData tire = new TireData();
        tire.setBrand(brand);
        tire.setName(name);
        tire.setSize(size);
        tire.setPrice(price);
        
        // Generate unique SKU
        String sku = brand.substring(0, Math.min(3, brand.length())).toUpperCase() + 
                    "-" + name.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(8, name.length())).toUpperCase() + 
                    "-" + size.replace("/", "").replace("R", "");
        tire.setSku(sku);
        
        // Generate GTIN
        tire.setGtin(GTINUtil.generateTireGTIN());
        tire.setBarcode(generateUPCBarcode());
        
        tire.setSource("Manual-Curated");
        tire.setStockQty(15 + (int)(Math.random() * 35)); // Random stock 15-50
        tire.setAvailableQty(tire.getStockQty());
        tire.setSeason(determineSeason(name));
        tire.setSpeedRating(determineSpeedRating(name));
        tire.setLoadIndex(85 + (int)(Math.random() * 25)); // Random load index 85-110
        tire.setSupplierCode(brand.substring(0, Math.min(4, brand.length())).toUpperCase() + "-001");
        tire.setWarehouse("Main Warehouse");
        tire.setReorderPoint(5 + (int)(Math.random() * 10));
        
        tires.add(tire);
    }
    
    private String determineSeason(String tireName) {
        String lowerName = tireName.toLowerCase();
        if (lowerName.contains("winter") || lowerName.contains("blizzak") || lowerName.contains("snow")) {
            return "Winter";
        } else if (lowerName.contains("sport") || lowerName.contains("pilot") || lowerName.contains("potenza")) {
            return "Summer";
        } else {
            return "All-Season";
        }
    }
    
    private String determineSpeedRating(String tireName) {
        String lowerName = tireName.toLowerCase();
        if (lowerName.contains("sport") || lowerName.contains("pilot") || lowerName.contains("potenza")) {
            return "Y"; // High performance
        } else if (lowerName.contains("touring") || lowerName.contains("primacy")) {
            return "V"; // Touring
        } else {
            return "H"; // Standard
        }
    }
    
    /**
     * Generate a UPC barcode
     */
    private String generateUPCBarcode() {
        StringBuilder barcode = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            barcode.append((int)(Math.random() * 10));
        }
        // Calculate check digit
        int checkDigit = calculateUPCCheckDigit(barcode.toString());
        barcode.append(checkDigit);
        return barcode.toString();
    }
    
    private int calculateUPCCheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            if (i % 2 == 0) {
                sum += digit * 3;
            } else {
                sum += digit;
            }
        }
        return (10 - (sum % 10)) % 10;
    }
    
    /**
     * Get all tires in the database
     */
    public List<TireData> getAllTires() {
        try {
            return tireDataDao.findAll();
        } catch (Exception e) {
            System.err.println("Error getting all tires: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Search tires by brand
     */
    public List<TireData> getTiresByBrand(String brand) {
        try {
            return tireDataDao.findByBrand(brand);
        } catch (Exception e) {
            System.err.println("Error getting tires by brand: " + e.getMessage());
            return new ArrayList<>();
        }
    }
} 