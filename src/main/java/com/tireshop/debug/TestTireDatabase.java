package com.tireshop.debug;

import com.tireshop.service.ManualTireDataService;
import com.tireshop.service.BarcodeScannerService;
import com.tireshop.model.TireData;

import java.util.List;

/**
 * Test program to validate tire database functionality
 */
public class TestTireDatabase {
    
    public static void main(String[] args) {
        System.out.println("🧪 TIRE DATABASE TEST");
        System.out.println("====================");
        
        try {
            // Test 1: Initialize the tire database
            System.out.println("\n1. Initializing tire database with real tire data...");
            testDatabaseInitialization();
            
            // Test 2: Test GTIN lookup for the user's tire
            System.out.println("\n2. Testing GTIN lookup for user's Michelin tire...");
            testGTINLookup();
            
            // Test 3: Test barcode scanning integration
            System.out.println("\n3. Testing barcode scanning integration...");
            testBarcodeScanningIntegration();
            
            System.out.println("\n✅ All tests completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDatabaseInitialization() {
        try {
            ManualTireDataService manualService = new ManualTireDataService();
            manualService.initializeCommonTires();
            
            // Check what was added
            List<TireData> allTires = manualService.getAllTires();
            System.out.println("✅ Database initialized with " + allTires.size() + " tire records");
            
            // Look for the specific Michelin tire from the user's screenshot
            boolean foundMichelinTire = false;
            for (TireData tire : allTires) {
                if ("086699105813".equals(tire.getGtin())) {
                    foundMichelinTire = true;
                    System.out.println("🎯 Found user's Michelin tire:");
                    System.out.println("   Brand: " + tire.getBrand());
                    System.out.println("   Name: " + tire.getName());
                    System.out.println("   Size: " + tire.getSize());
                    System.out.println("   Price: $" + tire.getPrice());
                    System.out.println("   GTIN: " + tire.getGtin());
                    System.out.println("   Source: " + tire.getSource());
                    break;
                }
            }
            
            if (!foundMichelinTire) {
                System.err.println("❌ User's Michelin tire (GTIN: 086699105813) not found in database!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
        }
    }
    
    private static void testGTINLookup() {
        try {
            String testGTIN = "086699105813"; // User's tire GTIN
            
            ManualTireDataService manualService = new ManualTireDataService();
            List<TireData> allTires = manualService.getAllTires();
            
            TireData foundTire = null;
            for (TireData tire : allTires) {
                if (testGTIN.equals(tire.getGtin())) {
                    foundTire = tire;
                    break;
                }
            }
            
            if (foundTire != null) {
                System.out.println("✅ GTIN lookup successful for " + testGTIN);
                System.out.println("   Found: " + foundTire.getBrand() + " " + foundTire.getName());
                System.out.println("   Price: $" + foundTire.getPrice());
                System.out.println("   Stock: " + foundTire.getStockQty() + " units");
            } else {
                System.err.println("❌ GTIN lookup failed for " + testGTIN);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during GTIN lookup: " + e.getMessage());
        }
    }
    
    private static void testBarcodeScanningIntegration() {
        try {
            String testGTIN = "086699105813"; // User's tire GTIN
            
            BarcodeScannerService scannerService = new BarcodeScannerService();
            
            // Test the barcode scanning workflow
            scannerService.scanBarcode(testGTIN).thenAccept(scanResult -> {
                if (scanResult.isSuccess() && scanResult.isTireData()) {
                    TireData tire = scanResult.getTireData();
                    System.out.println("✅ Barcode scanning integration successful!");
                    System.out.println("   Scanned GTIN: " + testGTIN);
                    System.out.println("   Found: " + tire.getBrand() + " " + tire.getName());
                    System.out.println("   Source: " + scanResult.getMessage());
                } else {
                    System.err.println("❌ Barcode scanning integration failed");
                    System.err.println("   Message: " + scanResult.getMessage());
                }
            }).exceptionally(throwable -> {
                System.err.println("❌ Error in barcode scanning: " + throwable.getMessage());
                return null;
            });
            
            // Give async operation time to complete
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.err.println("❌ Error testing barcode scanning: " + e.getMessage());
        }
    }
} 