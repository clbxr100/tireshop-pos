package com.tireshop.service;

import com.tireshop.model.*;
import com.tireshop.util.SettingsService;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for exporting data to QuickBooks IIF (Interchange File Format)
 * Supports sales, customers, inventory, and financial data export
 */
public class QuickBooksExportService {
    
    private static final Logger LOGGER = Logger.getLogger(QuickBooksExportService.class.getName());
    
    private final SettingsService settingsService;
    private final SalesService salesService;
    private final InventoryService inventoryService;
    
    // QuickBooks date format
    private static final DateTimeFormatter QB_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // QuickBooks account types
    private static final String SALES_ACCOUNT = "Sales";
    private static final String SALES_TAX_ACCOUNT = "Sales Tax Payable";
    private static final String INVENTORY_ACCOUNT = "Inventory Asset";
    private static final String COGS_ACCOUNT = "Cost of Goods Sold";
    private static final String SERVICE_INCOME_ACCOUNT = "Service Income";
    private static final String AR_ACCOUNT = "Accounts Receivable";
    private static final String CASH_ACCOUNT = "Undeposited Funds";
    
    public QuickBooksExportService(SettingsService settingsService, 
                                    SalesService salesService,
                                    InventoryService inventoryService) {
        this.settingsService = settingsService;
        this.salesService = salesService;
        this.inventoryService = inventoryService;
    }
    
    /**
     * Export sales data for a date range
     */
    public ExportResult exportSales(LocalDate fromDate, LocalDate toDate) {
        ExportResult result = new ExportResult();
        
        try {
            List<Sale> sales = salesService.getSalesByDateRange(fromDate, toDate);
            
            if (sales.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No sales found for the specified date range");
                return result;
            }
            
            String fileName = generateFileName("Sales", fromDate, toDate);
            Path exportPath = Paths.get(settingsService.getQuickBooksExportPath(), fileName);
            
            // Ensure export directory exists
            Files.createDirectories(exportPath.getParent());
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath.toFile()))) {
                // Write IIF header
                writeIIFHeader(writer);
                
                // Write transactions
                writeSalesTransactions(writer, sales);
                
                result.setSuccess(true);
                result.setFilePath(exportPath.toString());
                result.setRecordCount(sales.size());
                result.setMessage("Successfully exported " + sales.size() + " sales");
                
            } catch (IOException e) {
                throw new ExportException("Failed to write export file", e);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting sales", e);
            result.setSuccess(false);
            result.setMessage("Export failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Export customer data
     */
    public ExportResult exportCustomers() {
        ExportResult result = new ExportResult();
        
        try {
            List<Customer> customers = salesService.getAllCustomers();
            
            if (customers.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No customers found");
                return result;
            }
            
            String fileName = generateFileName("Customers", null, null);
            Path exportPath = Paths.get(settingsService.getQuickBooksExportPath(), fileName);
            
            Files.createDirectories(exportPath.getParent());
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath.toFile()))) {
                // Write customer header
                writeCustomerHeader(writer);
                
                // Write customer data
                for (Customer customer : customers) {
                    writeCustomer(writer, customer);
                }
                
                result.setSuccess(true);
                result.setFilePath(exportPath.toString());
                result.setRecordCount(customers.size());
                result.setMessage("Successfully exported " + customers.size() + " customers");
                
            } catch (IOException e) {
                throw new ExportException("Failed to write export file", e);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting customers", e);
            result.setSuccess(false);
            result.setMessage("Export failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Export inventory data
     */
    public ExportResult exportInventory() {
        ExportResult result = new ExportResult();
        
        try {
            List<Product> products = inventoryService.getAllProducts();
            
            if (products.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("No products found");
                return result;
            }
            
            String fileName = generateFileName("Inventory", null, null);
            Path exportPath = Paths.get(settingsService.getQuickBooksExportPath(), fileName);
            
            Files.createDirectories(exportPath.getParent());
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath.toFile()))) {
                // Write inventory item header
                writeInventoryHeader(writer);
                
                // Write inventory items
                for (Product product : products) {
                    writeInventoryItem(writer, product);
                }
                
                result.setSuccess(true);
                result.setFilePath(exportPath.toString());
                result.setRecordCount(products.size());
                result.setMessage("Successfully exported " + products.size() + " inventory items");
                
            } catch (IOException e) {
                throw new ExportException("Failed to write export file", e);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting inventory", e);
            result.setSuccess(false);
            result.setMessage("Export failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Write IIF header for transactions
     */
    private void writeIIFHeader(PrintWriter writer) {
        writer.println("!TRNS\tTRNSID\tTRNSTYPE\tDATE\tACCNT\tNAME\tCLASS\tAMOUNT\tDOCNUM\tMEMO\tCLEAR\tTOPRINT\tNAMEISTAXABLE\tADDR1\tADDR2\tADDR3\tADDR4\tADDR5\tDUEDATE\tTERMS\tPAID\tSHIPDATE");
        writer.println("!SPL\tSPLID\tTRNSTYPE\tDATE\tACCNT\tNAME\tCLASS\tAMOUNT\tDOCNUM\tMEMO\tCLEAR\tQNTY\tPRICE\tINVITEM\tPAYMETH\tTAXABLE\tREIMB\tEXTRA");
        writer.println("!ENDTRNS");
    }
    
    /**
     * Write customer header
     */
    private void writeCustomerHeader(PrintWriter writer) {
        writer.println("!CUST\tNAME\tBNAME\tCONT1\tCONT2\tPHONE1\tPHONE2\tFAXNUM\tEMAIL\tNOTE\tTAXABLE\tLIMIT");
    }
    
    /**
     * Write inventory header
     */
    private void writeInventoryHeader(PrintWriter writer) {
        writer.println("!INVITEM\tNAME\tINVITEMTYPE\tDESC\tPURCHASEDESC\tACCNT\tASSTACCT\tCOGSACCT\tPRICE\tCOST\tTAXABLE\tPAYMETH\tTAXVEND\tTAXDIST\tPREFVEND\tREORDERPOINT\tEXTRA\tCUSTFLD1\tCUSTFLD2\tCUSTFLD3\tCUSTFLD4\tCUSTFLD5\tDEP_TYPE\tISPASSEDTHRU");
    }
    
    /**
     * Write sales transactions
     */
    private void writeSalesTransactions(PrintWriter writer, List<Sale> sales) {
        for (Sale sale : sales) {
            if (!sale.isPaid()) {
                continue; // Skip unpaid sales
            }
            
            String transType = "INVOICE";
            String account = getAccountForPaymentType(sale.getPaymentType());
            String customerName = sale.getCustomer() != null ? 
                                 formatCustomerName(sale.getCustomer()) : "Cash Customer";
            
            // Transaction header line
            writer.printf("TRNS\t\t%s\t%s\t%s\t%s\t\t%s\t%s\t%s\tN\tN\tN\t\t\t\t\t\t\t\tY\t\n",
                         transType,
                         formatDate(sale.getTimestamp()),
                         account,
                         customerName,
                         formatAmount(sale.getTotal()),
                         sale.getInvoiceNumber(),
                         "Sale " + sale.getInvoiceNumber());
            
            // Sales line (negative amount for income)
            BigDecimal salesAmount = sale.getSubtotal().negate();
            writer.printf("SPL\t\t%s\t%s\t%s\t\t\t%s\t%s\t%s\t\t\t\t\t\tN\tEXEMP\t\n",
                         transType,
                         formatDate(sale.getTimestamp()),
                         SALES_ACCOUNT,
                         formatAmount(salesAmount),
                         sale.getInvoiceNumber(),
                         "Sales");
            
            // Sales tax line if applicable
            if (sale.getTax().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxAmount = sale.getTax().negate();
                writer.printf("SPL\t\t%s\t%s\t%s\t\t\t%s\t%s\t%s\t\t\t\t\t\tN\tEXEMP\t\n",
                             transType,
                             formatDate(sale.getTimestamp()),
                             SALES_TAX_ACCOUNT,
                             formatAmount(taxAmount),
                             sale.getInvoiceNumber(),
                             "Sales Tax");
            }
            
            // Credit card fee line if applicable
            if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal feeAmount = sale.getCreditCardFeeAmount().negate();
                writer.printf("SPL\t\t%s\t%s\t%s\t\t\t%s\t%s\t%s\t\t\t\t\t\tN\tEXEMP\t\n",
                             transType,
                             formatDate(sale.getTimestamp()),
                             "Credit Card Processing Fees",
                             formatAmount(feeAmount),
                             sale.getInvoiceNumber(),
                             "CC Processing Fee");
            }
            
            writer.println("ENDTRNS");
        }
    }
    
    /**
     * Write customer record
     */
    private void writeCustomer(PrintWriter writer, Customer customer) {
        writer.printf("CUST\t%s\t%s\t%s\t\t%s\t\t\t%s\t\tY\t0.00\n",
                     formatCustomerName(customer),
                     customer.getFullName(),
                     customer.getFullName(),
                     formatPhone(customer.getPhone()),
                     customer.getEmail() != null ? customer.getEmail() : "");
    }
    
    /**
     * Write inventory item
     */
    private void writeInventoryItem(PrintWriter writer, Product product) {
        String itemType = "INVENTORY";
        String description = product.getDescription() != null ? 
                           product.getDescription() : product.getName();
        
        writer.printf("INVITEM\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tY\t\t\t\t\t%d\t\t%s\t%s\t%s\t\t\t\tN\n",
                     formatItemName(product),
                     itemType,
                     description,
                     description,
                     SALES_ACCOUNT,
                     INVENTORY_ACCOUNT,
                     COGS_ACCOUNT,
                     formatAmount(product.getSellingPrice()),
                     formatAmount(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO),
                     product.getReorderLevel() != null ? product.getReorderLevel() : 0,
                     product.getSku() != null ? product.getSku() : "",
                     product.getSize() != null ? product.getSize() : "",
                     product.getManufacturer() != null ? product.getManufacturer() : "");
    }
    
    /**
     * Helper method to format customer name for QuickBooks
     */
    private String formatCustomerName(Customer customer) {
        // QuickBooks doesn't allow certain characters in names
        return customer.getFullName().replaceAll("[:\n\r]", "");
    }
    
    /**
     * Helper method to format item name for QuickBooks
     */
    private String formatItemName(Product product) {
        // Combine manufacturer and name for unique item identification
        String name = product.getName();
        if (product.getManufacturer() != null && !product.getManufacturer().isEmpty()) {
            name = product.getManufacturer() + " " + name;
        }
        // QuickBooks has a 31 character limit for item names
        if (name.length() > 31) {
            name = name.substring(0, 31);
        }
        return name.replaceAll("[:\n\r]", "");
    }
    
    /**
     * Helper method to format date
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate().format(QB_DATE_FORMAT);
    }
    
    /**
     * Helper method to format amount
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }
    
    /**
     * Helper method to format phone number
     */
    private String formatPhone(String phone) {
        if (phone == null) return "";
        // Remove all non-numeric characters
        return phone.replaceAll("[^0-9]", "");
    }
    
    /**
     * Get QuickBooks account based on payment type
     */
    private String getAccountForPaymentType(PaymentType paymentType) {
        if (paymentType == null) return CASH_ACCOUNT;
        
        switch (paymentType) {
            case CASH:
                return CASH_ACCOUNT;
            case CHECK:
                return "Checks Received";
            case CREDIT_CARD:
            case DEBIT_CARD:
                return "Credit Card Clearing";
            default:
                return CASH_ACCOUNT;
        }
    }
    
    /**
     * Generate export file name
     */
    private String generateFileName(String type, LocalDate fromDate, LocalDate toDate) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(type);
        fileName.append("_Export_");
        fileName.append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        if (fromDate != null && toDate != null) {
            fileName.append("_");
            fileName.append(fromDate.format(DateTimeFormatter.ofPattern("MMdd")));
            fileName.append("-");
            fileName.append(toDate.format(DateTimeFormatter.ofPattern("MMdd")));
        }
        
        fileName.append(".iif");
        return fileName.toString();
    }
    
    // Data Transfer Objects
    
    public static class ExportResult {
        private boolean success;
        private String message;
        private String filePath;
        private int recordCount;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    }
    
    public static class ExportException extends Exception {
        public ExportException(String message) {
            super(message);
        }
        
        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 