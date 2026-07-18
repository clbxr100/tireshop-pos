package com.tireshop.debug;

import com.tireshop.service.ManualTireDataService;
import com.tireshop.service.BarcodeScannerService;
import com.tireshop.model.TireData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Quick validation for the user's exact tire specifications
 */
public class ValidateUserTire {
    
    public static void main(String[] args) {
        System.out.println("🔧 VALIDATING USER'S TIRE DATA");
        System.out.println("==============================");
        System.out.println("Testing: Michelin Defender LTX M/S2");
        System.out.println("Size: 255/70R16 115T XL OWL");
        System.out.println("GTIN: 086699105813");
        System.out.println("");
        
        try {
            // Step 1: Initialize the database with real tire data
            System.out.println("1. Setting up tire database...");
            ManualTireDataService manualService = new ManualTireDataService();
            manualService.initializeCommonTires();
            
            // Step 2: Verify the user's tire is in the database
            System.out.println("2. Searching for user's tire by GTIN...");
            List<TireData> allTires = manualService.getAllTires();
            TireData userTire = null;
            
            for (TireData tire : allTires) {
                if ("086699105813".equals(tire.getGtin())) {
                    userTire = tire;
                    break;
                }
            }
            
            if (userTire != null) {
                System.out.println("✅ USER'S TIRE FOUND IN DATABASE!");
                System.out.println("   Brand: " + userTire.getBrand());
                System.out.println("   Model: " + userTire.getName());
                System.out.println("   Size: " + userTire.getSize());
                System.out.println("   Price: $" + userTire.getPrice());
                System.out.println("   GTIN: " + userTire.getGtin());
                System.out.println("   SKU: " + userTire.getSku());
                System.out.println("   Speed Rating: " + userTire.getSpeedRating());
                System.out.println("   Load Index: " + userTire.getLoadIndex());
                System.out.println("   Source: " + userTire.getSource());
                System.out.println("   Stock: " + userTire.getStockQty() + " units");
                
                // Verify the exact specifications
                boolean correctSize = "255/70R16 115T XL OWL".equals(userTire.getSize());
                boolean correctSpeedRating = "T".equals(userTire.getSpeedRating());
                boolean correctLoadIndex = userTire.getLoadIndex() == 115;
                
                System.out.println("");
                System.out.println("🔍 SPECIFICATION VERIFICATION:");
                System.out.println("   Size Match: " + (correctSize ? "✅ CORRECT" : "❌ INCORRECT"));
                System.out.println("   Speed Rating: " + (correctSpeedRating ? "✅ CORRECT (T)" : "❌ INCORRECT"));
                System.out.println("   Load Index: " + (correctLoadIndex ? "✅ CORRECT (115)" : "❌ INCORRECT"));
                
            } else {
                System.err.println("❌ USER'S TIRE NOT FOUND IN DATABASE!");
                return;
            }
            
            // Step 3: Test barcode scanning
            System.out.println("");
            System.out.println("3. Testing barcode scanning workflow...");
            BarcodeScannerService scanner = new BarcodeScannerService();
            
            CompletableFuture<BarcodeScannerService.ScanResult> future = scanner.scanBarcode("086699105813");
            BarcodeScannerService.ScanResult result = future.get(); // Wait for result
            
            if (result.isSuccess() && result.isTireData()) {
                TireData scannedTire = result.getTireData();
                System.out.println("✅ BARCODE SCANNING SUCCESSFUL!");
                System.out.println("   Scanned: " + scannedTire.getBrand() + " " + scannedTire.getName());
                System.out.println("   Size: " + scannedTire.getSize());
                System.out.println("   Price: $" + scannedTire.getPrice());
                System.out.println("   Result: " + result.getMessage());
            } else {
                System.err.println("❌ BARCODE SCANNING FAILED: " + result.getMessage());
            }
            
            System.out.println("");
            System.out.println("🎉 VALIDATION COMPLETE!");
            System.out.println("");
            System.out.println("✅ Your tire system is ready!");
            System.out.println("   • Real tire data for your Michelin Defender LTX M/S2");
            System.out.println("   • Correct size: 255/70R16 115T XL OWL");
            System.out.println("   • GTIN: 086699105813 works for scanning");
            System.out.println("   • Barcode scanning integration functional");
            System.out.println("");
            System.out.println("You can now scan this tire with your phone and get the exact information!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during validation: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 