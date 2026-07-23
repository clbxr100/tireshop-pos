package com.tireshop.test;

import com.tireshop.dao.*;
import com.tireshop.model.*;
import com.tireshop.service.InventoryService;
import com.tireshop.service.SalesService;
import com.tireshop.util.SettingsService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Headless integration test for the payment flows:
 *  - single store charge (balance tracking)
 *  - split payments (incl. store charge parts, validation, change)
 *  - charge account payoffs (recorded + capped)
 *  - charge account activity history
 *  - invoice number sequencing
 *
 * Runs against a throwaway H2 database created in a temp directory.
 * Run with:  java -cp target/classes;<deps> com.tireshop.test.PaymentFlowIntegrationTest
 */
public class PaymentFlowIntegrationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("payflow_test");
        String dbUrl = "jdbc:h2:" + tempDir.toAbsolutePath().toString().replace('\\', '/') + "/testdb";
        System.out.println("Scratch DB: " + dbUrl);

        Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
        cfg.setProperty("hibernate.connection.url", dbUrl);
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");

        SessionFactory sf = cfg.buildSessionFactory();
        try {
            runTests(sf);
        } finally {
            sf.close();
        }

        System.out.println("\n==================================================");
        System.out.println("RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("==================================================");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void runTests(SessionFactory sf) {
        SaleDao saleDao = new SaleDao(sf);
        SaleItemDao saleItemDao = new SaleItemDao(sf);
        SalePaymentDao salePaymentDao = new SalePaymentDao(sf);
        CustomerDao customerDao = new CustomerDao(sf);
        VehicleDao vehicleDao = new VehicleDao(sf);
        ServiceDao serviceDao = new ServiceDao(sf);
        TechnicianDao technicianDao = new TechnicianDao(sf);
        ChargeAccountPaymentDao chargeAccountPaymentDao = new ChargeAccountPaymentDao(sf);
        ProductDao productDao = new ProductDao(sf);

        SettingsService settings = SettingsService.getInstance();
        InventoryService inventoryService = new InventoryService(productDao, settings);
        SalesService salesService = new SalesService(
                saleDao, saleItemDao, salePaymentDao, customerDao, vehicleDao,
                serviceDao, technicianDao, chargeAccountPaymentDao,
                inventoryService, settings, null);

        // ---- Fixtures ----
        Product tire = new Product("Test Tire 225/60R16", "Tires", new BigDecimal("100.00"));
        tire.setQuantityInStock(50);
        tire = productDao.save(tire);

        Customer customer = new Customer("Jane", "Doe", "555-1234");
        customer = customerDao.save(customer);

        // =========================================================
        System.out.println("\n--- Scenario 1: single store charge updates balance ---");
        Sale sale1 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale1.getId(), tire.getId(), 2); // $200 + tax
        sale1 = salesService.getSaleById(sale1.getId()).get();
        BigDecimal sale1Total = sale1.getTotal();

        Optional<Sale> charged = salesService.completeStoreCharge(sale1.getId());
        check("1a: store charge completes", charged.isPresent());
        check("1b: sale marked paid", charged.get().isPaid());
        check("1c: payment type STORE_CHARGE", charged.get().getPaymentType() == PaymentType.STORE_CHARGE);

        Customer afterCharge = customerDao.findById(customer.getId()).get();
        check("1d: balance increased by sale total (exp " + sale1Total + ", got " + afterCharge.getChargeBalance() + ")",
                afterCharge.getChargeBalance().compareTo(sale1Total) == 0);

        List<SalePayment> sale1Payments = salePaymentDao.findBySaleId(sale1.getId());
        check("1e: one payment record created", sale1Payments.size() == 1);
        check("1f: payment record is STORE_CHARGE for full total",
                sale1Payments.size() == 1
                        && sale1Payments.get(0).getPaymentType() == PaymentType.STORE_CHARGE
                        && sale1Payments.get(0).getAmount().compareTo(sale1Total) == 0);

        // =========================================================
        System.out.println("\n--- Scenario 2: split cash + store charge ---");
        Sale sale2 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale2.getId(), tire.getId(), 1); // $100 + tax
        sale2 = salesService.getSaleById(sale2.getId()).get();
        BigDecimal sale2Total = sale2.getTotal();
        BigDecimal cashPart = sale2Total.multiply(new BigDecimal("0.40")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal chargePart = sale2Total.subtract(cashPart);

        BigDecimal balanceBefore2 = customerDao.findById(customer.getId()).get().getChargeBalance();

        List<SalePayment> parts2 = new ArrayList<>();
        parts2.add(new SalePayment(null, PaymentType.CASH, cashPart));
        parts2.add(new SalePayment(null, PaymentType.STORE_CHARGE, chargePart));
        Optional<Sale> splitDone = salesService.completeSplitSale(sale2.getId(), parts2, BigDecimal.ZERO);

        check("2a: split sale completes", splitDone.isPresent());
        check("2b: sale marked paid", splitDone.isPresent() && splitDone.get().isPaid());
        List<SalePayment> sale2Payments = salePaymentDao.findBySaleId(sale2.getId());
        check("2c: two payment records (got " + sale2Payments.size() + ")", sale2Payments.size() == 2);

        Customer afterSplit = customerDao.findById(customer.getId()).get();
        BigDecimal expectedBalance2 = balanceBefore2.add(chargePart);
        check("2d: balance only increased by charge part (exp " + expectedBalance2 + ", got "
                        + afterSplit.getChargeBalance() + ")",
                afterSplit.getChargeBalance().compareTo(expectedBalance2) == 0);
        check("2e: primary type is largest part (STORE_CHARGE)",
                splitDone.get().getPaymentType() == PaymentType.STORE_CHARGE);
        check("2f: split notes recorded on sale",
                splitDone.get().getNotes() != null && splitDone.get().getNotes().contains("SPLIT PAYMENT"));

        // =========================================================
        System.out.println("\n--- Scenario 3: split that doesn't cover total is rejected ---");
        Sale sale3 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale3.getId(), tire.getId(), 1);
        sale3 = salesService.getSaleById(sale3.getId()).get();
        List<SalePayment> parts3 = new ArrayList<>();
        parts3.add(new SalePayment(null, PaymentType.CASH, new BigDecimal("1.00"))); // way short
        Optional<Sale> rejected = salesService.completeSplitSale(sale3.getId(), parts3, BigDecimal.ZERO);
        check("3a: underpayment rejected", !rejected.isPresent());
        Sale sale3After = salesService.getSaleById(sale3.getId()).get();
        check("3b: sale still unpaid", !sale3After.isPaid());
        check("3c: no payment rows leaked", salePaymentDao.findBySaleId(sale3.getId()).isEmpty());

        // =========================================================
        System.out.println("\n--- Scenario 4: split store charge without customer is rejected ---");
        Sale sale4 = salesService.createSale(null, null); // walk-in, no customer
        salesService.addProductToSale(sale4.getId(), tire.getId(), 1);
        sale4 = salesService.getSaleById(sale4.getId()).get();
        BigDecimal sale4Total = sale4.getTotal();
        List<SalePayment> parts4 = new ArrayList<>();
        parts4.add(new SalePayment(null, PaymentType.CASH, sale4Total.subtract(new BigDecimal("10.00"))));
        parts4.add(new SalePayment(null, PaymentType.STORE_CHARGE, new BigDecimal("10.00")));
        Optional<Sale> rejected4 = salesService.completeSplitSale(sale4.getId(), parts4, BigDecimal.ZERO);
        check("4a: store charge split w/o customer rejected", !rejected4.isPresent());
        check("4b: sale still unpaid", !salesService.getSaleById(sale4.getId()).get().isPaid());

        // =========================================================
        System.out.println("\n--- Scenario 5: invalid split parts rejected ---");
        Sale sale5 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale5.getId(), tire.getId(), 1);
        sale5 = salesService.getSaleById(sale5.getId()).get();
        BigDecimal sale5Total = sale5.getTotal();
        List<SalePayment> parts5 = new ArrayList<>();
        parts5.add(new SalePayment(null, PaymentType.CASH, sale5Total));
        parts5.add(new SalePayment(null, PaymentType.CHECK, BigDecimal.ZERO)); // zero part
        check("5a: zero-amount part rejected", !salesService.completeSplitSale(sale5.getId(), parts5, BigDecimal.ZERO).isPresent());
        List<SalePayment> parts5b = new ArrayList<>();
        parts5b.add(new SalePayment(null, PaymentType.CASH, sale5Total));
        parts5b.add(new SalePayment(null, PaymentType.CHECK, new BigDecimal("-5.00"))); // negative part
        check("5b: negative-amount part rejected", !salesService.completeSplitSale(sale5.getId(), parts5b, BigDecimal.ZERO).isPresent());
        check("5c: empty parts list rejected", !salesService.completeSplitSale(sale5.getId(), new ArrayList<>(), BigDecimal.ZERO).isPresent());

        // =========================================================
        System.out.println("\n--- Scenario 6: charge account payoff recorded ---");
        Customer before6 = customerDao.findById(customer.getId()).get();
        BigDecimal balBefore6 = before6.getChargeBalance();
        BigDecimal payAmount = new BigDecimal("50.00");
        Optional<BigDecimal> newBalance = salesService.recordChargeAccountPayment(customer.getId(), payAmount);
        check("6a: payoff accepted", newBalance.isPresent());
        check("6b: balance reduced by payment (exp " + balBefore6.subtract(payAmount) + ", got " + newBalance.orElse(null) + ")",
                newBalance.isPresent() && newBalance.get().compareTo(balBefore6.subtract(payAmount)) == 0);
        List<ChargeAccountPayment> payoffRows = chargeAccountPaymentDao.findByCustomerId(customer.getId());
        check("6c: payoff row persisted", payoffRows.size() == 1);
        check("6d: payoff amount + balanceAfter correct",
                payoffRows.size() == 1
                        && payoffRows.get(0).getAmount().compareTo(payAmount) == 0
                        && payoffRows.get(0).getBalanceAfter().compareTo(balBefore6.subtract(payAmount)) == 0);
        check("6e: payoff has timestamp", payoffRows.size() == 1 && payoffRows.get(0).getPaymentTimestamp() != null);

        // Detailed variant returns the persisted payment record (used for receipts)
        Optional<ChargeAccountPayment> detailed = salesService.recordChargeAccountPaymentDetailed(customer.getId(), new BigDecimal("10.00"));
        check("6f: detailed payoff returns record", detailed.isPresent());
        check("6g: detailed record has amount, balanceAfter, timestamp",
                detailed.isPresent()
                        && detailed.get().getAmount().compareTo(new BigDecimal("10.00")) == 0
                        && detailed.get().getBalanceAfter() != null
                        && detailed.get().getPaymentTimestamp() != null
                        && detailed.get().getId() != null);
        // Balance reduced again by the extra $10 (scenario 7 expects this total)
        // (restore expectation: scenario 7 overpay pays off whatever remains)

        // =========================================================
        System.out.println("\n--- Scenario 7: overpay payoff is capped at balance ---");
        BigDecimal balBefore7 = customerDao.findById(customer.getId()).get().getChargeBalance();
        Optional<BigDecimal> after7 = salesService.recordChargeAccountPayment(customer.getId(), balBefore7.add(new BigDecimal("999.00")));
        check("7a: overpay accepted but capped", after7.isPresent());
        check("7b: balance is now zero", after7.isPresent() && after7.get().compareTo(BigDecimal.ZERO) == 0);
        List<ChargeAccountPayment> rows7 = chargeAccountPaymentDao.findByCustomerId(customer.getId());
        // (order-independent check - rows can share a timestamp)
        boolean cappedRowFound = false;
        for (ChargeAccountPayment cap : rows7) {
            if (cap.getAmount().compareTo(balBefore7) == 0) cappedRowFound = true;
        }
        check("7c: recorded amount equals previous balance, not the overpay",
                rows7.size() == 3 && cappedRowFound);

        // =========================================================
        System.out.println("\n--- Scenario 8: payoff with zero balance rejected ---");
        check("8a: nothing-to-pay returns empty", !salesService.recordChargeAccountPayment(customer.getId(), new BigDecimal("10.00")).isPresent());
        check("8b: negative payoff rejected", !salesService.recordChargeAccountPayment(customer.getId(), new BigDecimal("-5.00")).isPresent());
        check("8c: unknown customer rejected", !salesService.recordChargeAccountPayment(999999L, new BigDecimal("10.00")).isPresent());

        // =========================================================
        System.out.println("\n--- Scenario 9: charge account activity combines charges + payments ---");
        SalesService.ChargeAccountActivity activity = salesService.getChargeAccountActivity(customer.getId());
        check("9a: charges listed (exp 2, got " + activity.charges.size() + ")", activity.charges.size() == 2);
        check("9b: payments listed (exp 3, got " + activity.payments.size() + ")", activity.payments.size() == 3);
        boolean allStoreCharge = true;
        for (SalePayment sp : activity.charges) {
            if (sp.getPaymentType() != PaymentType.STORE_CHARGE) allStoreCharge = false;
        }
        check("9c: all listed charges are STORE_CHARGE type", allStoreCharge);

        // The charge history dialog dereferences sale.invoiceNumber after the DAO
        // session has closed - the sale must be usable outside the session
        boolean invoicesReadable = true;
        for (SalePayment sp : activity.charges) {
            try {
                if (sp.getSale() == null || sp.getSale().getInvoiceNumber() == null) invoicesReadable = false;
            } catch (Exception ex) {
                System.out.println("   (9d debug: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
                invoicesReadable = false;
            }
        }
        check("9d: charge sale invoice numbers readable outside session", invoicesReadable);

        // =========================================================
        System.out.println("\n--- Scenario 10: invoice numbers increment ---");
        Sale sA = salesService.createSale(null, null);
        Sale sB = salesService.createSale(null, null);
        check("10a: invoice numbers differ", !sA.getInvoiceNumber().equals(sB.getInvoiceNumber()));
        int numA = Integer.parseInt(sA.getInvoiceNumber().replace("INV-", ""));
        int numB = Integer.parseInt(sB.getInvoiceNumber().replace("INV-", ""));
        check("10b: second invoice = first + 1 (" + sA.getInvoiceNumber() + " -> " + sB.getInvoiceNumber() + ")",
                numB == numA + 1);

        // =========================================================
        System.out.println("\n--- Scenario 11: cash overpay (change) allowed in split ---");
        Sale sale11 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale11.getId(), tire.getId(), 1);
        sale11 = salesService.getSaleById(sale11.getId()).get();
        BigDecimal total11 = sale11.getTotal();
        List<SalePayment> parts11 = new ArrayList<>();
        parts11.add(new SalePayment(null, PaymentType.CASH, total11.add(new BigDecimal("20.00")))); // overpay cash
        BigDecimal change = new BigDecimal("20.00");
        Optional<Sale> doneChange = salesService.completeSplitSale(sale11.getId(), parts11, change);
        check("11a: overpay split completes", doneChange.isPresent());
        check("11b: change recorded in notes",
                doneChange.isPresent() && doneChange.get().getNotes() != null
                        && doneChange.get().getNotes().contains("Change Given"));
        // NOTE: overpay with only a cash part means the recorded payment exceeds the total.
        // Balance should NOT increase (no store charge part)
        BigDecimal bal11 = customerDao.findById(customer.getId()).get().getChargeBalance();
        check("11c: no charge balance added for cash overpay", bal11.compareTo(BigDecimal.ZERO) == 0);

        // =========================================================
        System.out.println("\n--- Scenario 12: single cash sale records one payment ---");
        Sale sale12 = salesService.createSale(null, null);
        salesService.addProductToSale(sale12.getId(), tire.getId(), 1);
        Optional<Sale> cashDone = salesService.completeSale(sale12.getId(), PaymentType.CASH);
        check("12a: cash sale completes", cashDone.isPresent());
        List<SalePayment> pays12 = salePaymentDao.findBySaleId(sale12.getId());
        check("12b: exactly one payment row", pays12.size() == 1);
        check("12c: payment matches sale total",
                pays12.size() == 1 && pays12.get(0).getAmount().compareTo(cashDone.get().getTotal()) == 0);

        // =========================================================
        System.out.println("\n--- Scenario 13: voiding a store charge sale reverses the balance ---");
        BigDecimal balBefore13 = customerDao.findById(customer.getId()).get().getChargeBalance();
        Sale sale13 = salesService.createSale(customer.getId(), null);
        salesService.addProductToSale(sale13.getId(), tire.getId(), 1);
        sale13 = salesService.getSaleById(sale13.getId()).get();
        BigDecimal total13 = sale13.getTotal();
        salesService.completeStoreCharge(sale13.getId());
        BigDecimal balCharged13 = customerDao.findById(customer.getId()).get().getChargeBalance();
        check("13a: charge applied before void",
                balCharged13.compareTo(balBefore13.add(total13)) == 0);

        Optional<Sale> voided13 = salesService.voidSale(sale13.getId(), "Customer Return");
        check("13b: void succeeds", voided13.isPresent());
        BigDecimal balAfterVoid13 = customerDao.findById(customer.getId()).get().getChargeBalance();
        check("13c: balance reversed back (exp " + balBefore13 + ", got " + balAfterVoid13 + ")",
                balAfterVoid13.compareTo(balBefore13) == 0);
        List<ChargeAccountPayment> rows13 = chargeAccountPaymentDao.findByCustomerId(customer.getId());
        boolean reversalFound = false;
        for (ChargeAccountPayment cap : rows13) {
            if (cap.getAmount().compareTo(BigDecimal.ZERO) < 0
                    && cap.getNotes() != null && cap.getNotes().contains("voided")) {
                reversalFound = true;
            }
        }
        check("13d: reversal entry recorded with audit note", reversalFound);

        // =========================================================
        System.out.println("\n--- Scenario 14: voided sale payments excluded from report queries ---");
        Product tireBefore14 = productDao.findById(tire.getId()).get();
        int stockBefore14 = tireBefore14.getQuantityInStock();
        Sale sale14 = salesService.createSale(null, null);
        salesService.addProductToSale(sale14.getId(), tire.getId(), 3);
        salesService.completeSale(sale14.getId(), PaymentType.CASH);
        int stockAfterSale14 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("14a: stock reduced by sale", stockAfterSale14 == stockBefore14 - 3);

        java.time.LocalDate today = java.time.LocalDate.now();
        int paymentsTodayBeforeVoid = salePaymentDao.findByDateRange(today, today).size();

        salesService.voidSale(sale14.getId(), "Cashier Error");
        int stockAfterVoid14 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("14b: void restores stock", stockAfterVoid14 == stockBefore14);
        int paymentsTodayAfterVoid = salePaymentDao.findByDateRange(today, today).size();
        check("14c: voided sale's payments excluded from date-range query ("
                        + paymentsTodayBeforeVoid + " -> " + paymentsTodayAfterVoid + ")",
                paymentsTodayAfterVoid == paymentsTodayBeforeVoid - 1);

        // =========================================================
        System.out.println("\n--- Scenario 15: daily email report body builds correctly ---");
        com.tireshop.service.DailyReportService dailyReport = new com.tireshop.service.DailyReportService(
                settings, saleDao, salePaymentDao, customerDao, null);
        String body = dailyReport.buildReportBody(today);
        check("15a: report body generated", body != null && !body.isEmpty());
        check("15b: has sales summary section", body.contains("SALES SUMMARY"));
        check("15c: has payments section", body.contains("PAYMENTS BY METHOD"));
        check("15d: has store charge section", body.contains("STORE CHARGE ACCOUNTS"));
        check("15e: has open work orders count", body.contains("Open work orders"));
        // The voided $318 cash sale must not inflate today's payment totals:
        // only the non-voided cash payments remain (scenario 2 cash part + scenario 11 overpay + scenario 12 cash)
        check("15f: voided sale not counted in payment methods",
                !body.contains("318.00"));

        // =========================================================
        System.out.println("\n--- Scenario 16: voiding already-voided / unpaid sale rejected ---");
        check("16a: double void rejected", !salesService.voidSale(sale14.getId(), "Again").isPresent());
        Sale sale16 = salesService.createSale(null, null);
        check("16b: void of unpaid sale rejected", !salesService.voidSale(sale16.getId(), "Nope").isPresent());

        // =========================================================
        System.out.println("\n--- Scenario 17: partial return on store charge sale reduces balance ---");
        Customer cust17 = customerDao.save(new Customer("Bob", "Smith", "555-9999"));
        int stockBefore17 = productDao.findById(tire.getId()).get().getQuantityInStock();
        Sale sale17 = salesService.createSale(cust17.getId(), null);
        salesService.addProductToSale(sale17.getId(), tire.getId(), 2); // 2 x $100 + tax
        sale17 = salesService.getSaleById(sale17.getId()).get();
        BigDecimal total17 = sale17.getTotal();
        salesService.completeStoreCharge(sale17.getId());
        BigDecimal balCharged17 = customerDao.findById(cust17.getId()).get().getChargeBalance();
        check("17a: full total charged (exp " + total17 + ", got " + balCharged17 + ")",
                balCharged17.compareTo(total17) == 0);

        // Return 1 of the 2 tires
        Sale refreshed17 = salesService.getSaleById(sale17.getId()).get();
        SaleItem item17 = refreshed17.getItems().get(0);
        List<com.tireshop.controller.SalesController.ReturnItem> returns17 = new ArrayList<>();
        returns17.add(new com.tireshop.controller.SalesController.ReturnItem(item17, 1));
        Optional<Sale> afterReturn17 = salesService.partialReturnSale(sale17.getId(), returns17, "Wrong size");
        check("17b: partial return succeeds", afterReturn17.isPresent());
        check("17c: sale flagged as partial return", afterReturn17.get().hasPartialReturn());

        BigDecimal newTotal17 = afterReturn17.get().getTotal();
        BigDecimal expectedReduction = total17.subtract(newTotal17);
        BigDecimal balAfterReturn17 = customerDao.findById(cust17.getId()).get().getChargeBalance();
        check("17d: balance reduced by returned amount (exp " + total17.subtract(expectedReduction)
                        + ", got " + balAfterReturn17 + ")",
                balAfterReturn17.compareTo(total17.subtract(expectedReduction)) == 0);
        check("17e: new total is half-ish of original (" + total17 + " -> " + newTotal17 + ")",
                newTotal17.compareTo(total17) < 0);

        // Store charge payment row should have shrunk to match the new total
        List<SalePayment> pays17 = salePaymentDao.findBySaleId(sale17.getId());
        BigDecimal chargeRowTotal = BigDecimal.ZERO;
        for (SalePayment p : pays17) {
            if (p.getPaymentType() == PaymentType.STORE_CHARGE) chargeRowTotal = chargeRowTotal.add(p.getAmount());
        }
        check("17f: store charge payment row shrunk (exp " + total17.subtract(expectedReduction)
                        + ", got " + chargeRowTotal + ")",
                chargeRowTotal.compareTo(total17.subtract(expectedReduction)) == 0);

        // Adjustment entry recorded
        List<ChargeAccountPayment> rows17 = chargeAccountPaymentDao.findByCustomerId(cust17.getId());
        boolean adjustFound = false;
        for (ChargeAccountPayment cap : rows17) {
            if (cap.getAmount().compareTo(BigDecimal.ZERO) < 0
                    && cap.getNotes() != null && cap.getNotes().contains("Partial return")) {
                adjustFound = true;
            }
        }
        check("17g: partial-return adjustment recorded with note", adjustFound);

        // Inventory restored for the returned tire
        int stockAfter17 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("17h: returned tire back in stock (exp " + (stockBefore17 - 1) + ", got " + stockAfter17 + ")",
                stockAfter17 == stockBefore17 - 1);

        // =========================================================
        System.out.println("\n--- Scenario 18: partial return on CASH sale does not touch charge balance ---");
        Sale sale18 = salesService.createSale(cust17.getId(), null);
        salesService.addProductToSale(sale18.getId(), tire.getId(), 2);
        salesService.completeSale(sale18.getId(), PaymentType.CASH);
        BigDecimal balBefore18 = customerDao.findById(cust17.getId()).get().getChargeBalance();
        Sale refreshed18 = salesService.getSaleById(sale18.getId()).get();
        List<com.tireshop.controller.SalesController.ReturnItem> returns18 = new ArrayList<>();
        returns18.add(new com.tireshop.controller.SalesController.ReturnItem(refreshed18.getItems().get(0), 1));
        salesService.partialReturnSale(sale18.getId(), returns18, "Changed mind");
        BigDecimal balAfter18 = customerDao.findById(cust17.getId()).get().getChargeBalance();
        check("18a: cash-sale return leaves charge balance untouched",
                balAfter18.compareTo(balBefore18) == 0);

        // =========================================================
        System.out.println("\n--- Scenario 19: report service methods run clean ---");
        java.time.LocalDate today19 = java.time.LocalDate.now();
        List<Sale> rangeSales = salesService.getSalesByDateRange(today19, today19);
        check("19a: getSalesByDateRange returns sales", rangeSales != null && !rangeSales.isEmpty());
        check("19b: product sales report builds",
                salesService.getProductSalesReport(today19, today19) != null);
        check("19c: daily financial summaries build",
                salesService.getDailyFinancialSummaries(today19, today19) != null);
        check("19d: sales summary for date builds",
                salesService.getSalesSummaryForDate(today19) != null);
        check("19e: technician performance report builds",
                salesService.getTechnicianPerformanceReport(null, today19, today19) != null);

        // =========================================================
        System.out.println("\n--- Scenario 20: over-returning is clamped ---");
        Sale sale20 = salesService.createSale(null, null);
        salesService.addProductToSale(sale20.getId(), tire.getId(), 2);
        salesService.completeSale(sale20.getId(), PaymentType.CASH);
        int stockBefore20 = productDao.findById(tire.getId()).get().getQuantityInStock();

        Sale refreshed20 = salesService.getSaleById(sale20.getId()).get();
        SaleItem item20 = refreshed20.getItems().get(0);
        // First return: both units
        List<com.tireshop.controller.SalesController.ReturnItem> ret20a = new ArrayList<>();
        ret20a.add(new com.tireshop.controller.SalesController.ReturnItem(item20, 2));
        salesService.partialReturnSale(sale20.getId(), ret20a, "first");
        int stockAfterFirst20 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("20a: first full return restores stock (+2)",
                stockAfterFirst20 == stockBefore20 + 2);

        // Second return attempt on the same line - everything already returned
        Sale refreshed20b = salesService.getSaleById(sale20.getId()).get();
        List<com.tireshop.controller.SalesController.ReturnItem> ret20b = new ArrayList<>();
        ret20b.add(new com.tireshop.controller.SalesController.ReturnItem(refreshed20b.getItems().get(0), 2));
        salesService.partialReturnSale(sale20.getId(), ret20b, "second");
        int stockAfterSecond20 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("20b: second return adds nothing more (clamped)",
                stockAfterSecond20 == stockAfterFirst20);

        // =========================================================
        System.out.println("\n--- Scenario 21: void after partial return doesn't double-restore ---");
        Sale sale21 = salesService.createSale(null, null);
        salesService.addProductToSale(sale21.getId(), tire.getId(), 4);
        salesService.completeSale(sale21.getId(), PaymentType.CASH);
        int stockAfterSale21 = productDao.findById(tire.getId()).get().getQuantityInStock();

        // Return 1 of 4
        Sale refreshed21 = salesService.getSaleById(sale21.getId()).get();
        List<com.tireshop.controller.SalesController.ReturnItem> ret21 = new ArrayList<>();
        ret21.add(new com.tireshop.controller.SalesController.ReturnItem(refreshed21.getItems().get(0), 1));
        salesService.partialReturnSale(sale21.getId(), ret21, "one back");
        int stockAfterReturn21 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("21a: partial return restores 1 unit", stockAfterReturn21 == stockAfterSale21 + 1);

        // Void the whole sale - should restore the remaining 3, NOT all 4
        salesService.voidSale(sale21.getId(), "full return");
        int stockAfterVoid21 = productDao.findById(tire.getId()).get().getQuantityInStock();
        check("21b: void restores only the 3 still out (exp " + (stockAfterSale21 + 4)
                        + ", got " + stockAfterVoid21 + ")",
                stockAfterVoid21 == stockAfterSale21 + 4);

        // =========================================================
        System.out.println("\n--- Scenario 22: partial return keeps military discount applied ---");
        // Force a discount onto a sale
        Sale sale22 = salesService.createSale(null, null);
        salesService.addProductToSale(sale22.getId(), tire.getId(), 4); // $400 subtotal
        Sale toDiscount = salesService.getSaleById(sale22.getId()).get();
        toDiscount.setDiscountAmount(new BigDecimal("40.00"));
        toDiscount.setDiscountReason("Military (10%)");
        toDiscount.recalculateAmounts(settings.getSalesTaxRate(), BigDecimal.ZERO);
        salesService.updateSale(toDiscount);
        BigDecimal discountedTotal = salesService.getSaleById(sale22.getId()).get().getTotal();
        salesService.completeSale(sale22.getId(), PaymentType.CASH);

        // Return 2 of 4 -> remaining subtotal $200, discount should still apply (capped at $200)
        Sale refreshed22 = salesService.getSaleById(sale22.getId()).get();
        List<com.tireshop.controller.SalesController.ReturnItem> ret22 = new ArrayList<>();
        ret22.add(new com.tireshop.controller.SalesController.ReturnItem(refreshed22.getItems().get(0), 2));
        Optional<Sale> afterReturn22 = salesService.partialReturnSale(sale22.getId(), ret22, "half back");
        check("22a: return succeeds", afterReturn22.isPresent());
        BigDecimal newTotal22 = afterReturn22.get().getTotal();
        // Remaining: $200 subtotal - $40 discount = $160 + 6% tax on $160 = $169.60
        BigDecimal expected22 = new BigDecimal("160.00").add(new BigDecimal("160.00").multiply(settings.getSalesTaxRate()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        check("22b: discount still applied after return (exp " + expected22 + ", got " + newTotal22 + ")",
                newTotal22.compareTo(expected22) == 0);
        check("22c: post-return total is less than discounted original (" + discountedTotal + ")",
                newTotal22.compareTo(discountedTotal) < 0);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }
}
