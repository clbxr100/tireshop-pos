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
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Import the DTO
import com.tireshop.model.dto.ProductSalesReportItem;
import com.tireshop.model.dto.TechnicianPerformanceReportItem;
import com.tireshop.model.Product;
import com.tireshop.service.InventoryService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;
import com.tireshop.model.Technician;
import com.tireshop.model.dto.DailyFinancialSummaryItem;

public class ReportController {

    private SalesService salesService; // Assuming we'll need this for sales reports
    private InventoryService inventoryService; // Add InventoryService
    private Stage stage;

    private ComboBox<String> reportTypeComboBox;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<Customer> customerReportComboBox; // ComboBox for selecting customer
    private ComboBox<Technician> technicianReportComboBox; // ComboBox for selecting technician
    private Button generateReportButton;
    private TableView reportTableView; // Generic for now, will be specialized
    private ObservableList reportDataList; // Generic for now

    // Placeholder for other services if needed for other reports
    // private CustomerService customerService; // If we create one

    public ReportController(SalesService salesService, InventoryService inventoryService /*, other services... */) {
        System.out.println("[CONSTRUCTOR] ReportController instance created.");
        this.salesService = salesService;
        this.inventoryService = inventoryService; // Assign InventoryService
    }

    public void initialize(BorderPane parentPane, Stage stage) {
        System.out.println("[INITIALIZE] ReportController.initialize called.");
        this.stage = stage;

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));

        // Report Selection and Parameters
        HBox reportSelectionBox1 = new HBox(10);
        reportTypeComboBox = new ComboBox<>();
        reportTypeComboBox.setPromptText("Select Report Type");
        reportTypeComboBox.getItems().addAll(
                "Sales Report by Date Range",
                "Product Sales Report",
                "Inventory Stock Levels",
                "Customer Purchase History",
                "Technician Performance",
                "Daily Financial Summary"
        );
        reportTypeComboBox.setOnAction(e -> updateParameterVisibility());
        reportSelectionBox1.getChildren().addAll(new Label("Report Type:"), reportTypeComboBox);

        HBox reportSelectionBox2 = new HBox(10);
        reportSelectionBox2.setSpacing(10); // Ensure spacing for multiple controls
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        toDatePicker = new DatePicker(LocalDate.now());
        
        customerReportComboBox = new ComboBox<>();
        customerReportComboBox.setPromptText("Select Customer");
        populateCustomerComboBox(); // Populate with customers
        customerReportComboBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? null : customer.getFullName();
            }
            @Override
            public Customer fromString(String string) { return null; } // Not needed for selection
        });

        technicianReportComboBox = new ComboBox<>();
        technicianReportComboBox.setPromptText("Select Technician");
        populateTechnicianComboBox();
        technicianReportComboBox.setConverter(new StringConverter<Technician>() {
            @Override
            public String toString(Technician t) { return t == null ? null : t.getFirstName() + " " + t.getLastName(); }
            @Override
            public Technician fromString(String s) { return null; }
        });

        generateReportButton = new Button("Generate Report");
        generateReportButton.setOnAction(e -> generateReport());

        reportSelectionBox2.getChildren().addAll(
            new Label("From:"), fromDatePicker, 
            new Label("To:"), toDatePicker, 
            new Label("Customer:"), customerReportComboBox,
            new Label("Technician:"), technicianReportComboBox,
            generateReportButton
        );

        // Report Display Area
        reportTableView = new TableView<>();
        VBox.setVgrow(reportTableView, javafx.scene.layout.Priority.ALWAYS);

        mainLayout.getChildren().addAll(reportSelectionBox1, reportSelectionBox2, reportTableView);
        parentPane.setCenter(mainLayout);

        updateParameterVisibility(); // Initial setup of parameter visibility
        System.out.println("[INITIALIZE] ReportController UI setup complete.");
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
                break;
            case "Product Sales Report":
                generateProductSalesReport(fromDate, toDate);
                break;
            case "Inventory Stock Levels":
                generateInventoryStockReport();
                break;
            case "Customer Purchase History":
                Customer selectedCustomer = customerReportComboBox.getValue();
                if (selectedCustomer == null) {
                    showAlert(Alert.AlertType.WARNING, "Customer Not Selected", "Please select a customer for the report.");
                    return;
                }
                generateCustomerPurchaseHistoryReport(selectedCustomer.getId(), fromDate, toDate);
                break;
            case "Technician Performance":
                Technician selectedTechnician = technicianReportComboBox.getValue();
                if (selectedTechnician == null) {
                    showAlert(Alert.AlertType.WARNING, "Technician Not Selected", "Please select a technician for the report.");
                    return;
                }
                generateTechnicianPerformanceReport(selectedTechnician.getId(), fromDate, toDate);
                break;
            case "Daily Financial Summary":
                if (fromDate == null || toDate == null) { 
                    showAlert(Alert.AlertType.WARNING, "Date Missing", "Please select both From and To dates for Daily Financial Summary.");
                    return;
                }
                generateDailyFinancialSummaryReport(fromDate, toDate);
                break;
        }
    }

    private void generateSalesReportByDateRange(LocalDate from, LocalDate to) {
        List<Sale> sales = salesService.getSalesByDateRange(from, to); // This method needs to be created in SalesService & SaleDao
        setupSalesByDateReportTable();
        reportDataList = FXCollections.observableArrayList(sales);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Sales Report by Date Range: " + sales.size() + " sales found.");
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
        setupInventoryStockReportTable(); // Create this method
        reportDataList = FXCollections.observableArrayList(products);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Inventory Stock Report: " + products.size() + " products listed.");
    }

    private void generateCustomerPurchaseHistoryReport(Long customerId, LocalDate from, LocalDate to) {
        List<Sale> sales = salesService.getCustomerPurchaseHistory(customerId, from, to); // New method in SalesService
        setupSalesByDateReportTable(); // Can reuse the same table structure
        reportDataList = FXCollections.observableArrayList(sales);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Customer Purchase History: " + sales.size() + " sales found.");
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
        setupDailyFinancialSummaryReportTable(); // Create this method
        reportDataList = FXCollections.observableArrayList(summaries);
        reportTableView.setItems(reportDataList);
        System.out.println("[ReportController] Generated Daily Financial Summary: " + summaries.size() + " daily records found.");
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
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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

    @SuppressWarnings("unchecked")
    private void setupInventoryStockReportTable() {
        reportTableView.getColumns().clear();
        reportTableView.setPlaceholder(new Label("No inventory data available."));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);

        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(150);

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Quantity In Stock");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        quantityCol.setPrefWidth(120);

        TableColumn<Product, BigDecimal> purchasePriceCol = new TableColumn<>("Purchase Price");
        purchasePriceCol.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        purchasePriceCol.setCellFactory(tc -> new TableCell<Product, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        purchasePriceCol.setPrefWidth(120);

        TableColumn<Product, BigDecimal> stockValueCol = new TableColumn<>("Stock Value");
        stockValueCol.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            if (p.getPurchasePrice() != null) {
                BigDecimal stockValue = p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getQuantityInStock()));
                return new SimpleObjectProperty<>(stockValue);
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        stockValueCol.setCellFactory(tc -> new TableCell<Product, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
            }
        });
        stockValueCol.setPrefWidth(120);

        reportTableView.getColumns().addAll(nameCol, categoryCol, quantityCol, purchasePriceCol, stockValueCol);
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
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
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
        alert.initOwner(this.stage);
        alert.showAndWait();
    }
} 