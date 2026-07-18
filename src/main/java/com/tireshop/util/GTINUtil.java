package com.tireshop.util;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * Utility class for GTIN (Global Trade Item Number) generation and validation
 * Supports UPC-A (12 digits), EAN-13 (13 digits), UPC-E (8 digits), and EAN-8 (8 digits)
 */
public class GTINUtil {
    
    // GTIN format patterns
    private static final Pattern UPC_A_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern EAN_13_PATTERN = Pattern.compile("^\\d{13}$");
    private static final Pattern UPC_E_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern EAN_8_PATTERN = Pattern.compile("^\\d{8}$");
    
    // Country codes for EAN-13 generation
    private static final String[] COUNTRY_CODES = {
        "000", "001", "002", "003", "004", "005", "006", "007", "008", "009", // USA/Canada
        "380", "383", // Bulgaria
        "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", // Germany
        "690", "691", "692", "693", "694", "695", // China
        "780", "781", "782", "783", "784", "785", "786", "787", "788", "789"  // Italy
    };
    
    public enum GTINType {
        UPC_A(12),
        EAN_13(13),
        UPC_E(8),
        EAN_8(8);
        
        private final int length;
        
        GTINType(int length) {
            this.length = length;
        }
        
        public int getLength() {
            return length;
        }
    }
    
    /**
     * Generate a random valid GTIN of the specified type
     */
    public static String generateGTIN(GTINType type) {
        Random random = new Random();
        
        switch (type) {
            case UPC_A:
                return generateUPCA();
            case EAN_13:
                return generateEAN13();
            case UPC_E:
                return generateUPCE();
            case EAN_8:
                return generateEAN8();
            default:
                throw new IllegalArgumentException("Unsupported GTIN type: " + type);
        }
    }
    
    /**
     * Generate a random UPC-A (12 digits)
     */
    public static String generateUPCA() {
        Random random = new Random();
        StringBuilder upc = new StringBuilder();
        
        for (int i = 0; i < 11; i++) {
            upc.append(random.nextInt(10));
        }
        
        int checkDigit = calculateUPCCheckDigit(upc.toString());
        upc.append(checkDigit);
        
        return upc.toString();
    }
    
    /**
     * Generate a random EAN-13 (13 digits)
     */
    public static String generateEAN13() {
        Random random = new Random();
        StringBuilder ean = new StringBuilder();
        
        String countryCode = COUNTRY_CODES[random.nextInt(COUNTRY_CODES.length)];
        ean.append(countryCode);
        
        for (int i = 0; i < 9; i++) {
            ean.append(random.nextInt(10));
        }
        
        int checkDigit = calculateEAN13CheckDigit(ean.toString());
        ean.append(checkDigit);
        
        return ean.toString();
    }
    
    /**
     * Generate a random UPC-E (8 digits)
     */
    public static String generateUPCE() {
        Random random = new Random();
        StringBuilder upce = new StringBuilder();
        
        upce.append(random.nextInt(2));
        
        for (int i = 0; i < 6; i++) {
            upce.append(random.nextInt(10));
        }
        
        upce.append(random.nextInt(10));
        
        return upce.toString();
    }
    
    /**
     * Generate a random EAN-8 (8 digits)
     */
    public static String generateEAN8() {
        Random random = new Random();
        StringBuilder ean8 = new StringBuilder();
        
        for (int i = 0; i < 7; i++) {
            ean8.append(random.nextInt(10));
        }
        
        int checkDigit = calculateEAN8CheckDigit(ean8.toString());
        ean8.append(checkDigit);
        
        return ean8.toString();
    }
    
    /**
     * Calculate UPC check digit (for UPC-A and UPC-E)
     */
    public static int calculateUPCCheckDigit(String code) {
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
     * Calculate EAN-13 check digit
     */
    public static int calculateEAN13CheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            if (i % 2 == 0) {
                sum += digit;
            } else {
                sum += digit * 3;
            }
        }
        return (10 - (sum % 10)) % 10;
    }
    
    /**
     * Calculate EAN-8 check digit
     */
    public static int calculateEAN8CheckDigit(String code) {
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
     * Validate a GTIN code
     */
    public static boolean isValidGTIN(String gtin) {
        if (gtin == null || gtin.isEmpty()) {
            return false;
        }
        
        gtin = gtin.replaceAll("[\\s-]", "");
        
        switch (gtin.length()) {
            case 8:
                return isValidEAN8(gtin);
            case 12:
                return isValidUPCA(gtin);
            case 13:
                return isValidEAN13(gtin);
            default:
                return false;
        }
    }
    
    /**
     * Validate UPC-A code
     */
    public static boolean isValidUPCA(String upc) {
        if (upc.length() != 12 || !upc.matches("\\d{12}")) {
            return false;
        }
        
        String codeWithoutCheck = upc.substring(0, 11);
        int providedCheckDigit = Character.getNumericValue(upc.charAt(11));
        int calculatedCheckDigit = calculateUPCCheckDigit(codeWithoutCheck);
        
        return providedCheckDigit == calculatedCheckDigit;
    }
    
    /**
     * Validate EAN-13 code
     */
    public static boolean isValidEAN13(String ean) {
        if (ean.length() != 13 || !ean.matches("\\d{13}")) {
            return false;
        }
        
        String codeWithoutCheck = ean.substring(0, 12);
        int providedCheckDigit = Character.getNumericValue(ean.charAt(12));
        int calculatedCheckDigit = calculateEAN13CheckDigit(codeWithoutCheck);
        
        return providedCheckDigit == calculatedCheckDigit;
    }
    
    /**
     * Validate EAN-8 code
     */
    public static boolean isValidEAN8(String ean8) {
        if (ean8.length() != 8 || !ean8.matches("\\d{8}")) {
            return false;
        }
        
        String codeWithoutCheck = ean8.substring(0, 7);
        int providedCheckDigit = Character.getNumericValue(ean8.charAt(7));
        int calculatedCheckDigit = calculateEAN8CheckDigit(codeWithoutCheck);
        
        return providedCheckDigit == calculatedCheckDigit;
    }
    
    /**
     * Determine the GTIN type from a string
     */
    public static GTINType getGTINType(String gtin) {
        if (gtin == null) {
            return null;
        }
        
        gtin = gtin.replaceAll("[\\s-]", "");
        
        switch (gtin.length()) {
            case 8:
                return GTINType.EAN_8;
            case 12:
                return GTINType.UPC_A;
            case 13:
                return GTINType.EAN_13;
            default:
                return null;
        }
    }
    
    /**
     * Convert UPC-A to EAN-13 by adding leading zero
     */
    public static String convertUPCAToEAN13(String upca) {
        if (!isValidUPCA(upca)) {
            throw new IllegalArgumentException("Invalid UPC-A code: " + upca);
        }
        return "0" + upca;
    }
    
    /**
     * Format GTIN for display with appropriate separators
     */
    public static String formatGTIN(String gtin) {
        if (!isValidGTIN(gtin)) {
            return gtin;
        }
        
        gtin = gtin.replaceAll("[\\s-]", "");
        
        switch (gtin.length()) {
            case 8:
                return gtin.substring(0, 4) + "-" + gtin.substring(4);
            case 12:
                return gtin.substring(0, 1) + "-" + 
                       gtin.substring(1, 6) + "-" + 
                       gtin.substring(6, 11) + "-" + 
                       gtin.substring(11);
            case 13:
                return gtin.substring(0, 3) + "-" + gtin.substring(3);
            default:
                return gtin;
        }
    }
    
    /**
     * Generate the most appropriate GTIN type for tire products
     * Generally uses EAN-13 for international compatibility
     */
    public static String generateTireGTIN() {
        return generateEAN13();
    }
} 