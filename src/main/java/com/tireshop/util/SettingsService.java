package com.tireshop.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;

public class SettingsService {
    private static final String CONFIG_FILE_NAME = "config.properties";
    // Save to current directory (runtime location) instead of src/main/resources
    // This prevents Maven from overwriting saved changes when recompiling
    private static final Path CONFIG_FILE_PATH = Paths.get(CONFIG_FILE_NAME);
    private static final Path FALLBACK_CONFIG_PATH = Paths.get("src/main/resources", CONFIG_FILE_NAME);
    private static SettingsService instance;
    private Properties properties;

    public SettingsService() {
        if (instance == null) {
            this.properties = new Properties();
            loadProperties();
            instance = this;
        } else {
            // If instance already exists, copy its properties
            this.properties = instance.properties;
        }
    }
    
    public static SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }

    private void loadProperties() {
        // Try loading from runtime location first (current directory)
        if (Files.exists(CONFIG_FILE_PATH)) {
            try (InputStream input = new FileInputStream(CONFIG_FILE_PATH.toFile())) {
                properties.load(input);
                System.out.println("[SettingsService] Properties loaded from runtime location: " + CONFIG_FILE_PATH.toAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("[SettingsService] Error loading properties from runtime location: " + e.getMessage());
            }
        }
        
        // Fallback to source location (for initial setup)
        if (Files.exists(FALLBACK_CONFIG_PATH)) {
            try (InputStream input = new FileInputStream(FALLBACK_CONFIG_PATH.toFile())) {
                properties.load(input);
                System.out.println("[SettingsService] Properties loaded from fallback location: " + FALLBACK_CONFIG_PATH);
                // Save to runtime location so future edits persist
                saveProperties();
                return;
            } catch (IOException e) {
                System.err.println("[SettingsService] Error loading properties from fallback: " + e.getMessage());
            }
        }
        
        // Neither file exists - initialize with defaults
        System.out.println("[SettingsService] No config file found. Initializing with defaults and creating file.");
        initializeDefaultProperties();
        saveProperties(); // Create the file with defaults
    }

    private void initializeDefaultProperties() {
        properties.setProperty("company.name", "Your Tire Shop");
        properties.setProperty("company.address", "123 Main Street, Anytown, USA");
        properties.setProperty("company.phone", "(555) 123-4567");
        properties.setProperty("company.logo.path", "");
        properties.setProperty("sales.tax.rate", "0.06");
        properties.setProperty("credit.card.fee.percentage", "0.00");
        properties.setProperty("discount.military.percent", "10");
        properties.setProperty("inventory.default.lowstock.threshold", "5"); // Default value
        properties.setProperty("quicklink.1.name", "Example Link"); // Default example quick link
        properties.setProperty("quicklink.1.url", "https://www.example.com");
        properties.setProperty("wheelapi.baseurl", "https://api.wheel-size.com/v2");
        properties.setProperty("wheelapi.userkey", "YOUR_API_KEY_HERE"); // Placeholder
        properties.setProperty("tire.api.lookup.enabled", "false"); // Disable API lookups by default
    }

    public void saveProperties() {
        // Always save to runtime location (current directory)
        // This prevents Maven from overwriting changes when recompiling
        try (OutputStream output = new FileOutputStream(CONFIG_FILE_PATH.toFile())) {
            properties.store(output, "Application Configuration - Saved to runtime location");
            System.out.println("[SettingsService] ✅ Properties SAVED to runtime location: " + CONFIG_FILE_PATH.toAbsolutePath());
            System.out.println("[SettingsService] Settings will persist across application restarts");
        } catch (IOException e) {
            System.err.println("[SettingsService] ❌ Error saving properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getter methods
    public String getCompanyName() {
        return properties.getProperty("company.name", "Your Tire Shop");
    }

    public String getCompanyAddress() {
        return properties.getProperty("company.address", "123 Main Street, Anytown, USA");
    }

    public String getCompanyPhone() {
        return properties.getProperty("company.phone", "(555) 123-4567");
    }

    public String getCompanyLogoPath() {
        return properties.getProperty("company.logo.path", "");
    }

    public BigDecimal getSalesTaxRate() {
        try {
            return new BigDecimal(properties.getProperty("sales.tax.rate", "0.00"));
        } catch (NumberFormatException e) {
            System.err.println("[SettingsService] Invalid sales.tax.rate format, returning 0.00: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getCreditCardFeePercentage() {
        try {
            return new BigDecimal(properties.getProperty("credit.card.fee.percentage", "0.00"));
        } catch (NumberFormatException e) {
            System.err.println("[SettingsService] Invalid credit.card.fee.percentage format, returning 0.00: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Military discount as a whole percent number (e.g. 10 means 10%)
     */
    public BigDecimal getMilitaryDiscountPercent() {
        try {
            return new BigDecimal(properties.getProperty("discount.military.percent", "10"));
        } catch (NumberFormatException e) {
            System.err.println("[SettingsService] Invalid discount.military.percent format, returning 10: " + e.getMessage());
            return new BigDecimal("10");
        }
    }

    public void setMilitaryDiscountPercent(BigDecimal percent) {
        properties.setProperty("discount.military.percent", percent.toPlainString());
    }

    public int getGlobalLowStockThreshold() {
        try {
            return Integer.parseInt(properties.getProperty("inventory.default.lowstock.threshold", "5"));
        } catch (NumberFormatException e) {
            System.err.println("[SettingsService] Invalid inventory.default.lowstock.threshold format, returning 5: " + e.getMessage());
            return 5; // Default fallback
        }
    }

    // Generic getter for any property
    public String getSetting(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    // Generic setter for any property
    public void setSetting(String key, String value) {
        properties.setProperty(key, value);
    }

    // Tire API lookup setting
    public boolean isTireApiLookupEnabled() {
        return Boolean.parseBoolean(properties.getProperty("tire.api.lookup.enabled", "false"));
    }

    public void setTireApiLookupEnabled(boolean enabled) {
        properties.setProperty("tire.api.lookup.enabled", String.valueOf(enabled));
    }

    // Setter methods
    public void setCompanyName(String name) {
        properties.setProperty("company.name", name);
    }

    public void setCompanyAddress(String address) {
        properties.setProperty("company.address", address);
    }

    public void setCompanyPhone(String phone) {
        properties.setProperty("company.phone", phone);
    }

    public void setCompanyLogoPath(String path) {
        properties.setProperty("company.logo.path", path);
    }

    public void setSalesTaxRate(BigDecimal rate) {
        properties.setProperty("sales.tax.rate", rate.toPlainString());
    }

    public void setCreditCardFeePercentage(BigDecimal fee) {
        properties.setProperty("credit.card.fee.percentage", fee.toPlainString());
    }

    public void setGlobalLowStockThreshold(int threshold) {
        properties.setProperty("inventory.default.lowstock.threshold", String.valueOf(threshold));
    }

    // Quick Links Management
    public static class QuickLink {
        public String name;
        public String url;

        public QuickLink(String name, String url) {
            this.name = name;
            this.url = url;
        }
        // Getter for name (good practice for TableView or other UI bindings)
        public String getName() { return name; }
        // Getter for URL
        public String getUrl() { return url; }
    }

    public List<QuickLink> getQuickLinks() {
        List<QuickLink> links = new ArrayList<>();
        int i = 1;
        while (true) {
            String name = properties.getProperty("quicklink." + i + ".name");
            String url = properties.getProperty("quicklink." + i + ".url");
            if (name != null && url != null && !name.trim().isEmpty() && !url.trim().isEmpty()) {
                links.add(new QuickLink(name, url));
                i++;
            } else {
                // If one is null but the other isn't, it might be a corrupted entry, stop anyway
                // Or if name/url is empty string after being set
                break; 
            }
        }
        return links;
    }

    public void saveQuickLinks(List<QuickLink> links) {
        // First, remove all existing quicklink properties to avoid orphaned entries
        List<String> keysToRemove = new ArrayList<>();
        for (Object key : properties.keySet()) {
            if (key.toString().startsWith("quicklink.")) {
                keysToRemove.add(key.toString());
            }
        }
        for (String key : keysToRemove) {
            properties.remove(key);
        }

        // Then, add the new links, ensuring they are not empty
        int validLinkIndex = 1;
        for (QuickLink link : links) {
            if (link.name != null && !link.name.trim().isEmpty() && 
                link.url != null && !link.url.trim().isEmpty()) {
                 properties.setProperty("quicklink." + validLinkIndex + ".name", link.name.trim());
                 properties.setProperty("quicklink." + validLinkIndex + ".url", link.url.trim());
                 validLinkIndex++;
            }
        }
        // The saveProperties() method should be called by the AdminController after all settings changes are made.
    }

    // Role Tab Permissions Management
    private static final String ROLE_TAB_PREFIX = "role.permission.";

    public void saveRoleTabPermissions(Map<String, Set<String>> rolePermissions) {
        // Remove old role permissions to avoid orphans
        List<String> keysToRemove = new ArrayList<>();
        for (Object key : properties.keySet()) {
            if (key.toString().startsWith(ROLE_TAB_PREFIX)) {
                keysToRemove.add(key.toString());
            }
        }
        for (String key : keysToRemove) {
            properties.remove(key);
        }

        // Add new ones
        for (Map.Entry<String, Set<String>> entry : rolePermissions.entrySet()) {
            String roleName = entry.getKey().toUpperCase(); // Ensure consistency
            String tabsString = String.join(",", entry.getValue());
            properties.setProperty(ROLE_TAB_PREFIX + roleName, tabsString);
        }
        // saveProperties(); // Caller (e.g., UserService or AdminController) should decide when to persist all
    }

    public Map<String, Set<String>> loadRoleTabPermissions() {
        Map<String, Set<String>> rolePermissions = new HashMap<>();
        for (Object keyObj : properties.keySet()) {
            String key = keyObj.toString();
            if (key.startsWith(ROLE_TAB_PREFIX)) {
                String roleName = key.substring(ROLE_TAB_PREFIX.length()).toUpperCase();
                String tabsString = properties.getProperty(key);
                if (tabsString != null && !tabsString.trim().isEmpty()) {
                    Set<String> tabs = new HashSet<>(Arrays.asList(tabsString.split(",")));
                    rolePermissions.put(roleName, tabs);
                }
            }
        }
        return rolePermissions;
    }

    // Wheel-Size API Settings
    public String getWheelApiBaseUrl() {
        return properties.getProperty("wheelapi.baseurl", "https://api.wheel-size.com/v2");
    }

    public String getWheelApiKey() {
        return properties.getProperty("wheelapi.userkey"); // No default, must be set
    }

    public void setWheelApiKey(String apiKey) {
        properties.setProperty("wheelapi.userkey", apiKey);
        // saveProperties(); // Typically saved by a higher-level action e.g. in AdminController
    }

    // Supplier API Settings
    public String getSupplierApiUrl(com.tireshop.service.SupplierCatalogService.Supplier supplier) {
        return properties.getProperty("supplier." + supplier.getConfigKey() + ".url");
    }
    
    public String getSupplierApiKey(com.tireshop.service.SupplierCatalogService.Supplier supplier) {
        return properties.getProperty("supplier." + supplier.getConfigKey() + ".apikey");
    }
    
    public void setSupplierApiUrl(com.tireshop.service.SupplierCatalogService.Supplier supplier, String url) {
        properties.setProperty("supplier." + supplier.getConfigKey() + ".url", url);
    }
    
    public void setSupplierApiKey(com.tireshop.service.SupplierCatalogService.Supplier supplier, String apiKey) {
        properties.setProperty("supplier." + supplier.getConfigKey() + ".apikey", apiKey);
    }
    
    // QuickBooks Export Settings
    public String getQuickBooksCompanyFile() {
        return properties.getProperty("quickbooks.companyfile", "");
    }
    
    public void setQuickBooksCompanyFile(String filePath) {
        properties.setProperty("quickbooks.companyfile", filePath);
    }
    
    public String getQuickBooksExportPath() {
        return properties.getProperty("quickbooks.exportpath", System.getProperty("user.home") + "/QuickBooksExports");
    }
    
    public void setQuickBooksExportPath(String path) {
        properties.setProperty("quickbooks.exportpath", path);
    }
    
    // TPMS Settings
    public String getTPMSToolType() {
        return properties.getProperty("tpms.tool.type", "ATEQ");
    }
    
    public void setTPMSToolType(String toolType) {
        properties.setProperty("tpms.tool.type", toolType);
    }
    
    public String getTPMSSerialPort() {
        return properties.getProperty("tpms.serial.port", "COM3");
    }
    
    public void setTPMSSerialPort(String port) {
        properties.setProperty("tpms.serial.port", port);
    }
} 