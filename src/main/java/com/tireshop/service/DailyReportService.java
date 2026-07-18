package com.tireshop.service;

import com.tireshop.dao.CustomerDao;
import com.tireshop.dao.SaleDao;
import com.tireshop.dao.SalePaymentDao;
import com.tireshop.model.Customer;
import com.tireshop.model.PaymentType;
import com.tireshop.model.Sale;
import com.tireshop.model.SalePayment;
import com.tireshop.util.SettingsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Emails a daily sales summary to the shop owner.
 *
 * Sends automatically once per day at the configured time (report.daily.time,
 * default 18:00) while the app is running, catches up if the configured time was
 * missed while the app was closed, and can also be triggered manually from
 * Admin -> General Settings -> Email & Reports.
 */
public class DailyReportService {

    private static final Logger LOGGER = Logger.getLogger(DailyReportService.class.getName());

    // Settings keys (stored in config.properties)
    public static final String KEY_ENABLED = "report.daily.enabled";
    public static final String KEY_TIME = "report.daily.time";
    public static final String KEY_RECIPIENT = "report.recipient.email";
    public static final String KEY_LAST_SENT = "report.daily.lastsent";

    private final SettingsService settingsService;
    private final SaleDao saleDao;
    private final SalePaymentDao salePaymentDao;
    private final CustomerDao customerDao;
    private final EmailService emailService;
    private ScheduledExecutorService scheduler;

    public DailyReportService(SettingsService settingsService, SaleDao saleDao,
                              SalePaymentDao salePaymentDao, CustomerDao customerDao,
                              EmailService emailService) {
        this.settingsService = settingsService;
        this.saleDao = saleDao;
        this.salePaymentDao = salePaymentDao;
        this.customerDao = customerDao;
        this.emailService = emailService;
    }

    /** Start the daily scheduler. Safe to call again - restarts the schedule. */
    public synchronized void start() {
        stop();

        if (!Boolean.parseBoolean(settingsService.getSetting(KEY_ENABLED, "true"))) {
            LOGGER.info("Daily report is disabled");
            return;
        }

        LocalTime reportTime = getReportTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.with(reportTime);
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1);
        }

        // Catch-up: if today's report time has passed but no report was sent
        // (e.g. the app was closed), send it shortly after startup
        LocalDate today = LocalDate.now();
        LocalDate lastSent = getLastSentDate();
        if (now.toLocalTime().isAfter(reportTime) && !today.equals(lastSent)) {
            LocalDate reportDate = today;
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DailyReportScheduler");
                t.setDaemon(true);
                return t;
            });
            scheduler.schedule(() -> sendDailyReportSafely(reportDate), 2, TimeUnit.MINUTES);
            LOGGER.info("Missed daily report for " + reportDate + " - will send shortly");
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DailyReportScheduler");
                t.setDaemon(true);
                return t;
            });
        }

        long delaySeconds = Duration.between(now, nextRun).getSeconds();
        scheduler.scheduleAtFixedRate(
                () -> sendDailyReportSafely(LocalDate.now()),
                delaySeconds, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        LOGGER.info("Daily report scheduled for " + reportTime + " (next run: " + nextRun + ")");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private LocalTime getReportTime() {
        try {
            return LocalTime.parse(settingsService.getSetting(KEY_TIME, "18:00"));
        } catch (Exception e) {
            LOGGER.warning("Invalid " + KEY_TIME + " setting, using 18:00");
            return LocalTime.of(18, 0);
        }
    }

    private LocalDate getLastSentDate() {
        try {
            String value = settingsService.getSetting(KEY_LAST_SENT, "");
            return value.isEmpty() ? null : LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendDailyReportSafely(LocalDate date) {
        try {
            sendDailyReport(date);
        } catch (Exception e) {
            LOGGER.severe("Daily report failed for " + date + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build and send the daily report for the given date.
     * @return true if the email was sent
     */
    public boolean sendDailyReport(LocalDate date) {
        String recipient = settingsService.getSetting(KEY_RECIPIENT, "");
        if (recipient.trim().isEmpty()) {
            LOGGER.warning("Daily report not sent - no recipient email configured (" + KEY_RECIPIENT + ")");
            return false;
        }

        String shopName = settingsService.getCompanyName();
        String subject = "Daily Sales Report - " + shopName + " - "
                + date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        String body = buildReportBody(date);

        boolean sent = emailService.sendReportEmail(recipient, subject, body);
        if (sent) {
            settingsService.setSetting(KEY_LAST_SENT, date.toString());
            settingsService.saveProperties();
            LOGGER.info("Daily report for " + date + " sent to " + recipient);
        }
        return sent;
    }

    /**
     * Build the report text for the given date. Public so it can be previewed/tested.
     */
    public String buildReportBody(LocalDate date) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        List<Sale> paidSales = saleDao.findPaidSalesInDateRange(date, date);

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal subtotalSum = BigDecimal.ZERO;
        BigDecimal taxSum = BigDecimal.ZERO;
        BigDecimal discountSum = BigDecimal.ZERO;
        int discountedSales = 0;
        int voidedCount = 0;

        // Payment breakdown - prefer real payment records (accurate for splits);
        // fall back to the sale's primary payment type for older sales
        Map<PaymentType, BigDecimal> paymentTotals = new EnumMap<>(PaymentType.class);
        java.util.Set<Long> salesWithPaymentRecords = new java.util.HashSet<>();
        for (SalePayment payment : salePaymentDao.findByDateRange(date, date)) {
            if (payment.getSale() != null) {
                salesWithPaymentRecords.add(payment.getSale().getId());
            }
            if (payment.getPaymentType() != null && payment.getAmount() != null) {
                paymentTotals.merge(payment.getPaymentType(), payment.getAmount(), BigDecimal::add);
            }
        }

        for (Sale sale : paidSales) {
            if (sale.isVoided()) {
                voidedCount++;
                continue;
            }
            BigDecimal total = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;
            gross = gross.add(total);
            subtotalSum = subtotalSum.add(sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO);
            taxSum = taxSum.add(sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO);

            BigDecimal discount = sale.getDiscountAmount();
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                discountSum = discountSum.add(discount);
                discountedSales++;
            }

            // Legacy fallback: no payment records for this sale -> count its primary type
            if (!salesWithPaymentRecords.contains(sale.getId()) && sale.getPaymentType() != null) {
                paymentTotals.merge(sale.getPaymentType(), total, BigDecimal::add);
            }
        }

        int completedSales = paidSales.size() - voidedCount;
        BigDecimal averageSale = completedSales > 0
                ? gross.divide(BigDecimal.valueOf(completedSales), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Store charge accounts
        BigDecimal chargedToday = paymentTotals.getOrDefault(PaymentType.STORE_CHARGE, BigDecimal.ZERO);
        List<Customer> customersOwing = customerDao.findCustomersWithChargeBalance();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        for (Customer c : customersOwing) {
            totalOutstanding = totalOutstanding.add(c.getChargeBalance());
        }

        // Open work orders (unpaid sales, all time)
        int openWorkOrders = saleDao.findUnpaidSales().size();

        StringBuilder sb = new StringBuilder();
        sb.append("Daily Sales Report\n");
        sb.append(date.format(dateFmt)).append("\n");
        sb.append(settingsService.getCompanyName()).append("\n");
        sb.append("==========================================\n\n");

        sb.append("SALES SUMMARY\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("Completed sales:    %d%n", completedSales));
        sb.append(String.format("Gross sales:        $%,.2f%n", gross));
        sb.append(String.format("Average sale:       $%,.2f%n", averageSale));
        sb.append(String.format("Subtotal:           $%,.2f%n", subtotalSum));
        sb.append(String.format("Tax collected:      $%,.2f%n", taxSum));
        if (discountedSales > 0) {
            sb.append(String.format("Discounts given:    $%,.2f (%d sale%s)%n",
                    discountSum, discountedSales, discountedSales == 1 ? "" : "s"));
        }
        if (voidedCount > 0) {
            sb.append(String.format("Voided sales:       %d%n", voidedCount));
        }
        sb.append("\n");

        sb.append("PAYMENTS BY METHOD\n");
        sb.append("------------------------------------------\n");
        if (paymentTotals.isEmpty()) {
            sb.append("No payments recorded today.\n");
        } else {
            for (Map.Entry<PaymentType, BigDecimal> entry : paymentTotals.entrySet()) {
                sb.append(String.format("%-20s $%,.2f%n", entry.getKey().getDisplayName() + ":", entry.getValue()));
            }
        }
        sb.append("\n");

        sb.append("STORE CHARGE ACCOUNTS\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("Charged today:      $%,.2f%n", chargedToday));
        sb.append(String.format("Total outstanding:  $%,.2f (%d customer%s)%n",
                totalOutstanding, customersOwing.size(), customersOwing.size() == 1 ? "" : "s"));
        if (!customersOwing.isEmpty()) {
            sb.append("\nCustomers with balances:\n");
            int shown = 0;
            for (Customer c : customersOwing) {
                if (++shown > 10) {
                    sb.append(String.format("  ... and %d more%n", customersOwing.size() - 10));
                    break;
                }
                sb.append(String.format("  %-30s $%,.2f%n", c.getFullName(), c.getChargeBalance()));
            }
        }
        sb.append("\n");

        sb.append("OTHER\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("Open work orders:   %d%n", openWorkOrders));
        sb.append("\n");
        sb.append("Report generated at ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a")))
          .append(" by Tire Shop POS\n");

        return sb.toString();
    }
}
