package com.tireshop.debug;

import com.tireshop.service.TireScrapingService;
import com.tireshop.model.TireData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Enhanced debug program to test tire scraping functionality
 */
public class TireScrapingDebug {
    
    public static void main(String[] args) {
        System.out.println("🔧 ENHANCED TIRE SCRAPING DEBUG TOOL");
        System.out.println("====================================");
        
        try {
            // Test 1: Examine the actual page content
            System.out.println("\n1. Examining actual page content for GTIN 086699105813...");
            examineActualPageContent();
            
            // Test 2: Try alternative search methods
            System.out.println("\n2. Testing alternative search approaches...");
            testAlternativeSearches();
            
            // Test 3: Try direct product URL if we can find it
            System.out.println("\n3. Testing direct product page access...");
            testDirectProductAccess();
            
            // Test 4: Create mock tire data for testing
            System.out.println("\n4. Creating test tire data for system validation...");
            createTestTireData();
            
        } catch (Exception e) {
            System.err.println("❌ Error during debugging: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void examineActualPageContent() {
        try {
            String searchUrl = "https://www.discounttire.com/search?q=086699105813";
            Document doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .get();
            
            System.out.println("📄 Page Analysis:");
            System.out.println("Title: " + doc.title());
            System.out.println("Total elements: " + doc.select("*").size());
            
            // Look for any script tags that might contain tire data
            Elements scripts = doc.select("script");
            System.out.println("Found " + scripts.size() + " script tags");
            
            // Look for any divs that might contain dynamic content
            Elements divs = doc.select("div");
            System.out.println("Found " + divs.size() + " div elements");
            
            // Check if there's any mention of "no results" or similar
            String pageText = doc.text().toLowerCase();
            if (pageText.contains("no results") || pageText.contains("no matches") || 
                pageText.contains("not found") || pageText.contains("0 results")) {
                System.out.println("⚠️ Page indicates no search results found");
            }
            
            // Look for any tire-related keywords
            String[] tireKeywords = {"tire", "tires", "wheel", "wheels", "michelin", "goodyear", "bridgestone"};
            for (String keyword : tireKeywords) {
                if (pageText.contains(keyword)) {
                    System.out.println("✅ Found keyword '" + keyword + "' in page content");
                }
            }
            
            // Save the page content for manual inspection
            System.out.println("\n📝 First 500 characters of page content:");
            System.out.println(pageText.substring(0, Math.min(500, pageText.length())) + "...");
            
        } catch (Exception e) {
            System.err.println("❌ Error examining page content: " + e.getMessage());
        }
    }
    
    private static void testAlternativeSearches() {
        try {
            String[] searchQueries = {
                "086699105813",           // Direct GTIN
                "Michelin Defender",      // Brand + Model
                "225/60R17",             // Size (example)
                "michelin+defender+ltx"   // URL encoded search
            };
            
            for (String query : searchQueries) {
                System.out.println("\n🔍 Testing search query: " + query);
                
                String searchUrl = "https://www.discounttire.com/search?q=" + query.replace(" ", "+");
                try {
                    Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                    
                    String pageText = doc.text().toLowerCase();
                    
                    // Check for tire-related content
                    if (pageText.contains("michelin") || pageText.contains("defender") || 
                        pageText.contains("tire") || pageText.contains("price")) {
                        System.out.println("✅ Found relevant content for: " + query);
                        
                        // Look for price indicators
                        if (pageText.contains("$") || pageText.contains("price")) {
                            System.out.println("💰 Page contains pricing information");
                        }
                    } else {
                        System.out.println("❌ No relevant tire content found for: " + query);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Error searching for '" + query + "': " + e.getMessage());
                }
                
                // Delay between requests to be respectful
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in alternative searches: " + e.getMessage());
        }
    }
    
    private static void testDirectProductAccess() {
        try {
            // Try to access the direct product URL based on the user's screenshot
            // This is from the URL pattern they might use
            String[] possibleUrls = {
                "https://www.discounttire.com/buy-tires/michelin-defender-ltx-m-s2",
                "https://www.discounttire.com/buy-tires/michelin/defender-ltx-m-s2", 
                "https://www.discounttire.com/tire/michelin-defender-ltx-m-s2"
            };
            
            for (String url : possibleUrls) {
                System.out.println("\n🔗 Testing direct URL: " + url);
                
                try {
                    Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();
                    
                    if (doc.text().toLowerCase().contains("michelin") && 
                        doc.text().toLowerCase().contains("defender")) {
                        System.out.println("✅ Found Michelin Defender page!");
                        
                        // Look for the GTIN
                        if (doc.text().contains("086699105813")) {
                            System.out.println("🎯 GTIN found on this page!");
                        }
                        
                        // Look for pricing
                        String pageText = doc.text();
                        if (pageText.contains("$")) {
                            System.out.println("💰 Pricing information available");
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ URL not accessible: " + url);
                }
                
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error testing direct URLs: " + e.getMessage());
        }
    }
    
    private static void createTestTireData() {
        try {
            System.out.println("\n🧪 Creating test tire data for system validation...");
            
            // Create a tire data object with the EXACT information from the user's screenshot
            TireData testTire = new TireData();
            testTire.setBrand("Michelin");
            testTire.setName("Defender LTX M/S2");
            testTire.setSize("255/70R16 115T XL OWL");  // Exact size from user
            testTire.setPrice("242.00");    // From the screenshot
            testTire.setSku("MICHELIN-LTX-MS2-255-70R16");
            testTire.setGtin("086699105813");  // From the screenshot
            testTire.setBarcode("086699105813");  // Use GTIN as barcode
            testTire.setSource("Real-Tire-Data");
            testTire.setSpeedRating("T");  // From user specification
            testTire.setLoadIndex(115);    // From user specification
            testTire.setSeason("All-Season");
            testTire.setStockQty(25);
            testTire.setAvailableQty(25);
            
            System.out.println("✅ Test tire data created:");
            System.out.println("Brand: " + testTire.getBrand());
            System.out.println("Name: " + testTire.getName());
            System.out.println("Size: " + testTire.getSize());
            System.out.println("Price: $" + testTire.getPrice());
            System.out.println("GTIN: " + testTire.getGtin());
            System.out.println("Source: " + testTire.getSource());
            
            System.out.println("\n💡 Recommendation:");
            System.out.println("Since live scraping faces challenges with dynamic content,");
            System.out.println("you could manually add key tire data like this to your database");
            System.out.println("and use barcode scanning to look up the stored information.");
            
        } catch (Exception e) {
            System.err.println("❌ Error creating test data: " + e.getMessage());
        }
    }
} 