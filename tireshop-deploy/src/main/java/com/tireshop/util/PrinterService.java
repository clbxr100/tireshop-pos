package com.tireshop.util;

import com.tireshop.model.Sale;
import com.tireshop.model.SaleItem;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling printing operations
 */
public class PrinterService {
    
    private static final int RECEIPT_WIDTH = 40;
    private String defaultPrinterName = null; // Store user's preferred printer name
    private final SettingsService settingsService;

    public PrinterService(SettingsService settingsService) {
        this.settingsService = settingsService;
        // Potentially load a saved defaultPrinterName preference here
    }

    /**
     * Get a list of available printers
     * @return List of printer names
     */
    public List<String> getAvailablePrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(printServices)
                .map(PrintService::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the default printer name
     * @return Default printer name
     */
    public String getDefaultPrinterName() {
        return defaultPrinterName;
    }
    
    /**
     * Set the default printer
     * @param printerName Name of the printer to set as default
     */
    public void setDefaultPrinterName(String printerName) {
        // In a real implementation, we would save this preference (e.g., via SettingsService)
        this.defaultPrinterName = printerName;
        System.out.println("[PrinterService] Default printer set to: " + (printerName == null ? "System Default" : printerName));
    }
    
    /**
     * Print a receipt for a sale
     * @param sale The sale to print receipt for
     * @param targetPrinterName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    public boolean printReceipt(Sale sale, String targetPrinterName) {
        String receipt = generateReceiptContent(sale);
        String printerToUse = (targetPrinterName != null) ? targetPrinterName : this.defaultPrinterName;
        return printString(receipt, printerToUse);
    }
    
    /**
     * Generate the receipt content for a sale
     * @param sale The sale to generate receipt for
     * @return Receipt content as a string
     */
    private String generateReceiptContent(Sale sale) {
        StringBuilder sb = new StringBuilder();
        
        // Use settingsService for company info
        sb.append(centerText(settingsService.getCompanyName(), RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyAddress(), RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyPhone(), RECEIPT_WIDTH)).append("\n");
        // TODO: Add logo printing if path is set and supported (complex, skip for now)
        sb.append("\n");
        
        // Invoice details
        sb.append("Invoice: ").append(sale.getInvoiceNumber()).append("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append("Date: ").append(sale.getTimestamp().format(formatter)).append("\n");
        
        if (sale.getCustomer() != null) {
            sb.append("Customer: ").append(sale.getCustomer().getFirstName())
                    .append(" ").append(sale.getCustomer().getLastName()).append("\n");
        }
        if (sale.getVehicle() != null) {
            sb.append("Vehicle: ").append(sale.getVehicle().toString()).append("\n");
        }
        sb.append("\n");
        
        sb.append(formatReceiptLine("Item", "Qty", "Price", "Total", RECEIPT_WIDTH)).append("\n");
        sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");
        
        for (SaleItem item : sale.getItems()) {
            String name = truncateString(getItemName(item), 20);
            String qty = String.valueOf(item.getQuantity());
            String price = formatCurrency(item.getUnitPrice());
            String total = formatCurrency(item.getSubtotal());
            sb.append(formatReceiptLine(name, qty, price, total, RECEIPT_WIDTH)).append("\n");
        }
        
        sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");
        // Use settingsService for tax calculation (Note: Sale object already calculates its own tax)
        // If we want PrinterService to re-verify or use a global tax rate, we'd fetch it here.
        // For now, using the tax from the Sale object.
        sb.append(formatReceiptLine("", "", "Subtotal:", formatCurrency(sale.getSubtotal()), RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "Tax:", formatCurrency(sale.getTax()), RECEIPT_WIDTH)).append("\n");
        
        // Add Credit Card Fee if applicable
        if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(formatReceiptLine("", "", "CC Fee:", formatCurrency(sale.getCreditCardFeeAmount()), RECEIPT_WIDTH)).append("\n");
        }

        sb.append(formatReceiptLine("", "", "Total:", formatCurrency(sale.getTotal()), RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        sb.append("Payment Method: ").append(sale.getPaymentMethod()).append("\n");
        if (sale.getPaymentType() != null) {
            switch (sale.getPaymentType()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    if (sale.getCardType() != null && !sale.getCardType().isEmpty()) {
                        sb.append("Card Type: ").append(sale.getCardType()).append("\n");
                    }
                    if (sale.getCardLastFour() != null && !sale.getCardLastFour().equals("XXXX") && !sale.getCardLastFour().isEmpty()) {
                        sb.append("Card Number: ending in ").append(sale.getCardLastFour()).append("\n");
                    }
                    if (sale.getAuthorizationCode() != null && !sale.getAuthorizationCode().equals("AUTH_EXT") && !sale.getAuthorizationCode().isEmpty()) {
                        sb.append("Auth Code: ").append(sale.getAuthorizationCode()).append("\n");
                    }
                    break;
                case CHECK:
                    if (sale.getCheckNumber() != null) {
                        sb.append("Check #: ").append(sale.getCheckNumber()).append("\n");
                    }
                    break;
                default:
                    break;
            }
        }
        sb.append("\n");
        sb.append(centerText("Thank you for your business!", RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Please come again!", RECEIPT_WIDTH)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Get the name of an item (product or service)
     * @param item The sale item
     * @return Item name
     */
    private String getItemName(SaleItem item) {
        if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
            return item.getProduct().getName();
        } else if ("SERVICE".equals(item.getItemType()) && item.getService() != null) {
            return item.getService().getName();
        } else if ("CUSTOM".equals(item.getItemType()) && item.getCustomItemName() != null) {
            return item.getCustomItemName();
        }
        return "Unknown Item";
    }
    
    /**
     * Format a line for the receipt
     * @param item Item name
     * @param qty Quantity
     * @param price Unit price
     * @param total Line total
     * @param width Receipt width
     * @return Formatted line
     */
    private String formatReceiptLine(String item, String qty, String price, String total, int width) {
        int itemWidth = width - 21; // Adjusting for the other fields and spaces
        
        if (item.length() > itemWidth) {
            item = item.substring(0, itemWidth - 3) + "...";
        }
        
        // Pad item to fixed width
        StringBuilder sb = new StringBuilder(item);
        while (sb.length() < itemWidth) {
            sb.append(" ");
        }
        
        // Add quantity (right-aligned in 3 spaces)
        sb.append(" ");
        String qtyString = padLeft(qty, 3);
        sb.append(qtyString);
        
        // Add price (right-aligned in 8 spaces)
        sb.append(" ");
        String priceString = padLeft(price, 8);
        sb.append(priceString);
        
        // Add total (right-aligned in 8 spaces)
        sb.append(" ");
        String totalString = padLeft(total, 8);
        sb.append(totalString);
        
        return sb.toString();
    }
    
    /**
     * Print a string to a printer
     * @param content Content to print
     * @param printerName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    private boolean printString(String content, String printerName) {
        System.out.println("[PrinterService] Attempting to print to: " + (printerName == null ? "System Default" : printerName));
        System.out.println("====== RECEIPT CONTENT (DEBUG) ======");
        System.out.println(content);
        System.out.println("=====================================");
        try {
            // In a real implementation, this would connect to the physical printer
            // For now, we'll just simulate the printing process
            
            // Convert content to input stream
            InputStream is = new ByteArrayInputStream(content.getBytes());
            
            // Find printer
            PrintService printService;
            if (printerName != null) {
                PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                Optional<PrintService> service = Arrays.stream(services)
                        .filter(s -> s.getName().equalsIgnoreCase(printerName))
                        .findFirst();
                printService = service.orElse(PrintServiceLookup.lookupDefaultPrintService());
            } else {
                printService = PrintServiceLookup.lookupDefaultPrintService();
            }
            
            if (printService == null) {
                System.err.println("[PrinterService] No printer found (selected: " + (printerName == null ? "System Default" : printerName) + "). Check printer configuration.");
                return false;
            }
            System.out.println("[PrinterService] Using printer: " + printService.getName());
            
            // Create print job
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(is, flavor, null);
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            
            // Print
            DocPrintJob job = printService.createPrintJob();
            job.addPrintJobListener(new PrintJobAdapter() {
                @Override
                public void printJobCompleted(PrintJobEvent pje) {
                    System.out.println("[PrinterService] Print job completed.");
                }
                @Override
                public void printJobFailed(PrintJobEvent pje) {
                    System.err.println("[PrinterService] Print job FAILED.");
                }
                @Override
                public void printJobCanceled(PrintJobEvent pje) {
                    System.out.println("[PrinterService] Print job canceled.");
                }
                @Override
                public void printJobNoMoreEvents(PrintJobEvent pje) {
                    System.out.println("[PrinterService] Print job: no more events.");
                }
                @Override
                public void printDataTransferCompleted(PrintJobEvent pje) {
                    System.out.println("[PrinterService] Print data transfer completed.");
                }
            });

            job.print(doc, attrs);
            System.out.println("[PrinterService] Print job sent to printer.");
            
            // For testing purposes, also print to console
            System.out.println("====== RECEIPT ======");
            System.out.println(content);
            System.out.println("====================");
            
            return true;
        } catch (PrintException e) {
            System.err.println("[PrinterService] Error printing: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Format currency value
     * @param value Currency value
     * @return Formatted string
     */
    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "$0.00";
        }
        return String.format("$%.2f", value);
    }
    
    /**
     * Center text in a fixed width
     * @param text Text to center
     * @param width Fixed width
     * @return Centered text
     */
    private String centerText(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) sb.append(" ");
        sb.append(text);
        while (sb.length() < width) sb.append(" ");
        return sb.toString();
    }
    
    /**
     * Pad a string with spaces on the left
     * @param s String to pad
     * @param width Desired width
     * @return Padded string
     */
    private String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width - s.length(); i++) sb.append(" ");
        sb.append(s);
        return sb.toString();
    }
    
    /**
     * Repeat a character n times
     * @param c Character to repeat
     * @param n Number of times to repeat
     * @return Resulting string
     */
    private String repeatChar(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
    
    /**
     * Truncate a string if it's longer than the specified length
     * @param s String to truncate
     * @param length Maximum length
     * @return Truncated string
     */
    private String truncateString(String s, int length) {
        if (s == null || s.length() <= length) {
            return s;
        }
        return s.substring(0, length - 3) + "...";
    }
} 