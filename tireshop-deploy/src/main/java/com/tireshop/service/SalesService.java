package com.tireshop.service;

import com.tireshop.dao.GenericDao;
import com.tireshop.dao.SaleDao;
import com.tireshop.dao.SaleItemDao;
import com.tireshop.dao.VehicleDao;
import com.tireshop.model.*;
import com.tireshop.model.dto.ProductSalesReportItem;
import com.tireshop.model.dto.TechnicianPerformanceReportItem;
import com.tireshop.model.dto.DailyFinancialSummaryItem;
import com.tireshop.model.dto.SalesSummaryData;
import com.tireshop.util.SettingsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for sales operations
 */
public class SalesService {
    
    private final GenericDao<Sale, Long> saleDao;
    private final GenericDao<SaleItem, Long> saleItemDao;
    private final GenericDao<Customer, Long> customerDao;
    private final GenericDao<Vehicle, Long> vehicleDao;
    private final GenericDao<Service, Long> serviceDao;
    private final GenericDao<Technician, Long> technicianDao;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    
    public SalesService(
            GenericDao<Sale, Long> saleDao,
            GenericDao<SaleItem, Long> saleItemDao,
            GenericDao<Customer, Long> customerDao,
            GenericDao<Vehicle, Long> vehicleDao,
            GenericDao<Service, Long> serviceDao,
            GenericDao<Technician, Long> technicianDao,
            InventoryService inventoryService,
            SettingsService settingsService) {
        this.saleDao = saleDao;
        this.saleItemDao = saleItemDao;
        this.customerDao = customerDao;
        this.vehicleDao = vehicleDao;
        this.serviceDao = serviceDao;
        this.technicianDao = technicianDao;
        this.inventoryService = inventoryService;
        this.settingsService = settingsService;
    }
    
    /**
     * Create a new sale
     * @param customerId Customer ID
     * @param vehicleId Vehicle ID (optional)
     * @return The created sale
     */
    public Sale createSale(Long customerId, Long vehicleId) {
        Sale sale = new Sale();
        sale.setTimestamp(LocalDateTime.now());
        sale.setInvoiceNumber(generateInvoiceNumber());
        
        if (customerId != null) {
            customerDao.findById(customerId).ifPresent(sale::setCustomer);
        }
        if (vehicleId != null) {
            vehicleDao.findById(vehicleId).ifPresent(sale::setVehicle);
        }
        // Initial recalculation with no CC fee as payment type not yet known
        sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO);
        return saleDao.save(sale);
    }
    
    /**
     * Add a product to a sale
     * @param saleId Sale ID
     * @param productId Product ID
     * @param quantity Quantity
     * @return The updated sale or empty Optional if sale or product not found
     */
    public Optional<Sale> addProductToSale(Long saleId, Long productId, int quantity) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (optionalSale.isPresent() && inventoryService.getProductById(productId).isPresent()) {
            Sale sale = optionalSale.get();
            Product product = inventoryService.getProductById(productId).get();
            
            // Check inventory
            if (product.getQuantityInStock() < quantity) {
                return Optional.empty(); // Not enough inventory
            }
            
            // Remove from inventory
            inventoryService.removeInventory(productId, quantity);
            
            // Add to sale
            SaleItem saleItem = new SaleItem(product, quantity);
            sale.addItem(saleItem);
            // Explicitly recalculate with potentially applicable CC fee based on current sale.paymentType
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                  PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
            saleItemDao.save(saleItem);
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Add a service to a sale
     * @param saleId Sale ID
     * @param serviceId Service ID
     * @param technicianId Technician ID
     * @return The updated sale or empty Optional if any entity not found
     */
    public Optional<Sale> addServiceToSale(Long saleId, Long serviceId, Long technicianId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (optionalSale.isPresent() && getServiceById(serviceId).isPresent() && getTechnicianById(technicianId).isPresent()) {
            Sale sale = optionalSale.get();
            Service service = getServiceById(serviceId).get();
            Technician technician = getTechnicianById(technicianId).get();
            
            SaleItem saleItem = new SaleItem(service, technician);
            sale.addItem(saleItem);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                  PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
            saleItemDao.save(saleItem);
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Remove an item from a sale
     * @param saleId Sale ID
     * @param saleItemId Sale Item ID
     * @return The updated sale or empty Optional if any entity not found
     */
    public Optional<Sale> removeItemFromSale(Long saleId, Long saleItemId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (optionalSale.isPresent() && saleItemDao.findById(saleItemId).isPresent()) {
            Sale sale = optionalSale.get();
            SaleItem saleItem = saleItemDao.findById(saleItemId).get();
            
            // If it's a product, add it back to inventory
            if ("PRODUCT".equals(saleItem.getItemType()) && saleItem.getProduct() != null) {
                inventoryService.addInventory(saleItem.getProduct().getId(), saleItem.getQuantity());
            }
            
            sale.removeItem(saleItem);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                  PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
            saleItemDao.delete(saleItem);
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Complete a sale by setting payment information
     * @param saleId Sale ID
     * @param paymentType Payment type
     * @return The completed sale or empty Optional if sale not found
     */
    public Optional<Sale> completeSale(Long saleId, PaymentType paymentType) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            sale.setPaymentType(paymentType);
            sale.setPaid(true);
            // Recalculate based on the now-set payment type
            BigDecimal ccFeePercent = (PaymentType.CREDIT_CARD.equals(paymentType) || PaymentType.DEBIT_CARD.equals(paymentType)) ? 
                                      settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO;
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), ccFeePercent);
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Complete a sale with credit card payment
     * @param saleId Sale ID
     * @param cardType Card type (e.g. Visa, MasterCard)
     * @param cardLastFour Last four digits of the card
     * @param authCode Authorization code
     * @return The completed sale or empty Optional if sale not found
     */
    public Optional<Sale> completeCardPayment(Long saleId, String cardType, 
                                             String cardLastFour, String authCode) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            sale.setPaymentType(PaymentType.CREDIT_CARD);
            sale.setCardType(cardType);
            sale.setCardLastFour(cardLastFour);
            sale.setAuthorizationCode(authCode);
            sale.setPaid(true);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), settingsService.getCreditCardFeePercentage());
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Complete a sale with check payment
     * @param saleId Sale ID
     * @param checkNumber Check number
     * @return The completed sale or empty Optional if sale not found
     */
    public Optional<Sale> completeCheckPayment(Long saleId, String checkNumber) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            sale.setPaymentType(PaymentType.CHECK);
            sale.setCheckNumber(checkNumber);
            sale.setPaid(true);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO); // No CC fee
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Complete a sale with cash payment
     * @param saleId Sale ID
     * @return The completed sale or empty Optional if sale not found
     */
    public Optional<Sale> completeCashPayment(Long saleId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            sale.setPaymentType(PaymentType.CASH);
            sale.setPaid(true);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO); // No CC fee
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Get a sale by ID
     * @param saleId Sale ID
     * @return Optional containing sale if found
     */
    public Optional<Sale> getSaleById(Long saleId) {
        return saleDao.findById(saleId);
    }
    
    /**
     * Get all sales
     * @return List of all sales
     */
    public List<Sale> getAllSales() {
        return saleDao.findAll();
    }
    
    /**
     * Get all customers
     * @return List of all customers
     */
    public List<Customer> getAllCustomers() {
        return customerDao.findAll();
    }
    
    /**
     * Get vehicles for a specific customer
     * @param customerId Customer ID
     * @return List of vehicles for the customer
     */
    public List<Vehicle> getVehiclesForCustomer(Long customerId) {
        // Use vehicleDao to get the customer's vehicles
        if (vehicleDao instanceof VehicleDao) {
            return ((VehicleDao) vehicleDao).findByCustomerId(customerId);
        }
        
        // Fallback to empty list if vehicleDao is not a VehicleDao
        return new ArrayList<>();
    }
    
    /**
     * Cancel a sale and restore inventory
     * @param saleId Sale ID
     * @return true if sale was canceled
     */
    public boolean cancelSale(Long saleId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            
            // Restore inventory for product items
            for (SaleItem item : sale.getItems()) {
                if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                    inventoryService.addInventory(item.getProduct().getId(), item.getQuantity());
                }
                saleItemDao.delete(item);
            }
            
            return saleDao.deleteById(saleId);
        }
        
        return false;
    }
    
    /**
     * Add a custom item to a sale
     * @param saleId Sale ID
     * @param itemName Name of the custom item
     * @param unitPrice Price per unit
     * @param quantity Quantity
     * @return The updated sale or empty Optional if sale not found
     */
    public Optional<Sale> addCustomItemToSale(Long saleId, String itemName, BigDecimal unitPrice, int quantity) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            
            // Create a custom sale item
            SaleItem saleItem = new SaleItem();
            saleItem.setItemType("CUSTOM");
            saleItem.setCustomItemName(itemName);
            saleItem.setUnitPrice(unitPrice);
            saleItem.setQuantity(quantity);
            saleItem.setSale(sale);
            
            // Calculate subtotal
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            saleItem.setSubtotal(subtotal);
            
            // Save and add to sale
            sale.addItem(saleItem);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                  PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
            saleItemDao.save(saleItem);
            
            return Optional.of(saleDao.update(sale));
        }
        
        return Optional.empty();
    }
    
    /**
     * Get service by ID
     * @param serviceId Service ID
     * @return Optional containing the service if found
     */
    public Optional<Service> getServiceById(Long serviceId) {
        return serviceDao.findById(serviceId);
    }
    
    /**
     * Get all services
     * @return List of all services
     */
    public List<Service> getAllServices() {
        return serviceDao.findAll();
    }
    
    /**
     * Get all technicians
     * @return List of all technicians
     */
    public List<Technician> getAllTechnicians() {
        return technicianDao.findAll();
    }
    
    public List<Sale> getSalesByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).findSalesByDateRange(fromDate, toDate);
        } else {
            // Fallback or error handling if saleDao is not an instance of SaleDao
            // This might happen if a generic Dao<Sale, Long> was injected that isn't the specific SaleDao
            System.err.println("[SalesService] Warning: saleDao is not an instance of SaleDao. Cannot fetch by date range.");
            return new ArrayList<>(); // Return empty list as a fallback
        }
    }
    
    public List<ProductSalesReportItem> getProductSalesReport(LocalDate fromDate, LocalDate toDate) {
        List<SaleItem> saleItems;
        if (saleItemDao instanceof SaleItemDao) {
            saleItems = ((SaleItemDao) saleItemDao).findProductItemsBySaleDateRange(fromDate, toDate);
        } else {
            System.err.println("[SalesService] Warning: saleItemDao is not an instance of SaleItemDao. Cannot fetch product sales data.");
            return new ArrayList<>();
        }

        // Aggregate data: Group by product, sum quantity and revenue
        Map<Product, List<SaleItem>> itemsByProduct = saleItems.stream()
            .filter(si -> si.getProduct() != null) // Ensure product is not null
            .collect(Collectors.groupingBy(SaleItem::getProduct));

        List<ProductSalesReportItem> reportItems = new ArrayList<>();
        for (Map.Entry<Product, List<SaleItem>> entry : itemsByProduct.entrySet()) {
            Product product = entry.getKey();
            List<SaleItem> productSaleItems = entry.getValue();

            int totalQuantitySold = productSaleItems.stream().mapToInt(SaleItem::getQuantity).sum();
            BigDecimal totalRevenue = productSaleItems.stream()
                                        .map(SaleItem::getSubtotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            reportItems.add(new ProductSalesReportItem(
                product.getName(),
                product.getCategory(), // Assuming Product has a getCategory() method
                totalQuantitySold,
                totalRevenue
            ));
        }
        return reportItems;
    }
    
    public List<DailyFinancialSummaryItem> getDailyFinancialSummaries(LocalDate fromDate, LocalDate toDate) {
        List<Sale> paidSalesInRange;
        if (saleDao instanceof SaleDao) {
            paidSalesInRange = ((SaleDao) saleDao).findPaidSalesInDateRange(fromDate, toDate);
        } else {
            System.err.println("[SalesService] Warning: saleDao is not an instance of SaleDao. Cannot fetch daily financial summaries.");
            return new ArrayList<>();
        }

        // Group sales by date and aggregate
        Map<LocalDate, List<Sale>> salesByDate = paidSalesInRange.stream()
                .collect(Collectors.groupingBy(sale -> sale.getTimestamp().toLocalDate()));

        List<DailyFinancialSummaryItem> summaries = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Sale>> entry : salesByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Sale> dailySales = entry.getValue();

            BigDecimal dailySubtotal = dailySales.stream()
                    .map(Sale::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dailyTax = dailySales.stream()
                    .map(Sale::getTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dailyTotal = dailySales.stream()
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            summaries.add(new DailyFinancialSummaryItem(date, dailySubtotal, dailyTax, dailyTotal));
        }

        // Sort by date if needed (already ordered by timestamp from DAO, but date grouping might change order)
        summaries.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        return summaries;
    }
    
    public SalesSummaryData getSalesSummaryForDate(LocalDate date) {
        System.out.println("[SalesService] getSalesSummaryForDate called for date: " + date);
        List<Sale> salesOnDate;
        if (saleDao instanceof SaleDao) {
            // Assuming SaleDao has or will have findByDate(LocalDate) method
            salesOnDate = ((SaleDao) saleDao).findByDate(date);
        } else {
            // Fallback: filter all sales by date (less efficient)
            salesOnDate = saleDao.findAll().stream()
                               .filter(s -> s.getTimestamp().toLocalDate().equals(date))
                               .collect(Collectors.toList());
        }

        int numberOfSales = salesOnDate.size();
        BigDecimal totalRevenue = salesOnDate.stream()
                                        .filter(Sale::isPaid) // Consider only paid sales for revenue
                                        .map(Sale::getTotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("[SalesService] Sales on date " + date + ": " + numberOfSales + ", Total Revenue: " + totalRevenue);
        return new SalesSummaryData(numberOfSales, totalRevenue);
    }
    
    // Helper methods
    private String generateInvoiceNumber() {
        // Format: INV-yyyyMMdd-uuid(first 8 chars)
        String timestamp = java.time.LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "INV-" + timestamp + "-" + uuid;
    }
    
    private Optional<Technician> getTechnicianById(Long technicianId) {
        return technicianDao.findById(technicianId);
    }

    /**
     * Update notes for a sale
     * @param saleId Sale ID
     * @param notes The notes to save
     * @return The updated sale or empty Optional if sale not found
     */
    public Optional<Sale> updateSaleNotes(Long saleId, String notes) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();
            sale.setNotes(notes);
            return Optional.of(saleDao.update(sale));
        }
        return Optional.empty();
    }

    public List<Sale> getCustomerPurchaseHistory(Long customerId, LocalDate fromDate, LocalDate toDate) {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).findByCustomerIdAndDateRange(customerId, fromDate, toDate);
        }
        System.err.println("[SalesService] Warning: saleDao is not an instance of SaleDao. Cannot fetch customer purchase history.");
        return new ArrayList<>();
    }

    public List<TechnicianPerformanceReportItem> getTechnicianPerformanceReport(Long technicianId, LocalDate fromDate, LocalDate toDate) {
        List<SaleItem> serviceItems;
        if (saleItemDao instanceof SaleItemDao) {
            serviceItems = ((SaleItemDao) saleItemDao).findServiceItemsByTechnicianAndDateRange(technicianId, fromDate, toDate);
        } else {
            System.err.println("[SalesService] Warning: saleItemDao is not an instance of SaleItemDao. Cannot fetch technician performance data.");
            return new ArrayList<>();
        }

        return serviceItems.stream()
            .filter(si -> si.getService() != null && si.getSale() != null)
            .map(si -> new TechnicianPerformanceReportItem(
                si.getService().getName(),
                si.getSale().getTimestamp(),
                si.getSale().getInvoiceNumber(),
                si.getUnitPrice() // This is the price of the service item
            ))
            .collect(Collectors.toList());
    }
}