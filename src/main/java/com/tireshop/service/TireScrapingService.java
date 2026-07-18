package com.tireshop.service;

import com.tireshop.model.TireData;
import com.tireshop.util.GTINUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;
import com.opencsv.CSVWriter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java implementation of the Python tire scraping system
 * Scrapes tire data from multiple retailers and provides barcode lookup functionality
 */
public class TireScrapingService {
    
    private static final String OUTPUT_DIR = "tire_data";
    private static final int SCRAPING_DELAY_MS = 2000; // Delay between requests
    private ExecutorService executorService;
    
    public TireScrapingService() {
        this.executorService = Executors.newFixedThreadPool(3); // Limit concurrent requests
        createOutputDirectory();
    }
    
    private void createOutputDirectory() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Main method to scrape tire data from all configured sources
     */
    public List<TireData> scrapeAllSources() {
        System.out.println("🏪 COMPREHENSIVE TIRE INVENTORY SYSTEM (Java)");
        System.out.println("=" + "=".repeat(60));
        System.out.println("🎯 Scraping from multiple tire retailers...");
        
        List<TireData> allTireData = new ArrayList<>();
        Map<String, ScrapingResult> scrapingResults = new HashMap<>();
        
        // Configure real tire retailer scrapers
        Map<String, Runnable> scrapers = new HashMap<>();
        
        scrapers.put("Discount Tire", () -> {
            try {
                List<TireData> data = scrapeDiscountTire();
                synchronized (allTireData) {
                    allTireData.addAll(data);
                }
                scrapingResults.put("Discount Tire", new ScrapingResult(data.size(), true));
            } catch (Exception e) {
                scrapingResults.put("Discount Tire", new ScrapingResult(0, false, e.getMessage()));
            }
        });
        
        // TODO: Add real scrapers for other tire retailers:
        // - TireRack
        // - SimpleTire  
        // - Firestone
        // - Goodyear Direct
        // - NTB (National Tire & Battery)
        // - Walmart Auto Center
        
        // Execute scrapers in parallel
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<String, Runnable> entry : scrapers.entrySet()) {
            futures.add(CompletableFuture.runAsync(entry.getValue(), executorService));
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Print results
        System.out.println("\n📊 SCRAPING RESULTS:");
        for (Map.Entry<String, ScrapingResult> entry : scrapingResults.entrySet()) {
            ScrapingResult result = entry.getValue();
            if (result.success) {
                System.out.println("✅ " + entry.getKey() + ": " + result.tiresScraped + " tires");
            } else {
                System.out.println("❌ " + entry.getKey() + ": Failed - " + result.errorMessage);
            }
        }
        
        // Save to CSV
        saveToCsv(allTireData, OUTPUT_DIR + "/comprehensive_tire_inventory.csv");
        
        System.out.println("\n🎉 SCRAPING COMPLETE!");
        System.out.println("📦 Total tire records: " + allTireData.size());
        
        return allTireData;
    }
    

    
    /**
     * Scrape Discount Tire website for real tire data
     */
    private List<TireData> scrapeDiscountTire() {
        System.out.println("🔍 Attempting to scrape Discount Tire website...");
        List<TireData> tires = new ArrayList<>();
        
        try {
            // Set up realistic headers for web scraping
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            headers.put("Cache-Control", "no-cache");
            
            // Try to scrape popular tire category pages
            String[] categoryUrls = {
                "https://www.discounttire.com/buy-tires/michelin",
                "https://www.discounttire.com/buy-tires/goodyear", 
                "https://www.discounttire.com/buy-tires/bridgestone",
                "https://www.discounttire.com/buy-tires/continental"
            };
            
            for (String categoryUrl : categoryUrls) {
                try {
                    System.out.println("🔍 Scraping category: " + categoryUrl);
                    
                    Document doc = Jsoup.connect(categoryUrl)
                        .userAgent(headers.get("User-Agent"))
                        .timeout(15000)
                        .get();
                    
                    // Look for tire product elements (these selectors would need to be researched)
                    Elements tireElements = doc.select(".product-tile, .tire-result, .product-card");
                    
                    System.out.println("📋 Found " + tireElements.size() + " potential tire elements");
                    
                    for (Element tireElement : tireElements) {
                        TireData tire = extractTireDataFromElement(tireElement, "Discount-Tire");
                        if (tire != null) {
                            tires.add(tire);
                        }
                    }
                    
                    // Add delay between requests to be respectful
                    Thread.sleep(2000);
                    
                } catch (Exception categoryError) {
                    System.err.println("❌ Error scraping category " + categoryUrl + ": " + categoryError.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in Discount Tire scraping: " + e.getMessage());
        }
        
        System.out.println("📊 Real tire data scraped: " + tires.size() + " tires");
        return tires;
    }
    
    /**
     * Look up specific tire by GTIN from Discount Tire
     */
    public TireData lookupTireByGTIN(String gtin) {
        System.out.println("🔍 Looking up tire by GTIN: " + gtin + " on Discount Tire");
        
        try {
            // Try to search for the specific GTIN
            String searchUrl = "https://www.discounttire.com/search?q=" + gtin;
            
            Document doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .get();
            
            System.out.println("🌐 Connected to Discount Tire search for GTIN: " + gtin);
            
            // Look for tire results in common container selectors
            Elements searchResults = doc.select(".product-tile, .tire-result, .product-card, .search-result, .product-item");
            
            if (searchResults.isEmpty()) {
                // Try broader search
                searchResults = doc.select("*:contains(" + gtin + ")").parents().select("div, section, article");
            }
            
            System.out.println("📋 Found " + searchResults.size() + " search results");
            
            for (Element result : searchResults) {
                // Check if this result contains our GTIN
                String resultText = result.text().toLowerCase();
                if (resultText.contains(gtin.toLowerCase()) || 
                    result.select("*:contains(" + gtin + ")").size() > 0) {
                    
                    TireData tire = extractTireDataFromElement(result, "Discount-Tire-GTIN");
                    if (tire != null) {
                        tire.setGtin(gtin); // Ensure the GTIN is set
                        System.out.println("✅ Found tire data for GTIN " + gtin + ": " + tire.getBrand() + " " + tire.getName());
                        return tire;
                    }
                }
            }
            
            // If direct search didn't work, try looking for the GTIN in table data (common on product pages)
            Elements gtinElements = doc.select("td:contains(" + gtin + "), span:contains(" + gtin + "), div:contains(" + gtin + ")");
            if (!gtinElements.isEmpty()) {
                System.out.println("📍 Found GTIN " + gtin + " in page content, attempting to extract tire data");
                
                for (Element gtinElement : gtinElements) {
                    // Navigate up to find the product container
                    Element productContainer = gtinElement.closest("table, .product, .tire, .item, .card, .specification-table");
                    if (productContainer != null) {
                        TireData tire = extractTireDataFromElement(productContainer, "Discount-Tire-GTIN");
                        if (tire != null) {
                            tire.setGtin(gtin);
                            System.out.println("✅ Extracted tire data for GTIN " + gtin + ": " + tire.getBrand() + " " + tire.getName());
                            return tire;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error looking up GTIN " + gtin + ": " + e.getMessage());
        }
        
        System.out.println("❌ No tire data found for GTIN: " + gtin);
        return null;
    }
    
    /**
     * Extract tire data from a web element
     */
    private TireData extractTireDataFromElement(Element element, String source) {
        try {
            TireData tire = new TireData();
            tire.setSource(source);
            
            // Extract brand (look for common patterns)
            String brand = extractTextBySelectors(element, 
                ".brand, .manufacturer, [data-brand], .product-brand, .tire-brand, h1, h2, h3",
                "(?i)(michelin|goodyear|bridgestone|continental|pirelli|firestone|cooper|dunlop|yokohama|toyo|nitto|falken|hankook|kumho|general|bf goodrich|bfgoodrich)"
            );
            if (brand != null) tire.setBrand(brand);
            
            // Extract tire name/model
            String name = extractTextBySelectors(element,
                ".model, .product-name, .tire-name, .title, h1, h2, h3, [data-model], .product-title",
                null
            );
            if (name != null) {
                // Clean up the name by removing brand if it's already included
                if (brand != null && name.toLowerCase().startsWith(brand.toLowerCase())) {
                    name = name.substring(brand.length()).trim();
                }
                tire.setName(name);
            }
            
            // Extract tire size
            String size = extractTextBySelectors(element,
                ".size, .tire-size, [data-size], .product-size",
                "\\d{3}/\\d{2}[rR]\\d{2}|\\d{3}/\\d{2}-\\d{2}|P\\d{3}/\\d{2}[rR]\\d{2}"
            );
            if (size != null) tire.setSize(size);
            
            // Extract price
            String price = extractTextBySelectors(element,
                ".price, .cost, .amount, [data-price], .product-price, .tire-price",
                "\\$[\\d,]+\\.?\\d*"
            );
            if (price != null) {
                // Clean price string
                price = price.replaceAll("[^\\d.]", "");
                tire.setPrice(price);
            }
            
            // Extract GTIN if present
            String gtin = extractTextBySelectors(element,
                ".gtin, .upc, .ean, [data-gtin], [data-upc]",
                "\\d{8,14}"
            );
            if (gtin != null) tire.setGtin(gtin);
            
            // Extract other specifications
            String speedRating = extractTextBySelectors(element,
                ".speed-rating, [data-speed]",
                "(?i)(?:speed|rating|mph).*?([A-Z]{1,2})"
            );
            if (speedRating != null) tire.setSpeedRating(speedRating);
            
            // Generate SKU if we have enough data
            if (brand != null && (name != null || size != null)) {
                String sku = "DT-" + brand.substring(0, Math.min(3, brand.length())).toUpperCase() + 
                            "-" + Math.abs((brand + (name != null ? name : "") + (size != null ? size : "")).hashCode()) % 10000;
                tire.setSku(sku);
                
                // Generate barcode if not found
                if (tire.getBarcode() == null) {
                    tire.setBarcode(generateUPCBarcode());
                }
                
                System.out.println("🎯 Extracted tire: " + brand + " " + (name != null ? name : "") + " " + (size != null ? size : ""));
                return tire;
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting tire data from element: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract text using CSS selectors and optional regex pattern
     */
    private String extractTextBySelectors(Element element, String selectors, String regexPattern) {
        try {
            // Try CSS selectors first
            for (String selector : selectors.split(",")) {
                Elements found = element.select(selector.trim());
                if (!found.isEmpty()) {
                    String text = found.first().text().trim();
                    if (!text.isEmpty()) {
                        if (regexPattern != null) {
                            Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(text);
                            if (matcher.find()) {
                                return matcher.group(1) != null ? matcher.group(1).trim() : matcher.group().trim();
                            }
                        } else {
                            return text;
                        }
                    }
                }
            }
            
            // If selectors don't work, try regex on full element text
            if (regexPattern != null) {
                Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(element.text());
                if (matcher.find()) {
                    return matcher.group(1) != null ? matcher.group(1).trim() : matcher.group().trim();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting text: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Generate a realistic UPC barcode
     */
    private String generateUPCBarcode() {
        Random random = new Random();
        StringBuilder barcode = new StringBuilder();
        
        // UPC-A format: 12 digits
        for (int i = 0; i < 11; i++) {
            barcode.append(random.nextInt(10));
        }
        
        // Calculate check digit
        int checkDigit = calculateUPCCheckDigit(barcode.toString());
        barcode.append(checkDigit);
        
        return barcode.toString();
    }
    
    /**
     * Calculate UPC check digit
     */
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
     * Look up tire by barcode
     */
    public TireData lookupByBarcode(String barcode) {
        System.out.println("🔍 Looking up tire with barcode: " + barcode);
        
        // In a real implementation, this would search your database or make API calls
        // For now, we'll simulate a lookup
        List<TireData> allTires = loadFromCsv(OUTPUT_DIR + "/comprehensive_tire_inventory.csv");
        
        for (TireData tire : allTires) {
            if (barcode.equals(tire.getBarcode())) {
                System.out.println("✅ Found tire: " + tire.getBrand() + " " + tire.getName());
                return tire;
            }
        }
        
        System.out.println("❌ No tire found for barcode: " + barcode);
        return null;
    }
    
    /**
     * Save tire data to CSV
     */
    private void saveToCsv(List<TireData> tires, String filePath) {
        try (FileWriter writer = new FileWriter(filePath);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            String[] header = {
                "Brand", "Name", "Size", "SKU", "Price", "Cost_Price", "Rating", 
                "Warehouse", "Stock_Qty", "Available_Qty", "Reorder_Point", 
                "Load_Index", "Speed_Rating", "Season", "Supplier_Code", "Barcode", "GTIN", "Source"
            };
            csvWriter.writeNext(header);
            
            // Write data
            for (TireData tire : tires) {
                String[] row = {
                    tire.getBrand(),
                    tire.getName(),
                    tire.getSize(),
                    tire.getSku(),
                    tire.getPrice(),
                    tire.getCostPrice(),
                    tire.getRating(),
                    tire.getWarehouse(),
                    String.valueOf(tire.getStockQty()),
                    String.valueOf(tire.getAvailableQty()),
                    String.valueOf(tire.getReorderPoint()),
                    String.valueOf(tire.getLoadIndex()),
                    tire.getSpeedRating(),
                    tire.getSeason(),
                    tire.getSupplierCode(),
                    tire.getBarcode(),
                    tire.getGtin(),
                    tire.getSource()
                };
                csvWriter.writeNext(row);
            }
            
            System.out.println("✅ Saved " + tires.size() + " tire records to " + filePath);
            
        } catch (IOException e) {
            System.err.println("Error saving to CSV: " + e.getMessage());
        }
    }
    
    /**
     * Load tire data from CSV
     */
    public List<TireData> loadFromCsv(String filePath) {
        List<TireData> tires = new ArrayList<>();
        File file = new File(filePath);
        
        if (!file.exists()) {
            return tires;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 17) {
                    TireData tire = new TireData();
                    tire.setBrand(parts[0]);
                    tire.setName(parts[1]);
                    tire.setSize(parts[2]);
                    tire.setSku(parts[3]);
                    tire.setPrice(parts[4]);
                    tire.setCostPrice(parts[5]);
                    tire.setRating(parts[6]);
                    tire.setWarehouse(parts[7]);
                    tire.setStockQty(Integer.parseInt(parts[8]));
                    tire.setAvailableQty(Integer.parseInt(parts[9]));
                    tire.setReorderPoint(Integer.parseInt(parts[10]));
                    tire.setLoadIndex(Integer.parseInt(parts[11]));
                    tire.setSpeedRating(parts[12]);
                    tire.setSeason(parts[13]);
                    tire.setSupplierCode(parts[14]);
                    tire.setBarcode(parts[15]);
                    tire.setGtin(parts[16]);
                    tire.setSource(parts[17]);
                    tires.add(tire);
                } else if (parts.length >= 16) {
                    // Handle old format without GTIN
                    TireData tire = new TireData();
                    tire.setBrand(parts[0]);
                    tire.setName(parts[1]);
                    tire.setSize(parts[2]);
                    tire.setSku(parts[3]);
                    tire.setPrice(parts[4]);
                    tire.setCostPrice(parts[5]);
                    tire.setRating(parts[6]);
                    tire.setWarehouse(parts[7]);
                    tire.setStockQty(Integer.parseInt(parts[8]));
                    tire.setAvailableQty(Integer.parseInt(parts[9]));
                    tire.setReorderPoint(Integer.parseInt(parts[10]));
                    tire.setLoadIndex(Integer.parseInt(parts[11]));
                    tire.setSpeedRating(parts[12]);
                    tire.setSeason(parts[13]);
                    tire.setSupplierCode(parts[14]);
                    tire.setBarcode(parts[15]);
                    tire.setSource(parts[16]);
                    // Generate GTIN for old records
                    tire.setGtin(GTINUtil.generateTireGTIN());
                    tires.add(tire);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading from CSV: " + e.getMessage());
        }
        
        return tires;
    }
    
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    // Helper classes
    private static class TireBrandInfo {
        List<String> models;
        String skuPrefix;
        
        TireBrandInfo(List<String> models, String skuPrefix) {
            this.models = models;
            this.skuPrefix = skuPrefix;
        }
    }
    
    private static class ScrapingResult {
        int tiresScraped;
        boolean success;
        String errorMessage;
        
        ScrapingResult(int tiresScraped, boolean success) {
            this.tiresScraped = tiresScraped;
            this.success = success;
        }
        
        ScrapingResult(int tiresScraped, boolean success, String errorMessage) {
            this.tiresScraped = tiresScraped;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
} 