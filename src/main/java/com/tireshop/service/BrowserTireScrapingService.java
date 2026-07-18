package com.tireshop.service;

import com.tireshop.model.TireData;
import com.tireshop.util.GTINUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Browser-based tire scraping service using Selenium WebDriver
 * This handles JavaScript-heavy websites like the Python version
 */
public class BrowserTireScrapingService {
    
    private WebDriver driver;
    private WebDriverWait wait;
    
    public BrowserTireScrapingService() {
        setupBrowser();
    }
    
    private void setupBrowser() {
        // Try different browsers in order of preference
        if (tryChrome()) return;
        if (tryEdge()) return;
        if (tryFirefox()) return;
        
        System.err.println("❌ No supported browser found. Please install Chrome, Edge, or Firefox.");
        this.driver = null;
    }
    
    private boolean tryChrome() {
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            this.driver = new ChromeDriver(options);
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            System.out.println("✅ Chrome browser initialized successfully");
            return true;
        } catch (Exception e) {
            System.out.println("- Chrome not available: " + e.getMessage());
            return false;
        }
    }
    
    private boolean tryEdge() {
        try {
            WebDriverManager.edgedriver().setup();
            EdgeOptions options = new EdgeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            
            this.driver = new EdgeDriver(options);
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            System.out.println("✅ Edge browser initialized successfully");
            return true;
        } catch (Exception e) {
            System.out.println("- Edge not available: " + e.getMessage());
            return false;
        }
    }
    
    private boolean tryFirefox() {
        try {
            WebDriverManager.firefoxdriver().setup();
            FirefoxOptions options = new FirefoxOptions();
            options.addArguments("--headless");
            
            this.driver = new FirefoxDriver(options);
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            System.out.println("✅ Firefox browser initialized successfully");
            return true;
        } catch (Exception e) {
            System.out.println("- Firefox not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Look up tire by GTIN using browser automation (like Python version)
     */
    public TireData lookupTireByGTIN(String gtin) {
        if (driver == null) {
            System.err.println("❌ Browser not initialized");
            return null;
        }
        
        System.out.println("🌐 Looking up GTIN " + gtin + " using browser automation...");
        
        try {
            // Try Discount Tire search
            TireData discountTireResult = searchDiscountTire(gtin);
            if (discountTireResult != null) {
                return discountTireResult;
            }
            
            // Try TireRack search
            TireData tireRackResult = searchTireRack(gtin);
            if (tireRackResult != null) {
                return tireRackResult;
            }
            
            // Try Tire Barn search
            TireData tireBarnResult = searchTireBarn(gtin);
            if (tireBarnResult != null) {
                return tireBarnResult;
            }
            
            System.out.println("❌ No tire data found for GTIN: " + gtin);
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error in browser tire lookup: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Search Discount Tire using browser automation
     */
    private TireData searchDiscountTire(String gtin) {
        try {
            System.out.println("🔍 Searching Discount Tire for: " + gtin);
            
            // Navigate to search page
            String searchUrl = "https://www.discounttire.com/search?q=" + gtin;
            driver.get(searchUrl);
            
            // Wait for page to load and JavaScript to execute
            Thread.sleep(3000);
            
            // Look for tire results using multiple selectors
            List<WebElement> tireElements = new ArrayList<>();
            
            // Try various selectors that might contain tire data
            String[] selectors = {
                "[data-testid*='tire']",
                "[data-testid*='product']", 
                ".product",
                ".tire-result",
                ".search-result",
                ".product-tile",
                "[class*='product']",
                "[class*='tire']"
            };
            
            for (String selector : selectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        tireElements.addAll(elements);
                        System.out.println("✅ Found " + elements.size() + " elements with selector: " + selector);
                        break;
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }
            
            // If no structured elements, look for GTIN in page text
            if (tireElements.isEmpty()) {
                String pageText = driver.getPageSource();
                if (pageText.contains(gtin)) {
                    System.out.println("📍 Found GTIN in page content, extracting tire data...");
                    return extractTireDataFromPageText(pageText, gtin);
                }
            } else {
                // Extract tire data from structured elements
                for (WebElement element : tireElements) {
                    TireData tire = extractTireDataFromElement(element, gtin);
                    if (tire != null) {
                        tire.setSource("Discount-Tire-Browser");
                        return tire;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error searching Discount Tire: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search TireRack using browser automation
     */
    private TireData searchTireRack(String gtin) {
        try {
            System.out.println("🔍 Searching TireRack for: " + gtin);
            
            String searchUrl = "https://www.tirerack.com/tires/TireSearchResults.jsp?searchTerm=" + gtin;
            driver.get(searchUrl);
            Thread.sleep(3000);
            
            // Look for tire data on TireRack
            String pageText = driver.getPageSource();
            if (pageText.contains(gtin) || pageText.toLowerCase().contains("michelin")) {
                return extractTireDataFromPageText(pageText, gtin);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error searching TireRack: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search Tire Barn using browser automation
     */
    private TireData searchTireBarn(String gtin) {
        try {
            System.out.println("🔍 Searching Tire Barn for: " + gtin);
            
            String searchUrl = "https://www.tirebarn.com/search/?q=" + gtin;
            driver.get(searchUrl);
            Thread.sleep(3000);
            
            String pageText = driver.getPageSource();
            if (pageText.contains(gtin)) {
                return extractTireDataFromPageText(pageText, gtin);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error searching Tire Barn: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract tire data from web element
     */
    private TireData extractTireDataFromElement(WebElement element, String gtin) {
        try {
            String elementText = element.getText();
            
            // Look for tire brand names
            String brand = extractBrand(elementText);
            if (brand == null) return null;
            
            String name = extractTireName(elementText, brand);
            String size = extractTireSize(elementText);
            String price = extractPrice(elementText);
            
            if (brand != null && (name != null || size != null)) {
                TireData tire = new TireData();
                tire.setBrand(brand);
                tire.setName(name != null ? name : "Unknown Model");
                tire.setSize(size != null ? size : "Unknown Size");
                tire.setPrice(price != null ? price : "0.00");
                tire.setGtin(gtin);
                tire.setBarcode(gtin);
                tire.setSku(generateSKU(brand, name, size));
                
                System.out.println("✅ Extracted tire: " + brand + " " + name + " " + size + " $" + price);
                return tire;
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting tire data from element: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract tire data from page text when GTIN is found
     */
    private TireData extractTireDataFromPageText(String pageText, String gtin) {
        try {
            // Look for tire information around the GTIN
            String lowerText = pageText.toLowerCase();
            
            String brand = extractBrand(pageText);
            String name = extractTireName(pageText, brand);
            String size = extractTireSize(pageText);
            String price = extractPrice(pageText);
            
            if (brand != null) {
                TireData tire = new TireData();
                tire.setBrand(brand);
                tire.setName(name != null ? name : "Unknown Model");
                tire.setSize(size != null ? size : "Unknown Size");
                tire.setPrice(price != null ? price : "0.00");
                tire.setGtin(gtin);
                tire.setBarcode(gtin);
                tire.setSku(generateSKU(brand, name, size));
                tire.setSource("Browser-Extracted");
                
                System.out.println("✅ Extracted from page text: " + brand + " " + name);
                return tire;
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting tire data from page text: " + e.getMessage());
        }
        
        return null;
    }
    
    private String extractBrand(String text) {
        String[] brands = {"Michelin", "Goodyear", "Bridgestone", "Continental", "Pirelli", 
                          "Firestone", "Cooper", "Dunlop", "Yokohama", "Toyo", "Nitto", 
                          "Falken", "Hankook", "Kumho", "General", "BFGoodrich"};
        
        for (String brand : brands) {
            if (text.toLowerCase().contains(brand.toLowerCase())) {
                return brand;
            }
        }
        return null;
    }
    
    private String extractTireName(String text, String brand) {
        if (brand == null) return null;
        
        // Look for common tire model patterns
        String[] patterns = {
            "(?i)" + brand + "\\s+([\\w\\s-+]+?)(?=\\s|$|\\d{3}/)",
            "(?i)([\\w\\s-+]+?)\\s+" + brand,
            "(?i)(defender|pilot|eagle|potenza|primacy|assurance|wrangler|blizzak|crossclimate).*?(?=\\s|$|\\d{3}/)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                if (name.length() > 2 && !name.equalsIgnoreCase(brand)) {
                    return name;
                }
            }
        }
        
        return null;
    }
    
    private String extractTireSize(String text) {
        // Look for tire size patterns
        Pattern sizePattern = Pattern.compile("\\b\\d{3}/\\d{2}[rR]\\d{2}(?:\\s+\\d{2,3}[A-Z])?(?:\\s+XL)?(?:\\s+OWL)?\\b");
        Matcher matcher = sizePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    private String extractPrice(String text) {
        Pattern pricePattern = Pattern.compile("\\$([\\d,]+\\.\\d{2})");
        Matcher matcher = pricePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace(",", "");
        }
        return null;
    }
    
    private String generateSKU(String brand, String name, String size) {
        if (brand == null) return "UNKNOWN-SKU";
        
        String brandCode = brand.substring(0, Math.min(3, brand.length())).toUpperCase();
        String nameCode = name != null ? name.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(6, name.length())).toUpperCase() : "UNK";
        String sizeCode = size != null ? size.replaceAll("[^A-Za-z0-9]", "") : "UNK";
        
        return brandCode + "-" + nameCode + "-" + Math.abs((brandCode + nameCode + sizeCode).hashCode()) % 1000;
    }
    
    /**
     * Scrape multiple tire retailers (like Python version)
     */
    public List<TireData> scrapeAllSources() {
        List<TireData> allTires = new ArrayList<>();
        
        System.out.println("🏪 BROWSER-BASED TIRE SCRAPING (Java equivalent of Python)");
        System.out.println("=" + "=".repeat(60));
        
        // Test with known GTINs
        String[] testGTINs = {"086699105813"}; // User's tire
        
        for (String gtin : testGTINs) {
            TireData tire = lookupTireByGTIN(gtin);
            if (tire != null) {
                allTires.add(tire);
            }
        }
        
        System.out.println("📊 Browser scraping complete: " + allTires.size() + " tires found");
        return allTires;
    }
    
    public void shutdown() {
        if (driver != null) {
            driver.quit();
            System.out.println("🔒 Browser automation shut down");
        }
    }
} 