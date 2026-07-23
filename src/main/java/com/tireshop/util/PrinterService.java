package com.tireshop.util;

import com.tireshop.model.Sale;
import com.tireshop.model.SaleItem;
import com.tireshop.model.Product;
import com.tireshop.model.Customer;
import com.tireshop.model.ChargeAccountPayment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.net.Socket;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Service for handling printing operations
 */
public class PrinterService {
    
    private static final int TEXT_RECEIPT_WIDTH = 40; // For plain text receipts (thermal printers)
    private static final int FULL_PAGE_RECEIPT_WIDTH = 80; // For full-page receipts (8.5x11 paper)
    private String defaultPrinterName = null; // Store user's preferred printer name
    private final SettingsService settingsService;
    private static final Logger LOGGER = Logger.getLogger(PrinterService.class.getName());
    
    // Remote printing configuration
    private String remotePrintServerIP = "192.168.1.59"; // POS computer IP
    private int remotePrintServerPort = 8080; // Default port for print server
    private boolean useRemotePrinting = true; // Enable remote printing by default

    // Store current font and size for PDF generation to help with text width calculation
    private PDFont currentPdfFont = PDType1Font.HELVETICA;
    private float currentPdfFontSize = 10;

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
        LOGGER.info("[PrinterService] Default printer set to: " + (printerName == null ? "System Default" : printerName));
    }
    
    /**
     * Print a full-page receipt for a sale (instead of narrow thermal format)
     * @param sale The sale to print receipt for
     * @param targetPrinterName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    public boolean printFullPageReceipt(Sale sale, String targetPrinterName) {
        String receipt = generateFullPageReceiptContent(sale);
        
        // Try direct network printing first (port 9100)
        if (useRemotePrinting) {
            LOGGER.info("[PrinterService] Attempting direct network printing to printer at " + remotePrintServerIP + ":9100");
            if (sendDirectPrintJob(receipt)) {
                LOGGER.info("[PrinterService] Direct network printing successful");
                return true;
            } else {
                LOGGER.warning("[PrinterService] Direct network printing failed, trying HTTP print server");
                // Try HTTP server as fallback
                if (sendRemotePrintJob(receipt, targetPrinterName)) {
                    LOGGER.info("[PrinterService] HTTP remote printing successful");
                    return true;
                } else {
                    LOGGER.warning("[PrinterService] Both network printing methods failed, falling back to local printing");
                }
            }
        }
        
        // Fallback to local printing
        String printerToUse = (targetPrinterName != null) ? targetPrinterName : this.defaultPrinterName;
        return printString(receipt, printerToUse);
    }
    
    /**
     * Print a receipt for a sale (original thermal receipt format)
     * @param sale The sale to print receipt for
     * @param targetPrinterName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    public boolean printReceipt(Sale sale, String targetPrinterName) {
        String receipt = generateTextReceiptContent(sale);
        
        // Try direct network printing first (port 9100)
        if (useRemotePrinting) {
            LOGGER.info("[PrinterService] Attempting direct network printing to printer at " + remotePrintServerIP + ":9100");
            if (sendDirectPrintJob(receipt)) {
                LOGGER.info("[PrinterService] Direct network printing successful");
                return true;
            } else {
                LOGGER.warning("[PrinterService] Direct network printing failed, trying HTTP print server");
                // Try HTTP server as fallback
                if (sendRemotePrintJob(receipt, targetPrinterName)) {
                    LOGGER.info("[PrinterService] HTTP remote printing successful");
                    return true;
                } else {
                    LOGGER.warning("[PrinterService] Both network printing methods failed, falling back to local printing");
                }
            }
        }
        
        // Fallback to local printing
        String printerToUse = (targetPrinterName != null) ? targetPrinterName : this.defaultPrinterName;
        return printString(receipt, printerToUse);
    }

    /**
     * Print a receipt for a store charge account payment (payoff).
     * @param payment The recorded payment
     * @param customer The customer who made the payment
     * @param targetPrinterName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    public boolean printChargePaymentReceipt(ChargeAccountPayment payment, Customer customer, String targetPrinterName) {
        String receipt = generateChargePaymentReceiptContent(payment, customer);

        // Try direct network printing first (port 9100)
        if (useRemotePrinting) {
            LOGGER.info("[PrinterService] Attempting direct network printing to printer at " + remotePrintServerIP + ":9100");
            if (sendDirectPrintJob(receipt)) {
                LOGGER.info("[PrinterService] Direct network printing successful");
                return true;
            } else {
                LOGGER.warning("[PrinterService] Direct network printing failed, trying HTTP print server");
                if (sendRemotePrintJob(receipt, targetPrinterName)) {
                    LOGGER.info("[PrinterService] HTTP remote printing successful");
                    return true;
                } else {
                    LOGGER.warning("[PrinterService] Both network printing methods failed, falling back to local printing");
                }
            }
        }

        // Fallback to local printing
        String printerToUse = (targetPrinterName != null) ? targetPrinterName : this.defaultPrinterName;
        return printString(receipt, printerToUse);
    }

    /**
     * Generate the plain-text content of a charge account payment receipt
     * (thermal-printer width, same style as sale receipts).
     */
    private String generateChargePaymentReceiptContent(ChargeAccountPayment payment, Customer customer) {
        if (payment == null) {
            LOGGER.warning("[PrinterService] Cannot generate payment receipt: payment is null");
            return "ERROR: Invalid payment data";
        }

        StringBuilder sb = new StringBuilder();

        String companyName = settingsService != null ? settingsService.getCompanyName() : "POS System";
        String companyAddress = settingsService != null ? settingsService.getCompanyAddress() : "";
        String companyPhone = settingsService != null ? settingsService.getCompanyPhone() : "";

        sb.append(centerText(companyName != null ? companyName : "POS System", TEXT_RECEIPT_WIDTH)).append("\n");
        if (companyAddress != null && !companyAddress.trim().isEmpty()) {
            sb.append(centerText(companyAddress, TEXT_RECEIPT_WIDTH)).append("\n");
        }
        if (companyPhone != null && !companyPhone.trim().isEmpty()) {
            sb.append(centerText(companyPhone, TEXT_RECEIPT_WIDTH)).append("\n");
        }
        sb.append("\n");

        sb.append(centerText("CHARGE ACCOUNT PAYMENT", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");

        if (payment.getId() != null) {
            sb.append("Receipt #: PAY-").append(payment.getId()).append("\n");
        }
        if (payment.getPaymentTimestamp() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
            sb.append("Date: ").append(payment.getPaymentTimestamp().format(formatter)).append("\n");
        }
        String customerName = customer != null ? customer.getFullName()
                : (payment.getCustomer() != null ? payment.getCustomer().getFullName() : null);
        if (customerName != null) {
            sb.append("Customer: ").append(customerName).append("\n");
        }
        sb.append("\n");

        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "Payment:", formatCurrency(payment.getAmount()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "Balance After:", formatCurrency(payment.getBalanceAfter()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");

        if (payment.getNotes() != null && !payment.getNotes().trim().isEmpty()) {
            sb.append("Notes: ").append(payment.getNotes()).append("\n");
            sb.append("\n");
        }

        sb.append(centerText("Thank you!", TEXT_RECEIPT_WIDTH)).append("\n");
        return sb.toString();
    }

    /**
     * Generate a PDF receipt for a store charge account payment.
     * @param payment The recorded payment
     * @param customer The customer who made the payment
     * @param filePath Target file path for the PDF
     * @return true if the PDF was generated successfully
     */
    public boolean generateChargePaymentPdf(ChargeAccountPayment payment, Customer customer, String filePath) {
        if (payment == null || filePath == null || filePath.trim().isEmpty()) {
            LOGGER.warning("[PrinterService] Cannot generate payment PDF: payment or file path missing");
            return false;
        }
        LOGGER.info("[PrinterService] Generating charge payment PDF to: " + filePath);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float currentY = yStart;
            float leading = 14.5f;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Company info
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(settingsService != null ? settingsService.getCompanyName() : "POS System"); contentStream.endText(); currentY -= leading;

                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                String address = settingsService != null ? settingsService.getCompanyAddress() : "";
                String phone = settingsService != null ? settingsService.getCompanyPhone() : "";
                if (address != null && !address.isEmpty()) {
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(address); contentStream.endText(); currentY -= leading;
                }
                if (phone != null && !phone.isEmpty()) {
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(phone); contentStream.endText(); currentY -= leading;
                }
                currentY -= leading;

                // Title
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 14);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("CHARGE ACCOUNT PAYMENT RECEIPT"); contentStream.endText(); currentY -= (leading * 2);

                setPdfFont(contentStream, PDType1Font.HELVETICA, 11);
                if (payment.getId() != null) {
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Receipt #: PAY-" + payment.getId()); contentStream.endText(); currentY -= leading;
                }
                if (payment.getPaymentTimestamp() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Date: " + payment.getPaymentTimestamp().format(formatter)); contentStream.endText(); currentY -= leading;
                }
                String customerName = customer != null ? customer.getFullName()
                        : (payment.getCustomer() != null ? payment.getCustomer().getFullName() : "");
                if (customerName != null && !customerName.isEmpty()) {
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Customer: " + customerName); contentStream.endText(); currentY -= leading;
                }
                currentY -= leading;

                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Payment Amount: " + formatCurrency(payment.getAmount())); contentStream.endText(); currentY -= leading;
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Balance After Payment: " + formatCurrency(payment.getBalanceAfter())); contentStream.endText(); currentY -= (leading * 2);

                if (payment.getNotes() != null && !payment.getNotes().trim().isEmpty()) {
                    setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                    contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Notes: " + payment.getNotes()); contentStream.endText(); currentY -= (leading * 2);
                }

                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Thank you!"); contentStream.endText();
            }

            document.save(filePath);
            LOGGER.info("[PrinterService] Charge payment PDF saved successfully: " + filePath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrinterService] Error generating charge payment PDF: " + filePath, e);
            return false;
        }
    }

    /**
     * Send print job directly to network printer on port 9100 (RAW printing)
     * @param content Content to print
     * @return true if successful
     */
    private boolean sendDirectPrintJob(String content) {
        try {
            LOGGER.info("[PrinterService] Connecting directly to printer at " + remotePrintServerIP + ":9100");
            
            // Connect directly to printer's RAW port
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(10000); // 10 second timeout
                socket.connect(new java.net.InetSocketAddress(remotePrintServerIP, 9100), 5000);
                
                LOGGER.info("[PrinterService] Connected to printer, sending print data");
                
                // Send the receipt content directly to printer with proper formatting
                try (java.io.OutputStream out = socket.getOutputStream()) {
                    // Start with printer initialization and darkness settings
                    StringBuilder printData = new StringBuilder();
                    
                    // ESC/P commands for Brother printers to improve print quality
                    printData.append("\u001B@");        // Initialize printer (ESC @)
                    printData.append("\u001B!\u0008");   // Select condensed font (ESC ! 8)
                    printData.append("\u001Bx\u0001");   // Select NLQ (Near Letter Quality) (ESC x 1)
                    printData.append("\u001BE");         // Select bold font (ESC E)
                    
                    // Add the actual content with proper line endings
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        printData.append(line);
                        printData.append("\r\n");  // Use both CR and LF for better compatibility
                    }
                    
                    // End with form feed and reset
                    printData.append("\r\n\r\n\r\n");   // Extra spacing
                    printData.append("\u000C");         // Form feed (FF) to advance paper
                    printData.append("\u001B@");        // Reset printer (ESC @)
                    
                    // Convert to bytes and send
                    byte[] printBytes = printData.toString().getBytes("UTF-8");
                    out.write(printBytes);
                    out.flush();
                    
                    // Wait a moment for the printer to process
                    Thread.sleep(100);
                }
                
                LOGGER.info("[PrinterService] Print data sent successfully to network printer with enhanced formatting");
                return true;
            }
            
        } catch (java.net.ConnectException e) {
            LOGGER.warning("[PrinterService] Cannot connect to network printer at " + remotePrintServerIP + ":9100 - " + e.getMessage());
            LOGGER.info("[PrinterService] Network printer troubleshooting:");
            LOGGER.info("[PrinterService] 1. Verify printer IP: " + remotePrintServerIP);
            LOGGER.info("[PrinterService] 2. Check if printer is powered on and connected to network");
            LOGGER.info("[PrinterService] 3. Test connectivity: ping " + remotePrintServerIP);
            LOGGER.info("[PrinterService] 4. Verify port 9100 is open on printer");
            return false;
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.warning("[PrinterService] Timeout connecting to network printer - " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PrinterService] Error with direct network printing: ", e);
            return false;
        }
    }
    
    /**
     * Send a print job to the remote POS computer
     * @param content Content to print
     * @param printerName Target printer name
     * @return true if successful
     */
    private boolean sendRemotePrintJob(String content, String printerName) {
        try {
            // Create URL for the remote print service
            String urlString = "http://" + remotePrintServerIP + ":" + remotePrintServerPort + "/print";
            URL url = new URL(urlString);
            
            LOGGER.info("[PrinterService] Connecting to remote print server: " + urlString);
            
            // Open HTTP connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(10000); // 10 second read timeout
            
            // Create JSON payload with proper escaping
            StringBuilder jsonPayload = new StringBuilder();
            jsonPayload.append("{");
            
            // Properly escape the content for JSON
            String escapedContent = content
                .replace("\\", "\\\\")  // Escape backslashes first
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r")   // Escape carriage returns
                .replace("\t", "\\t");  // Escape tabs
            
            jsonPayload.append("\"content\":\"").append(escapedContent).append("\",");
            jsonPayload.append("\"printer\":\"").append(printerName != null ? printerName : "default").append("\"");
            jsonPayload.append("}");
            
            // Send the request
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(jsonPayload.toString());
                writer.flush();
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = reader.readLine();
                    LOGGER.info("[PrinterService] Remote print response: " + response);
                    return true;
                }
            } else {
                LOGGER.warning("[PrinterService] Remote print server returned error code: " + responseCode);
                return false;
            }
            
        } catch (java.net.ConnectException e) {
            LOGGER.warning("[PrinterService] Cannot connect to remote print server at " + remotePrintServerIP + ":" + remotePrintServerPort + " - " + e.getMessage());
            LOGGER.info("[PrinterService] Troubleshooting tips:");
            LOGGER.info("[PrinterService] 1. Verify the POS computer (" + remotePrintServerIP + ") is running");
            LOGGER.info("[PrinterService] 2. Check if print server is started with 'start-pos.bat' on POS computer");
            LOGGER.info("[PrinterService] 3. Test connectivity: ping " + remotePrintServerIP);
            LOGGER.info("[PrinterService] 4. Check firewall settings on POS computer");
            LOGGER.info("[PrinterService] 5. Verify port " + remotePrintServerPort + " is open and not blocked");
            return false;
        } catch (java.net.SocketTimeoutException e) {
            LOGGER.warning("[PrinterService] Timeout connecting to remote print server - " + e.getMessage());
            LOGGER.info("[PrinterService] The POS computer may be running but print server is not responding");
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PrinterService] Error sending remote print job: ", e);
            return false;
        }
    }
    
    /**
     * Generate the receipt content for a sale
     * @param sale The sale to generate receipt for
     * @return Receipt content as a string
     */
    private String generateTextReceiptContent(Sale sale) {
        if (sale == null) {
            LOGGER.warning("[PrinterService] Cannot generate receipt: sale is null");
            return "ERROR: Invalid sale data";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Use settingsService for company info with null safety
        String companyName = settingsService != null ? settingsService.getCompanyName() : "POS System";
        String companyAddress = settingsService != null ? settingsService.getCompanyAddress() : "";
        String companyPhone = settingsService != null ? settingsService.getCompanyPhone() : "";
        
        sb.append(centerText(companyName != null ? companyName : "POS System", TEXT_RECEIPT_WIDTH)).append("\n");
        if (companyAddress != null && !companyAddress.trim().isEmpty()) {
            sb.append(centerText(companyAddress, TEXT_RECEIPT_WIDTH)).append("\n");
        }
        if (companyPhone != null && !companyPhone.trim().isEmpty()) {
            sb.append(centerText(companyPhone, TEXT_RECEIPT_WIDTH)).append("\n");
        }
        sb.append("\n");
        
        // Invoice details with null safety
        String invoiceNumber = sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "NO-INVOICE";
        sb.append("Invoice: ").append(invoiceNumber).append("\n");

        // PO Number if present
        if (sale.getPoNumber() != null && !sale.getPoNumber().trim().isEmpty()) {
            sb.append("PO #: ").append(sale.getPoNumber()).append("\n");
        }

        if (sale.getTimestamp() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
            sb.append("Date: ").append(sale.getTimestamp().format(formatter)).append("\n");
        } else {
            sb.append("Date: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"))).append("\n");
        }
        
        if (sale.getCustomer() != null && sale.getCustomer().getFullName() != null) {
            sb.append("Customer: ").append(sale.getCustomer().getFullName()).append("\n");
        }
        if (sale.getVehicle() != null) {
            String vehicleInfo = "";
            if (sale.getVehicle().getMake() != null) vehicleInfo += sale.getVehicle().getMake() + " ";
            if (sale.getVehicle().getModel() != null) vehicleInfo += sale.getVehicle().getModel() + " ";
            if (sale.getVehicle().getLicensePlate() != null) vehicleInfo += "(" + sale.getVehicle().getLicensePlate() + ")";
            if (!vehicleInfo.trim().isEmpty()) {
                sb.append("Vehicle: ").append(vehicleInfo.trim()).append("\n");
            }
        }
        sb.append("\n");
        
        sb.append(formatReceiptLine("Item", "Qty", "Price", "Total", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        
        // Handle items with null safety
        if (sale.getItems() != null && !sale.getItems().isEmpty()) {
            for (SaleItem item : sale.getItems()) {
                if (item != null) {
                    String name = truncateString(getItemName(item), 20);
                    String qty = String.valueOf(item.getQuantity());
                    String price = formatCurrency(item.getUnitPrice());
                    String total = formatCurrency(item.getSubtotal());
                    sb.append(formatReceiptLine(name, qty, price, total, TEXT_RECEIPT_WIDTH)).append("\n");
                }
            }
        } else {
            sb.append(formatReceiptLine("No items", "0", "$0.00", "$0.00", TEXT_RECEIPT_WIDTH)).append("\n");
        }
        
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "Subtotal:", formatCurrency(sale.getSubtotal()), TEXT_RECEIPT_WIDTH)).append("\n");
        if (sale.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            String discountLabel = (sale.getDiscountReason() != null ? sale.getDiscountReason() : "Discount") + ":";
            sb.append(formatReceiptLine("", "", discountLabel, "-" + formatCurrency(sale.getDiscountAmount()), TEXT_RECEIPT_WIDTH)).append("\n");
        }
        sb.append(formatReceiptLine("", "", "Tax:", formatCurrency(sale.getTax()), TEXT_RECEIPT_WIDTH)).append("\n");

        // Credit Card Fee removed - handled by external card machine

        sb.append(formatReceiptLine("", "", "Total:", formatCurrency(sale.getTotal()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Payment information with null safety
        // Check for split payment in notes
        String notes = sale.getNotes();
        if (notes != null && notes.contains("SPLIT PAYMENT:")) {
            // Extract and display split payment info
            int startIdx = notes.indexOf("SPLIT PAYMENT:");
            int endIdx = notes.indexOf("\n\n", startIdx);
            String splitPaymentInfo = endIdx > 0 ? notes.substring(startIdx, endIdx) : notes.substring(startIdx);
            sb.append(splitPaymentInfo).append("\n");
        } else {
            String paymentMethod = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Unknown";
            sb.append("Payment Method: ").append(paymentMethod).append("\n");

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
        }
        sb.append("\n");
        sb.append(centerText("Thank you for your business!", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Please come again!", TEXT_RECEIPT_WIDTH)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Generate full-page receipt content matching the professional work order format
     * @param sale The sale to generate receipt for
     * @return Full-page receipt content as a string
     */
    private String generateFullPageReceiptContent(Sale sale) {
        if (sale == null) {
            LOGGER.warning("[PrinterService] Cannot generate receipt: sale is null");
            return "ERROR: Invalid sale data";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Use settingsService for company info with null safety
        String companyName = settingsService != null ? settingsService.getCompanyName() : "POS System";
        String companyAddress = settingsService != null ? settingsService.getCompanyAddress() : "";
        String companyPhone = settingsService != null ? settingsService.getCompanyPhone() : "";
        
        // Header section - Company name centered
        sb.append("\n");
        sb.append(centerText(companyName, FULL_PAGE_RECEIPT_WIDTH)).append("\n");
        sb.append("\n\n");
        
        // Top section layout - Company info on left, Work Order info on right
        String invoiceNumber = sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "NO-INVOICE";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy");
        String dateStr = sale.getTimestamp() != null ? sale.getTimestamp().format(dateFormatter) : 
                        java.time.LocalDateTime.now().format(dateFormatter);
        
        // Line 1: Company address on left, FINAL BILL or INVOICE on right
        String documentType = sale.isPaid() ? "FINAL BILL" : "INVOICE";
        sb.append(String.format("%-50s%30s",
            "Company/Location Name:", documentType)).append("\n");

        // Line 2: Address details and order number
        sb.append(String.format("%-50s%30s",
            companyAddress.length() > 40 ? companyAddress.substring(0, 40) : companyAddress,
            invoiceNumber)).append("\n");

        // Line 3: Company name and PO # (if present)
        String poDisplay = (sale.getPoNumber() != null && !sale.getPoNumber().trim().isEmpty())
            ? "PO#: " + sale.getPoNumber() : "";
        sb.append(String.format("%-50s%15s%15s",
            companyName, "CASHCUS", poDisplay)).append("\n");
        
        // Line 4: Phone and date
        sb.append(String.format("%-50s%15s%15s", 
            companyPhone, dateStr, "N/A")).append("\n");
        
        // Line 5: Empty line with N/A fields
        sb.append(String.format("%-50s%15s%15s", 
            "", "", "N/A")).append("\n");
        
        sb.append("\n");
        
        // Customer and Vehicle section
        sb.append("Billing Info:").append(String.format("%45s", "Vehicle:")).append("\n");
        
        // Customer details on left, Vehicle details on right
        String customerName = "";
        String customerAddress = "";
        String customerPhone = "";
        
        if (sale.getCustomer() != null) {
            customerName = sale.getCustomer().getFullName() != null ? sale.getCustomer().getFullName() : "";
            customerAddress = sale.getCustomer().getAddress() != null ? sale.getCustomer().getAddress() : "";
            customerPhone = sale.getCustomer().getPhone() != null ? sale.getCustomer().getPhone() : "";
        }
        
        // Vehicle details
        String license = "";
        String make = "";
        String vin = "";
        String year = "";
        String color = "";
        String model = "";
        
        if (sale.getVehicle() != null) {
            license = sale.getVehicle().getLicensePlate() != null ? sale.getVehicle().getLicensePlate() : "";
            make = sale.getVehicle().getMake() != null ? sale.getVehicle().getMake() : "";
            vin = sale.getVehicle().getVin() != null ? sale.getVehicle().getVin() : "";
            year = sale.getVehicle().getModelYear() > 0 ? String.valueOf(sale.getVehicle().getModelYear()) : "";
            color = sale.getVehicle().getColor() != null ? sale.getVehicle().getColor() : "";
            model = sale.getVehicle().getModel() != null ? sale.getVehicle().getModel() : "";
        }
        
        // Format customer and vehicle info side by side
        sb.append(String.format("%-35s%-15s%s", customerName, "License:", license)).append("\n");
        sb.append(String.format("%-35s%-15s%s", customerAddress, "Make:", make)).append("\n");
        sb.append(String.format("%-35s%-15s%s", customerPhone, "VIN #:", vin)).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Year:", year)).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Color:", color)).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Model:", model)).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Option:", "")).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Unit ID:", "")).append("\n");
        sb.append(String.format("%-35s%-15s%s", "", "Comment:", "")).append("\n");
        
        sb.append("\n");
        
        // Salesperson and Department line
        sb.append(String.format("Salesperson: %-30s Department: %s", 
            "System User", "01")).append("\n");
        sb.append("\n");
        
        // Items table header (removed Mfr Sku, expanded Description)
        sb.append(String.format("%-12s %-4s %-35s %-12s %3s %8s",
            "Tire Size", "Sub", "Description", "Tech", "Qty", "Price")).append("\n");
        
        // Separator line
        sb.append(repeatChar('-', FULL_PAGE_RECEIPT_WIDTH)).append("\n");
        
        // Items
        BigDecimal subtotalAmount = BigDecimal.ZERO;
        if (sale.getItems() != null && !sale.getItems().isEmpty()) {
            for (SaleItem item : sale.getItems()) {
                if (item != null) {
                    String itemName = getItemName(item);
                    String description = itemName.length() > 35 ? itemName.substring(0, 32) + "..." : itemName;
                    String qty = String.valueOf(item.getQuantity());
                    String price = String.format("$%.2f", item.getUnitPrice());
                    String extended = String.format("$%.2f", item.getSubtotal());

                    // Get tire size information
                    String tireSize = "";
                    if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                        Product product = item.getProduct();
                        if (product.getSize() != null && !product.getSize().isEmpty()) {
                            tireSize = product.getSize();
                        } else {
                            // Try to extract tire size from product name
                            String productName = product.getName();
                            if (productName != null) {
                                String[] words = productName.split("\\s+");
                                for (String word : words) {
                                    if (word.matches("\\d{3}/\\d{2}R?\\d{2}.*") ||
                                        word.matches("\\d{2}[x.]\\d{1,2}[.-]\\d{2}.*") ||
                                        word.matches("LT\\d{3}/\\d{2}R?\\d{2}.*")) {
                                        tireSize = word;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Get technician name for services
                    String techName = "";
                    if ("SERVICE".equals(item.getItemType()) && item.getTechnician() != null) {
                        techName = item.getTechnician().getFirstName() + " " + item.getTechnician().getLastName();
                    }

                    // Build the item row (without Mfr Sku)
                    String row = formatFullPageReceiptLine(tireSize, "", description, techName, qty, price);
                    sb.append(row).append("\n");

                    subtotalAmount = subtotalAmount.add(item.getSubtotal());
                }
            }
        }

        // Add several blank rows for additional items
        for (int i = 0; i < 6; i++) {
            String blankRow = formatFullPageReceiptLine("", "", "", "", "", "");
            sb.append(blankRow).append("\n");
        }
        
        sb.append("\n");
        
        // Disclaimer lines
        sb.append(String.format("%-25s%35s", "No disclaimer", "No disclaimer")).append("\n");
        sb.append("\n");
        
        // Totals section (right-aligned)
        sb.append(String.format("%60s %12s", "Sub Total:", String.format("$%.2f", sale.getSubtotal()))).append("\n");
        if (sale.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            String discountLabel = (sale.getDiscountReason() != null ? sale.getDiscountReason() : "Discount") + ":";
            sb.append(String.format("%60s %12s", discountLabel, String.format("-$%.2f", sale.getDiscountAmount()))).append("\n");
        }
        sb.append(String.format("%60s %12s", "Tax Total:", String.format("$%.2f", sale.getTax()))).append("\n");
        
        // Credit Card Fee removed - handled by external card machine
        
        sb.append(String.format("%60s %12s", "Total Due:", String.format("$%.2f", sale.getTotal()))).append("\n");
        sb.append("\n\n");
        
        // Payment section
        sb.append(String.format("%-25s%35s", "No disclaimer", "")).append("\n");
        sb.append("\n\n");
        
        // Payment method info
        sb.append(String.format("%-40s%40s", "Payments", "Amounts")).append("\n");

        // Check for split payment in notes
        String saleNotes = sale.getNotes();
        if (saleNotes != null && saleNotes.contains("SPLIT PAYMENT:")) {
            // Extract and display split payment info
            int startIdx = saleNotes.indexOf("SPLIT PAYMENT:");
            int endIdx = saleNotes.indexOf("\n\n", startIdx);
            String splitPaymentInfo = endIdx > 0 ? saleNotes.substring(startIdx, endIdx) : saleNotes.substring(startIdx);
            // Parse and display each payment line
            String[] lines = splitPaymentInfo.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("SPLIT")) {
                    sb.append(String.format("%-40s%40s", "SPLIT PAYMENT", "")).append("\n");
                } else if (line.trim().length() > 0) {
                    sb.append(String.format("%-80s", line.trim())).append("\n");
                }
            }
        } else {
            String paymentMethod = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Cash";
            sb.append(String.format("%-40s%40s", paymentMethod, String.format("$%.2f", sale.getTotal()))).append("\n");

            // Add payment details if card payment
            if (sale.getPaymentType() != null) {
                switch (sale.getPaymentType()) {
                    case CREDIT_CARD:
                    case DEBIT_CARD:
                        if (sale.getCardType() != null && !sale.getCardType().isEmpty()) {
                            sb.append(String.format("%-40s", "Card: " + sale.getCardType())).append("\n");
                        }
                        if (sale.getCardLastFour() != null && !sale.getCardLastFour().equals("XXXX") && !sale.getCardLastFour().isEmpty()) {
                            sb.append(String.format("%-40s", "****-****-****-" + sale.getCardLastFour())).append("\n");
                        }
                        break;
                    case CHECK:
                        if (sale.getCheckNumber() != null) {
                            sb.append(String.format("%-40s", "Check #: " + sale.getCheckNumber())).append("\n");
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        
        sb.append("\n\n\n\n");
        
        // Customer signature line
        sb.append("CUSTOMER SIGNATURE: X________________________________").append("\n");
        sb.append("\n");
        
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
     * Format a line for the full-page work order receipt
     * @param tireSize Tire size
     * @param sub Sub category
     * @param description Item description
     * @param tech Technician name
     * @param qty Quantity
     * @param price Unit price
     * @return Formatted line
     */
    private String formatFullPageReceiptLine(String tireSize, String sub, String description,
                                           String tech, String qty, String price) {
        // Fixed column widths - removed Mfr Sku, expanded Description to 35
        return String.format("%-12s %-4s %-35s %-12s %3s %8s",
            truncateString(tireSize != null ? tireSize : "", 12),
            truncateString(sub != null ? sub : "", 4),
            truncateString(description != null ? description : "", 35), 
            truncateString(tech != null ? tech : "", 12), 
            qty != null ? qty : "", 
            price != null ? price : "");
    }
    
    /**
     * Print a string to a printer
     * @param content Content to print
     * @param printerName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    private boolean printString(String content, String printerName) {
        LOGGER.info("[PrinterService] Attempting to print to: " + (printerName == null ? "System Default" : printerName));
        LOGGER.info("====== RECEIPT CONTENT (DEBUG) ======");
        LOGGER.info(content);
        LOGGER.info("=====================================");
        
        try {
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
                LOGGER.warning("[PrinterService] No printer found (selected: " + (printerName == null ? "System Default" : printerName) + "). Check printer configuration.");
                return false;
            }
            LOGGER.info("[PrinterService] Using printer: " + printService.getName());
            
            // Check if printer is available
            if (printService.getAttribute(javax.print.attribute.standard.PrinterState.class) == javax.print.attribute.standard.PrinterState.STOPPED) {
                LOGGER.warning("[PrinterService] Printer is stopped/offline: " + printService.getName());
                return false;
            }
            
            // Try different DocFlavors in order of preference
            DocFlavor[] flavorsToTry = {
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,           // Most compatible
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,          // Good for UTF-8 content
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,       // Basic ASCII
                DocFlavor.BYTE_ARRAY.AUTOSENSE,                 // Let printer decide
                DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,         // Stream-based fallback
                DocFlavor.INPUT_STREAM.AUTOSENSE                // Last resort
            };
            
            for (DocFlavor flavor : flavorsToTry) {
                if (printService.isDocFlavorSupported(flavor)) {
                    LOGGER.info("[PrinterService] Trying DocFlavor: " + flavor);
                    
                    if (attemptPrint(content, printService, flavor)) {
                        LOGGER.info("[PrinterService] Print successful with DocFlavor: " + flavor);
                        return true;
                    } else {
                        LOGGER.warning("[PrinterService] Print failed with DocFlavor: " + flavor + ", trying next...");
                    }
                } else {
                    LOGGER.fine("[PrinterService] DocFlavor not supported: " + flavor);
                }
            }
            
            LOGGER.warning("[PrinterService] All DocFlavor attempts failed for printer: " + printService.getName());
            return false;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[PrinterService] Unexpected error during printing: ", e);
            return false;
        }
    }
    
    /**
     * Attempt to print with a specific DocFlavor
     * @param content Content to print
     * @param printService The print service to use
     * @param flavor The DocFlavor to use
     * @return true if successful
     */
    private boolean attemptPrint(String content, PrintService printService, DocFlavor flavor) {
        try {
            Doc doc;
            
            // Prepare the document based on the DocFlavor type
            if (flavor.getRepresentationClassName().equals("[B")) {
                // Byte array flavors
                byte[] contentBytes;
                if (flavor.equals(DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8)) {
                    contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                } else if (flavor.equals(DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII)) {
                    contentBytes = content.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                } else {
                    // HOST or AUTOSENSE - use default encoding
                    contentBytes = content.getBytes();
                }
                doc = new SimpleDoc(contentBytes, flavor, null);
            } else {
                // Input stream flavors
                InputStream is = new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                doc = new SimpleDoc(is, flavor, null);
            }
            
            // Set up print attributes
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            
            // Create print job with better error handling
            DocPrintJob job = printService.createPrintJob();
            
            // Use a CountDownLatch to wait for print completion
            final java.util.concurrent.CountDownLatch printLatch = new java.util.concurrent.CountDownLatch(1);
            final boolean[] printSuccess = {false};
            final String[] errorMessage = {null};
            
            job.addPrintJobListener(new PrintJobAdapter() {
                @Override
                public void printJobCompleted(PrintJobEvent pje) {
                    LOGGER.info("[PrinterService] Print job completed successfully.");
                    printSuccess[0] = true;
                    printLatch.countDown();
                }
                
                @Override
                public void printJobFailed(PrintJobEvent pje) {
                    LOGGER.warning("[PrinterService] Print job FAILED.");
                    errorMessage[0] = "Print job failed";
                    printSuccess[0] = false;
                    printLatch.countDown();
                }
                
                @Override
                public void printJobCanceled(PrintJobEvent pje) {
                    LOGGER.info("[PrinterService] Print job was canceled.");
                    errorMessage[0] = "Print job canceled";
                    printSuccess[0] = false;
                    printLatch.countDown();
                }
                
                @Override
                public void printJobNoMoreEvents(PrintJobEvent pje) {
                    LOGGER.info("[PrinterService] Print job: no more events.");
                    // Only count down if we haven't already finished
                    if (printLatch.getCount() > 0) {
                        printSuccess[0] = true; // Assume success if no explicit failure
                        printLatch.countDown();
                    }
                }
                
                @Override
                public void printDataTransferCompleted(PrintJobEvent pje) {
                    LOGGER.info("[PrinterService] Print data transfer completed.");
                }
            });

            // Submit the print job
            job.print(doc, attrs);
            LOGGER.info("[PrinterService] Print job submitted to printer queue.");
            
            // Wait for print job to complete (with timeout)
            try {
                boolean completed = printLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    LOGGER.warning("[PrinterService] Print job timed out after 30 seconds.");
                    return false;
                }
                
                if (!printSuccess[0]) {
                    LOGGER.warning("[PrinterService] Print job failed: " + (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
                    return false;
                }
                
                return true;
                
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "[PrinterService] Print job interrupted: ", e);
                Thread.currentThread().interrupt();
                return false;
            }
            
        } catch (PrintException e) {
            LOGGER.log(Level.WARNING, "[PrinterService] PrintException with DocFlavor " + flavor + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[PrinterService] Exception with DocFlavor " + flavor + ": " + e.getMessage());
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

    private void setPdfFont(PDPageContentStream stream, PDFont font, float size) throws IOException {
        this.currentPdfFont = font;
        this.currentPdfFontSize = size;
        stream.setFont(font, size);
    }

    public boolean generateReceiptPdf(Sale sale, String filePath) {
        LOGGER.info("[PrinterService] Attempting to generate PDF receipt for sale ID: " + sale.getId() + " to path: " + filePath);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = null; 

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin; // Top of the content area
            float currentY = yStart; // Current Y position for drawing, starts at top
            float leading = 14.5f;
            float tableCellPadding = 5f;
            float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
            float logoAreaHeight = 0f; // Keep track of space used by logo

            try {
                contentStream = new PDPageContentStream(document, page);
                LOGGER.fine("[PrinterService] PDF content stream opened.");

                // Company Logo (Top-Right)
                String logoPath = settingsService.getCompanyLogoPath();
                if (logoPath != null && !logoPath.isEmpty()) {
                    LOGGER.fine("[PrinterService] Attempting to load logo from: " + logoPath);
                    try {
                        File logoFile = null;
                        if (logoPath.startsWith("file:")) {
                           logoFile = new File(new URL(logoPath).toURI());
                        } else if (new File(logoPath).exists()) { 
                            logoFile = new File(logoPath);
                        } else {
                            LOGGER.warning("[PrinterService] Logo path is not a valid file or URI: " + logoPath);
                        }
                        
                        if (logoFile != null && logoFile.exists()) {
                            PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(logoFile, document);
                            float logoDisplayWidth = 100; 
                            float scale = logoDisplayWidth / pdImage.getWidth();
                            float logoDisplayHeight = pdImage.getHeight() * scale;
                            
                            float logoX = page.getMediaBox().getWidth() - margin - logoDisplayWidth;
                            float logoY = yStart - logoDisplayHeight; // Align top of logo with yStart

                            contentStream.drawImage(pdImage, logoX, logoY, logoDisplayWidth, logoDisplayHeight);
                            logoAreaHeight = logoDisplayHeight + 20f; // Height of logo + padding
                            LOGGER.fine("[PrinterService] Logo drawn successfully at top-right.");
                        } else if (logoFile != null) { 
                            LOGGER.warning("[PrinterService] Logo file not found at resolved path: " + logoFile.getAbsolutePath());
                        }
                    } catch (Exception e) { // Catching a broader range of exceptions for logo
                         LOGGER.log(Level.WARNING, "[PrinterService] Error processing logo: " + logoPath, e);
                    }
                } else {
                    LOGGER.fine("[PrinterService] No logo path configured.");
                }

                // Adjust currentY to start below the logo area if a logo was drawn, otherwise it stays at yStart
                // This ensures company info starts below the logo area regardless of logo height.
                currentY = yStart - logoAreaHeight; 

                // Company Info - drawn starting at currentY
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(settingsService.getCompanyName()); contentStream.endText(); currentY -= leading;
                
                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(settingsService.getCompanyAddress()); contentStream.endText(); currentY -= leading;
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText(settingsService.getCompanyPhone()); contentStream.endText(); currentY -= (leading * 2);
                
                // Sale Details
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 11);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("RECEIPT"); contentStream.endText(); currentY -= (leading * 1.5f);

                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Invoice #: " + sale.getInvoiceNumber()); contentStream.endText(); currentY -= leading;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Date: " + sale.getTimestamp().format(formatter)); contentStream.endText(); currentY -= leading;
                if (sale.getCustomer() != null) { contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Customer: " + sale.getCustomer().getFullName()); contentStream.endText(); currentY -= leading; }
                if (sale.getVehicle() != null) { contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Vehicle: " + sale.getVehicle().getMake() + " " + sale.getVehicle().getModel() + " (" + sale.getVehicle().getLicensePlate() + ")"); contentStream.endText(); currentY -= leading; }
                currentY -= (leading * 1.5f);
                float[] columnWidths = {tableWidth * 0.45f, tableWidth * 0.15f, tableWidth * 0.20f, tableWidth * 0.20f};
                String[] headers = {"Item", "Qty", "Unit Price", "Total"};
                float currentX = margin;
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 10);
                for (int i = 0; i < headers.length; i++) { contentStream.beginText(); contentStream.newLineAtOffset(currentX + tableCellPadding, currentY); contentStream.showText(headers[i]); contentStream.endText(); currentX += columnWidths[i]; }
                currentY -= leading;
                contentStream.moveTo(margin, currentY + (leading / 2) - tableCellPadding); contentStream.lineTo(margin + tableWidth, currentY + (leading / 2) - tableCellPadding); contentStream.stroke(); currentY -= (leading * 0.5f);
                setPdfFont(contentStream, PDType1Font.HELVETICA, 9);
                for (SaleItem item : sale.getItems()) {
                    if (currentY < margin + 50) { 
                        contentStream.close(); 
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page); 
                        setPdfFont(contentStream, PDType1Font.HELVETICA, 9); 
                        currentY = yStart; // Reset Y for new page (header might be redrawn below)
                        LOGGER.fine("[PrinterService] New PDF page added for items.");
                        // Optionally re-draw headers
                        currentX = margin;
                        setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 10);
                        for (int i = 0; i < headers.length; i++) { contentStream.beginText(); contentStream.newLineAtOffset(currentX + tableCellPadding, currentY); contentStream.showText(headers[i]); contentStream.endText(); currentX += columnWidths[i]; }
                        currentY -= leading;
                        contentStream.moveTo(margin, currentY + (leading / 2) - tableCellPadding); contentStream.lineTo(margin + tableWidth, currentY + (leading / 2) - tableCellPadding); contentStream.stroke(); currentY -= (leading * 0.5f);
                        setPdfFont(contentStream, PDType1Font.HELVETICA, 9); 
                    }
                    currentX = margin;
                    String[] rowData = { truncateString(getItemName(item), 40), String.valueOf(item.getQuantity()), formatCurrency(item.getUnitPrice()), formatCurrency(item.getSubtotal()) };
                    for (int i = 0; i < rowData.length; i++) { contentStream.beginText(); contentStream.newLineAtOffset(currentX + tableCellPadding, currentY); contentStream.showText(rowData[i]); contentStream.endText(); currentX += columnWidths[i]; }
                    currentY -= leading;
                }
                currentY -= (leading * 0.5f);
                contentStream.moveTo(margin, currentY + (leading / 2) - tableCellPadding); contentStream.lineTo(margin + tableWidth, currentY + (leading / 2) - tableCellPadding); contentStream.stroke(); currentY -= leading;
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 10);
                drawRightAlignedText(contentStream, "Subtotal:", formatCurrency(sale.getSubtotal()), margin + tableWidth - columnWidths[2] - columnWidths[3], currentY, columnWidths[2], columnWidths[3], tableCellPadding, currentPdfFont, currentPdfFontSize); currentY -= leading;
                if (sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) { String discLabel = (sale.getDiscountReason() != null ? sale.getDiscountReason() : "Discount") + ":"; drawRightAlignedText(contentStream, discLabel, "-" + formatCurrency(sale.getDiscountAmount()), margin + tableWidth - columnWidths[2] - columnWidths[3], currentY, columnWidths[2], columnWidths[3], tableCellPadding, currentPdfFont, currentPdfFontSize); currentY -= leading; }
                drawRightAlignedText(contentStream, "Tax:", formatCurrency(sale.getTax()), margin + tableWidth - columnWidths[2] - columnWidths[3], currentY, columnWidths[2], columnWidths[3], tableCellPadding, currentPdfFont, currentPdfFontSize); currentY -= leading;
                if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) { drawRightAlignedText(contentStream, "CC Fee:", formatCurrency(sale.getCreditCardFeeAmount()), margin + tableWidth - columnWidths[2] - columnWidths[3], currentY, columnWidths[2], columnWidths[3], tableCellPadding, currentPdfFont, currentPdfFontSize); currentY -= leading; }
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 11);
                drawRightAlignedText(contentStream, "Total:", formatCurrency(sale.getTotal()), margin + tableWidth - columnWidths[2] - columnWidths[3], currentY, columnWidths[2], columnWidths[3], tableCellPadding, currentPdfFont, currentPdfFontSize); currentY -= (leading * 2);
                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText(); contentStream.newLineAtOffset(margin, currentY); contentStream.showText("Payment Method: " + sale.getPaymentMethod()); contentStream.endText(); currentY -= leading;
                currentY -= (leading * 2);
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 10);
                contentStream.beginText(); float footerWidth = this.currentPdfFont.getStringWidth("Thank you for your business!") / 1000 * this.currentPdfFontSize; contentStream.newLineAtOffset((page.getMediaBox().getWidth() - footerWidth) / 2, currentY); contentStream.showText("Thank you for your business!"); contentStream.endText();
                LOGGER.fine("[PrinterService] PDF content generation complete.");

            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "[PrinterService] IOException during PDF content stream operations.", ioe);
                return false; 
            } finally {
                if (contentStream != null) {
                    try {
                        contentStream.close();
                        LOGGER.fine("[PrinterService] PDF content stream closed.");
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "[PrinterService] Failed to close PDF content stream.", e);
                    }
                }
            }
            document.save(filePath);
            LOGGER.info("[PrinterService] PDF receipt saved successfully to: " + filePath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrinterService] Error generating or saving PDF document: " + filePath, e);
            return false;
        }
    }
    
    private void drawRightAlignedText(PDPageContentStream contentStream, String label, String value, float xStart, float y, float labelWidth, float valueWidth, float padding, PDFont font, float fontSize) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(xStart + padding, y);
        contentStream.showText(label);
        contentStream.endText();

        float valueActualWidth = font.getStringWidth(value) / 1000 * fontSize;
        contentStream.beginText();
        contentStream.newLineAtOffset(xStart + labelWidth + valueWidth - valueActualWidth - padding, y);
        contentStream.showText(value);
        contentStream.endText();
    }

    /**
     * Print a test page to verify printer functionality
     * @param targetPrinterName Optional printer name, null for default printer
     * @return true if printed successfully
     */
    public boolean printTestPage(String targetPrinterName) {
        LOGGER.info("[PrinterService] Printing test page to verify printer functionality");
        
        StringBuilder testContent = new StringBuilder();
        testContent.append(centerText("PRINTER TEST PAGE", TEXT_RECEIPT_WIDTH)).append("\n");
        testContent.append(centerText("=================", TEXT_RECEIPT_WIDTH)).append("\n");
        testContent.append("\n");
        testContent.append("Date: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        testContent.append("Printer: ").append(targetPrinterName != null ? targetPrinterName : "System Default").append("\n");
        testContent.append("\n");
        testContent.append("This is a test print to verify your").append("\n");
        testContent.append("POS system printer is working correctly.").append("\n");
        testContent.append("\n");
        testContent.append("If you can see this message, your").append("\n");
        testContent.append("printer is functioning properly!").append("\n");
        testContent.append("\n");
        testContent.append(centerText("Test Complete", TEXT_RECEIPT_WIDTH)).append("\n");
        testContent.append(centerText("=============", TEXT_RECEIPT_WIDTH)).append("\n");
        
        String printerToUse = (targetPrinterName != null) ? targetPrinterName : this.defaultPrinterName;
        return printString(testContent.toString(), printerToUse);
    }

    /**
     * Get diagnostic information about a printer's supported DocFlavors
     * @param printerName The printer name to diagnose
     * @return Diagnostic string with supported flavors
     */
    public String getPrinterDiagnostics(String printerName) {
        StringBuilder diagnostics = new StringBuilder();
        try {
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
                return "No printer found: " + (printerName != null ? printerName : "System Default");
            }
            
            diagnostics.append("Printer: ").append(printService.getName()).append("\n");
            diagnostics.append("Supported DocFlavors:\n");
            
            DocFlavor[] supportedFlavors = printService.getSupportedDocFlavors();
            for (DocFlavor flavor : supportedFlavors) {
                diagnostics.append("  - ").append(flavor.toString()).append("\n");
            }
            
            // Check specific text flavors
            DocFlavor[] textFlavors = {
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
                DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,
                DocFlavor.BYTE_ARRAY.AUTOSENSE,
                DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,
                DocFlavor.INPUT_STREAM.AUTOSENSE
            };
            
            diagnostics.append("\nText DocFlavor Support:\n");
            for (DocFlavor flavor : textFlavors) {
                boolean supported = printService.isDocFlavorSupported(flavor);
                diagnostics.append("  ").append(supported ? "✓" : "✗").append(" ").append(flavor.toString()).append("\n");
            }
            
        } catch (Exception e) {
            diagnostics.append("Error getting diagnostics: ").append(e.getMessage());
        }
        
        return diagnostics.toString();
    }

    /**
     * Generate a PDF receipt in the professional work order format
     * @param sale The sale to generate PDF for
     * @param filePath Path where to save the PDF
     * @return true if generated successfully
     */
    public boolean generateWorkOrderPdf(Sale sale, String filePath) {
        LOGGER.info("[PrinterService] Attempting to generate work order PDF for sale ID: " + sale.getId() + " to path: " + filePath);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                
                // Get the text content from our work order format method
                String workOrderContent = generateFullPageReceiptContent(sale);
                
                // Set up PDF positioning
                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float currentY = yStart;
                float leading = 12f;
                
                // Set font
                contentStream.setFont(PDType1Font.COURIER, 10);
                
                // Split the content into lines and draw each line
                String[] lines = workOrderContent.split("\n");
                
                for (String line : lines) {
                    // Check if we need a new page
                    if (currentY < margin + 20) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        // Note: contentStream is closed, so we'd need to create a new one
                        // For simplicity, we'll just continue on the same page for now
                        currentY = yStart;
                    }
                    
                    // Draw the text line
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, currentY);
                    contentStream.showText(line);
                    contentStream.endText();
                    
                    currentY -= leading;
                }
                
                LOGGER.fine("[PrinterService] Work order PDF content generation complete.");
                
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "[PrinterService] Error creating PDF content stream", e);
                return false;
            }
            
            // Save the document
            document.save(filePath);
            LOGGER.info("[PrinterService] Work order PDF saved successfully to: " + filePath);
            return true;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[PrinterService] Error generating work order PDF: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Print a return/void receipt showing negative amounts
     * @param sale The voided sale to print return receipt for
     * @return true if printed successfully
     */
    public boolean printReturnReceipt(Sale sale) {
        String receipt = generateReturnReceipt(sale);
        return printString(receipt, defaultPrinterName);
    }
    
    /**
     * Generate return receipt text with negative amounts
     * @param sale The voided sale
     * @return Receipt text
     */
    private String generateReturnReceipt(Sale sale) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append(centerText("*** RETURN / VOID RECEIPT ***", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyName(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyAddress(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyPhone(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(repeatChar('=', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Return information
        sb.append("RETURN FOR ORIGINAL SALE\n");
        sb.append("Invoice #: ").append(sale.getInvoiceNumber()).append("\n");
        sb.append("Void Date: ").append(sale.getVoidTimestamp() != null ? 
                sale.getVoidTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) : 
                "").append("\n");
        sb.append("Original Date: ").append(sale.getTimestamp() != null ? 
                sale.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) : 
                "").append("\n");
        sb.append("Reason: ").append(sale.getVoidReason() != null ? sale.getVoidReason() : "N/A").append("\n");
        
        if (sale.getCustomer() != null) {
            sb.append("Customer: ").append(sale.getCustomer().getFullName()).append("\n");
        }
        
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Items (with negative quantities/amounts)
        if (!sale.getItems().isEmpty()) {
            sb.append("ITEMS RETURNED:\n");
            sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
            
            for (SaleItem item : sale.getItems()) {
                String itemName = truncateString(item.getItemName(), 20);
                String qty = "-" + item.getQuantity(); // Negative quantity for returns
                String price = formatCurrency(item.getUnitPrice().negate()); // Negative price
                String subtotal = formatCurrency(item.getSubtotal().negate()); // Negative subtotal
                
                sb.append(formatReceiptLine(itemName, qty, price, subtotal, TEXT_RECEIPT_WIDTH)).append("\n");
            }
        } else {
            sb.append(formatReceiptLine("No items", "0", "$0.00", "$0.00", TEXT_RECEIPT_WIDTH)).append("\n");
        }
        
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        
        // Totals (all negative)
        sb.append(formatReceiptLine("", "", "Subtotal:", formatCurrency(sale.getSubtotal().negate()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "Tax:", formatCurrency(sale.getTax().negate()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(formatReceiptLine("", "", "REFUND TOTAL:", formatCurrency(sale.getTotal().negate()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Refund information
        sb.append("Original Payment: ").append(sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "N/A").append("\n");
        sb.append("\n");
        sb.append(centerText("*** CUSTOMER REFUND ***", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Amount: " + formatCurrency(sale.getTotal()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Items returned to inventory notice
        sb.append(centerText("All items returned to inventory", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Footer
        sb.append(repeatChar('=', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Thank you for your business", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n\n\n");
        
        return sb.toString();
    }
    
    /**
     * Print partial return receipt for selected items
     */
    public boolean printPartialReturnReceipt(Sale sale, List<com.tireshop.controller.SalesController.ReturnItem> returnedItems, BigDecimal refundAmount) {
        String receipt = generatePartialReturnReceipt(sale, returnedItems, refundAmount);
        return printString(receipt, defaultPrinterName);
    }
    
    /**
     * Print partial return receipt to specific printer
     */
    public boolean printPartialReturnReceipt(Sale sale, List<com.tireshop.controller.SalesController.ReturnItem> returnedItems, BigDecimal refundAmount, String printerName) {
        String receipt = generatePartialReturnReceipt(sale, returnedItems, refundAmount);
        return printString(receipt, printerName);
    }
    
    /**
     * Generate partial return receipt as PDF
     */
    public boolean generatePartialReturnPdf(Sale sale, List<com.tireshop.controller.SalesController.ReturnItem> returnedItems, BigDecimal refundAmount, String filePath) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float currentY = yStart;
                float leading = 14.5f;
                
                // Header - PARTIAL RETURN in bold
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText("*** PARTIAL RETURN RECEIPT ***");
                contentStream.endText();
                currentY -= (leading * 2);
                
                // Company info
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(settingsService.getCompanyName());
                contentStream.endText();
                currentY -= leading;
                
                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(settingsService.getCompanyAddress());
                contentStream.endText();
                currentY -= leading;
                
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(settingsService.getCompanyPhone());
                contentStream.endText();
                currentY -= (leading * 2);
                
                // Invoice details
                setPdfFont(contentStream, PDType1Font.HELVETICA, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText("Original Invoice: " + sale.getInvoiceNumber());
                contentStream.endText();
                currentY -= leading;
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText("Return Date: " + java.time.LocalDateTime.now().format(formatter));
                contentStream.endText();
                currentY -= leading;
                
                if (sale.getCustomer() != null) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, currentY);
                    contentStream.showText("Customer: " + sale.getCustomer().getFullName());
                    contentStream.endText();
                    currentY -= leading;
                }
                
                currentY -= leading;
                
                // Items returned header
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText("ITEMS RETURNED:");
                contentStream.endText();
                currentY -= (leading * 1.5f);
                
                // Table for returned items
                setPdfFont(contentStream, PDType1Font.HELVETICA, 9);
                for (com.tireshop.controller.SalesController.ReturnItem returnItem : returnedItems) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, currentY);
                    contentStream.showText(returnItem.item.getItemName());
                    contentStream.endText();
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 300, currentY);
                    contentStream.showText("Qty: -" + returnItem.quantity);
                    contentStream.endText();
                    
                    BigDecimal lineTotal = returnItem.item.getUnitPrice().multiply(BigDecimal.valueOf(returnItem.quantity));
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 400, currentY);
                    contentStream.showText("-$" + lineTotal);
                    contentStream.endText();
                    
                    currentY -= leading;
                }
                
                currentY -= leading;
                
                // Refund total
                setPdfFont(contentStream, PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin + 300, currentY);
                contentStream.showText("REFUND TOTAL: -$" + refundAmount);
                contentStream.endText();
                currentY -= (leading * 2);
                
                // Footer note
                setPdfFont(contentStream, PDType1Font.HELVETICA, 9);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText("All items have been returned to inventory.");
                contentStream.endText();
            }
            
            document.save(filePath);
            System.out.println("✅ Partial return PDF generated: " + filePath);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error generating partial return PDF: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generate partial return receipt text
     */
    private String generatePartialReturnReceipt(Sale sale, List<com.tireshop.controller.SalesController.ReturnItem> returnedItems, BigDecimal refundAmount) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append(centerText("*** PARTIAL RETURN RECEIPT ***", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyName(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyAddress(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText(settingsService.getCompanyPhone(), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(repeatChar('=', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Return information
        sb.append("PARTIAL RETURN\n");
        sb.append("Original Invoice: ").append(sale.getInvoiceNumber()).append("\n");
        sb.append("Return Date: ").append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"))).append("\n");
        
        if (sale.getCustomer() != null) {
            sb.append("Customer: ").append(sale.getCustomer().getFullName()).append("\n");
        }
        
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Items being returned (with negative amounts)
        sb.append("ITEMS RETURNED:\n");
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        
        for (com.tireshop.controller.SalesController.ReturnItem returnItem : returnedItems) {
            String itemName = truncateString(returnItem.item.getItemName(), 20);
            String qty = "-" + returnItem.quantity; // Negative for return
            String price = formatCurrency(returnItem.item.getUnitPrice().negate());
            BigDecimal lineTotal = returnItem.item.getUnitPrice().multiply(BigDecimal.valueOf(returnItem.quantity));
            String subtotal = formatCurrency(lineTotal.negate());
            
            sb.append(formatReceiptLine(itemName, qty, price, subtotal, TEXT_RECEIPT_WIDTH)).append("\n");
        }
        
        sb.append(repeatChar('-', TEXT_RECEIPT_WIDTH)).append("\n");
        
        // Refund total
        sb.append(formatReceiptLine("", "", "REFUND TOTAL:", formatCurrency(refundAmount.negate()), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        sb.append(centerText("*** CUSTOMER REFUND ***", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Amount: $" + String.format("%.2f", refundAmount), TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        sb.append(centerText("Items returned to inventory", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Original invoice remains on file", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n");
        
        // Footer
        sb.append(repeatChar('=', TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append(centerText("Thank you", TEXT_RECEIPT_WIDTH)).append("\n");
        sb.append("\n\n\n");
        
        return sb.toString();
    }
} 