package com.tireshop.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tireshop.model.Product;
import com.tireshop.service.InventoryService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * A simple HTTP server that provides a web interface for scanning barcodes with a mobile device
 */
public class ScannerServer {
    private static final Logger LOGGER = Logger.getLogger(ScannerServer.class.getName());
    private static final int DEFAULT_PORT = 8080;
    private final HttpServer server;
    private final InventoryService inventoryService;

    public ScannerServer(InventoryService inventoryService) throws IOException {
        this.inventoryService = inventoryService;
        server = HttpServer.create(new InetSocketAddress(DEFAULT_PORT), 0);
        
        // Set up routes
        server.createContext("/", new MainHandler());
        server.createContext("/scan", new ScanHandler());
        server.createContext("/api/product", new ProductApiHandler());
        
        server.setExecutor(null); // Use the default executor
    }
    
    public void start() {
        server.start();
        LOGGER.info("Scanner server started on port " + DEFAULT_PORT);
        LOGGER.info("Access the scanner interface from your mobile device at http://[your-computer-ip]:" + DEFAULT_PORT);
    }
    
    public void stop() {
        server.stop(0);
        LOGGER.info("Scanner server stopped");
    }
    
    /**
     * Handle CORS preflight requests
     */
    private boolean handleCorsPreflightRequest(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
    
    /**
     * Handler for the main scanner interface
     */
    class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (handleCorsPreflightRequest(exchange)) {
                return;
            }
            
            String response = getScannerHtml();
            sendResponse(exchange, response);
        }
        
        private String getScannerHtml() {
            // In a real application, this would be a proper HTML file
            return "<!DOCTYPE html>\n" +
                   "<html>\n" +
                   "<head>\n" +
                   "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                   "    <title>Tire Shop Barcode Scanner</title>\n" +
                   "    <style>\n" +
                   "        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }\n" +
                   "        .container { max-width: 500px; margin: 0 auto; }\n" +
                   "        h1 { color: #4c84ff; }\n" +
                   "        button { background-color: #4c84ff; color: white; border: none; padding: 10px 15px; border-radius: 4px; cursor: pointer; margin-right: 5px; }\n" +
                   "        button.secondary { background-color: #6c757d; }\n" +
                   "        #scanner-container { margin: 20px 0; max-width: 100%; overflow: hidden; position: relative; }\n" +
                   "        #scanner-container video { width: 100%; max-height: 70vh; }\n" +
                   "        #result { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-radius: 4px; display: none; }\n" +
                   "        .info-box { padding: 10px; margin: 10px 0; border-radius: 4px; }\n" +
                   "        .success { background-color: #d4edda; color: #155724; }\n" +
                   "        .warning { background-color: #fff3cd; color: #856404; }\n" +
                   "        .error { background-color: #f8d7da; color: #721c24; }\n" +
                   "        .form-row { margin-bottom: 10px; }\n" +
                   "        label { display: block; margin-bottom: 5px; }\n" +
                   "        input { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }\n" +
                   "        input[type=\"file\"] { padding: 10px; }\n" +
                   "        .hidden { display: none; }\n" +
                   "        #error-message { color: red; margin: 10px 0; }\n" +
                   "        #camera-permissions { display: none; margin: 20px 0; padding: 15px; background-color: #f8d7da; border-radius: 4px; }\n" +
                   "        .method-selector { margin: 20px 0; }\n" +
                   "        .tab-buttons { display: flex; margin-bottom: 10px; }\n" +
                   "        .tab-button { flex: 1; text-align: center; padding: 10px; background: #f0f0f0; cursor: pointer; }\n" +
                   "        .tab-button.active { background: #4c84ff; color: white; }\n" +
                   "        .method-content { display: none; }\n" +
                   "        .method-content.active { display: block; }\n" +
                   "        #manual-input { margin-top: 15px; }\n" +
                   "    </style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <div class=\"container\">\n" +
                   "        <h1>Tire Barcode Scanner</h1>\n" +
                   "        <p>Use your phone to scan tire barcodes and update inventory.</p>\n" +
                   "        \n" +
                   "        <div id=\"error-message\"></div>\n" +
                   "        \n" +
                   "        <div class=\"method-selector\">\n" +
                   "            <div class=\"tab-buttons\">\n" +
                   "                <div id=\"live-tab\" class=\"tab-button active\">Live Camera</div>\n" +
                   "                <div id=\"photo-tab\" class=\"tab-button\">Take Photo</div>\n" +
                   "                <div id=\"manual-tab\" class=\"tab-button\">Manual Entry</div>\n" +
                   "            </div>\n" +
                   "            \n" +
                   "            <!-- Live Scanner Method -->\n" +
                   "            <div id=\"live-method\" class=\"method-content active\">\n" +
                   "                <button id=\"start-button\">Start Live Scanning</button>\n" +
                   "                <div id=\"camera-permissions\" class=\"hidden\">\n" +
                   "                    <h3>Camera Permission Required</h3>\n" +
                   "                    <p>Please allow camera access to scan barcodes. If you denied permission, you'll need to reset it in your browser settings.</p>\n" +
                   "                </div>\n" +
                   "                <div id=\"scanner-container\"></div>\n" +
                   "            </div>\n" +
                   "            \n" +
                   "            <!-- Photo Method -->\n" +
                   "            <div id=\"photo-method\" class=\"method-content\">\n" +
                   "                <p>Take a photo of the barcode:</p>\n" +
                   "                <input type=\"file\" id=\"file-input\" accept=\"image/*\" capture=\"environment\">\n" +
                   "                <div id=\"photo-preview\" class=\"hidden\">\n" +
                   "                    <h4>Preview:</h4>\n" +
                   "                    <img id=\"preview-image\" style=\"max-width: 100%; max-height: 300px;\">\n" +
                   "                    <button id=\"process-photo\">Process Barcode</button>\n" +
                   "                </div>\n" +
                   "            </div>\n" +
                   "            \n" +
                   "            <!-- Manual Entry Method -->\n" +
                   "            <div id=\"manual-method\" class=\"method-content\">\n" +
                   "                <p>Enter the barcode manually:</p>\n" +
                   "                <input type=\"text\" id=\"manual-barcode\" placeholder=\"Enter barcode...\">\n" +
                   "                <button id=\"manual-submit\">Look Up</button>\n" +
                   "            </div>\n" +
                   "        </div>\n" +
                   "        \n" +
                   "        <div id=\"result\">\n" +
                   "            <h3>Scanned Product:</h3>\n" +
                   "            <p id=\"barcode-value\"></p>\n" +
                   "            <div id=\"product-info\"></div>\n" +
                   "            \n" +
                   "            <div id=\"existing-product\" class=\"info-box success hidden\">\n" +
                   "                <h4>Product Found in Inventory</h4>\n" +
                   "                <p id=\"existing-details\"></p>\n" +
                   "                <button id=\"add-existing-button\">Add to Inventory</button>\n" +
                   "                <button id=\"cancel-button\" class=\"secondary\">Cancel</button>\n" +
                   "            </div>\n" +
                   "            \n" +
                   "            <div id=\"new-product\" class=\"info-box warning hidden\">\n" +
                   "                <h4>New Product Detected</h4>\n" +
                   "                <p id=\"new-details\"></p>\n" +
                   "                <div class=\"form-row\">\n" +
                   "                    <label for=\"price-input\">Price ($):</label>\n" +
                   "                    <input type=\"number\" id=\"price-input\" step=\"0.01\" min=\"0\" placeholder=\"Enter price\">\n" +
                   "                </div>\n" +
                   "                <button id=\"add-new-button\">Add New Product</button>\n" +
                   "                <button id=\"cancel-new-button\" class=\"secondary\">Cancel</button>\n" +
                   "            </div>\n" +
                   "            \n" +
                   "            <div id=\"unknown-product\" class=\"info-box error hidden\">\n" +
                   "                <h4>Unknown Barcode</h4>\n" +
                   "                <p>This barcode format is not recognized. Please add product details manually.</p>\n" +
                   "                <button id=\"cancel-unknown-button\" class=\"secondary\">Cancel</button>\n" +
                   "            </div>\n" +
                   "        </div>\n" +
                   "    </div>\n" +
                   "    \n" +
                   "    <!-- Load QuaggaJS for barcode scanning -->\n" +
                   "    <script src=\"https://cdn.jsdelivr.net/npm/quagga@0.12.1/dist/quagga.min.js\"></script>\n" +
                   "    <script>\n" +
                   "        // Tab navigation\n" +
                   "        document.getElementById('live-tab').addEventListener('click', () => switchTab('live'));\n" +
                   "        document.getElementById('photo-tab').addEventListener('click', () => switchTab('photo'));\n" +
                   "        document.getElementById('manual-tab').addEventListener('click', () => switchTab('manual'));\n" +
                   "        \n" +
                   "        // Button click handlers\n" +
                   "        document.getElementById('start-button').addEventListener('click', startScanner);\n" +
                   "        document.getElementById('add-existing-button').addEventListener('click', addExistingToInventory);\n" +
                   "        document.getElementById('add-new-button').addEventListener('click', addNewProduct);\n" +
                   "        document.getElementById('cancel-button').addEventListener('click', cancelScan);\n" +
                   "        document.getElementById('cancel-new-button').addEventListener('click', cancelScan);\n" +
                   "        document.getElementById('cancel-unknown-button').addEventListener('click', cancelScan);\n" +
                   "        document.getElementById('file-input').addEventListener('change', handleFileSelect);\n" +
                   "        document.getElementById('process-photo').addEventListener('click', processPhoto);\n" +
                   "        document.getElementById('manual-submit').addEventListener('click', submitManualBarcode);\n" +
                   "        \n" +
                   "        let scannedBarcode = '';\n" +
                   "        \n" +
                   "        function switchTab(tabName) {\n" +
                   "            // Reset all tabs\n" +
                   "            document.querySelectorAll('.tab-button').forEach(tab => tab.classList.remove('active'));\n" +
                   "            document.querySelectorAll('.method-content').forEach(content => content.classList.remove('active'));\n" +
                   "            \n" +
                   "            // Activate selected tab\n" +
                   "            document.getElementById(tabName + '-tab').classList.add('active');\n" +
                   "            document.getElementById(tabName + '-method').classList.add('active');\n" +
                   "            \n" +
                   "            // Reset any ongoing operations\n" +
                   "            if (tabName !== 'live') {\n" +
                   "                try { Quagga.stop(); } catch(e) { /* ignore */ }\n" +
                   "            }\n" +
                   "        }\n" +
                   "        \n" +
                   "        function showError(message) {\n" +
                   "            const errorElement = document.getElementById('error-message');\n" +
                   "            errorElement.textContent = message;\n" +
                   "            errorElement.style.display = 'block';\n" +
                   "        }\n" +
                   "        \n" +
                   "        function clearError() {\n" +
                   "            const errorElement = document.getElementById('error-message');\n" +
                   "            errorElement.textContent = '';\n" +
                   "            errorElement.style.display = 'none';\n" +
                   "        }\n" +
                   "        \n" +
                   "        function handleFileSelect(event) {\n" +
                   "            clearError();\n" +
                   "            const file = event.target.files[0];\n" +
                   "            if (!file) return;\n" +
                   "            \n" +
                   "            // Show preview\n" +
                   "            const reader = new FileReader();\n" +
                   "            reader.onload = function(e) {\n" +
                   "                document.getElementById('preview-image').src = e.target.result;\n" +
                   "                document.getElementById('photo-preview').classList.remove('hidden');\n" +
                   "            };\n" +
                   "            reader.readAsDataURL(file);\n" +
                   "        }\n" +
                   "        \n" +
                   "        function processPhoto() {\n" +
                   "            clearError();\n" +
                   "            const image = document.getElementById('preview-image');\n" +
                   "            if (!image.src) {\n" +
                   "                showError('Please select an image first');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            try {\n" +
                   "                Quagga.decodeSingle({\n" +
                   "                    decoder: {\n" +
                   "                        readers: [\n" +
                   "                            \"code_128_reader\",\n" +
                   "                            \"ean_reader\",\n" +
                   "                            \"ean_8_reader\",\n" +
                   "                            \"code_39_reader\",\n" +
                   "                            \"code_39_vin_reader\",\n" +
                   "                            \"upc_reader\",\n" +
                   "                            \"upc_e_reader\"\n" +
                   "                        ]\n" +
                   "                    },\n" +
                   "                    locate: true,\n" +
                   "                    src: image.src\n" +
                   "                }, function(result) {\n" +
                   "                    if (result && result.codeResult) {\n" +
                   "                        scannedBarcode = result.codeResult.code;\n" +
                   "                        document.getElementById('barcode-value').textContent = 'Barcode: ' + scannedBarcode;\n" +
                   "                        document.getElementById('result').style.display = 'block';\n" +
                   "                        checkProductInfo(scannedBarcode);\n" +
                   "                    } else {\n" +
                   "                        showError('No barcode detected in image. Try a clearer photo or enter manually.');\n" +
                   "                    }\n" +
                   "                });\n" +
                   "            } catch (e) {\n" +
                   "                console.error('Error processing image:', e);\n" +
                   "                showError('Failed to process image. Try the manual entry method.');\n" +
                   "                switchTab('manual');\n" +
                   "            }\n" +
                   "        }\n" +
                   "        \n" +
                   "        function submitManualBarcode() {\n" +
                   "            clearError();\n" +
                   "            const barcodeInput = document.getElementById('manual-barcode');\n" +
                   "            const barcode = barcodeInput.value.trim();\n" +
                   "            \n" +
                   "            if (!barcode) {\n" +
                   "                showError('Please enter a barcode');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            // Clean up barcode - remove spaces and non-digit characters\n" +
                   "            let cleanBarcode = barcode.replace(/[^0-9]/g, '');\n" +
                   "            \n" +
                   "            // Handle multiple leading zeros properly (standardize format)\n" +
                   "            if (cleanBarcode.startsWith('00')) {\n" +
                   "                // Remove all leading zeros first\n" +
                   "                let withoutLeadingZeros = cleanBarcode.replace(/^0+/, '');\n" +
                   "                // Add back a single zero if needed for standard UPC format (12 digits)\n" +
                   "                if (withoutLeadingZeros.length === 11) {\n" +
                   "                    cleanBarcode = '0' + withoutLeadingZeros;\n" +
                   "                    console.log('Standardized barcode to UPC format:', cleanBarcode);\n" +
                   "                } else {\n" +
                   "                    cleanBarcode = withoutLeadingZeros;\n" +
                   "                    console.log('Removed leading zeros:', cleanBarcode);\n" +
                   "                }\n" +
                   "            }\n" +
                   "            \n" +
                   "            if (cleanBarcode.length < 8) {\n" +
                   "                showError('Barcode must have at least 8 digits');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            scannedBarcode = cleanBarcode;\n" +
                   "            document.getElementById('barcode-value').textContent = 'Barcode: ' + scannedBarcode;\n" +
                   "            document.getElementById('result').style.display = 'block';\n" +
                   "            checkProductInfo(scannedBarcode);\n" +
                   "        }\n" +
                   "        \n" +
                   "        function startScanner() {\n" +
                   "            clearError();\n" +
                   "            document.getElementById('camera-permissions').style.display = 'none';\n" +
                   "            \n" +
                   "            // Check if Quagga is available\n" +
                   "            if (typeof Quagga === 'undefined') {\n" +
                   "                showError('Barcode scanning library failed to load. Please try the photo or manual method.');\n" +
                   "                switchTab('photo');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            // Set up scanner with better error handling\n" +
                   "            try {\n" +
                   "                Quagga.init({\n" +
                   "                    inputStream: {\n" +
                   "                        name: \"Live\",\n" +
                   "                        type: \"LiveStream\",\n" +
                   "                        target: document.querySelector('#scanner-container'),\n" +
                   "                        constraints: {\n" +
                   "                            width: { min: 320 },\n" +
                   "                            height: { min: 240 },\n" +
                   "                            facingMode: \"environment\",\n" +
                   "                            aspectRatio: { min: 1, max: 2 }\n" +
                   "                        },\n" +
                   "                    },\n" +
                   "                    locator: {\n" +
                   "                        patchSize: \"medium\",\n" +
                   "                        halfSample: true\n" +
                   "                    },\n" +
                   "                    numOfWorkers: navigator.hardwareConcurrency || 4,\n" +
                   "                    frequency: 10,\n" +
                   "                    decoder: {\n" +
                   "                        readers: [\n" +
                   "                            \"code_128_reader\",\n" +
                   "                            \"ean_reader\",\n" +
                   "                            \"ean_8_reader\",\n" +
                   "                            \"code_39_reader\",\n" +
                   "                            \"code_39_vin_reader\",\n" +
                   "                            \"upc_reader\",\n" +
                   "                            \"upc_e_reader\"\n" +
                   "                        ]\n" +
                   "                    },\n" +
                   "                    locate: true\n" +
                   "                }, function(err) {\n" +
                   "                    if (err) {\n" +
                   "                        console.error('Quagga initialization failed:', err);\n" +
                   "                        \n" +
                   "                        if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {\n" +
                   "                            // Camera permission denied\n" +
                   "                            document.getElementById('camera-permissions').style.display = 'block';\n" +
                   "                            showError('Camera access denied. Please use the photo or manual method instead.');\n" +
                   "                            switchTab('photo');\n" +
                   "                        } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {\n" +
                   "                            // No camera found\n" +
                   "                            showError('No camera found. Please use the photo or manual method.');\n" +
                   "                            switchTab('photo');\n" +
                   "                        } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {\n" +
                   "                            // Camera in use by another application\n" +
                   "                            showError('Camera is in use by another application. Please use the photo method.');\n" +
                   "                            switchTab('photo');\n" +
                   "                        } else {\n" +
                   "                            // Generic error\n" +
                   "                            showError('Live scanning not supported on this device. Please use the photo or manual method.');\n" +
                   "                            switchTab('photo');\n" +
                   "                        }\n" +
                   "                        return;\n" +
                   "                    }\n" +
                   "                    \n" +
                   "                    console.log('Quagga initialized successfully');\n" +
                   "                    Quagga.start();\n" +
                   "                    \n" +
                   "                    // Update UI to show scanner is active\n" +
                   "                    document.getElementById('start-button').textContent = 'Scanning...';\n" +
                   "                    document.getElementById('start-button').disabled = true;\n" +
                   "                });\n" +
                   "                \n" +
                   "                // Set up detection handler\n" +
                   "                Quagga.onDetected(handleBarcodeDetection);\n" +
                   "                \n" +
                   "            } catch (e) {\n" +
                   "                console.error('Error starting scanner:', e);\n" +
                   "                showError('Live scanning not supported on this device. Please use the photo or manual method.');\n" +
                   "                switchTab('photo');\n" +
                   "            }\n" +
                   "        }\n" +
                   "        \n" +
                   "        function handleBarcodeDetection(result) {\n" +
                   "            if (!result || !result.codeResult) {\n" +
                   "                console.log('Invalid detection result');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            console.log('Barcode detected:', result.codeResult.code);\n" +
                   "            scannedBarcode = result.codeResult.code;\n" +
                   "            document.getElementById('barcode-value').textContent = 'Barcode: ' + scannedBarcode;\n" +
                   "            document.getElementById('result').style.display = 'block';\n" +
                   "            \n" +
                   "            // Stop scanner after detection\n" +
                   "            try {\n" +
                   "                Quagga.stop();\n" +
                   "                console.log('Scanner stopped');\n" +
                   "            } catch (e) {\n" +
                   "                console.error('Error stopping scanner:', e);\n" +
                   "            }\n" +
                   "            \n" +
                   "            // Reset UI\n" +
                   "            document.getElementById('start-button').textContent = 'Start Live Scanning';\n" +
                   "            document.getElementById('start-button').disabled = false;\n" +
                   "            \n" +
                   "            // Check product info\n" +
                   "            checkProductInfo(scannedBarcode);\n" +
                   "        }\n" +
                   "        \n" +
                   "        function checkProductInfo(barcode) {\n" +
                   "            fetch('/api/product?barcode=' + barcode)\n" +
                   "                .then(response => response.json())\n" +
                   "                .then(data => {\n" +
                   "                    // Hide all product info divs\n" +
                   "                    document.getElementById('existing-product').classList.add('hidden');\n" +
                   "                    document.getElementById('new-product').classList.add('hidden');\n" +
                   "                    document.getElementById('unknown-product').classList.add('hidden');\n" +
                   "                    \n" +
                   "                    if (data.found) {\n" +
                   "                        // Existing product\n" +
                   "                        document.getElementById('existing-details').innerHTML = \n" +
                   "                            `<strong>${data.name}</strong><br>` +\n" +
                   "                            `Price: $${data.price}<br>` +\n" +
                   "                            `Current Stock: ${data.stock}<br>` +\n" +
                   "                            `${data.size ? 'Size: ' + data.size + '<br>' : ''}` +\n" +
                   "                            `${data.modelNumber ? 'Model: ' + data.modelNumber + '<br>' : ''}` +\n" +
                   "                            `${data.location ? 'Location: ' + data.location + '<br>' : ''}` +\n" +
                   "                            `${data.description || ''}`;\n" +
                   "                            \n" +
                   "                        document.getElementById('existing-product').classList.remove('hidden');\n" +
                   "                    } else if (data.parsed) {\n" +
                   "                        // New product with parsed info\n" +
                   "                        document.getElementById('new-details').innerHTML = \n" +
                   "                            `<strong>${data.name}</strong><br>` +\n" +
                   "                            `Manufacturer: ${data.manufacturer || 'Unknown'}<br>` +\n" +
                   "                            `${data.size ? 'Size: ' + data.size + '<br>' : ''}` +\n" +
                   "                            `${data.modelNumber ? 'Model: ' + data.modelNumber + '<br>' : ''}` +\n" +
                   "                            `${data.description || ''}`;\n" +
                   "                            \n" +
                   "                        document.getElementById('price-input').value = '';\n" +
                   "                        document.getElementById('new-product').classList.remove('hidden');\n" +
                   "                    } else {\n" +
                   "                        // Unknown product\n" +
                   "                        document.getElementById('unknown-product').classList.remove('hidden');\n" +
                   "                    }\n" +
                   "                })\n" +
                   "                .catch(error => {\n" +
                   "                    console.error('Error checking product:', error);\n" +
                   "                    showError('Failed to check product information. Please try again.');\n" +
                   "                });\n" +
                   "        }\n" +
                   "        \n" +
                   "        function addExistingToInventory() {\n" +
                   "            fetch('/scan', {\n" +
                   "                method: 'POST',\n" +
                   "                headers: {\n" +
                   "                    'Content-Type': 'application/json',\n" +
                   "                },\n" +
                   "                body: JSON.stringify({\n" +
                   "                    barcode: scannedBarcode,\n" +
                   "                    action: 'add'\n" +
                   "                })\n" +
                   "            })\n" +
                   "            .then(response => response.json())\n" +
                   "            .then(data => {\n" +
                   "                if (data.success) {\n" +
                   "                    alert('Success: ' + data.message);\n" +
                   "                } else {\n" +
                   "                    alert('Error: ' + data.message);\n" +
                   "                }\n" +
                   "                resetScanner();\n" +
                   "            })\n" +
                   "            .catch(error => {\n" +
                   "                console.error('Error adding to inventory:', error);\n" +
                   "                showError('Failed to update inventory. Please try again.');\n" +
                   "            });\n" +
                   "        }\n" +
                   "        \n" +
                   "        function addNewProduct() {\n" +
                   "            const price = document.getElementById('price-input').value;\n" +
                   "            if (!price || isNaN(price) || price <= 0) {\n" +
                   "                alert('Please enter a valid price');\n" +
                   "                return;\n" +
                   "            }\n" +
                   "            \n" +
                   "            fetch('/scan', {\n" +
                   "                method: 'POST',\n" +
                   "                headers: {\n" +
                   "                    'Content-Type': 'application/json',\n" +
                   "                },\n" +
                   "                body: JSON.stringify({\n" +
                   "                    barcode: scannedBarcode,\n" +
                   "                    action: 'add',\n" +
                   "                    price: parseFloat(price)\n" +
                   "                })\n" +
                   "            })\n" +
                   "            .then(response => response.json())\n" +
                   "            .then(data => {\n" +
                   "                if (data.success) {\n" +
                   "                    alert('Success: ' + data.message);\n" +
                   "                } else {\n" +
                   "                    alert('Error: ' + data.message);\n" +
                   "                }\n" +
                   "                resetScanner();\n" +
                   "            })\n" +
                   "            .catch(error => {\n" +
                   "                console.error('Error adding new product:', error);\n" +
                   "                showError('Failed to add new product. Please try again.');\n" +
                   "            });\n" +
                   "        }\n" +
                   "        \n" +
                   "        function cancelScan() {\n" +
                   "            resetScanner();\n" +
                   "        }\n" +
                   "        \n" +
                   "        function resetScanner() {\n" +
                   "            document.getElementById('result').style.display = 'none';\n" +
                   "            document.getElementById('barcode-value').textContent = '';\n" +
                   "            document.getElementById('scanner-container').innerHTML = '';\n" +
                   "            document.getElementById('start-button').textContent = 'Start Live Scanning';\n" +
                   "            document.getElementById('start-button').disabled = false;\n" +
                   "            document.getElementById('photo-preview').classList.add('hidden');\n" +
                   "            document.getElementById('file-input').value = '';\n" +
                   "            clearError();\n" +
                   "            scannedBarcode = '';\n" +
                   "        }\n" +
                   "        \n" +
                   "        // Detect device/browser to show appropriate methods\n" +
                   "        document.addEventListener('DOMContentLoaded', function() {\n" +
                   "            // If no support for mediaDevices, default to photo method\n" +
                   "            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {\n" +
                   "                document.getElementById('start-button').disabled = true;\n" +
                   "                showError('Live scanning not supported on this device. Please use the photo or manual method.');\n" +
                   "                switchTab('photo');\n" +
                   "            }\n" +
                   "            \n" +
                   "            // For Android devices specifically, try to detect if QuaggaJS is supported\n" +
                   "            const isAndroid = /Android/i.test(navigator.userAgent);\n" +
                   "            if (isAndroid) {\n" +
                   "                try {\n" +
                   "                    if (typeof Quagga === 'undefined') {\n" +
                   "                        switchTab('photo');\n" +
                   "                    }\n" +
                   "                } catch (e) {\n" +
                   "                    switchTab('photo');\n" +
                   "                }\n" +
                   "            }\n" +
                   "        });\n" +
                   "    </script>\n" +
                   "</body>\n" +
                   "</html>";
        }
    }
    
    /**
     * Handler for processing scanned barcodes
     */
    class ScanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (handleCorsPreflightRequest(exchange)) {
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read the request body
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                
                try {
                    // Parse the JSON request
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) parser.parse(requestBody.toString());
                    String barcode = (String) jsonObject.get("barcode");
                    String action = (String) jsonObject.get("action");
                    
                    // Clean up and normalize barcode
                    if (barcode != null && !barcode.isEmpty()) {
                        // Remove non-digits
                        barcode = barcode.replaceAll("[^0-9]", "");
                        
                        // Handle multiple leading zeros (common with mobile phone cameras)
                        if (barcode.startsWith("00")) {
                            String cleanedBarcode = barcode.replaceAll("^0+", "");
                            System.out.println("Scan: Removed multiple leading zeros: " + barcode + " -> " + cleanedBarcode);
                            // Add back a single leading zero if it would make it 12 digits (standard UPC)
                            if (cleanedBarcode.length() == 11) {
                                cleanedBarcode = "0" + cleanedBarcode;
                                System.out.println("Scan: Standardized to UPC-12 format: " + cleanedBarcode);
                            }
                            barcode = cleanedBarcode;
                        }
                    }
                    
                    // Process the barcode based on the action
                    JSONObject responseJson = new JSONObject();
                    
                    if ("add".equals(action)) {
                        // Check if product exists in the database
                        Optional<Product> existingProduct = inventoryService.findProductByBarcode(barcode);
                        
                        if (existingProduct.isPresent()) {
                            // If product exists, add to inventory
                            Product product = existingProduct.get();
                            product.setQuantityInStock(product.getQuantityInStock() + 1);
                            inventoryService.updateProduct(product);
                            
                            responseJson.put("success", true);
                            responseJson.put("message", "Added 1 unit of " + product.getName() + " to inventory");
                        } else {
                            // If product doesn't exist, try to extract info from barcode
                            Product newProduct = BarcodeParser.parseBarcode(barcode);
                            
                            if (newProduct != null && newProduct.getName() != null) {
                                // We have some information from the barcode, but need to set initial values
                                newProduct.setQuantityInStock(1);
                                
                                // Check if price was supplied
                                BigDecimal price = BigDecimal.ZERO;
                                if (jsonObject.containsKey("price")) {
                                    try {
                                        double priceValue = ((Number) jsonObject.get("price")).doubleValue();
                                        price = BigDecimal.valueOf(priceValue);
                                    } catch (Exception e) {
                                        LOGGER.warning("Invalid price format: " + jsonObject.get("price"));
                                    }
                                }
                                
                                newProduct.setPrice(price);
                                newProduct.setReorderLevel(5); // Default reorder level
                                
                                // Save the new product
                                Product savedProduct = inventoryService.addProduct(newProduct);
                                
                                responseJson.put("success", true);
                                responseJson.put("new_product", true);
                                responseJson.put("message", "Created new product: " + savedProduct.getName() + " with price $" + price);
                                responseJson.put("product_id", savedProduct.getId());
                            } else {
                                // Could not extract info from barcode
                                responseJson.put("success", false);
                                responseJson.put("message", "Product not found and barcode format not recognized. Please add product manually.");
                            }
                        }
                    } else if ("check".equals(action)) {
                        // Just check for product info without adding
                        Optional<Product> existingProduct = inventoryService.findProductByBarcode(barcode);
                        
                        if (existingProduct.isPresent()) {
                            Product product = existingProduct.get();
                            responseJson.put("success", true);
                            responseJson.put("found", true);
                            responseJson.put("name", product.getName());
                            responseJson.put("price", product.getPrice());
                            responseJson.put("stock", product.getQuantityInStock());
                        } else {
                            Product parsedProduct = BarcodeParser.parseBarcode(barcode);
                            if (parsedProduct != null && parsedProduct.getName() != null) {
                                responseJson.put("success", true);
                                responseJson.put("found", false);
                                responseJson.put("parsed", true);
                                responseJson.put("name", parsedProduct.getName());
                                responseJson.put("description", parsedProduct.getDescription());
                                responseJson.put("manufacturer", parsedProduct.getManufacturer());
                                responseJson.put("category", parsedProduct.getCategory());
                                responseJson.put("modelNumber", parsedProduct.getModelNumber());
                                responseJson.put("size", parsedProduct.getSize());
                                responseJson.put("message", "Product not in inventory but information extracted from barcode");
                            } else {
                                responseJson.put("success", false);
                                responseJson.put("message", "Barcode not recognized");
                            }
                        }
                    } else {
                        responseJson.put("success", false);
                        responseJson.put("message", "Invalid action");
                    }
                    
                    // Send the response
                    String response = responseJson.toJSONString();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    sendResponse(exchange, response);
                    
                } catch (Exception e) {
                    LOGGER.severe("Error processing scan request: " + e.getMessage());
                    sendErrorResponse(exchange, "Invalid request format");
                }
            } else {
                // Method not allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * Handler for product API
     */
    class ProductApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight
            if (handleCorsPreflightRequest(exchange)) {
                return;
            }
            
            // Get the barcode from the query parameter
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            String barcode = params.get("barcode");
            
            JSONObject responseJson = new JSONObject();
            
            if (barcode != null && !barcode.isEmpty()) {
                // Clean up barcode - remove spaces and any non-digit characters
                barcode = barcode.replaceAll("[^0-9]", "");
                System.out.println("Processing barcode query: " + barcode);
                
                // Handle multiple leading zeros (common with mobile phone cameras)
                if (barcode.startsWith("00")) {
                    String cleanedBarcode = barcode.replaceAll("^0+", "");
                    System.out.println("Removed multiple leading zeros: " + barcode + " -> " + cleanedBarcode);
                    // Add back a single leading zero if it would make it 12 digits (standard UPC)
                    if (cleanedBarcode.length() == 11) {
                        cleanedBarcode = "0" + cleanedBarcode;
                        System.out.println("Standardized to UPC-12 format: " + cleanedBarcode);
                    }
                    barcode = cleanedBarcode;
                }
                
                // First, try to find the product in our database
                Optional<Product> existingProduct = inventoryService.findProductByBarcode(barcode);
                
                if (existingProduct.isPresent()) {
                    Product product = existingProduct.get();
                    responseJson.put("found", true);
                    responseJson.put("name", product.getName());
                    responseJson.put("price", product.getPrice());
                    responseJson.put("stock", product.getQuantityInStock());
                    responseJson.put("id", product.getId());
                    responseJson.put("description", product.getDescription());
                    responseJson.put("manufacturer", product.getManufacturer());
                    responseJson.put("category", product.getCategory());
                    responseJson.put("size", product.getSize());
                    responseJson.put("modelNumber", product.getModelNumber());
                    responseJson.put("location", product.getLocation());
                } else {
                    // If not in database, try to parse information from the barcode
                    Product parsedProduct = BarcodeParser.parseBarcode(barcode);
                    
                    if (parsedProduct != null && parsedProduct.getName() != null) {
                        // We got some information from the barcode
                        responseJson.put("found", false);
                        responseJson.put("parsed", true);
                        responseJson.put("name", parsedProduct.getName());
                        responseJson.put("description", parsedProduct.getDescription());
                        responseJson.put("manufacturer", parsedProduct.getManufacturer());
                        responseJson.put("category", parsedProduct.getCategory());
                        responseJson.put("modelNumber", parsedProduct.getModelNumber());
                        responseJson.put("size", parsedProduct.getSize());
                        responseJson.put("message", "Product not in inventory but information extracted from barcode");
                    } else {
                        // No information could be extracted
                        responseJson.put("found", false);
                        responseJson.put("parsed", false);
                        responseJson.put("message", "Product not found and barcode format not recognized");
                    }
                }
            } else {
                responseJson.put("found", false);
                responseJson.put("error", "Missing barcode parameter");
            }
            
            // Send the response
            String response = responseJson.toJSONString();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, response);
        }
    }
    
    /**
     * Helper method to send a response
     */
    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        // Add CORS headers for better mobile browser compatibility
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        // Set content type
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        
        // Send response
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    /**
     * Helper method to send an error response
     */
    private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        // Add CORS headers for better mobile browser compatibility
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        JSONObject errorJson = new JSONObject();
        errorJson.put("success", false);
        errorJson.put("message", errorMessage);
        
        String response = errorJson.toJSONString();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(400, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    /**
     * Helper method to parse query parameters
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
} 