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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for sales operations
 */
public class SalesService {
    
    private final GenericDao<Sale, Long> saleDao;
    private final GenericDao<SaleItem, Long> saleItemDao;
    private final GenericDao<SalePayment, Long> salePaymentDao;
    private final GenericDao<Customer, Long> customerDao;
    private final GenericDao<Vehicle, Long> vehicleDao;
    private final GenericDao<Service, Long> serviceDao;
    private final GenericDao<Technician, Long> technicianDao;
    private final GenericDao<ChargeAccountPayment, Long> chargeAccountPaymentDao;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;
    private final VehicleServiceHistoryService vehicleServiceHistoryService;

    public SalesService(
            GenericDao<Sale, Long> saleDao,
            GenericDao<SaleItem, Long> saleItemDao,
            GenericDao<SalePayment, Long> salePaymentDao,
            GenericDao<Customer, Long> customerDao,
            GenericDao<Vehicle, Long> vehicleDao,
            GenericDao<Service, Long> serviceDao,
            GenericDao<Technician, Long> technicianDao,
            GenericDao<ChargeAccountPayment, Long> chargeAccountPaymentDao,
            InventoryService inventoryService,
            SettingsService settingsService,
            VehicleServiceHistoryService vehicleServiceHistoryService) {
        this.saleDao = saleDao;
        this.saleItemDao = saleItemDao;
        this.salePaymentDao = salePaymentDao;
        this.customerDao = customerDao;
        this.vehicleDao = vehicleDao;
        this.serviceDao = serviceDao;
        this.technicianDao = technicianDao;
        this.chargeAccountPaymentDao = chargeAccountPaymentDao;
        this.inventoryService = inventoryService;
        this.settingsService = settingsService;
        this.vehicleServiceHistoryService = vehicleServiceHistoryService;
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
        if (quantity <= 0) {
            System.out.println("[SalesService] Rejected addProductToSale - invalid quantity: " + quantity);
            return Optional.empty();
        }

        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (optionalSale.isPresent() && inventoryService.getProductById(productId).isPresent()) {
            Sale sale = optionalSale.get();
            Product product = inventoryService.getProductById(productId).get();

            // Products without a price (e.g. auto-scraped tires) cannot be sold
            // until a price is set - prevents downstream NPEs and $0 line items
            if (product.getSellingPrice() == null) {
                System.out.println("[SalesService] Rejected addProductToSale - product has no price: "
                        + product.getName() + " (ID: " + productId + ")");
                return Optional.empty();
            }

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
        System.out.println("[SalesService] removeItemFromSale called - SaleID: " + saleId + ", ItemID: " + saleItemId);

        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (!optionalSale.isPresent()) {
            System.out.println("[SalesService] Sale not found");
            return Optional.empty();
        }

        Sale sale = optionalSale.get();
        System.out.println("[SalesService] Sale found with " + sale.getItems().size() + " items");

        // Find the sale item within the sale's items collection by ID
        // This ensures we get the exact object instance that Hibernate is tracking
        SaleItem saleItemToRemove = null;
        for (SaleItem item : sale.getItems()) {
            if (item.getId() != null && item.getId().equals(saleItemId)) {
                saleItemToRemove = item;
                break;
            }
        }

        if (saleItemToRemove == null) {
            System.out.println("[SalesService] Sale item with ID " + saleItemId + " not found in sale's items");
            return Optional.empty();
        }

        System.out.println("[SalesService] Found item to remove: " + saleItemToRemove.getItemName());

        // If it's a product, add it back to inventory
        if ("PRODUCT".equals(saleItemToRemove.getItemType()) && saleItemToRemove.getProduct() != null) {
            inventoryService.addInventory(saleItemToRemove.getProduct().getId(), saleItemToRemove.getQuantity());
            System.out.println("[SalesService] Restored " + saleItemToRemove.getQuantity() + " to inventory");
        }

        // Remove from the collection - this will trigger orphanRemoval
        sale.getItems().remove(saleItemToRemove);
        saleItemToRemove.setSale(null);

        System.out.println("[SalesService] Item removed from collection, now " + sale.getItems().size() + " items");

        // Recalculate amounts
        sale.recalculateAmounts(settingsService.getSalesTaxRate(),
                              PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);

        // Update the sale - orphanRemoval should delete the item
        Sale updatedSale = saleDao.update(sale);
        System.out.println("[SalesService] Sale updated successfully");

        return Optional.of(updatedSale);
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
            // Set paid date to now (timestamp updates automatically via setPaidDate)
            sale.setPaidDate(LocalDateTime.now());
            // Recalculate based on the now-set payment type
            BigDecimal ccFeePercent = (PaymentType.CREDIT_CARD.equals(paymentType) || PaymentType.DEBIT_CARD.equals(paymentType)) ? 
                                      settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO;
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), ccFeePercent);
            Sale completedSale = saleDao.update(sale);
            
            // Record the payment for reporting (no-op if payments already recorded, e.g. split)
            recordSinglePayment(completedSale);

            // Create service record if vehicle is associated
            createServiceRecordSafely(completedSale);

            return Optional.of(completedSale);
        }
        
        return Optional.empty();
    }
    
    /**
     * Complete a sale paid with multiple payment methods (split payment).
     * Creates a real SalePayment record for each part so reports and
     * cash/card totals stay accurate.
     *
     * @param saleId Sale ID
     * @param payments Payment parts - amounts must be NET amounts (cash change already deducted)
     * @param changeGiven Change given back to the customer (cash overpayment), recorded in notes
     * @return The completed sale or empty Optional if validation fails
     */
    public Optional<Sale> completeSplitSale(Long saleId, List<SalePayment> payments, BigDecimal changeGiven) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (!optionalSale.isPresent() || payments == null || payments.isEmpty()) {
            return Optional.empty();
        }

        Sale sale = optionalSale.get();

        // Validate: every part must be a positive amount
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal storeChargeTotal = BigDecimal.ZERO;
        for (SalePayment payment : payments) {
            if (payment.getPaymentType() == null || payment.getAmount() == null
                    || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("[SalesService] Invalid split payment part rejected");
                return Optional.empty();
            }
            if (payment.getPaymentType() == PaymentType.STORE_CHARGE) {
                storeChargeTotal = storeChargeTotal.add(payment.getAmount());
            }
            totalPaid = totalPaid.add(payment.getAmount());
        }

        // Store charge parts need a customer account to charge against
        if (storeChargeTotal.compareTo(BigDecimal.ZERO) > 0 && sale.getCustomer() == null) {
            System.out.println("[SalesService] Split with store charge requires a customer on the sale");
            return Optional.empty();
        }

        // Parts must cover the sale total
        BigDecimal saleTotal = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;
        if (totalPaid.compareTo(saleTotal) < 0) {
            System.out.println("[SalesService] Split payments (" + totalPaid
                    + ") do not cover sale total (" + saleTotal + ")");
            return Optional.empty();
        }

        // Primary payment type = type of the largest part (used by simple displays/reports)
        SalePayment primary = payments.get(0);
        for (SalePayment payment : payments) {
            if (payment.getAmount().compareTo(primary.getAmount()) > 0) {
                primary = payment;
            }
        }

        sale.setPaymentType(primary.getPaymentType());
        sale.setPaid(true);
        sale.setPaidDate(LocalDateTime.now());
        // Card fees are handled by the external card machine - no extra fee for splits
        sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO);
        Sale completedSale = saleDao.update(sale);

        // Persist each payment part as a real record
        for (SalePayment payment : payments) {
            payment.setSale(completedSale);
            salePaymentDao.save(payment);
        }

        // Store charge parts go on the customer's charge account balance
        if (storeChargeTotal.compareTo(BigDecimal.ZERO) > 0) {
            Customer customer = completedSale.getCustomer();
            customer.setChargeBalance(customer.getChargeBalance().add(storeChargeTotal));
            customerDao.update(customer);
            System.out.println("[SalesService] Split: charged $" + storeChargeTotal + " to "
                    + customer.getFullName() + "'s account (new balance: $" + customer.getChargeBalance() + ")");
        }

        // Keep the human-readable split block in notes - the receipt printer reads it
        StringBuilder paymentNotes = new StringBuilder("SPLIT PAYMENT:\n");
        for (SalePayment payment : payments) {
            paymentNotes.append("  ").append(payment.getPaymentType().getDisplayName())
                        .append(": $").append(payment.getAmount().setScale(2, RoundingMode.HALF_UP)).append("\n");
        }
        if (changeGiven != null && changeGiven.compareTo(BigDecimal.ZERO) > 0) {
            paymentNotes.append("  Change Given: $")
                        .append(changeGiven.setScale(2, RoundingMode.HALF_UP)).append("\n");
        }
        String existingNotes = completedSale.getNotes() != null ? completedSale.getNotes() + "\n\n" : "";
        completedSale.setNotes(existingNotes + paymentNotes.toString().trim());
        completedSale = saleDao.update(completedSale);

        // Create service record if vehicle is associated
        createServiceRecordSafely(completedSale);

        return Optional.of(completedSale);
    }

    /**
     * Create a service history record for a completed sale without ever letting a
     * failure here break payment completion. Re-fetches the sale so its item graph
     * is fully initialized outside of a Hibernate session.
     */
    private void createServiceRecordSafely(Sale completedSale) {
        try {
            if (vehicleServiceHistoryService == null || completedSale.getVehicle() == null) {
                return;
            }
            Optional<Sale> fresh = saleDao.findById(completedSale.getId());
            vehicleServiceHistoryService.createServiceRecordFromSale(fresh.orElse(completedSale));
        } catch (Exception e) {
            System.err.println("[SalesService] Could not create service record for sale "
                    + completedSale.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Record a single SalePayment for a single-payment sale, so all paid sales
     * have payment records for accurate reporting. Does nothing if the sale
     * already has payment records (e.g. split payments).
     */
    private void recordSinglePayment(Sale completedSale) {
        try {
            if (salePaymentDao instanceof com.tireshop.dao.SalePaymentDao) {
                List<SalePayment> existing = ((com.tireshop.dao.SalePaymentDao) salePaymentDao)
                        .findBySaleId(completedSale.getId());
                if (!existing.isEmpty()) {
                    return; // Already has payment records
                }
            }
            BigDecimal amount = completedSale.getTotal() != null ? completedSale.getTotal() : BigDecimal.ZERO;
            SalePayment payment = new SalePayment(completedSale, completedSale.getPaymentType(), amount);
            payment.setCardType(completedSale.getCardType());
            payment.setCardLastFour(completedSale.getCardLastFour());
            payment.setAuthorizationCode(completedSale.getAuthorizationCode());
            payment.setCheckNumber(completedSale.getCheckNumber());
            salePaymentDao.save(payment);
        } catch (Exception e) {
            // Payment record is for reporting - never fail a sale because of it
            System.err.println("[SalesService] Could not record payment record: " + e.getMessage());
        }
    }

    /**
     * Apply the military discount to a pending sale.
     * The percentage is configured in Admin settings (discount.military.percent).
     *
     * @param saleId Sale ID
     * @return The updated sale, or empty if the sale can't be discounted
     */
    public Optional<Sale> applyMilitaryDiscount(Long saleId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (!optionalSale.isPresent()) {
            return Optional.empty();
        }

        Sale sale = optionalSale.get();
        if (sale.isPaid()) {
            System.out.println("[SalesService] Cannot apply discount - sale already paid");
            return Optional.empty();
        }

        BigDecimal percent = settingsService.getMilitaryDiscountPercent();
        if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[SalesService] Military discount is disabled (0%)");
            return Optional.empty();
        }

        BigDecimal subtotal = sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = subtotal.multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        sale.setDiscountAmount(discount);
        sale.setDiscountReason("Military (" + percent.stripTrailingZeros().toPlainString() + "%)");
        sale.recalculateAmounts(settingsService.getSalesTaxRate(),
                PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);

        System.out.println("[SalesService] Applied military discount: $" + discount + " (" + percent + "%) to sale " + saleId);
        return Optional.of(saleDao.update(sale));
    }

    /**
     * Remove any sale-level discount from a pending sale.
     */
    public Optional<Sale> removeDiscount(Long saleId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (!optionalSale.isPresent()) {
            return Optional.empty();
        }

        Sale sale = optionalSale.get();
        if (sale.isPaid()) {
            return Optional.empty();
        }

        sale.setDiscountAmount(BigDecimal.ZERO);
        sale.setDiscountReason(null);
        sale.recalculateAmounts(settingsService.getSalesTaxRate(),
                PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
        return Optional.of(saleDao.update(sale));
    }

    /**
     * Complete a sale by charging it to the customer's store charge account.
     * The sale is marked paid and the total is added to the customer's balance.
     *
     * @param saleId Sale ID (must have a customer assigned)
     * @return The completed sale, or empty if there is no customer on the sale
     */
    public Optional<Sale> completeStoreCharge(Long saleId) {
        Optional<Sale> optionalSale = saleDao.findById(saleId);
        if (!optionalSale.isPresent()) {
            return Optional.empty();
        }

        Sale sale = optionalSale.get();
        Customer customer = sale.getCustomer();
        if (customer == null) {
            System.out.println("[SalesService] Store charge requires a customer on the sale");
            return Optional.empty();
        }

        sale.setPaymentType(PaymentType.STORE_CHARGE);
        sale.setPaid(true);
        sale.setPaidDate(LocalDateTime.now());
        sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO);
        Sale completedSale = saleDao.update(sale);

        // Add the total to the customer's charge account balance
        BigDecimal total = completedSale.getTotal() != null ? completedSale.getTotal() : BigDecimal.ZERO;
        customer.setChargeBalance(customer.getChargeBalance().add(total));
        customerDao.update(customer);
        System.out.println("[SalesService] Store charge: added $" + total + " to "
                + customer.getFullName() + "'s account (new balance: $" + customer.getChargeBalance() + ")");

        // Record the payment for reporting
        recordSinglePayment(completedSale);

        // Create service record if vehicle is associated
        createServiceRecordSafely(completedSale);

        return Optional.of(completedSale);
    }

    /**
     * Record a payment against a customer's store charge balance.
     *
     * @param customerId Customer ID
     * @param amount Payment amount (must be positive, capped at the balance)
     * @return The new balance, or empty if the payment was invalid
     */
    public Optional<BigDecimal> recordChargeAccountPayment(Long customerId, BigDecimal amount) {
        return recordChargeAccountPaymentDetailed(customerId, amount)
                .map(ChargeAccountPayment::getBalanceAfter);
    }

    /**
     * Record a payment against a customer's store charge balance, returning the
     * persisted payment record (for receipts).
     *
     * @param customerId Customer ID
     * @param amount Payment amount (must be positive, capped at the balance)
     * @return The payment record, or empty if the payment was invalid
     */
    public Optional<ChargeAccountPayment> recordChargeAccountPaymentDetailed(Long customerId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        Optional<Customer> optionalCustomer = customerDao.findById(customerId);
        if (!optionalCustomer.isPresent()) {
            return Optional.empty();
        }

        Customer customer = optionalCustomer.get();
        BigDecimal balance = customer.getChargeBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty(); // Nothing to pay
        }

        // Cap at the outstanding balance (overpay goes back as change, handled by caller)
        BigDecimal applied = amount.min(balance);
        customer.setChargeBalance(balance.subtract(applied));
        customerDao.update(customer);

        // Keep an auditable record of the payoff
        ChargeAccountPayment accountPayment = new ChargeAccountPayment(customer, applied);
        accountPayment.setBalanceAfter(customer.getChargeBalance());
        chargeAccountPaymentDao.save(accountPayment);

        System.out.println("[SalesService] Charge account payment: $" + applied + " from "
                + customer.getFullName() + " (remaining balance: $" + customer.getChargeBalance() + ")");
        return Optional.of(accountPayment);
    }

    /**
     * Combined store charge account activity for a customer:
     * charges made against the account and payoff payments made towards it.
     */
    public static class ChargeAccountActivity {
        public final List<SalePayment> charges;
        public final List<ChargeAccountPayment> payments;

        public ChargeAccountActivity(List<SalePayment> charges, List<ChargeAccountPayment> payments) {
            this.charges = charges;
            this.payments = payments;
        }
    }

    /**
     * Get the full store charge account history for a customer.
     */
    public ChargeAccountActivity getChargeAccountActivity(Long customerId) {
        List<SalePayment> charges = new ArrayList<>();
        List<ChargeAccountPayment> payments = new ArrayList<>();
        if (salePaymentDao instanceof com.tireshop.dao.SalePaymentDao) {
            charges = ((com.tireshop.dao.SalePaymentDao) salePaymentDao).findStoreChargesByCustomer(customerId);
        }
        if (chargeAccountPaymentDao instanceof com.tireshop.dao.ChargeAccountPaymentDao) {
            payments = ((com.tireshop.dao.ChargeAccountPaymentDao) chargeAccountPaymentDao).findByCustomerId(customerId);
        }
        return new ChargeAccountActivity(charges, payments);
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
            // Set paid date to now
            sale.setPaidDate(LocalDateTime.now());
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), settingsService.getCreditCardFeePercentage());
            Sale completedSale = saleDao.update(sale);
            
            // Record the payment for reporting (no-op if payments already recorded, e.g. split)
            recordSinglePayment(completedSale);

            // Create service record if vehicle is associated
            createServiceRecordSafely(completedSale);

            return Optional.of(completedSale);
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
            // Set paid date to now
            sale.setPaidDate(LocalDateTime.now());
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO); // No CC fee
            Sale completedSale = saleDao.update(sale);
            
            // Record the payment for reporting (no-op if payments already recorded, e.g. split)
            recordSinglePayment(completedSale);

            // Create service record if vehicle is associated
            createServiceRecordSafely(completedSale);

            return Optional.of(completedSale);
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
            // Set paid date to now
            sale.setPaidDate(LocalDateTime.now());
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), BigDecimal.ZERO); // No CC fee
            Sale completedSale = saleDao.update(sale);
            
            // Record the payment for reporting (no-op if payments already recorded, e.g. split)
            recordSinglePayment(completedSale);

            // Create service record if vehicle is associated
            createServiceRecordSafely(completedSale);

            return Optional.of(completedSale);
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
     * Get a sale by ID with all related data loaded (customer, vehicle, items)
     * Use this for detail views, receipts, etc.
     * @param saleId Sale ID
     * @return Sale with all details or null if not found
     */
    public Sale getSaleByIdWithDetails(Long saleId) {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).findByIdWithDetails(saleId);
        }
        return saleDao.findById(saleId).orElse(null);
    }

    /**
     * Get all sales (optimized for table display - loads customer only)
     * @return List of all sales with customer info
     */
    public List<Sale> getAllSales() {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).findAllWithCustomer();
        }
        return saleDao.findAll();
    }

    /**
     * Get total count of sales
     * @return Total number of sales
     */
    public int getTotalSalesCount() {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).getTotalCount();
        }
        return saleDao.findAll().size();
    }

    /**
     * Get sales with pagination
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return List of sales for the requested page
     */
    public List<Sale> getSalesPaginated(int page, int pageSize) {
        if (saleDao instanceof SaleDao) {
            return ((SaleDao) saleDao).findPaginated(page, pageSize);
        }
        // Fallback for non-SaleDao implementations
        List<Sale> allSales = saleDao.findAll();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allSales.size());
        if (fromIndex >= allSales.size()) {
            return new ArrayList<>();
        }
        return allSales.subList(fromIndex, toIndex);
    }

    /**
     * Get all customers
     * @return List of all customers
     */
    public List<Customer> getAllCustomers() {
        return customerDao.findAll();
    }
    
    /**
     * Get all customers with their vehicles loaded
     * @return List of all customers with vehicles
     */
    public List<Customer> getAllCustomersWithVehicles() {
        List<Customer> customers = customerDao.findAll();
        // Load vehicles for each customer
        for (Customer customer : customers) {
            if (customer.getId() != null) {
                List<Vehicle> vehicles = getVehiclesForCustomer(customer.getId());
                customer.setVehicles(vehicles);
            }
        }
        return customers;
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
        try {
            Optional<Sale> optionalSale = saleDao.findById(saleId);
            
            if (optionalSale.isPresent()) {
                Sale sale = optionalSale.get();
                
                // Only allow canceling unpaid sales
                if (sale.isPaid()) {
                    System.err.println("[SalesService] Cannot cancel a paid sale. Use voidSale() for returns.");
                    return false;
                }
                
                // Restore inventory for product items BEFORE deleting
                for (SaleItem item : sale.getItems()) {
                    if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                        inventoryService.addInventory(item.getProduct().getId(), item.getQuantity());
                    }
                }
                
                // Delete the sale - SaleItems will be automatically deleted due to cascade configuration
                // Don't manually delete SaleItems as this conflicts with the cascade
                return saleDao.deleteById(saleId);
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error canceling sale with ID " + saleId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Void/Return a completed sale and restore inventory
     * Used for customer returns - marks sale as voided and restores all products to inventory
     * The voided sale is kept in database for accounting records
     * @param saleId Sale ID
     * @param reason Reason for voiding (e.g., "Customer Return", "Cashier Error")
     * @return The voided sale or empty Optional if sale not found, not paid, or already voided
     */
    public Optional<Sale> voidSale(Long saleId, String reason) {
        try {
            Optional<Sale> optionalSale = saleDao.findById(saleId);
            
            if (optionalSale.isPresent()) {
                Sale sale = optionalSale.get();
                
                // Only void paid sales (use cancelSale for unpaid)
                if (!sale.isPaid()) {
                    System.err.println("[SalesService] Cannot void an unpaid sale. Use cancelSale() instead.");
                    return Optional.empty();
                }
                
                // Already voided?
                if (sale.isVoided()) {
                    System.err.println("[SalesService] Sale " + sale.getInvoiceNumber() + " is already voided.");
                    return Optional.empty();
                }
                
                System.out.println("[SalesService] ⚠️ VOIDING SALE " + sale.getInvoiceNumber() + " - Reason: " + reason);
                
                // Restore inventory for all product items. Only restore units that are
                // still "out" - units already returned via a partial return were put
                // back in stock then, and must not be restored twice.
                for (SaleItem item : sale.getItems()) {
                    if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                        int remainingOut = item.getQuantity() - item.getQuantityReturned();
                        if (remainingOut > 0) {
                            inventoryService.addInventory(item.getProduct().getId(), remainingOut);
                            System.out.println("[SalesService] ✓ Restored " + remainingOut + " × " +
                                             item.getProduct().getName() + " to inventory");
                        }
                    }
                }
                
                // Mark sale as voided (keep record for accounting/audit trail)
                sale.setVoided(true);
                sale.setVoidReason(reason != null ? reason : "Customer Return");
                sale.setVoidTimestamp(LocalDateTime.now());

                Sale voidedSale = saleDao.update(sale);

                // Reverse any store charge so the customer's account isn't left
                // holding a balance for merchandise they returned
                reverseStoreChargeIfNeeded(voidedSale);

                System.out.println("[SalesService] ✅ Sale " + sale.getInvoiceNumber() + " voided successfully - inventory restored");

                return Optional.of(voidedSale);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("❌ Error voiding sale with ID " + saleId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * If a voided sale had store charge payments, subtract them from the customer's
     * charge account balance and record an auditable reversal entry.
     * Never lets a failure here break the void itself.
     */
    private void reverseStoreChargeIfNeeded(Sale voidedSale) {
        try {
            if (!(salePaymentDao instanceof com.tireshop.dao.SalePaymentDao)) {
                return;
            }
            List<SalePayment> payments = ((com.tireshop.dao.SalePaymentDao) salePaymentDao)
                    .findBySaleId(voidedSale.getId());
            BigDecimal chargedAmount = BigDecimal.ZERO;
            for (SalePayment payment : payments) {
                if (payment.getPaymentType() == PaymentType.STORE_CHARGE && payment.getAmount() != null) {
                    chargedAmount = chargedAmount.add(payment.getAmount());
                }
            }
            if (chargedAmount.compareTo(BigDecimal.ZERO) <= 0 || voidedSale.getCustomer() == null) {
                return;
            }

            Customer customer = voidedSale.getCustomer();
            // Balance may go negative = shop owes the customer (they already paid down the charge)
            BigDecimal newBalance = customer.getChargeBalance().subtract(chargedAmount);
            customer.setChargeBalance(newBalance);
            customerDao.update(customer);

            ChargeAccountPayment reversal = new ChargeAccountPayment(customer, chargedAmount.negate());
            reversal.setBalanceAfter(newBalance);
            reversal.setNotes("Charge reversed - voided sale " + voidedSale.getInvoiceNumber());
            chargeAccountPaymentDao.save(reversal);

            System.out.println("[SalesService] Void: reversed $" + chargedAmount + " store charge on "
                    + customer.getFullName() + "'s account (new balance: $" + newBalance + ")");
        } catch (Exception e) {
            System.err.println("[SalesService] Could not reverse store charge for voided sale "
                    + voidedSale.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Update sale item price (only for pending sales)
     * @param saleId Sale ID
     * @param saleItemId Sale Item ID
     * @param newUnitPrice New unit price
     * @return The updated sale or empty Optional if sale not found or already paid
     */
    public Optional<Sale> updateSaleItemPrice(Long saleId, Long saleItemId, BigDecimal newUnitPrice) {
        System.out.println("[SalesService] Updating sale item price - SaleID: " + saleId + ", ItemID: " + saleItemId + ", New Price: " + newUnitPrice);
        
        // Refresh sale from database to get latest data
        Optional<Sale> freshSale = saleDao.findById(saleId);
        if (!freshSale.isPresent()) {
            System.out.println("[SalesService] Sale not found");
            return Optional.empty();
        }
        
        Sale sale = freshSale.get();
        
        // Only allow editing if sale is not paid
        if (sale.isPaid()) {
            System.out.println("[SalesService] Cannot update - sale is already paid");
            return Optional.empty();
        }
        
        // Find the sale item within the sale's items collection
        SaleItem saleItemToUpdate = null;
        for (SaleItem item : sale.getItems()) {
            if (item.getId().equals(saleItemId)) {
                saleItemToUpdate = item;
                break;
            }
        }
        
        if (saleItemToUpdate == null) {
            System.out.println("[SalesService] Sale item not found in sale");
            return Optional.empty();
        }
        
        System.out.println("[SalesService] Current price: " + saleItemToUpdate.getUnitPrice());
        
        // Update the price directly on the attached entity
        saleItemToUpdate.setUnitPrice(newUnitPrice);
        saleItemToUpdate.calculateSubtotal();
        
        System.out.println("[SalesService] New price set: " + saleItemToUpdate.getUnitPrice() + ", New subtotal: " + saleItemToUpdate.getSubtotal());
        
        // Recalculate sale totals
        BigDecimal oldTotal = sale.getTotal();
        sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                              PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
        System.out.println("[SalesService] Sale total updated from " + oldTotal + " to " + sale.getTotal());
        
        // Update the sale (which will cascade to sale items)
        Sale updatedSale = saleDao.update(sale);
        System.out.println("[SalesService] Sale and items updated in database successfully");
        
        // Verify the update worked
        Optional<Sale> verifiedSale = saleDao.findById(saleId);
        if (verifiedSale.isPresent()) {
            for (SaleItem item : verifiedSale.get().getItems()) {
                if (item.getId().equals(saleItemId)) {
                    System.out.println("[SalesService] VERIFICATION - Item price in DB is now: " + item.getUnitPrice());
                    break;
                }
            }
        }
        
        return Optional.of(updatedSale);
    }
    
    /**
     * Update sale item quantity (only for pending sales)
     * @param saleId Sale ID
     * @param saleItemId Sale Item ID
     * @param newQuantity New quantity
     * @return The updated sale or empty Optional if sale not found, already paid, or insufficient inventory
     */
    public Optional<Sale> updateSaleItemQuantity(Long saleId, Long saleItemId, int newQuantity) {
        System.out.println("[SalesService] Updating sale item quantity - SaleID: " + saleId + ", ItemID: " + saleItemId + ", New Quantity: " + newQuantity);
        
        // Refresh sale from database to get latest data
        Optional<Sale> freshSale = saleDao.findById(saleId);
        if (!freshSale.isPresent()) {
            System.out.println("[SalesService] Sale not found");
            return Optional.empty();
        }
        
        Sale sale = freshSale.get();
        
        // Only allow editing if sale is not paid
        if (sale.isPaid()) {
            System.out.println("[SalesService] Cannot update - sale is already paid");
            return Optional.empty();
        }
        
        // Find the sale item within the sale's items collection
        SaleItem saleItemToUpdate = null;
        for (SaleItem item : sale.getItems()) {
            if (item.getId().equals(saleItemId)) {
                saleItemToUpdate = item;
                break;
            }
        }
        
        if (saleItemToUpdate == null) {
            System.out.println("[SalesService] Sale item not found in sale");
            return Optional.empty();
        }
        
        int oldQuantity = saleItemToUpdate.getQuantity();
        int quantityDifference = newQuantity - oldQuantity;
        
        System.out.println("[SalesService] Old quantity: " + oldQuantity + ", Quantity difference: " + quantityDifference);
        
        // If this is a product, we need to adjust inventory
        if ("PRODUCT".equals(saleItemToUpdate.getItemType()) && saleItemToUpdate.getProduct() != null) {
            Product product = saleItemToUpdate.getProduct();
            
            if (quantityDifference > 0) {
                // Increasing quantity - need to remove more from inventory
                if (product.getQuantityInStock() < quantityDifference) {
                    System.out.println("[SalesService] Insufficient inventory - available: " + product.getQuantityInStock() + ", needed: " + quantityDifference);
                    return Optional.empty();
                }
                inventoryService.removeInventory(product.getId(), quantityDifference);
                System.out.println("[SalesService] Removed " + quantityDifference + " from inventory");
            } else if (quantityDifference < 0) {
                // Decreasing quantity - add back to inventory
                inventoryService.addInventory(product.getId(), Math.abs(quantityDifference));
                System.out.println("[SalesService] Added " + Math.abs(quantityDifference) + " back to inventory");
            }
        }
        
        // Update the quantity
        saleItemToUpdate.setQuantity(newQuantity);
        saleItemToUpdate.calculateSubtotal();
        
        System.out.println("[SalesService] New quantity set: " + saleItemToUpdate.getQuantity() + ", New subtotal: " + saleItemToUpdate.getSubtotal());
        
        // Recalculate sale totals
        sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                              PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
        
        // Update the sale
        Sale updatedSale = saleDao.update(sale);
        System.out.println("[SalesService] Sale and items updated in database successfully");
        
        return Optional.of(updatedSale);
    }
    
    /**
     * Update sale item price and description (only for pending sales)
     * @param saleId Sale ID
     * @param saleItemId Sale Item ID
     * @param newUnitPrice New unit price
     * @param newDescription New description (for custom items only)
     * @return The updated sale or empty Optional if sale not found or already paid
     */
    public Optional<Sale> updateSaleItemPriceAndDescription(Long saleId, Long saleItemId, BigDecimal newUnitPrice, String newDescription) {
        System.out.println("[SalesService] Updating sale item price and description - SaleID: " + saleId + ", ItemID: " + saleItemId + ", New Price: " + newUnitPrice + ", New Description: " + newDescription);
        
        // Refresh sale from database to get latest data
        Optional<Sale> freshSale = saleDao.findById(saleId);
        if (!freshSale.isPresent()) {
            System.out.println("[SalesService] Sale not found");
            return Optional.empty();
        }
        
        Sale sale = freshSale.get();
        
        // Only allow editing if sale is not paid
        if (sale.isPaid()) {
            System.out.println("[SalesService] Cannot update - sale is already paid");
            return Optional.empty();
        }
        
        // Find the sale item within the sale's items collection
        SaleItem saleItemToUpdate = null;
        for (SaleItem item : sale.getItems()) {
            if (item.getId().equals(saleItemId)) {
                saleItemToUpdate = item;
                break;
            }
        }
        
        if (saleItemToUpdate == null) {
            System.out.println("[SalesService] Sale item not found in sale");
            return Optional.empty();
        }
        
        System.out.println("[SalesService] Current price: " + saleItemToUpdate.getUnitPrice());
        System.out.println("[SalesService] Current description: " + saleItemToUpdate.getCustomItemName());
        
        // Update the price and description directly on the attached entity
        saleItemToUpdate.setUnitPrice(newUnitPrice);
        if (saleItemToUpdate.getItemType().equals("CUSTOM") && newDescription != null) {
            saleItemToUpdate.setCustomItemName(newDescription);
        }
        saleItemToUpdate.calculateSubtotal();
        
        System.out.println("[SalesService] New price set: " + saleItemToUpdate.getUnitPrice() + ", New description: " + saleItemToUpdate.getCustomItemName() + ", New subtotal: " + saleItemToUpdate.getSubtotal());
        
        // Recalculate sale totals
        BigDecimal oldTotal = sale.getTotal();
        sale.recalculateAmounts(settingsService.getSalesTaxRate(),
                              PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
        System.out.println("[SalesService] Sale total updated from " + oldTotal + " to " + sale.getTotal());
        
        // Update the sale (which will cascade to sale items)
        Sale updatedSale = saleDao.update(sale);
        System.out.println("[SalesService] Sale and items updated in database successfully");
        
        return Optional.of(updatedSale);
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

        // Filter out items from voided sales BEFORE aggregating
        saleItems = saleItems.stream()
            .filter(item -> item.getSale() != null && !item.getSale().isVoided())
            .collect(Collectors.toList());

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

        // Filter out voided sales for accurate financial reporting
        paidSalesInRange = paidSalesInRange.stream()
                .filter(sale -> !sale.isVoided())
                .collect(Collectors.toList());

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

        // Filter out voided sales for accurate counts and revenue
        List<Sale> validSales = salesOnDate.stream()
                                .filter(sale -> !sale.isVoided())
                                .collect(Collectors.toList());

        int numberOfSales = validSales.size();
        BigDecimal totalRevenue = validSales.stream()
                                        .filter(Sale::isPaid) // Consider only paid sales for revenue
                                        .map(Sale::getTotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("[SalesService] Sales on date " + date + ": " + numberOfSales + " (valid), Total Revenue: $" + totalRevenue + 
                         " (excluded " + (salesOnDate.size() - validSales.size()) + " voided sales)");
        return new SalesSummaryData(numberOfSales, totalRevenue);
    }
    
    /**
     * Process partial return - return some items from a sale
     * @param saleId Sale ID
     * @param returnItems List of items and quantities to return
     * @param reason Return reason
     * @return Updated sale or empty if failed
     */
    public Optional<Sale> partialReturnSale(Long saleId, List<com.tireshop.controller.SalesController.ReturnItem> returnItems, String reason) {
        try {
            Optional<Sale> optionalSale = saleDao.findById(saleId);
            
            if (optionalSale.isPresent()) {
                Sale sale = optionalSale.get();
                
                if (!sale.isPaid() || sale.isVoided()) {
                    System.err.println("[SalesService] Cannot partial return - sale not paid or already voided");
                    return Optional.empty();
                }
                
                System.out.println("[SalesService] Processing partial return for " + sale.getInvoiceNumber());

                // Capture pre-return total so we can adjust the charge account afterwards
                BigDecimal totalBeforeReturn = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;

                // Process each returned item. Match by ID against THIS sale's own items -
                // the caller's item objects may come from a different (detached) sale instance,
                // and modifying those would silently lose the return.
                for (com.tireshop.controller.SalesController.ReturnItem returnItem : returnItems) {
                    if (returnItem.item == null || returnItem.item.getId() == null || returnItem.quantity <= 0) {
                        continue;
                    }
                    SaleItem saleItem = null;
                    for (SaleItem si : sale.getItems()) {
                        if (returnItem.item.getId().equals(si.getId())) {
                            saleItem = si;
                            break;
                        }
                    }
                    if (saleItem == null) {
                        System.err.println("[SalesService] Return item " + returnItem.item.getId()
                                + " does not belong to sale " + sale.getInvoiceNumber() + " - skipped");
                        continue;
                    }

                    // Clamp: can't return more units than are still outstanding on this line
                    // (a previous partial return may have already returned some)
                    int stillOut = saleItem.getQuantity() - saleItem.getQuantityReturned();
                    int returnQty = Math.min(returnItem.quantity, stillOut);
                    if (returnQty <= 0) {
                        System.err.println("[SalesService] All units of " + saleItem.getItemName()
                                + " were already returned - skipped");
                        continue;
                    }

                    // Update quantity returned
                    saleItem.setQuantityReturned(saleItem.getQuantityReturned() + returnQty);

                    // Restore inventory for products
                    if ("PRODUCT".equals(saleItem.getItemType()) && saleItem.getProduct() != null) {
                        inventoryService.addInventory(saleItem.getProduct().getId(), returnQty);
                        System.out.println("[SalesService] Restored " + returnQty + " of " +
                                         saleItem.getProduct().getName() + " to inventory");
                    }
                }
                
                // Mark sale as having partial return
                sale.setHasPartialReturn(true);
                
                // Recalculate sale totals to reflect returned items
                // Subtract returned items from the sale total
                BigDecimal newSubtotal = BigDecimal.ZERO;
                BigDecimal taxableSubtotal = BigDecimal.ZERO;
                
                for (SaleItem item : sale.getItems()) {
                    // Only count items that weren't fully returned
                    int remainingQty = item.getQuantity() - item.getQuantityReturned();
                    if (remainingQty > 0) {
                        BigDecimal itemSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(remainingQty));
                        newSubtotal = newSubtotal.add(itemSubtotal);
                        
                        // Add to taxable subtotal if item is taxable
                        if (item.isTaxable()) {
                            taxableSubtotal = taxableSubtotal.add(itemSubtotal);
                        }
                    }
                }
                
                // Re-apply the sale-level discount (e.g. military) the same way
                // Sale.recalculateAmounts does - capped at the new subtotal, and
                // reducing the taxable base (floor at zero)
                BigDecimal discount = sale.getDiscountAmount() != null ? sale.getDiscountAmount() : BigDecimal.ZERO;
                if (discount.compareTo(newSubtotal) > 0) {
                    discount = newSubtotal;
                }
                BigDecimal discountedSubtotal = newSubtotal.subtract(discount);
                BigDecimal discountedTaxable = taxableSubtotal.subtract(discount);
                if (discountedTaxable.compareTo(BigDecimal.ZERO) < 0) {
                    discountedTaxable = BigDecimal.ZERO;
                }

                // Check if customer is tax exempt
                boolean isTaxExempt = (sale.getCustomer() != null && sale.getCustomer().isTaxExempt());

                // Calculate new tax
                BigDecimal newTax = BigDecimal.ZERO;
                if (!isTaxExempt && discountedTaxable.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal taxRate = settingsService.getSalesTaxRate();
                    newTax = discountedTaxable.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
                }

                // Calculate new total
                BigDecimal newTotal = discountedSubtotal.add(newTax);
                
                // Update sale amounts
                sale.setSubtotal(newSubtotal);
                sale.setTax(newTax);
                sale.setTotal(newTotal);
                
                System.out.println("[SalesService] Recalculated sale totals after partial return:");
                System.out.println("  New Subtotal: $" + newSubtotal);
                System.out.println("  New Tax: $" + newTax);
                System.out.println("  New Total: $" + newTotal);
                
                // Save updates
                Sale updatedSale = saleDao.update(sale);

                // If any of this sale was charged to the customer's account, the return
                // must reduce what they owe (cash/card parts are refunded physically)
                adjustStoreChargeForPartialReturn(updatedSale, totalBeforeReturn.subtract(newTotal));

                System.out.println("[SalesService] Partial return processed successfully");

                return Optional.of(updatedSale);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Error processing partial return: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * After a partial return, reduce the customer's store charge balance by the
     * returned amount (up to what was actually charged on this sale) and record
     * an auditable adjustment. The store charge payment rows are shrunk to match
     * so payment-method reports stay accurate. Never breaks the return itself.
     */
    private void adjustStoreChargeForPartialReturn(Sale sale, BigDecimal reduction) {
        try {
            if (reduction == null || reduction.compareTo(BigDecimal.ZERO) <= 0
                    || sale.getCustomer() == null
                    || !(salePaymentDao instanceof com.tireshop.dao.SalePaymentDao)) {
                return;
            }

            List<SalePayment> payments = ((com.tireshop.dao.SalePaymentDao) salePaymentDao)
                    .findBySaleId(sale.getId());
            BigDecimal chargedAmount = BigDecimal.ZERO;
            for (SalePayment payment : payments) {
                if (payment.getPaymentType() == PaymentType.STORE_CHARGE && payment.getAmount() != null) {
                    chargedAmount = chargedAmount.add(payment.getAmount());
                }
            }
            if (chargedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return; // Nothing was charged to the account on this sale
            }

            BigDecimal applied = reduction.min(chargedAmount);
            Customer customer = sale.getCustomer();
            // Balance may go negative = shop owes the customer (they already paid down the charge)
            BigDecimal newBalance = customer.getChargeBalance().subtract(applied);
            customer.setChargeBalance(newBalance);
            customerDao.update(customer);

            // Shrink the store charge payment rows so reports don't overstate charges
            BigDecimal remaining = applied;
            for (SalePayment payment : payments) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (payment.getPaymentType() == PaymentType.STORE_CHARGE && payment.getAmount() != null) {
                    BigDecimal cut = payment.getAmount().min(remaining);
                    payment.setAmount(payment.getAmount().subtract(cut));
                    salePaymentDao.update(payment);
                    remaining = remaining.subtract(cut);
                }
            }

            ChargeAccountPayment adjustment = new ChargeAccountPayment(customer, applied.negate());
            adjustment.setBalanceAfter(newBalance);
            adjustment.setNotes("Partial return - sale " + sale.getInvoiceNumber());
            chargeAccountPaymentDao.save(adjustment);

            System.out.println("[SalesService] Partial return: reduced store charge by $" + applied + " on "
                    + customer.getFullName() + "'s account (new balance: $" + newBalance + ")");
        } catch (Exception e) {
            System.err.println("[SalesService] Could not adjust store charge for partial return on sale "
                    + sale.getId() + ": " + e.getMessage());
        }
    }

    // Helper methods
    private synchronized String generateInvoiceNumber() {
        // Format: INV-001, INV-002, etc. (sequential)
        // SYNCHRONIZED to prevent race conditions with concurrent sales
        int maxNumber = 0;
        if (saleDao instanceof SaleDao) {
            maxNumber = ((SaleDao) saleDao).getMaxInvoiceNumber();
        }
        int nextNumber = maxNumber + 1;
        // Pad with zeros to make it at least 3 digits
        String invoiceNumber = String.format("INV-%03d", nextNumber);
        System.out.println("[SalesService] Generated invoice number: " + invoiceNumber);
        return invoiceNumber;
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

    /**
     * Update a sale
     * @param sale The sale to update
     * @return The updated sale
     */
    public Sale updateSale(Sale sale) {
        return saleDao.update(sale);
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