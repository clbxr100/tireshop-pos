package com.tireshop.controller;

import com.tireshop.model.Sale;
import com.tireshop.model.Customer;
import com.tireshop.model.PaymentType;
import com.tireshop.service.SalesService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// Import the DTO
import com.tireshop.model.dto.ProductSalesReportItem;
import com.tireshop.model.dto.TechnicianPerformanceReportItem;
import com.tireshop.model.Product;
import com.tireshop.service.InventoryService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;
import com.tireshop.model.Technician;
import com.tireshop.model.dto.DailyFinancialSummaryItem;
import com.tireshop.service.QuickBooksExportService;
import com.tireshop.util.SettingsService;

public class ReportController {
    
    private SalesService salesService; // Assuming we'll need this for sales reports
    private InventoryService inventoryService; // Add InventoryService
    private QuickBooksExportService quickBooksExportService;
    private SettingsService settingsService;
    private Stage stage;
    private BorderPane reportsPane;
    
    // Analytics dashboard components for refreshing
    private VBox monthRevenueBox;
    private VBox yearRevenueBox;
    private VBox lowStockBox;
    private VBox avgSaleBox;
    private HBox revenueChart;
    private TableView<ProductSalesReportItem> topProductsTable;
    private VBox activityFeed;

    private ComboBox<String> reportTypeComboBox;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<Customer> customerReportComboBox; // ComboBox for selecting customer
    private ComboBox<Technician> technicianReportComboBox; // ComboBox for selecting technician
    private CheckBox tiresOnlyCheckBox; // Checkbox to filter only tires in inventory report
    private Button generateReportButton;
    private Button exportToQuickBooksButton;
    private TableView reportTableView; // Generic for now, will be specialized
    private ObservableList reportDataList; // Generic for now

    // Placeholder for other services if needed for other reports
    // private CustomerService customerService; // If we create one

    public ReportController(SalesService salesService, InventoryService inventoryService /*, other services... */) {
        System.out.println("[CONSTRUCTOR] ReportController instance created.");
        this.salesService = salesService;
        this.inventoryService = inventoryService; // Assign InventoryService
        this.settingsService = new SettingsService();
        this.quickBooksExportService = new QuickBooksExportService(settingsService, salesService, inventoryService);
    }

    /**
     * Initialize the reports view
     * @param reportsPane The main reports pane
     * @param stage The application stage
     */
    public void initialize(BorderPane reportsPane, Stage stage) {
        this.reportsPane = reportsPane;
        this.stage = stage;
        
        // Create enhanced header
        VBox headerPanel = createReportsHeader();
        reportsPane.setTop(headerPanel);
        
        // Create main content with dashboard
        TabPane reportTabs = new TabPane();
        reportTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Add dashboard as first tab
        Tab dashboardTab = createDashboardTab();
        Tab reportGeneratorTab = createReportGeneratorTab();
        
        reportTabs.getTabs().addAll(dashboardTab, reportGeneratorTab);
        
        reportsPane.setCenter(reportTabs);
    }
    
    private VBox createReportsHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.getStyleClass().add("search-panel");
        
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("Business Reports & Analytics");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Quick export buttons
        Button exportAllBtn = new Button("Export All Reports");
        exportAllBtn.getStyleClass().add("secondary");
        
        Button printBtn = new Button("Print");
        printBtn.getStyleClass().add("secondary");
        
        // Add event handlers for the buttons
        exportAllBtn.setOnAction(e -> exportAllReports());
        printBtn.setOnAction(e -> printCurrentReport());
        
        titleRow.getChildren().addAll(titleLabel, spacer, exportAllBtn, printBtn);
        header.getChildren().add(titleRow);
        
        return header;
    }
    
    private Tab createReportGeneratorTab() {
        Tab tab = new Tab("Generate Reports");
        
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(20));
        
        // Create report selection and parameters panel
        VBox controlsPanel = new VBox(15);
        controlsPanel.setPadding(new Insets(20));
        controlsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        Label titleLabel = new Label("Report Generator");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Report type selection
        HBox reportTypeRow = new HBox(10);
        reportTypeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label reportTypeLabel = new Label("Report Type:");
        reportTypeLabel.setPrefWidth(100);
        
        reportTypeComboBox = new ComboBox<>();
        reportTypeComboBox.setItems(FXCollections.observableArrayList(
            "Sales Report by Date Range",
            "Product Sales Report",
            "Inventory Stock Levels",
            "Customer Purchase History",
            "Technician Performance",
            "Daily Financial Summary",
            "Voided/Returned Sales Report"
        ));
        reportTypeComboBox.setPrefWidth(250);
        reportTypeComboBox.setOnAction(e -> updateParameterVisibility());
        
        reportTypeRow.getChildren().addAll(reportTypeLabel, reportTypeComboBox);
        
        // Date range selection
        HBox dateRangeRow = new HBox(10);
        dateRangeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label fromLabel = new Label("From Date:");
        fromLabel.setPrefWidth(100);
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        fromDatePicker.setPrefWidth(150);
        
        Label toLabel = new Label("To Date:");
        toLabel.setPrefWidth(80);
        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(150);
        
        dateRangeRow.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);
        
        // Customer selection
        HBox customerRow = new HBox(10);
        customerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label customerLabel = new Label("Customer:");
        customerLabel.setPrefWidth(100);
        
        customerReportComboBox = new ComboBox<>();
        customerReportComboBox.setPrefWidth(250);
        customerReportComboBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer != null ? customer.getFullName() : "";
            }
            @Override
            public Customer fromString(String string) {
                return null;
            }
        });
        populateCustomerComboBox();
        
        customerRow.getChildren().addAll(customerLabel, customerReportComboBox);
        
        // Technician selection
        HBox technicianRow = new HBox(10);
        technicianRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label technicianLabel = new Label("Technician:");
        technicianLabel.setPrefWidth(100);
        
        technicianReportComboBox = new ComboBox<>();
        technicianReportComboBox.setPrefWidth(250);
        technicianReportComboBox.setConverter(new StringConverter<Technician>() {
            @Override
            public String toString(Technician technician) {
                return technician != null ? technician.getFirstName() + " " + technician.getLastName() : "";
            }
            @Override
            public Technician fromString(String string) {
                return null;
            }
        });
        populateTechnicianComboBox();
        
        technicianRow.getChildren().addAll(technicianLabel, technicianReportComboBox);
        
        // Inventory filter options
        HBox inventoryFilterRow = new HBox(10);
        inventoryFilterRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label filterLabel = new Label("Filter:");
        filterLabel.setPrefWidth(100);
        
        tiresOnlyCheckBox = new CheckBox("Tires Only");
        tiresOnlyCheckBox.setStyle("-fx-font-size: 13px;");
        
        inventoryFilterRow.getChildren().addAll(filterLabel, tiresOnlyCheckBox);
        
        // Buttons
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER);
        buttonRow.setPadding(new Insets(20, 0, 0, 0));
        
        generateReportButton = new Button("Generate Report");
        generateReportButton.getStyleClass().add("primary");
        generateReportButton.setOnAction(e -> generateReport());
        
        exportToQuickBooksButton = new Button("Export to QuickBooks");
        exportToQuickBooksButton.getStyleClass().add("secondary");
        exportToQuickBooksButton.setDisable(true);
        exportToQuickBooksButton.setOnAction(e -> exportToQuickBooks());
        
        Button exportSalesBtn = new Button("Export All Sales");
        exportSalesBtn.getStyleClass().add("secondary");
        exportSalesBtn.setOnAction(e -> exportSalesToQuickBooks());
        
        Button exportCustomersBtn = new Button("Export Customers");
        exportCustomersBtn.getStyleClass().add("secondary");
        exportCustomersBtn.setOnAction(e -> exportCustomersToQuickBooks());
        
        Button exportInventoryBtn = new Button("Export Inventory");
        exportInventoryBtn.getStyleClass().add("secondary");
        exportInventoryBtn.setOnAction(e -> exportInventoryToQuickBooks());
        
        buttonRow.getChildren().addAll(generateReportButton, exportToQuickBooksButton,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            exportSalesBtn, exportCustomersBtn, exportInventoryBtn);
        
        controlsPanel.getChildren().addAll(titleLabel, reportTypeRow, dateRangeRow, 
            customerRow, technicianRow, inventoryFilterRow, buttonRow);
        
        content.setTop(controlsPanel);
        
        // Report display area
        reportTableView = new TableView<>();
        reportTableView.setPlaceholder(new Label("Select a report type and click 'Generate Report' to view data"));
        content.setCenter(reportTableView);
        
        // Initially hide parameter fields
        updateParameterVisibility();
        
        tab.setContent(content);
        return tab;
    }
    
    private Tab createDashboardTab() {
        Tab tab = new Tab("Analytics Dashboard");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Key metrics section
        Label metricsLabel = new Label("Key Performance Indicators");
        metricsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        HBox metricsRow = createKeyMetricsRow();
        
        // Revenue chart section
        Label revenueLabel = new Label("Revenue Trends");
        revenueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox revenueChart = createRevenueChart();
        
        // Top products section
        Label topProductsLabel = new Label("Top Selling Products");
        topProductsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        TableView<ProductSalesReportItem> topProductsTable = createTopProductsTable();
        topProductsTable.setPrefHeight(200);
        
        // Recent activity section
        Label activityLabel = new Label("Recent Sales Activity");
        activityLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox activityFeed = createActivityFeed();
        
        // Add refresh button
        Button refreshButton = new Button("🔄 Refresh Analytics");
        refreshButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5px;");
        refreshButton.setOnAction(e -> refreshAnalyticsMetrics());
        
        HBox refreshContainer = new HBox();
        refreshContainer.setAlignment(javafx.geometry.Pos.CENTER);
        refreshContainer.setPadding(new Insets(10, 0, 0, 0));
        refreshContainer.getChildren().add(refreshButton);
        
        content.getChildren().addAll(
            metricsLabel, metricsRow,
            new Separator(),
            revenueLabel, revenueChart,
            new Separator(),
            topProductsLabel, topProductsTable,
            new Separator(),
            activityLabel, activityFeed,
            new Separator(),
            refreshContainer
        );
        
        scrollPane.setContent(content);
        tab.setContent(scrollPane);
        return tab;
    }
    
    private HBox createKeyMetricsRow() {
        HBox row = new HBox(20);
        row.setAlignment(javafx.geometry.Pos.CENTER);
        row.setPadding(new Insets(20, 0, 20, 0));
        
        // Create metric boxes and store references for refreshing
        monthRevenueBox = createMetricBox("Month Revenue", "Loading...", "#4CAF50", "📈");
        yearRevenueBox = createMetricBox("Year Revenue", "Loading...", "#2196F3", "💰");
        lowStockBox = createMetricBox("Low Stock Items", "Loading...", "#FF9800", "📦");
        avgSaleBox = createMetricBox("Avg Sale Value", "Loading...", "#9C27B0", "💵");
        
        row.getChildren().addAll(monthRevenueBox, yearRevenueBox, lowStockBox, avgSaleBox);
        
        // Refresh with actual data
        refreshAnalyticsMetrics();
        
        return row;
    }
    
    private VBox createMetricBox(String title, String value, String color, String icon) {
        VBox box = new VBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPrefWidth(200);
        box.setPrefHeight(120);
        box.getStyleClass().add("stat-box");
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 30px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setId("metric-value"); // Add ID for easier updating
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        box.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return box;
    }
    
    /**
     * Refresh analytics metrics with current data
     */
    public void refreshAnalyticsMetrics() {
        // Run in background to avoid blocking UI
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                LocalDate today = LocalDate.now();
                LocalDate monthStart = today.withDayOfMonth(1);
                LocalDate yearStart = today.withDayOfYear(1);
                
                // Calculate metrics (EXCLUDING VOIDED SALES for accurate revenue)
                List<Sale> monthSales = salesService.getSalesByDateRange(monthStart, today);
                List<Sale> yearSales = salesService.getSalesByDateRange(yearStart, today);
                
                // Filter to valid (non-voided) sales only
                List<Sale> validMonthSales = monthSales.stream()
                    .filter(sale -> !sale.isVoided())
                    .collect(java.util.stream.Collectors.toList());
                    
                List<Sale> validYearSales = yearSales.stream()
                    .filter(sale -> !sale.isVoided())
                    .collect(java.util.stream.Collectors.toList());
                
                BigDecimal monthRevenue = validMonthSales.stream()
                    .filter(Sale::isPaid)
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                BigDecimal yearRevenue = validYearSales.stream()
                    .filter(Sale::isPaid)
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                int lowStockCount = inventoryService.getLowStockProducts().size();
                
                double avgSaleValue = validMonthSales.isEmpty() ? 0.0 : 
                    monthRevenue.divide(BigDecimal.valueOf(validMonthSales.size()), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    updateMetricBox(monthRevenueBox, "$" + String.format("%,.2f", monthRevenue), "#4CAF50");
                    updateMetricBox(yearRevenueBox, "$" + String.format("%,.2f", yearRevenue), "#2196F3");
                    updateMetricBox(lowStockBox, String.valueOf(lowStockCount), lowStockCount > 0 ? "#FF9800" : "#4CAF50");
                    updateMetricBox(avgSaleBox, "$" + String.format("%.2f", avgSaleValue), "#9C27B0");
                    
                    // Refresh other components if they exist
                    refreshRevenueChart();
                    refreshTopProductsTable();
                    refreshActivityFeed();
                });
                
                return null;
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Update a metric box with new value and color
     */
    private void updateMetricBox(VBox metricBox, String newValue, String newColor) {
        if (metricBox != null) {
            Label valueLabel = (Label) metricBox.getChildren().stream()
                .filter(node -> node instanceof Label && "metric-value".equals(node.getId()))
                .findFirst()
                .orElse(null);
            
            if (valueLabel != null) {
                valueLabel.setText(newValue);
                valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + newColor + ";");
            }
        }
    }
    
    /**
     * Refresh the revenue chart with current data
     */
    private void refreshRevenueChart() {
        if (revenueChart != null) {
            // Clear existing chart content and rebuild
            revenueChart.getChildren().clear();
            
            // Recreate the revenue chart bars
            LocalDate today = LocalDate.now();
            
            // Last 7 days
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                List<Sale> daySales = salesService.getSalesByDateRange(date, date);
                BigDecimal dayRevenue = daySales.stream()
                    .filter(Sale::isPaid)
                    .map(Sale::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                VBox dayBar = createDayBar(date, dayRevenue);
                revenueChart.getChildren().add(dayBar);
            }
        }
    }
    
    /**
     * Refresh the top products table
     */
    private void refreshTopProductsTable() {
        if (topProductsTable != null) {
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            List<ProductSalesReportItem> products = salesService.getProductSalesReport(monthStart, LocalDate.now());
            products.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
            
            if (products.size() > 5) {
                products = products.subList(0, 5);
            }
            
            topProductsTable.setItems(FXCollections.observableArrayList(products));
        }
    }
    
    /**
     * Refresh the activity feed
     */
    private void refreshActivityFeed() {
        if (activityFeed != null) {
            // Clear existing content and rebuild
            activityFeed.getChildren().clear();
            
            // Recreate activity feed content directly
            LocalDate today = LocalDate.now();
            List<Sale> recentSales = salesService.getSalesByDateRange(today.minusDays(7), today);
            recentSales = recentSales.stream()
                .filter(Sale::isPaid)
                .filter(sale -> !sale.isVoided()) // Exclude voided sales from activity feed
                .sorted((s1, s2) -> s2.getTimestamp().compareTo(s1.getTimestamp()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
            
            if (recentSales.isEmpty()) {
                Label noActivityLabel = new Label("No recent sales activity");
                noActivityLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #999;");
                activityFeed.getChildren().add(noActivityLabel);
            } else {
                for (Sale sale : recentSales) {
                    HBox activityItem = createActivityItem(sale);
                    activityFeed.getChildren().add(activityItem);
                }
            }
        }
    }
    
    private HBox createActivityItem(Sale sale) {
        HBox item = new HBox(10);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setPadding(new Insets(5));
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5px;");
        
        Label timeLabel = new Label(sale.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        timeLabel.setPrefWidth(80);
        
        String customerName = sale.getCustomer() != null ? sale.getCustomer().getFullName() : "Walk-in";
        Label customerLabel = new Label(customerName);
        customerLabel.setStyle("-fx-font-size: 11px;");
        customerLabel.setPrefWidth(120);
        
        Label amountLabel = new Label("$" + String.format("%.2f", sale.getTotal()));
        amountLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        item.getChildren().addAll(timeLabel, customerLabel, amountLabel);
        return item;
    }
    
    private VBox createRevenueChart() {
        VBox chartContainer = new VBox(10);
        chartContainer.setPadding(new Insets(20));
        chartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        // Simple bar chart simulation using rectangles
        revenueChart = new HBox(15);
        revenueChart.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
        revenueChart.setPrefHeight(200);
        
        LocalDate today = LocalDate.now();
        
        // Last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<Sale> daySales = salesService.getSalesByDateRange(date, date);
            BigDecimal dayRevenue = daySales.stream()
                .filter(Sale::isPaid)
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            VBox dayBar = createDayBar(date, dayRevenue);
            revenueChart.getChildren().add(dayBar);
        }
        
        chartContainer.getChildren().add(revenueChart);
        return chartContainer;
    }
    
    private VBox createDayBar(LocalDate date, BigDecimal revenue) {
        VBox container = new VBox(5);
        container.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
        
        // Calculate bar height (max 150px)
        double maxRevenue = 10000.0; // Adjust based on your data
        double height = Math.min(150, (revenue.doubleValue() / maxRevenue) * 150);
        
        javafx.scene.shape.Rectangle bar = new javafx.scene.shape.Rectangle(40, height);
        bar.setFill(javafx.scene.paint.Color.web("#4CAF50"));
        bar.setArcWidth(5);
        bar.setArcHeight(5);
        
        Label revenueLabel = new Label("$" + String.format("%.0f", revenue));
        revenueLabel.setStyle("-fx-font-size: 10px;");
        
        Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("MMM dd")));
        dateLabel.setStyle("-fx-font-size: 11px;");
        
        container.getChildren().addAll(revenueLabel, bar, dateLabel);
        return container;
    }
    
    private TableView<ProductSalesReportItem> createTopProductsTable() {
        topProductsTable = new TableView<>();
        
        TableColumn<ProductSalesReportItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        nameCol.setPrefWidth(200);
        
        TableColumn<ProductSalesReportItem, Integer> qtyCol = new TableColumn<>("Qty Sold");
        qtyCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getQuantitySold()));
        qtyCol.setPrefWidth(100);
        
        TableColumn<ProductSalesReportItem, String> revenueCol = new TableColumn<>("Revenue");
        revenueCol.setCellValueFactory(data -> 
            new SimpleStringProperty("$" + String.format("%.2f", data.getValue().getTotalRevenue())));
        revenueCol.setPrefWidth(100);
        
        topProductsTable.getColumns().addAll(nameCol, qtyCol, revenueCol);
        
        // Load top 5 products for current month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<ProductSalesReportItem> products = salesService.getProductSalesReport(monthStart, LocalDate.now());
        products.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        
        if (products.size() > 5) {
            products = products.subList(0, 5);
        }
        
        topProductsTable.setItems(FXCollections.observableArrayList(products));
        return topProductsTable;
    }
    
    private VBox createActivityFeed() {
        activityFeed = new VBox(10);
        activityFeed.setPadding(new Insets(15));
        activityFeed.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        
        // Get recent sales
        List<Sale> recentSales = salesService.getAllSales();
        recentSales.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        int count = 0;
        for (Sale sale : recentSales) {
            if (count++ >= 5) break;
            
            HBox activityItem = new HBox(10);
            activityItem.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            activityItem.setPadding(new Insets(8));
            activityItem.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px;");
            
            javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(5);
            statusDot.setFill(sale.isPaid() ? 
                javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.ORANGE);
            
            VBox details = new VBox(2);
            Label saleLabel = new Label("Sale #" + sale.getInvoiceNumber());
            saleLabel.setStyle("-fx-font-weight: bold;");
            
            String customerName = sale.getCustomer() != null ? 
                sale.getCustomer().getFirstName() + " " + sale.getCustomer().getLastName() : "Walk-in";
            Label customerLabel = new Label(customerName + " - $" + String.format("%.2f", sale.getTotal()));
            customerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            
            Label timeLabel = new Label(sale.getTimestamp().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
            
            details.getChildren().addAll(saleLabel, customerLabel, timeLabel);
            activityItem.getChildren().addAll(statusDot, details);
            
            activityFeed.getChildren().add(activityItem);
        }
        
        return activityFeed;
    }

    private void updateParameterVisibility() {
        String selectedReport = reportTypeComboBox.getValue();
        boolean dateRangeVisible = "Sales Report by Date Range".equals(selectedReport) || 
                                   "Product Sales Report".equals(selectedReport) ||
                                   "Customer Purchase History".equals(selectedReport) ||
                                   "Technician Performance".equals(selectedReport) ||
                                   "Daily Financial Summary".equals(selectedReport);
        boolean noParamsVisible = "Inventory Stock Levels".equals(selectedReport);
        boolean customerSelectVisible = "Customer Purchase History".equals(selectedReport);
        boolean technicianSelectVisible = "Technician Performance".equals(selectedReport);
        boolean inventoryReportSelected = "Inventory Stock Levels".equals(selectedReport);

        fromDatePicker.setVisible(dateRangeVisible && !inventoryReportSelected);
        fromDatePicker.setManaged(dateRangeVisible && !inventoryReportSelected);
        toDatePicker.setVisible(dateRangeVisible && !inventoryReportSelected);
        toDatePicker.setManaged(dateRangeVisible && !inventoryReportSelected);

        if (inventoryReportSelected) {
            fromDatePicker.setVisible(false); fromDatePicker.setManaged(false);
            toDatePicker.setVisible(false); toDatePicker.setManaged(false);
        }

        customerReportComboBox.setVisible(customerSelectVisible);
        customerReportComboBox.setManaged(customerSelectVisible);
        technicianReportComboBox.setVisible(technicianSelectVisible);
        technicianReportComboBox.setManaged(technicianSelectVisible);
        
        tiresOnlyCheckBox.setVisible(inventoryReportSelected);
        tiresOnlyCheckBox.setManaged(inventoryReportSelected);

        if ("Daily Financial Summary".equals(selectedReport)) {
            customerReportComboBox.setVisible(false);
            customerReportComboBox.setManaged(false);
            technicianReportComboBox.setVisible(false);
            technicianReportComboBox.setManaged(false);
        }
    }

    private void populateCustomerComboBox() {
        if (salesService != null) {
            List<Customer> customers = salesService.getAllCustomers();
            customerReportComboBox.setItems(FXCollections.observableArrayList(customers));
        } else {
            System.err.println("[ReportController] SalesService is null. Cannot populate customer ComboBox.");
        }
    }

    private void populateTechnicianComboBox() {
        if (salesService != null) { 
            List<Technician> technicians = salesService.getAllTechnicians();
            technicianReportComboBox.setItems(FXCollections.observableArrayList(technicians));
        } else {
            System.err.println("[ReportController] SalesService is null. Cannot populate technician ComboBox.");
        }
    }

    private void generateReport() {
        String selectedReport = reportTypeComboBox.getValue();
        if (selectedReport == null) {
            showAlert(Alert.AlertType.WARNING, "No Report Selected", "Please select a report type.");
            return;
        }

        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (("Sales Report by Date Range".equals(selectedReport) || "Product Sales Report".equals(selectedReport)) && 
            (fromDate == null || toDate == null)) {
            showAlert(Alert.AlertType.WARNING, "Date Missing", "Please select both From and To dates.");
            return;
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Dates", "'From' date cannot be after 'To' date.");
            return;
        }

        reportTableView.getColumns().clear();
        reportTableView.getItems().clear();

        switch (selectedReport) {
            case "Sales Report by Date Range":
                generateSalesReportByDateRange(fromDate, toDate);
                exportToQuickBooksButton.setDisable(false);
                break;
            case "Product Sales Report":
                generateProductSalesReport(fromDate, toDate);
                exportToQuickBooksButton.setDisable(true); // Not applicable for product sales report
                break;
            case "Inventory Stock Levels":
                generateInventoryStockReport();
                exportToQuickBooksButton.setDisable(true); // Use dedicated inventory export button
                break;
            case "Customer Purchase History":
                Customer selectedCustomer = customerReportComboBox.getValue();
                if (selectedCustomer == null) {
                    showAlert(Alert.AlertType.WARNING, "Customer Not Selected", "Please select a customer for the report.");
                    return;
                }
                generateCustomerPurchaseHistoryReport(selectedCustomer.getId(), fromDate, toDate);
                exportToQuickBooksButton.setDisable(false);
                break;
            case "Technician Performance":
                Technician selectedTechnician = technicianReportComboBox.getValue();
                if (selectedTechnician == null) {
                    showAlert(Alert.AlertType.WARNING, "Technician Not Selected", "Please select a technician for the report.");
                    return;
                }
                generateTechnicianPerformanceReport(selectedTechnician.getId(), fromDate, toDate);
                exportToQuickBooksButton.setDisable(true); // Not applicable for technician performance
                break;
            case "Daily Financial Summary":
                if (fromDate == null || toDate == null) { 
                    showAlert(Alert.AlertType.WARNING, "Date Missing", "Please select both From and To dates for Daily Financial Summary.");
                    return;
                }
                generateDailyFinancialSummaryReport(fromDate, toDate);
                exportToQuickBooksButton.setDisable(true); // Summary report, not directly exportable
                break;
        }
    }

    private void generateSalesReportByDateRange(LocalDate from, LocalDate to) {
        List<Sale> sales = salesService.getSalesByDateRange(from, to);
        
        // Filter out voided sales for accurate revenue reporting
        List<Sale> validSales = sales.stream()
            .filter(sale -> !sale.isVoided())
            .collect(java.util.stream.Collectors.toList());
        
        setupSalesByDateReportTable();
        reportDataList = FXCollections.observableArrayList(validSales);
        reportTableView.setItems(reportDataList);
        
        int voidedCount = sales.size() - validSales.size();
        System.out.println("[ReportController] Sales Report: " + validSales.size() + " valid sales, " + 
                         voidedCount + " voided sales excluded");
    }

    private void generateProductSalesReport(LocalDate from, LocalDate to) {
        List<ProductSalesReportItem> productSales = salesService.getProductSalesReport(from, to);
        setupProductSalesReportTable();
        reportDataList = FXCollections.observableArrayList(productSales);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Product Sales Report: " + productSales.size() + " products found.");
    }

    private void generateInventoryStockReport() {
        List<Product> products = inventoryService.getAllProducts();
        
        // Filter for tires only if checkbox is selected
        if (tiresOnlyCheckBox.isSelected()) {
            products = products.stream()
                .filter(p -> "Tires".equalsIgnoreCase(p.getCategory()) || 
                           (p.getName() != null && p.getSize() != null && !p.getSize().isEmpty()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        setupInventoryStockReportTable(); // Create this method
        reportDataList = FXCollections.observableArrayList(products);
        reportTableView.setItems(reportDataList);
        
        // Calculate totals
        int totalQuantity = products.stream()
            .mapToInt(Product::getQuantityInStock)
            .sum();
        
        // Calculate total value using selling price (most products have this)
        BigDecimal totalValue = products.stream()
            .filter(p -> p.getSellingPrice() != null)
            .map(p -> p.getSellingPrice().multiply(BigDecimal.valueOf(p.getQuantityInStock())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Also calculate cost if available
        BigDecimal totalCost = products.stream()
            .filter(p -> p.getPurchasePrice() != null)
            .map(p -> p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getQuantityInStock())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Show summary below the table
        String reportType = tiresOnlyCheckBox.isSelected() ? "TIRE INVENTORY" : "FULL INVENTORY";
        showInventorySummary(reportType, products.size(), totalQuantity, totalValue);
        
        System.out.println("[ReportController] Generated Inventory Stock Report: " + products.size() + " products listed.");
        System.out.println("[ReportController] Total Quantity: " + totalQuantity + ", Total Value: $" + totalValue + 
                         ", Total Cost: $" + totalCost);
    }

    private void generateCustomerPurchaseHistoryReport(Long customerId, LocalDate from, LocalDate to) {
        List<Sale> sales = salesService.getCustomerPurchaseHistory(customerId, from, to);
        
        // Filter out voided sales for accurate customer history
        List<Sale> validSales = sales.stream()
            .filter(sale -> !sale.isVoided())
            .collect(java.util.stream.Collectors.toList());
        
        setupSalesByDateReportTable(); // Can reuse the same table structure
        reportDataList = FXCollections.observableArrayList(validSales);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Customer Purchase History: " + validSales.size() + " valid sales found.");
    }

    private void generateTechnicianPerformanceReport(Long technicianId, LocalDate from, LocalDate to) {
        List<TechnicianPerformanceReportItem> performanceItems = salesService.getTechnicianPerformanceReport(technicianId, from, to);
        setupTechnicianPerformanceReportTable(); 
        reportDataList = FXCollections.observableArrayList(performanceItems);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Technician Performance Report: " + performanceItems.size() + " service items found.");
    }

    private void generateDailyFinancialSummaryReport(LocalDate from, LocalDate to) {
        List<DailyFinancialSummaryItem> summaries = salesService.getDailyFinancialSummaries(from, to);
        // Note: getDailyFinancialSummaries already filters to paid sales, but we should also exclude voided
        setupDailyFinancialSummaryReportTable(); // Create this method
        reportDataList = FXCollections.observableArrayList(summaries);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Daily Financial Summary: " + summaries.size() + " daily records found.");
    }
    
    private void generateVoidedSalesReport(LocalDate from, LocalDate to) {
        List<Sale> sales = salesService.getSalesByDateRange(from, to);
        
        // Filter to show ONLY voided sales
        List<Sale> voidedSales = sales.stream()
            .filter(Sale::isVoided)
            .collect(java.util.stream.Collectors.toList());
        
        setupVoidedSalesReportTable();
        reportDataList = FXCollections.observableArrayList(voidedSales);
        reportTableView.setItems(reportDataList);
        
        // Calculate total refunds
        BigDecimal totalRefunds = voidedSales.stream()
            .map(Sale::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.out.println("[ReportController] Voided Sales Report: " + voidedSales.size() + 
                         " returns/voids, Total refunds: $" + totalRefunds);
    }

    @SuppressWarnings("unchecked")
    private void setupSalesByDateReportTable() {
        reportTableView.getColumns().clear();

        TableColumn<Sale, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Sale, String> invoiceCol = new TableColumn<>("Invoice #");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Sale, LocalDateTime> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        dateCol.setCellFactory(column -> new TableCell<Sale, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        TableColumn<Sale, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? customer.getFullName() : "N/A");
        });

        TableColumn<Sale, BigDecimal> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        subtotalCol.setCellFactory(tc -> new TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        TableColumn<Sale, BigDecimal> taxCol = new TableColumn<>("Tax");
        taxCol.setCellValueFactory(new PropertyValueFactory<>("tax"));
        taxCol.setCellFactory(tc -> new TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });

        TableColumn<Sale, BigDecimal> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setCellFactory(tc -> new TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        
        TableColumn<Sale, String> paymentTypeCol = new TableColumn<>("Payment Type");
        paymentTypeCol.setCellValueFactory(cellData -> {
            PaymentType pt = cellData.getValue().getPaymentType();
            return new SimpleStringProperty(pt != null ? pt.getDisplayName() : "N/A");
        });

        TableColumn<Sale, Boolean> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(new PropertyValueFactory<>("paid"));
        paidCol.setCellFactory(tc -> new TableCell<Sale, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Yes" : "No"));
            }
        });

        reportTableView.getColumns().addAll(idCol, invoiceCol, dateCol, customerCol, subtotalCol, taxCol, totalCol, paymentTypeCol, paidCol);
    }
    
    @SuppressWarnings("unchecked")
    private void setupVoidedSalesReportTable() {
        reportTableView.getColumns().clear();

        TableColumn<Sale, String> invoiceCol = new TableColumn<>("Invoice #");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Sale, LocalDateTime> saleDateCol = new TableColumn<>("Original Sale Date");
        saleDateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        saleDateCol.setCellFactory(column -> new TableCell<Sale, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        TableColumn<Sale, LocalDateTime> voidDateCol = new TableColumn<>("Void Date");
        voidDateCol.setCellValueFactory(new PropertyValueFactory<>("voidTimestamp"));
        voidDateCol.setCellFactory(column -> new TableCell<Sale, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        TableColumn<Sale, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? customer.getFullName() : "N/A");
        });
        
        TableColumn<Sale, String> reasonCol = new TableColumn<>("Void Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("voidReason"));

        TableColumn<Sale, BigDecimal> totalCol = new TableColumn<>("Refund Amount");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setCellFactory(tc -> new TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
                if (!empty && item != null) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });
        
        TableColumn<Sale, String> paymentTypeCol = new TableColumn<>("Original Payment");
        paymentTypeCol.setCellValueFactory(cellData -> {
            PaymentType pt = cellData.getValue().getPaymentType();
            return new SimpleStringProperty(pt != null ? pt.getDisplayName() : "N/A");
        });

        reportTableView.getColumns().addAll(invoiceCol, saleDateCol, voidDateCol, customerCol, 
                                          reasonCol, totalCol, paymentTypeCol);
    }

    @SuppressWarnings("unchecked")
    private void setupProductSalesReportTable() {
        reportTableView.getColumns().clear();
        reportTableView.setPlaceholder(new Label("No product sales data for the selected period."));

        TableColumn<ProductSalesReportItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(250);

        TableColumn<ProductSalesReportItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
        categoryCol.setPrefWidth(150);

        TableColumn<ProductSalesReportItem, Integer> quantityCol = new TableColumn<>("Quantity Sold");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        quantityCol.setPrefWidth(100);

        TableColumn<ProductSalesReportItem, BigDecimal> revenueCol = new TableColumn<>("Total Revenue");
        revenueCol.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        revenueCol.setCellFactory(tc -> new TableCell<ProductSalesReportItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        revenueCol.setPrefWidth(150);

        reportTableView.getColumns().addAll(nameCol, categoryCol, quantityCol, revenueCol);
    }

    /**
     * Show inventory summary with totals
     */
    private void showInventorySummary(String reportType, int productCount, int totalQuantity, BigDecimal totalCost) {
        // Create summary panel
        VBox summaryPanel = new VBox(10);
        summaryPanel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15px; -fx-background-radius: 5px;");
        
        Label titleLabel = new Label("📊 " + reportType + " SUMMARY");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(30);
        summaryGrid.setVgap(10);
        summaryGrid.setPadding(new Insets(10, 0, 0, 0));
        
        // Total Products
        Label productsLabel = new Label("Total Products:");
        productsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label productsValue = new Label(String.valueOf(productCount));
        productsValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        
        // Total Quantity
        Label quantityLabel = new Label("Total Units in Stock:");
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label quantityValue = new Label(String.format("%,d", totalQuantity));
        quantityValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        
        // Total Cost
        Label costLabel = new Label("Total Inventory Value:");
        costLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label costValue = new Label(String.format("$%,.2f", totalCost));
        costValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        summaryGrid.add(productsLabel, 0, 0);
        summaryGrid.add(productsValue, 1, 0);
        summaryGrid.add(quantityLabel, 0, 1);
        summaryGrid.add(quantityValue, 1, 1);
        summaryGrid.add(costLabel, 0, 2);
        summaryGrid.add(costValue, 1, 2);
        
        summaryPanel.getChildren().addAll(titleLabel, summaryGrid);
        
        // Find the report generator pane and add summary at bottom
        if (reportsPane != null && reportsPane.getCenter() instanceof TabPane) {
            TabPane tabPane = (TabPane) reportsPane.getCenter();
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.getContent() instanceof BorderPane) {
                BorderPane reportGenPane = (BorderPane) selectedTab.getContent();
                reportGenPane.setBottom(summaryPanel);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setupInventoryStockReportTable() {
        reportTableView.getColumns().clear();
        reportTableView.setPlaceholder(new Label("No inventory data available."));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);
        
        TableColumn<Product, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeCol.setPrefWidth(120);

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Qty");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        quantityCol.setPrefWidth(80);
        quantityCol.setCellFactory(tc -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%,d", item));
                    // Set tire quantity text to black for better readability
                    setStyle("-fx-text-fill: black;");
                }
            }
        });

        // Price column - uses sellingPrice (what you charge customers)
        TableColumn<Product, BigDecimal> sellingPriceCol = new TableColumn<>("Price");
        sellingPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        sellingPriceCol.setCellFactory(tc -> new TableCell<Product, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
                if (!empty && item != null) {
                    setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
                }
            }
        });
        sellingPriceCol.setPrefWidth(100);

        // Total Value column - calculated from selling price × quantity
        TableColumn<Product, BigDecimal> stockValueCol = new TableColumn<>("Total Value");
        stockValueCol.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            if (p.getSellingPrice() != null) {
                BigDecimal stockValue = p.getSellingPrice().multiply(BigDecimal.valueOf(p.getQuantityInStock()));
                return new SimpleObjectProperty<>(stockValue);
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        stockValueCol.setCellFactory(tc -> new TableCell<Product, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%,.2f", item));
                if (!empty && item != null) {
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        stockValueCol.setPrefWidth(120);

        reportTableView.getColumns().addAll(nameCol, categoryCol, sizeCol, quantityCol, sellingPriceCol, stockValueCol);
    }

    @SuppressWarnings("unchecked")
    private void setupTechnicianPerformanceReportTable() {
        reportTableView.getColumns().clear();
        reportTableView.setPlaceholder(new Label("No performance data for the selected technician and period."));

        TableColumn<TechnicianPerformanceReportItem, String> serviceNameCol = new TableColumn<>("Service Name");
        serviceNameCol.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        serviceNameCol.setPrefWidth(250);

        TableColumn<TechnicianPerformanceReportItem, LocalDateTime> dateCol = new TableColumn<>("Date/Time");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("serviceDate"));
        dateCol.setCellFactory(column -> new TableCell<TechnicianPerformanceReportItem, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });
        dateCol.setPrefWidth(150);

        TableColumn<TechnicianPerformanceReportItem, String> invoiceCol = new TableColumn<>("Invoice #");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("saleInvoiceNumber"));
        invoiceCol.setPrefWidth(150);

        TableColumn<TechnicianPerformanceReportItem, BigDecimal> priceCol = new TableColumn<>("Service Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("servicePrice"));
        priceCol.setCellFactory(tc -> new TableCell<TechnicianPerformanceReportItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        priceCol.setPrefWidth(120);

        reportTableView.getColumns().addAll(serviceNameCol, dateCol, invoiceCol, priceCol);
    }

    @SuppressWarnings("unchecked")
    private void setupDailyFinancialSummaryReportTable() {
        reportTableView.getColumns().clear();
        reportTableView.setPlaceholder(new Label("No financial summary data for the selected period."));

        TableColumn<DailyFinancialSummaryItem, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(120);
        dateCol.setCellFactory(column -> new TableCell<DailyFinancialSummaryItem, LocalDate>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });

        TableColumn<DailyFinancialSummaryItem, BigDecimal> subtotalCol = new TableColumn<>("Total Subtotal");
        subtotalCol.setCellValueFactory(new PropertyValueFactory<>("totalSubtotal"));
        subtotalCol.setCellFactory(this.createCurrencyCellFactory());
        subtotalCol.setPrefWidth(150);

        TableColumn<DailyFinancialSummaryItem, BigDecimal> taxCol = new TableColumn<>("Total Tax");
        taxCol.setCellValueFactory(new PropertyValueFactory<>("totalTax"));
        taxCol.setCellFactory(this.createCurrencyCellFactory());
        taxCol.setPrefWidth(150);

        TableColumn<DailyFinancialSummaryItem, BigDecimal> revenueCol = new TableColumn<>("Total Gross Revenue");
        revenueCol.setCellValueFactory(new PropertyValueFactory<>("totalGrossRevenue"));
        revenueCol.setCellFactory(this.createCurrencyCellFactory());
        revenueCol.setPrefWidth(180);

        reportTableView.getColumns().addAll(dateCol, subtotalCol, taxCol, revenueCol);
    }

    private <S> javafx.util.Callback<TableColumn<S, BigDecimal>, TableCell<S, BigDecimal>> createCurrencyCellFactory() {
        return tc -> new TableCell<S, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        };
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }
    
    private void exportToQuickBooks() {
        String selectedReport = reportTypeComboBox.getValue();
        if (selectedReport == null || reportDataList == null || reportDataList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "Please generate a report first before exporting.");
            return;
        }
        
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        
        if ("Sales Report by Date Range".equals(selectedReport) || "Customer Purchase History".equals(selectedReport)) {
            if (fromDate == null || toDate == null) {
                showAlert(Alert.AlertType.WARNING, "Date Required", "Please select date range for export.");
                return;
            }
            
            QuickBooksExportService.ExportResult result = quickBooksExportService.exportSales(fromDate, toDate);
            
            if (result.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                         result.getMessage() + "\n\nFile saved to: " + result.getFilePath());
            } else {
                showAlert(Alert.AlertType.ERROR, "Export Failed", result.getMessage());
            }
        }
    }
    
    private void exportSalesToQuickBooks() {
        // Create dialog for date range selection
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Export Sales to QuickBooks");
        dialog.setHeaderText("Select date range for sales export");
        dialog.initOwner(stage);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        DatePicker fromPicker = new DatePicker(LocalDate.now().minusMonths(1));
        DatePicker toPicker = new DatePicker(LocalDate.now());
        
        grid.add(new Label("From Date:"), 0, 0);
        grid.add(fromPicker, 1, 0);
        grid.add(new Label("To Date:"), 0, 1);
        grid.add(toPicker, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            LocalDate from = fromPicker.getValue();
            LocalDate to = toPicker.getValue();
            
            if (from == null || to == null) {
                showAlert(Alert.AlertType.WARNING, "Date Required", "Please select both dates.");
                return;
            }
            
            QuickBooksExportService.ExportResult exportResult = quickBooksExportService.exportSales(from, to);
            
            if (exportResult.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                         exportResult.getMessage() + "\n\nFile saved to: " + exportResult.getFilePath());
            } else {
                showAlert(Alert.AlertType.ERROR, "Export Failed", exportResult.getMessage());
            }
        }
    }
    
    private void exportCustomersToQuickBooks() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Export Customers");
        confirmAlert.setHeaderText("Export all customers to QuickBooks?");
        confirmAlert.setContentText("This will create an IIF file with all customer data.");
        confirmAlert.initOwner(stage);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            QuickBooksExportService.ExportResult exportResult = quickBooksExportService.exportCustomers();
            
            if (exportResult.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                         exportResult.getMessage() + "\n\nFile saved to: " + exportResult.getFilePath());
            } else {
                showAlert(Alert.AlertType.ERROR, "Export Failed", exportResult.getMessage());
            }
        }
    }
    
    private void exportInventoryToQuickBooks() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Export Inventory");
        confirmAlert.setHeaderText("Export all inventory items to QuickBooks?");
        confirmAlert.setContentText("This will create an IIF file with all product data.");
        confirmAlert.initOwner(stage);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            QuickBooksExportService.ExportResult exportResult = quickBooksExportService.exportInventory();
            
            if (exportResult.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                         exportResult.getMessage() + "\n\nFile saved to: " + exportResult.getFilePath());
            } else {
                showAlert(Alert.AlertType.ERROR, "Export Failed", exportResult.getMessage());
            }
        }
    }
    
    /**
     * Export all available reports to a selected directory
     */
    private void exportAllReports() {
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Directory for Export");
        directoryChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        
        java.io.File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory == null) {
            return;
        }
        
        // Show progress dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Exporting Reports");
        progressAlert.setHeaderText("Exporting all reports...");
        progressAlert.setContentText("This may take a few moments.");
        progressAlert.initOwner(stage);
        progressAlert.show();
        
        // Export all reports in a background thread
        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now();
                LocalDate monthStart = today.withDayOfMonth(1);
                LocalDate yearStart = today.withDayOfYear(1);
                
                // Export Sales Report
                QuickBooksExportService.ExportResult salesResult = 
                    quickBooksExportService.exportSales(monthStart, today);
                    
                // Export Customer Report
                QuickBooksExportService.ExportResult customerResult = 
                    quickBooksExportService.exportCustomers();
                    
                // Export Inventory Report
                QuickBooksExportService.ExportResult inventoryResult = 
                    quickBooksExportService.exportInventory();
                
                // Export Product Sales Report as CSV
                List<ProductSalesReportItem> productSales = 
                    salesService.getProductSalesReport(monthStart, today);
                exportProductSalesToCsv(productSales, 
                    selectedDirectory.getAbsolutePath() + "/ProductSalesReport.csv");
                
                // Export Financial Summary as CSV
                List<DailyFinancialSummaryItem> financialSummary = 
                    salesService.getDailyFinancialSummaries(monthStart, today);
                exportFinancialSummaryToCsv(financialSummary, 
                    selectedDirectory.getAbsolutePath() + "/FinancialSummary.csv");
                
                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert(Alert.AlertType.INFORMATION, "Export Complete", 
                        "All reports have been exported to:\n" + selectedDirectory.getAbsolutePath());
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    showAlert(Alert.AlertType.ERROR, "Export Failed", 
                        "An error occurred while exporting reports: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Print the currently displayed report
     */
    private void printCurrentReport() {
        if (reportTableView == null || reportTableView.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Report", 
                "Please generate a report first before printing.");
            return;
        }
        
        // Create a printer job
        javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
        if (printerJob == null) {
            showAlert(Alert.AlertType.ERROR, "Print Error", 
                "No printer available. Please check your printer settings.");
            return;
        }
        
        // Show print dialog
        boolean proceed = printerJob.showPrintDialog(stage);
        if (!proceed) {
            return;
        }
        
        // Create a printable version of the report with DARK text
        VBox printContent = new VBox(10);
        printContent.setPadding(new Insets(20));
        printContent.setStyle("-fx-background-color: white;");
        
        // Add company header
        Label companyLabel = new Label(settingsService.getCompanyName());
        companyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");
        
        Label addressLabel = new Label(settingsService.getCompanyAddress());
        addressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: black;");
        
        Label phoneLabel = new Label(settingsService.getCompanyPhone());
        phoneLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: black;");
        
        // Add report title
        String reportTitle = reportTypeComboBox.getValue() != null ? 
            reportTypeComboBox.getValue() : "Report";
        Label titleLabel = new Label(reportTitle);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: black; -fx-underline: true;");
        
        // Add date range if applicable
        Label dateRangeLabel = new Label("");
        if (fromDatePicker.getValue() != null && toDatePicker.getValue() != null) {
            dateRangeLabel.setText("Date Range: " + fromDatePicker.getValue() + " to " + toDatePicker.getValue());
            dateRangeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: black; -fx-font-weight: bold;");
        }
        
        Label printDateLabel = new Label("Printed: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
        printDateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
        
        Separator separator1 = new Separator();
        
        printContent.getChildren().addAll(companyLabel, addressLabel, phoneLabel, 
                                         new Label(""), titleLabel, dateRangeLabel, printDateLabel, separator1);
        
        // Create a simplified table for printing with DARK text styling
        TableView printTable = new TableView();
        printTable.getColumns().addAll(reportTableView.getColumns());
        printTable.setItems(reportTableView.getItems());
        printTable.setPrefHeight(600);
        // CRITICAL: Make table text DARK for printing
        printTable.setStyle("-fx-text-fill: black; -fx-font-weight: bold; " +
                          "-fx-control-inner-background: white; " +
                          "-fx-background-color: white;");
        
        printContent.getChildren().add(printTable);
        
        // Add totals footer if applicable
        addReportTotals(printContent);
        
        // Print the content
        boolean printed = printerJob.printPage(printContent);
        
        if (printed) {
            printerJob.endJob();
            showAlert(Alert.AlertType.INFORMATION, "Print Complete", 
                "The report has been sent to the printer.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Print Failed", 
                "Failed to print the report.");
        }
    }
    
    /**
     * Add totals footer to printed report
     */
    private void addReportTotals(VBox printContent) {
        if (reportDataList == null || reportDataList.isEmpty()) {
            return;
        }
        
        Separator separator = new Separator();
        printContent.getChildren().add(separator);
        
        // Calculate totals based on report type
        String reportType = reportTypeComboBox.getValue();
        
        if (reportType != null && (reportType.contains("Sales") || reportType.contains("Customer") || reportType.contains("Financial"))) {
            // Try to calculate revenue totals
            BigDecimal totalRevenue = BigDecimal.ZERO;
            int recordCount = reportDataList.size();
            
            // Check if items are Sale objects
            if (!reportDataList.isEmpty() && reportDataList.get(0) instanceof Sale) {
                for (Object obj : reportDataList) {
                    Sale sale = (Sale) obj;
                    if (sale.getTotal() != null) {
                        totalRevenue = totalRevenue.add(sale.getTotal());
                    }
                }
                
                Label totalLabel = new Label(String.format("Total Revenue: $%.2f  |  Sales Count: %d", totalRevenue, recordCount));
                totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: black;");
                printContent.getChildren().add(totalLabel);
            }
            // Check if items are ProductSalesReportItem objects
            else if (!reportDataList.isEmpty() && reportDataList.get(0) instanceof ProductSalesReportItem) {
                for (Object obj : reportDataList) {
                    ProductSalesReportItem item = (ProductSalesReportItem) obj;
                    if (item.getTotalRevenue() != null) {
                        totalRevenue = totalRevenue.add(item.getTotalRevenue());
                    }
                }
                
                Label totalLabel = new Label(String.format("Total Revenue: $%.2f  |  Products: %d", totalRevenue, recordCount));
                totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: black;");
                printContent.getChildren().add(totalLabel);
            }
            // Check if items are DailyFinancialSummaryItem objects
            else if (!reportDataList.isEmpty() && reportDataList.get(0) instanceof DailyFinancialSummaryItem) {
                for (Object obj : reportDataList) {
                    DailyFinancialSummaryItem item = (DailyFinancialSummaryItem) obj;
                    if (item.getTotalGrossRevenue() != null) {
                        totalRevenue = totalRevenue.add(item.getTotalGrossRevenue());
                    }
                }
                
                Label totalLabel = new Label(String.format("Total Revenue: $%.2f  |  Days: %d", totalRevenue, recordCount));
                totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: black;");
                printContent.getChildren().add(totalLabel);
            }
        }
        
        // Add print timestamp
        Label footerLabel = new Label("Report generated by " + settingsService.getCompanyName());
        footerLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: black; -fx-font-style: italic;");
        printContent.getChildren().add(new Label(""));
        printContent.getChildren().add(footerLabel);
    }
    
    /**
     * Export product sales data to CSV
     */
    private void exportProductSalesToCsv(List<ProductSalesReportItem> items, String filePath) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File(filePath))) {
            // Write headers
            writer.println("Product Name,Category,Quantity Sold,Total Revenue");
            
            // Write data
            for (ProductSalesReportItem item : items) {
                writer.println(String.format("%s,%s,%d,$%.2f",
                    item.getProductName(),
                    item.getProductCategory() != null ? item.getProductCategory() : "",
                    item.getQuantitySold(),
                    item.getTotalRevenue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Export financial summary data to CSV
     */
    private void exportFinancialSummaryToCsv(List<DailyFinancialSummaryItem> items, String filePath) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.File(filePath))) {
            // Write headers
            writer.println("Date,Total Subtotal,Total Tax,Total Gross Revenue");
            
            // Write data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (DailyFinancialSummaryItem item : items) {
                writer.println(String.format("%s,$%.2f,$%.2f,$%.2f",
                    item.getDate().format(formatter),
                    item.getTotalSubtotal(),
                    item.getTotalTax(),
                    item.getTotalGrossRevenue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 