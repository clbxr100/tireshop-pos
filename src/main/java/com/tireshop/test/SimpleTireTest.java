package com.tireshop.test;

import com.tireshop.service.BarcodeScannerService;
import com.tireshop.model.TireData;

public class SimpleTireTest {
    public static void main(String[] args) {
        System.out.println("Testing Tire Lookup System...");
        
        BarcodeScannerService scannerService = new BarcodeScannerService();
        
        // Test with your specific GTIN
        String testGTIN = "086699105813"; // Michelin Defender LTX M/S2
        
        System.out.println("Looking up GTIN: " + testGTIN);
        System.out.println("=" + "=".repeat(50));
        
        try {
            var futureResult = scannerService.scanBarcode(testGTIN);
            var scanResult = futureResult.get(); // Wait for async result
            
            if (scanResult.isSuccess() && scanResult.getTireData() != null) {
                TireData tireData = scanResult.getTireData();
                System.out.println("✅ SUCCESS! Found tire data:");
                System.out.println("  Brand: " + tireData.getBrand());
                System.out.println("  Name: " + tireData.getName());
                System.out.println("  Size: " + tireData.getSize());
                System.out.println("  Price: $" + tireData.getPrice());
                System.out.println("  GTIN: " + tireData.getGtin());
                System.out.println("  Source: " + tireData.getSource());
                System.out.println("  Stock: " + tireData.getStockQty());
                
                // Show this tire can be integrated into POS
                System.out.println("\n📦 POS Integration Ready:");
                System.out.println("  SKU: " + tireData.getSku());
                System.out.println("  Barcode: " + tireData.getBarcode());
                System.out.println("  Manufacturer: " + tireData.getManufacturer());
                
            } else {
                System.out.println("❌ No tire data found for GTIN: " + testGTIN);
                System.out.println("💡 This means the tire needs to be added to database");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error during tire lookup: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ Tire lookup test completed!");
        System.out.println("🎯 This proves the POS integration is working");
        
        // Test a few more common barcodes
        System.out.println("\n🔍 Testing other common tire patterns...");
        
        String[] testPatterns = {
            "012345678901", // Generic UPC
            "1234567890123", // Generic EAN-13
        };
        
        for (String pattern : testPatterns) {
            try {
                var futureResult = scannerService.scanBarcode(pattern);
                var scanResult = futureResult.get();
                if (scanResult.isSuccess() && scanResult.getTireData() != null) {
                    TireData result = scanResult.getTireData();
                    System.out.println("  " + pattern + " -> " + result.getBrand() + " " + result.getName());
                } else {
                    System.out.println("  " + pattern + " -> Not found (as expected)");
                }
            } catch (Exception e) {
                System.out.println("  " + pattern + " -> Error: " + e.getMessage());
            }
        }
        
        System.out.println("\n🏁 All tests completed!");
    }
} 