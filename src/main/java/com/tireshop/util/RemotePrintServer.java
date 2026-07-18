package com.tireshop.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Remote Print Server - Runs on the POS computer to handle print requests from client computers
 */
public class RemotePrintServer {
    
    private static final Logger LOGGER = Logger.getLogger(RemotePrintServer.class.getName());
    private static final int DEFAULT_PORT = 8080;
    private HttpServer server;
    private boolean isRunning = false;
    
    /**
     * Start the print server
     * @param port Port to listen on
     */
    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/print", new PrintHandler());
            server.createContext("/status", new StatusHandler());
            server.setExecutor(Executors.newFixedThreadPool(4)); // Handle up to 4 concurrent requests
            server.start();
            
            isRunning = true;
            LOGGER.info("[RemotePrintServer] Print server started on port " + port);
            LOGGER.info("[RemotePrintServer] Ready to receive print jobs from client computers");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[RemotePrintServer] Failed to start print server: ", e);
        }
    }
    
    /**
     * Stop the print server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
            LOGGER.info("[RemotePrintServer] Print server stopped");
        }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * HTTP handler for print requests
     */
    private static class PrintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LOGGER.info("[RemotePrintServer] Received print request from: " + exchange.getRemoteAddress());
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read the request body
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                // Parse simple JSON (basic parsing without external libraries)
                String jsonContent = requestBody.toString();
                String content = extractJsonValue(jsonContent, "content");
                String printer = extractJsonValue(jsonContent, "printer");
                
                if (content == null) {
                    sendResponse(exchange, 400, "Missing content in request");
                    return;
                }
                
                // Unescape the content
                content = content.replace("\\n", "\n").replace("\\\"", "\"");
                
                LOGGER.info("[RemotePrintServer] Processing print job for printer: " + (printer != null ? printer : "default"));
                
                // Print the content
                boolean success = printContent(content, printer);
                
                if (success) {
                    sendResponse(exchange, 200, "Print job completed successfully");
                } else {
                    sendResponse(exchange, 500, "Print job failed");
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[RemotePrintServer] Error processing print request: ", e);
                sendResponse(exchange, 500, "Error processing print request: " + e.getMessage());
            }
        }
        
        /**
         * Extract value from simple JSON string
         */
        private String extractJsonValue(String json, String key) {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;
            
            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;
            
            return json.substring(startIndex, endIndex);
        }
        
        /**
         * Send HTTP response
         */
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        
        /**
         * Print content to the local printer
         */
        private boolean printContent(String content, String printerName) {
            try {
                // Find printer
                PrintService printService;
                if (printerName != null && !"default".equals(printerName)) {
                    PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                    PrintService targetService = null;
                    for (PrintService service : services) {
                        if (service.getName().equalsIgnoreCase(printerName)) {
                            targetService = service;
                            break;
                        }
                    }
                    printService = targetService != null ? targetService : PrintServiceLookup.lookupDefaultPrintService();
                } else {
                    printService = PrintServiceLookup.lookupDefaultPrintService();
                }
                
                if (printService == null) {
                    LOGGER.warning("[RemotePrintServer] No printer found");
                    return false;
                }
                
                LOGGER.info("[RemotePrintServer] Using printer: " + printService.getName());
                
                // Try different DocFlavors (same logic as PrinterService)
                DocFlavor[] flavorsToTry = {
                    DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
                    DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
                    DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,
                    DocFlavor.BYTE_ARRAY.AUTOSENSE,
                    DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,
                    DocFlavor.INPUT_STREAM.AUTOSENSE
                };
                
                for (DocFlavor flavor : flavorsToTry) {
                    if (printService.isDocFlavorSupported(flavor)) {
                        try {
                            Doc doc;
                            if (flavor.getRepresentationClassName().equals("[B")) {
                                // Byte array flavors
                                byte[] contentBytes = content.getBytes();
                                doc = new SimpleDoc(contentBytes, flavor, null);
                            } else {
                                // Input stream flavors
                                InputStream is = new ByteArrayInputStream(content.getBytes());
                                doc = new SimpleDoc(is, flavor, null);
                            }
                            
                            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
                            attrs.add(new Copies(1));
                            
                            DocPrintJob job = printService.createPrintJob();
                            job.print(doc, attrs);
                            
                            LOGGER.info("[RemotePrintServer] Print job submitted successfully with DocFlavor: " + flavor);
                            return true;
                            
                        } catch (PrintException e) {
                            LOGGER.warning("[RemotePrintServer] Failed with DocFlavor " + flavor + ": " + e.getMessage());
                            // Continue to next flavor
                        }
                    }
                }
                
                LOGGER.warning("[RemotePrintServer] All DocFlavor attempts failed");
                return false;
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[RemotePrintServer] Error printing: ", e);
                return false;
            }
        }
    }
    
    /**
     * HTTP handler for status requests
     */
    private static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Print Server is running";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Main method to run the print server standalone
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }
        
        RemotePrintServer server = new RemotePrintServer();
        server.start(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        System.out.println("Remote Print Server started on port " + port);
        System.out.println("Press Ctrl+C to stop the server");
        
        // Keep the server running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.stop();
        }
    }
} 