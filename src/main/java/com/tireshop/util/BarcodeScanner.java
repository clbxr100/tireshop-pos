package com.tireshop.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for scanning barcodes using ZXing
 */
public class BarcodeScanner {
    
    private final MultiFormatReader reader;
    
    public BarcodeScanner() {
        reader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.EAN_13);
        reader.setHints(hints);
    }
    
    /**
     * Scan a barcode from an image file
     * @param file Image file containing barcode
     * @return Scanned barcode text or null if not found
     */
    public String scanFromFile(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            return scanImage(image);
        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Scan a barcode from an input stream
     * @param inputStream Input stream containing image
     * @return Scanned barcode text or null if not found
     */
    public String scanFromStream(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            return scanImage(image);
        } catch (IOException e) {
            System.err.println("Error reading image stream: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Scan a barcode from a BufferedImage
     * @param image Image to scan
     * @return Scanned barcode text or null if not found
     */
    public String scanImage(BufferedImage image) {
        if (image == null) {
            return null;
        }
        
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = reader.decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            System.out.println("No barcode found in image");
            return null;
        } catch (Exception e) {
            System.err.println("Error scanning barcode: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate a barcode (would need implementation with ZXing Writer)
     * @param content Content to encode
     * @param format Format (e.g., EAN_13)
     * @param width Image width
     * @param height Image height
     * @return Generated barcode file or null on error
     */
    public File generateBarcode(String content, BarcodeFormat format, int width, int height) {
        // Implementation would use ZXing BarcodeWriter
        // This is a placeholder for future implementation
        return null;
    }
} 