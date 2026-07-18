package com.tireshop.test;

import com.tireshop.service.BrowserTireScrapingService;
import com.tireshop.model.TireData;

public class ScrapingTest {
    public static void main(String[] args) {
        System.out.println("Testing Tire Scraping with Browser Automation...");
        
        BrowserTireScrapingService scrapingService = new BrowserTireScrapingService();
        
        // Test with your specific GTIN
        String testGTIN = "086699105813"; // Michelin Defender LTX M/S2
        
        System.out.println("Looking up GTIN: " + testGTIN);
        
        try {
            TireData tireData = scrapingService.lookupTireByGTIN(testGTIN);
            
            if (tireData != null) {
                System.out.println("SUCCESS! Found tire data:");
                System.out.println("Brand: " + tireData.getBrand());
                System.out.println("Name: " + tireData.getName());
                System.out.println("Size: " + tireData.getSize());
                System.out.println("Type: " + tireData.getTireType());
                System.out.println("Price: $" + tireData.getPrice());
                System.out.println("GTIN: " + tireData.getGtin());
                System.out.println("Source: " + tireData.getSource());
                System.out.println("Pattern: " + tireData.getPattern());
            } else {
                System.out.println("No tire data found for GTIN: " + testGTIN);
            }
            
        } catch (Exception e) {
            System.err.println("Error during scraping: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Test completed.");
    }
} 