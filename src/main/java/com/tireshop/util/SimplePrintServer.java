package com.tireshop.util;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple Print Server - Java 8 compatible version for POS computer
 * Receives HTTP print requests and prints them locally
 */
public class SimplePrintServer {
    
    private static final int DEFAULT_PORT = 8080;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService threadPool;
    
    /**
     * Start the print server
     */
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newFixedThreadPool(4);
            isRunning = true;
            
            System.out.println("[SimplePrintServer] Print server started on port " + port);
            System.out.println("[SimplePrintServer] Ready to receive print jobs from client computers");
            
            // Accept connections in a loop
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new PrintRequestHandler(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("[SimplePrintServer] Error accepting connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[SimplePrintServer] Failed to start print server: " + e.getMessage());
        }
    }
    
    /**
     * Stop the print server
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            System.out.println("[SimplePrintServer] Print server stopped");
        } catch (IOException e) {
            System.err.println("[SimplePrintServer] Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * Handler for individual print requests
     */
    private static class PrintRequestHandler implements Runnable {
        private final Socket clientSocket;
        
        public PrintRequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                System.out.println("[SimplePrintServer] Received connection from: " + clientSocket.getRemoteSocketAddress());
                
                // Read HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) {
                    sendHttpResponse(out, 400, "Bad Request");
                    return;
                }
                
                System.out.println("[SimplePrintServer] Request: " + requestLine);
                
                // Handle different endpoints
                if (requestLine.startsWith("GET /status")) {
                    sendHttpResponse(out, 200, "Print Server is running");
                    return;
                }
                
                if (!requestLine.startsWith("POST /print")) {
                    sendHttpResponse(out, 404, "Not Found");
                    return;
                }
                
                // Read headers to find content length
                String line;
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
                
                // Read request body
                StringBuilder body = new StringBuilder();
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int read = in.read(buffer, totalRead, contentLength - totalRead);
                        if (read == -1) break;
                        totalRead += read;
                    }
                    body.append(buffer, 0, totalRead);
                }
                
                String jsonContent = body.toString();
                System.out.println("[SimplePrintServer] Received JSON: " + jsonContent);
                
                // Parse JSON manually (simple parsing)
                String content = extractJsonValue(jsonContent, "content");
                String printer = extractJsonValue(jsonContent, "printer");
                
                if (content == null) {
                    sendHttpResponse(out, 400, "Missing content in request");
                    return;
                }
                
                // Unescape content
                content = content.replace("\\n", "\n").replace("\\\"", "\"");
                
                // Print the content
                boolean success = printContent(content, printer);
                
                if (success) {
                    sendHttpResponse(out, 200, "Print job completed successfully");
                } else {
                    sendHttpResponse(out, 500, "Print job failed");
                }
                
            } catch (Exception e) {
                System.err.println("[SimplePrintServer] Error handling request: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("[SimplePrintServer] Error closing client socket: " + e.getMessage());
                }
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
        private void sendHttpResponse(PrintWriter out, int statusCode, String response) {
            out.println("HTTP/1.1 " + statusCode + " " + getStatusText(statusCode));
            out.println("Content-Type: text/plain");
            out.println("Content-Length: " + response.length());
            out.println("Connection: close");
            out.println();
            out.println(response);
            out.flush();
        }
        
        /**
         * Get HTTP status text
         */
        private String getStatusText(int statusCode) {
            switch (statusCode) {
                case 200: return "OK";
                case 400: return "Bad Request";
                case 404: return "Not Found";
                case 500: return "Internal Server Error";
                default: return "Unknown";
            }
        }
        
        /**
         * Print content to the local printer
         */
        private boolean printContent(String content, String printerName) {
            try {
                System.out.println("[SimplePrintServer] Processing print job for printer: " + (printerName != null ? printerName : "default"));
                
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
                    System.err.println("[SimplePrintServer] No printer found");
                    return false;
                }
                
                System.out.println("[SimplePrintServer] Using printer: " + printService.getName());
                
                // Try different DocFlavors for compatibility
                DocFlavor[] flavorsToTry = {
                    DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
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
                            
                            System.out.println("[SimplePrintServer] Print job submitted successfully with DocFlavor: " + flavor);
                            return true;
                            
                        } catch (PrintException e) {
                            System.err.println("[SimplePrintServer] Failed with DocFlavor " + flavor + ": " + e.getMessage());
                            // Continue to next flavor
                        }
                    }
                }
                
                System.err.println("[SimplePrintServer] All DocFlavor attempts failed");
                return false;
                
            } catch (Exception e) {
                System.err.println("[SimplePrintServer] Error printing: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
    
    /**
     * Main method to run the print server
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
        
        final SimplePrintServer server = new SimplePrintServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.stop();
            }
        }));
        
        System.out.println("Simple Print Server starting on port " + port);
        System.out.println("This server is compatible with Java 8+");
        System.out.println("Press Ctrl+C to stop the server");
        System.out.println();
        
        server.start(port);
    }
} 