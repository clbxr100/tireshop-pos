package com.tireshop.view;

import com.tireshop.controller.InventoryController;
import com.tireshop.controller.PaymentController;
import com.tireshop.controller.SalesController;
import com.tireshop.controller.AppointmentController;
import com.tireshop.controller.ServiceController;
import com.tireshop.controller.AdminController;
import com.tireshop.controller.ReportController;
import com.tireshop.dao.CustomerDao;
import com.tireshop.dao.HibernateDao;
import com.tireshop.dao.ProductDao;
import com.tireshop.dao.SaleDao;
import com.tireshop.dao.SaleItemDao;
import com.tireshop.dao.AppointmentDao;
import com.tireshop.dao.VehicleDao;
import com.tireshop.dao.ServiceDao;
import com.tireshop.dao.TechnicianDao;
import com.tireshop.model.Customer;
import com.tireshop.model.Product;
import com.tireshop.model.Sale;
import com.tireshop.model.SaleItem;
import com.tireshop.model.Appointment;
import com.tireshop.model.Vehicle;
import com.tireshop.model.Technician;
import com.tireshop.model.AppointmentStatus;
import com.tireshop.model.dto.SalesSummaryData;
import com.tireshop.service.InventoryService;
import com.tireshop.service.SalesService;
import com.tireshop.service.AppointmentService;
import com.tireshop.util.DatabaseManager;
import com.tireshop.util.SettingsService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalTime;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.layout.ColumnConstraints;

/**
 * Main application view with navigation
 */
public class MainView {
    
    private Stage stage;
    private TabPane tabPane;
    private InventoryController inventoryController;
    private SalesController salesController;
    private PaymentController paymentController;
    private AppointmentController appointmentController;
    private ServiceController serviceController;
    private AdminController adminController;
    private ReportController reportController;
    private MainView mainViewInstance; // To access dialogs
    private InventoryService inventoryService;
    private SalesService salesService;
    
    // Dashboard components that need updating
    private VBox salesStatsContent;
    private VBox inventoryAlertsContent;
    private TitledPane appointmentsPane;
    
    /**
     * Initialize the main view
     * @param primaryStage JavaFX primary stage
     */
    public void initialize(Stage primaryStage) {
        this.stage = primaryStage;
        
        // Initialize controllers
        initializeControllers();
        
        BorderPane root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);
        
        // Create status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        // Create tab pane for main content
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabPane);
        
        // Add default tabs
        createDashboardTab();
        createInventoryTab();
        createSalesTab();
        createCustomersTab();
        createServicesTab();
        createAppointmentsTab();
        createAdminSettingsTab();
        createReportsTab();
        
        // Add listener to refresh dashboard when its tab is selected
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            // Dashboard is at index 0
            if (newVal.intValue() == 0) {
                refreshDashboard();
            }
        });
        
        Scene scene = new Scene(root, 1200, 800);
        
        // Load modern CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/styles/modern.css").toExternalForm());
        
        primaryStage.setTitle("Tire Shop POS");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }
    
    /**
     * Initialize controllers
     */
    private void initializeControllers() {
        this.mainViewInstance = this;
        SettingsService settingsService = new SettingsService();

        // Create DAOs
        ProductDao productDao = new ProductDao(DatabaseManager.getSessionFactory());
        CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
        SaleDao saleDao = new SaleDao(DatabaseManager.getSessionFactory());
        SaleItemDao saleItemDao = new SaleItemDao(DatabaseManager.getSessionFactory());
        VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
        ServiceDao serviceDao = new ServiceDao(DatabaseManager.getSessionFactory());
        TechnicianDao technicianDao = new TechnicianDao(DatabaseManager.getSessionFactory());
        AppointmentDao appointmentDao = new AppointmentDao(DatabaseManager.getSessionFactory());
        
        // Create services
        this.inventoryService = new InventoryService(productDao, settingsService);
        this.salesService = new SalesService(
                saleDao, saleItemDao, customerDao, vehicleDao, serviceDao, technicianDao, this.inventoryService, settingsService);
        
        // Create controllers
        inventoryController = new InventoryController(this.inventoryService);
        inventoryController.setMainView(this); // Set MainView reference for dashboard refreshing
        
        salesController = new SalesController(this.salesService, this.inventoryService, 
                this.mainViewInstance, settingsService);
        appointmentController = new AppointmentController(appointmentDao, customerDao, vehicleDao, technicianDao);
        serviceController = new ServiceController(serviceDao, technicianDao);
        adminController = new AdminController();
        reportController = new ReportController(this.salesService, this.inventoryService);
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem settingsItem = new MenuItem("Settings");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(settingsItem, new SeparatorMenuItem(), exitItem);
        
        // Reports menu
        Menu reportsMenu = new Menu("Reports");
        MenuItem salesReportItem = new MenuItem("Sales Report");
        MenuItem inventoryReportItem = new MenuItem("Inventory Report");
        MenuItem technicianReportItem = new MenuItem("Technician Performance");
        reportsMenu.getItems().addAll(salesReportItem, inventoryReportItem, technicianReportItem);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        MenuItem helpItem = new MenuItem("Help Contents");
        helpMenu.getItems().addAll(helpItem, aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, reportsMenu, helpMenu);
        return menuBar;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setStyle("-fx-background-color: #f1f1f1; -fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;");
        
        // Status indicator
        HBox statusBox = new HBox(5);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Green indicator for "Ready" status
        javafx.scene.shape.Circle statusIndicator = new javafx.scene.shape.Circle(5);
        statusIndicator.setFill(javafx.scene.paint.Color.rgb(76, 175, 80));
        statusIndicator.setStroke(javafx.scene.paint.Color.rgb(46, 125, 50));
        statusIndicator.setStrokeWidth(1);
        
        Label statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("success");
        
        statusBox.getChildren().addAll(statusIndicator, statusLabel);
        
        // Database connection indicator with icon
        HBox dbBox = new HBox(5);
        dbBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        javafx.scene.shape.Circle dbIndicator = new javafx.scene.shape.Circle(5);
        dbIndicator.setFill(javafx.scene.paint.Color.rgb(76, 175, 80));
        dbIndicator.setStroke(javafx.scene.paint.Color.rgb(46, 125, 50));
        dbIndicator.setStrokeWidth(1);
        
        Label dbLabel = new Label("Database: Connected");
        
        dbBox.getChildren().addAll(dbIndicator, dbLabel);
        
        // User info with icon
        HBox userBox = new HBox(5);
        userBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label userLabel = new Label("User: Admin");
        
        userBox.getChildren().addAll(userLabel);
        
        // Current date/time with auto-update
        Label dateTimeLabel = new Label();
        dateTimeLabel.getStyleClass().add("info");
        
        // Update the date/time every second
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                e -> {
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy - hh:mm:ss a");
                    dateTimeLabel.setText(now.format(formatter));
                }
            )
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
        
        // Using a spring to push the date/time to the far right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        statusBar.getChildren().addAll(statusBox, dbBox, userBox, spacer, dateTimeLabel);
        return statusBar;
    }
    
    private void createDashboardTab() {
        Tab tab = new Tab("Dashboard");
        
        // Main container
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(15));
        
        // Dashboard header
        HBox headerBox = new HBox();
        headerBox.setPadding(new Insets(0, 0, 15, 0));
        Label titleLabel = new Label("Tire Shop POS Dashboard");
        titleLabel.getStyleClass().add("section-title");
        headerBox.getChildren().add(titleLabel);
        mainContainer.setTop(headerBox);
        
        // Quick access buttons in a horizontal row
        HBox quickAccessBox = new HBox(10);
        quickAccessBox.getStyleClass().addAll("card", "spacing-10");
        quickAccessBox.setPadding(new Insets(10));
        quickAccessBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button newSaleBtn = new Button("New Sale");
        Button newCustomerBtn = new Button("New Customer");
        Button inventoryBtn = new Button("Inventory");
        Button servicesBtn = new Button("Services");
        Button appointmentsBtn = new Button("Appointments");
        
        // Apply styles to buttons
        newSaleBtn.getStyleClass().add("action-button");
        newCustomerBtn.getStyleClass().add("action-button");
        inventoryBtn.getStyleClass().add("action-button");
        servicesBtn.getStyleClass().add("action-button");
        appointmentsBtn.getStyleClass().add("action-button");
        
        // Button actions
        newSaleBtn.setOnAction(e -> tabPane.getSelectionModel().select(2)); // Switch to Sales tab
        newCustomerBtn.setOnAction(e -> tabPane.getSelectionModel().select(3)); // Switch to Customers tab
        inventoryBtn.setOnAction(e -> tabPane.getSelectionModel().select(1)); // Switch to Inventory tab
        servicesBtn.setOnAction(e -> tabPane.getSelectionModel().select(4)); // Switch to Services tab
        appointmentsBtn.setOnAction(e -> tabPane.getSelectionModel().select(5)); // Switch to Appointments tab
        
        quickAccessBox.getChildren().addAll(newSaleBtn, newCustomerBtn, inventoryBtn, servicesBtn, appointmentsBtn);
        
        // Main content area - using GridPane for better layout control
        GridPane dashboard = new GridPane();
        dashboard.setHgap(15);
        dashboard.setVgap(15);
        dashboard.setPadding(new Insets(15, 0, 0, 0));
        
        // Create and configure dashboard components
        appointmentsPane = createTodayAppointmentsPane();
        appointmentsPane.getStyleClass().add("dashboard-tile");
        appointmentsPane.setPrefHeight(350); // Fixed height for appointments
        
        // Today's Sales statistics in a VBox
        salesStatsContent = new VBox(5);
        salesStatsContent.getStyleClass().add("spacing-10");
        updateSalesStats();
        TitledPane salesStats = new TitledPane("Today's Sales", salesStatsContent);
        salesStats.setCollapsible(false);
        salesStats.getStyleClass().add("dashboard-tile");
        
        // Inventory Alerts
        inventoryAlertsContent = new VBox(5);
        inventoryAlertsContent.getStyleClass().add("spacing-10");
        updateInventoryAlerts();
        TitledPane inventoryStats = new TitledPane("Inventory Alerts (Low Stock)", inventoryAlertsContent);
        inventoryStats.setCollapsible(false);
        inventoryStats.getStyleClass().add("dashboard-tile");
        
        // Container for sales stats and inventory alerts
        VBox statsContainer = new VBox(15);
        statsContainer.getChildren().addAll(salesStats, inventoryStats);
        
        // Add components to the grid - 2 columns side by side
        dashboard.add(appointmentsPane, 0, 0); // Left side
        dashboard.add(statsContainer, 1, 0);    // Right side
        
        // Set column constraints to make them equal width
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(50);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(50);
        dashboard.getColumnConstraints().addAll(column1, column2);
        
        // Layout structure
        VBox contentBox = new VBox(15);
        contentBox.getChildren().addAll(quickAccessBox, dashboard);
        mainContainer.setCenter(contentBox);
        
        tab.setContent(mainContainer);
        tabPane.getTabs().add(tab);
    }
    
    /**
     * Update sales statistics on the dashboard
     */
    private void updateSalesStats() {
        System.out.println("[MainView Dashboard] Fetching today's sales summary...");
        SalesSummaryData todaySummary = this.salesService.getSalesSummaryForDate(LocalDate.now());
        System.out.println("[MainView Dashboard] Today's Sales: Count=" + todaySummary.getNumberOfSales() + ", Revenue=" + todaySummary.getTotalRevenue());
        
        salesStatsContent.getChildren().clear();
        
        // Header with view reports button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label todayLabel = new Label("Today's Performance");
        todayLabel.getStyleClass().add("dashboard-tile-title");
        
        Button reportsBtn = new Button("View Reports");
        reportsBtn.getStyleClass().addAll("secondary", "action-button");
        reportsBtn.setOnAction(e -> tabPane.getSelectionModel().select(7)); // Go to reports tab
        
        // Push button to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        headerBox.getChildren().addAll(todayLabel, spacer, reportsBtn);
        
        // Create a visual layout for the metrics with icons
        HBox metricsBox = new HBox(20);
        metricsBox.setPadding(new Insets(10, 0, 10, 0));
        metricsBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Sales count metric card
        VBox salesCountBox = new VBox(5);
        salesCountBox.setAlignment(javafx.geometry.Pos.CENTER);
        salesCountBox.getStyleClass().add("metric-card");
        salesCountBox.setPadding(new Insets(10));
        
        Label salesCountTitle = new Label("Sales");
        salesCountTitle.getStyleClass().add("dashboard-tile-title");
        
        Label salesCountValue = new Label(String.valueOf(todaySummary.getNumberOfSales()));
        salesCountValue.getStyleClass().add("dashboard-tile-value");
        
        salesCountBox.getChildren().addAll(salesCountTitle, salesCountValue);
        
        // Revenue metric card
        VBox revenueBox = new VBox(5);
        revenueBox.setAlignment(javafx.geometry.Pos.CENTER);
        revenueBox.getStyleClass().add("metric-card");
        revenueBox.setPadding(new Insets(10));
        
        Label revenueTitle = new Label("Revenue");
        revenueTitle.getStyleClass().add("dashboard-tile-title");
        
        Label revenueValue = new Label("$" + String.format("%.2f", todaySummary.getTotalRevenue()));
        revenueValue.getStyleClass().add("dashboard-tile-value");
        
        revenueBox.getChildren().addAll(revenueTitle, revenueValue);
        
        // Add both metrics to the container
        metricsBox.getChildren().addAll(salesCountBox, revenueBox);
        
        // Add quick action buttons for sales
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button newSaleBtn = new Button("New Sale");
        newSaleBtn.setOnAction(e -> tabPane.getSelectionModel().select(2)); // Go to sales tab
        
        Button salesHistoryBtn = new Button("Sales History");
        salesHistoryBtn.getStyleClass().add("secondary");
        salesHistoryBtn.setOnAction(e -> tabPane.getSelectionModel().select(2)); // Go to sales tab
        
        actionBox.getChildren().addAll(newSaleBtn, salesHistoryBtn);
        
        // Add all components
        salesStatsContent.getChildren().addAll(headerBox, metricsBox, actionBox);
    }
    
    /**
     * Update inventory alerts on the dashboard
     */
    private void updateInventoryAlerts() {
        System.out.println("[MainView Dashboard] Fetching low stock products...");
        List<Product> lowStockProducts = this.inventoryService.getLowStockProducts();
        System.out.println("[MainView Dashboard] Low stock products found: " + lowStockProducts.size());
        
        inventoryAlertsContent.getChildren().clear();
        
        if (lowStockProducts.isEmpty()) {
            Label noAlertsLabel = new Label("No low stock items currently.");
            noAlertsLabel.getStyleClass().add("info");
            inventoryAlertsContent.getChildren().add(noAlertsLabel);
        } else {
            // Header with count and action button in the same row
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label countLabel = new Label(lowStockProducts.size() + " products need attention");
            countLabel.getStyleClass().addAll("dashboard-tile-title", "warning");
            
            Button orderButton = new Button("Order Products");
            orderButton.getStyleClass().addAll("secondary", "action-button");
            orderButton.setOnAction(e -> tabPane.getSelectionModel().select(1)); // Go to inventory tab
            
            // Push button to the right
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            headerBox.getChildren().addAll(countLabel, spacer, orderButton);
            
            // Create a more compact list view with fixed height
            ListView<String> lowStockListView = new ListView<>();
            lowStockListView.getStyleClass().add("inventory-list");
            lowStockListView.setPrefHeight(120); // Fixed smaller height
            
            ObservableList<String> lowStockItems = FXCollections.observableArrayList();
            for (Product p : lowStockProducts) {
                lowStockItems.add(p.getName() + " (Stock: " + p.getQuantityInStock() + 
                        ", Reorder: " + p.getReorderLevel() + ")");
            }
            lowStockListView.setItems(lowStockItems);
            
            inventoryAlertsContent.getChildren().addAll(headerBox, lowStockListView);
        }
    }
    
    /**
     * Refresh the dashboard with current data
     */
    public void refreshDashboard() {
        System.out.println("[MainView] Refreshing dashboard...");
        
        // Refresh sales statistics
        updateSalesStats();
        
        // Refresh inventory alerts
        updateInventoryAlerts();
        
        // Refresh today's appointments
        VBox appointmentsContent = (VBox) appointmentsPane.getContent();
        if (appointmentsContent != null && !appointmentsContent.getChildren().isEmpty()) {
            TableView<Appointment> appointmentsTable = null;
            
            // Find the TableView in the children
            for (javafx.scene.Node node : appointmentsContent.getChildren()) {
                if (node instanceof TableView) {
                    appointmentsTable = (TableView<Appointment>) node;
                    break;
                }
            }
            
            if (appointmentsTable != null) {
                // Get today's appointments from the database
                AppointmentDao appointmentDao = new AppointmentDao(DatabaseManager.getSessionFactory());
                List<Appointment> todaysAppointments = appointmentDao.findByDate(LocalDate.now());
                System.out.println("[MainView] Refreshed dashboard appointments: " + todaysAppointments.size());
                appointmentsTable.setItems(FXCollections.observableArrayList(todaysAppointments));
            }
        }
    }
    
    /**
     * Create a pane to display today's appointments
     * @return TitledPane with today's appointments
     */
    private TitledPane createTodayAppointmentsPane() {
        // Create a VBox to hold the appointments
        VBox appointmentsBox = new VBox(10);
        appointmentsBox.setPadding(new Insets(10));
        appointmentsBox.getStyleClass().add("spacing-10");
        
        // Header with count
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Get today's appointments from the controller
        AppointmentDao appointmentDao = new AppointmentDao(DatabaseManager.getSessionFactory());
        List<Appointment> todaysAppointments = appointmentDao.findByDate(LocalDate.now());
        
        System.out.println("Found " + todaysAppointments.size() + " appointments for today's dashboard");
        
        // Add a count badge
        Label countLabel = new Label(todaysAppointments.size() + " Appointments Today");
        countLabel.getStyleClass().add("dashboard-tile-title");
        headerBox.getChildren().add(countLabel);
        
        // Create a table to display appointments with modern styling
        TableView<Appointment> appointmentsTable = new TableView<>();
        
        // Create columns
        TableColumn<Appointment, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            return new SimpleStringProperty(data.getValue().getStartTime().format(formatter));
        });
        
        TableColumn<Appointment, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        
        TableColumn<Appointment, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(data -> {
            Customer customer = data.getValue().getCustomer();
            if (customer != null) {
                return new SimpleStringProperty(customer.getFirstName() + " " + customer.getLastName());
            } else {
                return new SimpleStringProperty("N/A");
            }
        });
        
        TableColumn<Appointment, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        
        // Set column widths
        timeColumn.setPrefWidth(80);
        titleColumn.setPrefWidth(150);
        customerColumn.setPrefWidth(150);
        statusColumn.setPrefWidth(100);
        
        // Add columns to table
        appointmentsTable.getColumns().addAll(timeColumn, titleColumn, customerColumn, statusColumn);
        
        // Add data to table
        appointmentsTable.setItems(FXCollections.observableArrayList(todaysAppointments));
        
        // Set fixed height for table
        appointmentsTable.setPrefHeight(200);
        
        // Add buttons with modern styling
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button viewAllBtn = new Button("View All");
        viewAllBtn.getStyleClass().add("secondary");
        viewAllBtn.setOnAction(e -> tabPane.getSelectionModel().select(5)); // Switch to Appointments tab
        
        Button newAppointmentBtn = new Button("New Appointment");
        newAppointmentBtn.setOnAction(e -> {
            tabPane.getSelectionModel().select(5); // Switch to Appointments tab
            // TODO: Trigger the new appointment dialog from the appointments tab
        });
        
        buttonBox.getChildren().addAll(viewAllBtn, newAppointmentBtn);
        
        // Show a message when no appointments
        if (todaysAppointments.isEmpty()) {
            VBox emptyStateBox = new VBox(10);
            emptyStateBox.setAlignment(javafx.geometry.Pos.CENTER);
            emptyStateBox.setPadding(new Insets(40, 0, 40, 0));
            
            Label emptyLabel = new Label("No appointments scheduled for today");
            emptyLabel.getStyleClass().add("info");
            emptyStateBox.getChildren().addAll(emptyLabel, newAppointmentBtn);
            
            // Add components to container
            appointmentsBox.getChildren().addAll(headerBox, emptyStateBox);
        } else {
            // Add components to container
            appointmentsBox.getChildren().addAll(headerBox, appointmentsTable, buttonBox);
        }
        
        // Create and return the titled pane
        TitledPane appointmentsPane = new TitledPane("Today's Appointments", appointmentsBox);
        appointmentsPane.setCollapsible(true);
        appointmentsPane.setExpanded(true);
        
        return appointmentsPane;
    }
    
    /**
     * Helper class to hold appointment data for the table
     */
    private static class AppointmentData {
        private final SimpleStringProperty time;
        private final SimpleStringProperty title;
        private final SimpleStringProperty customer;
        private final SimpleStringProperty status;
        
        public AppointmentData(String time, String title, String customer, String status) {
            this.time = new SimpleStringProperty(time);
            this.title = new SimpleStringProperty(title);
            this.customer = new SimpleStringProperty(customer);
            this.status = new SimpleStringProperty(status);
        }
        
        public SimpleStringProperty timeProperty() { return time; }
        public SimpleStringProperty titleProperty() { return title; }
        public SimpleStringProperty customerProperty() { return customer; }
        public SimpleStringProperty statusProperty() { return status; }
    }
    
    private void createInventoryTab() {
        Tab tab = new Tab("Inventory");
        
        BorderPane content = new BorderPane();
        
        // Top controls
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        Button addBtn = new Button("Add Product");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button refreshBtn = new Button("Refresh");
        Button scanBtn = new Button("Mobile Scanner");
        scanBtn.getStyleClass().add("primary");
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        Button searchBtn = new Button("Search");
        
        // Add scanner button action
        scanBtn.setOnAction(e -> {
            try {
                // Start scanner server if not already running
                if (inventoryController.getScannerServer() == null) {
                    inventoryController.startScannerServer();
                    showAlert("Mobile Scanner", 
                            "Mobile scanner server started.\n\nAccess from your phone at:\nhttp://" + 
                            java.net.InetAddress.getLocalHost().getHostAddress() + ":8080", 
                            Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Mobile Scanner", 
                            "Mobile scanner is already running.\n\nAccess from your phone at:\nhttp://" + 
                            java.net.InetAddress.getLocalHost().getHostAddress() + ":8080", 
                            Alert.AlertType.INFORMATION);
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to start mobile scanner: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        
        controls.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn, scanBtn, searchField, searchBtn);
        content.setTop(controls);
        
        // Table
        TableView<Product> table = new TableView<>();
        
        // Initialize the inventory controller with the table
        inventoryController.initialize(table);
        
        // Connect buttons to controller actions
        addBtn.setOnAction(e -> inventoryController.showAddProductDialog(stage));
        refreshBtn.setOnAction(e -> inventoryController.refreshProducts());
        searchBtn.setOnAction(e -> inventoryController.searchProducts(searchField.getText()));
        
        editBtn.setOnAction(e -> {
            Product selectedProduct = inventoryController.getSelectedProduct();
            if (selectedProduct != null) {
                inventoryController.showAdjustPriceDialog(selectedProduct, stage);
            } else {
                showAlert("No Selection", "Please select a product to edit.", Alert.AlertType.WARNING);
            }
        });
        
        deleteBtn.setOnAction(e -> {
            Product selectedProduct = inventoryController.getSelectedProduct();
            if (selectedProduct != null) {
                inventoryController.showDeleteConfirmation(selectedProduct, stage);
            } else {
                showAlert("No Selection", "Please select a product to delete.", Alert.AlertType.WARNING);
            }
        });
        
        content.setCenter(table);
        
        tab.setContent(content);
        tabPane.getTabs().add(tab);
    }
    
    private void createSalesTab() {
        Tab tab = new Tab("Sales");
        
        BorderPane content = new BorderPane();
        
        // Initialize the sales controller with the content pane
        salesController.initialize(content, stage);
        
        tab.setContent(content);
        tabPane.getTabs().add(tab);
    }
    
    private void createCustomersTab() {
        Tab tab = new Tab("Customers");
        
        BorderPane content = new BorderPane();
        
        // Top controls
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        Button addBtn = new Button("Add Customer");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button vehiclesBtn = new Button("Manage Vehicles");
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        Button searchBtn = new Button("Search");
        
        controls.getChildren().addAll(addBtn, editBtn, deleteBtn, vehiclesBtn, searchField, searchBtn);
        content.setTop(controls);
        
        // Create a split pane for customers and vehicles
        SplitPane splitPane = new SplitPane();
        
        // Customer table
        TableView<Customer> customerTable = new TableView<>();
        
        // Customer columns
        TableColumn<Customer, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        
        TableColumn<Customer, String> firstNameColumn = new TableColumn<>("First Name");
        firstNameColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getFirstName()));
        
        TableColumn<Customer, String> lastNameColumn = new TableColumn<>("Last Name");
        lastNameColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getLastName()));
        
        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getPhone()));
        
        TableColumn<Customer, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getEmail()));
        
        customerTable.getColumns().addAll(idColumn, firstNameColumn, lastNameColumn, 
                phoneColumn, emailColumn);
        
        // Vehicle table
        TableView<Vehicle> vehicleTable = new TableView<>();
        
        // Vehicle columns
        TableColumn<Vehicle, String> makeColumn = new TableColumn<>("Make");
        makeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getMake()));
        
        TableColumn<Vehicle, String> modelColumn = new TableColumn<>("Model");
        modelColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getModel()));
        
        TableColumn<Vehicle, Number> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getModelYear()));
        
        TableColumn<Vehicle, String> licensePlateColumn = new TableColumn<>("License Plate");
        licensePlateColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getLicensePlate()));
        
        TableColumn<Vehicle, String> vinColumn = new TableColumn<>("VIN");
        vinColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getVin()));
        
        vehicleTable.getColumns().addAll(makeColumn, modelColumn, yearColumn, 
                licensePlateColumn, vinColumn);
        
        // Create containers for each table with labels
        VBox customerBox = new VBox(5);
        Label customerLabel = new Label("Customers");
        customerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        customerBox.getChildren().addAll(customerLabel, customerTable);
        VBox.setVgrow(customerTable, javafx.scene.layout.Priority.ALWAYS);
        
        VBox vehicleBox = new VBox(5);
        HBox vehicleHeader = new HBox(10);
        Label vehicleLabel = new Label("Vehicles");
        vehicleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Button addVehicleBtn = new Button("Add Vehicle");
        Button editVehicleBtn = new Button("Edit Vehicle");
        Button deleteVehicleBtn = new Button("Delete Vehicle");
        vehicleHeader.getChildren().addAll(vehicleLabel, addVehicleBtn, editVehicleBtn, deleteVehicleBtn);
        vehicleBox.getChildren().addAll(vehicleHeader, vehicleTable);
        VBox.setVgrow(vehicleTable, javafx.scene.layout.Priority.ALWAYS);
        
        // Load customer data
        CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
        List<Customer> customers = customerDao.findAll();
        customerTable.setItems(FXCollections.observableArrayList(customers));
        
        // When a customer is selected, load their vehicles
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                List<Vehicle> vehicles = vehicleDao.findByCustomerId(newSelection.getId());
                vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
                
                // Enable vehicle buttons
                addVehicleBtn.setDisable(false);
                editVehicleBtn.setDisable(false);
                deleteVehicleBtn.setDisable(false);
            } else {
                vehicleTable.getItems().clear();
                
                // Disable vehicle buttons
                addVehicleBtn.setDisable(true);
                editVehicleBtn.setDisable(true);
                deleteVehicleBtn.setDisable(true);
            }
        });
        
        // Disable vehicle buttons initially
        addVehicleBtn.setDisable(true);
        editVehicleBtn.setDisable(true);
        deleteVehicleBtn.setDisable(true);
        
        // Add vehicle button action
        addVehicleBtn.setOnAction(e -> {
            Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                showAddVehicleDialog(selectedCustomer);
                
                // Refresh vehicles
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                List<Vehicle> vehicles = vehicleDao.findByCustomerId(selectedCustomer.getId());
                vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
            }
        });
        
        // Edit vehicle button action
        editVehicleBtn.setOnAction(e -> {
            Vehicle selectedVehicle = vehicleTable.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                showEditVehicleDialog(selectedVehicle);
                
                // Refresh vehicles
                Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                List<Vehicle> vehicles = vehicleDao.findByCustomerId(selectedCustomer.getId());
                vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
            } else {
                showAlert("No Selection", "Please select a vehicle to edit.", Alert.AlertType.WARNING);
            }
        });
        
        // Delete vehicle button action
        deleteVehicleBtn.setOnAction(e -> {
            Vehicle selectedVehicle = vehicleTable.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                if (showConfirmationDialog("Delete Vehicle", 
                        "Are you sure you want to delete this vehicle?")) {
                    // Delete the vehicle
                    VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                    vehicleDao.delete(selectedVehicle);
                    
                    // Refresh vehicles
                    Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
                    List<Vehicle> vehicles = vehicleDao.findByCustomerId(selectedCustomer.getId());
                    vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
                }
            } else {
                showAlert("No Selection", "Please select a vehicle to delete.", Alert.AlertType.WARNING);
            }
        });
        
        // Add customer button action
        addBtn.setOnAction(e -> {
            showAddCustomerDialog();
            
            // Refresh customers
            List<Customer> updatedCustomers = customerDao.findAll();
            customerTable.setItems(FXCollections.observableArrayList(updatedCustomers));
        });
        
        // Edit customer button action
        editBtn.setOnAction(e -> {
            Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                showEditCustomerDialog(selectedCustomer);
                
                // Refresh customers
                List<Customer> updatedCustomers = customerDao.findAll();
                customerTable.setItems(FXCollections.observableArrayList(updatedCustomers));
            } else {
                showAlert("No Selection", "Please select a customer to edit.", Alert.AlertType.WARNING);
            }
        });
        
        // Delete customer button action
        deleteBtn.setOnAction(e -> {
            Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                if (showConfirmationDialog("Delete Customer", 
                        "Are you sure you want to delete this customer? All associated vehicles will also be deleted.")) {
                    // Delete the customer
                    customerDao.delete(selectedCustomer);
                    
                    // Refresh customers
                    List<Customer> updatedCustomers = customerDao.findAll();
                    customerTable.setItems(FXCollections.observableArrayList(updatedCustomers));
                    
                    // Clear vehicles
                    vehicleTable.getItems().clear();
                }
            } else {
                showAlert("No Selection", "Please select a customer to delete.", Alert.AlertType.WARNING);
            }
        });
        
        // Search button action
        searchBtn.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                List<Customer> searchResults = customerDao.search(searchTerm);
                customerTable.setItems(FXCollections.observableArrayList(searchResults));
            } else {
                // If search is empty, show all customers
                List<Customer> allCustomers = customerDao.findAll();
                customerTable.setItems(FXCollections.observableArrayList(allCustomers));
            }
        });
        
        // Add tables to split pane
        splitPane.getItems().addAll(customerBox, vehicleBox);
        splitPane.setDividerPositions(0.5);
        
        content.setCenter(splitPane);
        
        tab.setContent(content);
        tabPane.getTabs().add(tab);
    }
    
    /**
     * Show dialog to add a new customer
     */
    public void showAddCustomerDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add Customer");
        dialog.setHeaderText("Create a new customer");
        dialog.initOwner(stage);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Fields
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        
        // Add fields to grid
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);
        
        grid.add(new Label("Address:"), 0, 4);
        grid.add(addressField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on first field
        Platform.runLater(firstNameField::requestFocus);
        
        // Add validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() 
                    || phoneField.getText().isEmpty()) {
                showAlert("Missing Information", 
                        "Please enter first name, last name, and phone number.", 
                        Alert.AlertType.ERROR);
                event.consume();
            }
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Customer customer = new Customer();
                customer.setFirstName(firstNameField.getText());
                customer.setLastName(lastNameField.getText());
                customer.setPhone(phoneField.getText());
                customer.setEmail(emailField.getText());
                customer.setAddress(addressField.getText());
                
                // Save customer to database
                CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
                return customerDao.save(customer);
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show dialog to edit an existing customer
     * @param customer The customer to edit
     */
    public void showEditCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");
        dialog.setHeaderText("Edit customer information");
        dialog.initOwner(stage);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Fields
        TextField firstNameField = new TextField(customer.getFirstName());
        TextField lastNameField = new TextField(customer.getLastName());
        TextField phoneField = new TextField(customer.getPhone());
        TextField emailField = new TextField(customer.getEmail());
        TextField addressField = new TextField(customer.getAddress());
        
        // Add fields to grid
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);
        
        grid.add(new Label("Address:"), 0, 4);
        grid.add(addressField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() 
                    || phoneField.getText().isEmpty()) {
                showAlert("Missing Information", 
                        "Please enter first name, last name, and phone number.", 
                        Alert.AlertType.ERROR);
                event.consume();
            }
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                customer.setFirstName(firstNameField.getText());
                customer.setLastName(lastNameField.getText());
                customer.setPhone(phoneField.getText());
                customer.setEmail(emailField.getText());
                customer.setAddress(addressField.getText());
                
                // Update customer in database
                CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
                return customerDao.update(customer);
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show dialog to add a new vehicle
     * @param customer The customer to add a vehicle for
     */
    public void showAddVehicleDialog(Customer customer) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Add Vehicle");
        dialog.setHeaderText("Add a new vehicle for " + customer.getFullName());
        dialog.initOwner(stage);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Fields
        TextField makeField = new TextField();
        makeField.setPromptText("Make");
        
        TextField modelField = new TextField();
        modelField.setPromptText("Model");
        
        TextField yearField = new TextField();
        yearField.setPromptText("Year");
        
        TextField licensePlateField = new TextField();
        licensePlateField.setPromptText("License Plate");
        
        TextField vinField = new TextField();
        vinField.setPromptText("VIN");
        
        TextField colorField = new TextField();
        colorField.setPromptText("Color");
        
        // Add fields to grid
        grid.add(new Label("Make:"), 0, 0);
        grid.add(makeField, 1, 0);
        
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        
        grid.add(new Label("Year:"), 0, 2);
        grid.add(yearField, 1, 2);
        
        grid.add(new Label("License Plate:"), 0, 3);
        grid.add(licensePlateField, 1, 3);
        
        grid.add(new Label("VIN:"), 0, 4);
        grid.add(vinField, 1, 4);
        
        grid.add(new Label("Color:"), 0, 5);
        grid.add(colorField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on first field
        Platform.runLater(makeField::requestFocus);
        
        // Add validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (makeField.getText().isEmpty() || modelField.getText().isEmpty() 
                    || yearField.getText().isEmpty()) {
                showAlert("Missing Information", 
                        "Please enter make, model, and year.", 
                        Alert.AlertType.ERROR);
                event.consume();
            } else {
                try {
                    Integer.parseInt(yearField.getText());
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Year", 
                            "Year must be a valid number.", 
                            Alert.AlertType.ERROR);
                    event.consume();
                }
            }
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Vehicle vehicle = new Vehicle();
                vehicle.setMake(makeField.getText());
                vehicle.setModel(modelField.getText());
                vehicle.setModelYear(Integer.parseInt(yearField.getText()));
                vehicle.setLicensePlate(licensePlateField.getText());
                vehicle.setVin(vinField.getText());
                vehicle.setColor(colorField.getText());
                vehicle.setCustomer(customer);
                
                // Save vehicle to database
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                return vehicleDao.save(vehicle);
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show dialog to edit an existing vehicle
     * @param vehicle The vehicle to edit
     */
    public void showEditVehicleDialog(Vehicle vehicle) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Edit Vehicle");
        dialog.setHeaderText("Edit vehicle information");
        dialog.initOwner(stage);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Fields
        TextField makeField = new TextField(vehicle.getMake());
        TextField modelField = new TextField(vehicle.getModel());
        TextField yearField = new TextField(String.valueOf(vehicle.getModelYear()));
        TextField licensePlateField = new TextField(vehicle.getLicensePlate());
        TextField vinField = new TextField(vehicle.getVin());
        TextField colorField = new TextField(vehicle.getColor());
        
        // Add fields to grid
        grid.add(new Label("Make:"), 0, 0);
        grid.add(makeField, 1, 0);
        
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        
        grid.add(new Label("Year:"), 0, 2);
        grid.add(yearField, 1, 2);
        
        grid.add(new Label("License Plate:"), 0, 3);
        grid.add(licensePlateField, 1, 3);
        
        grid.add(new Label("VIN:"), 0, 4);
        grid.add(vinField, 1, 4);
        
        grid.add(new Label("Color:"), 0, 5);
        grid.add(colorField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (makeField.getText().isEmpty() || modelField.getText().isEmpty() 
                    || yearField.getText().isEmpty()) {
                showAlert("Missing Information", 
                        "Please enter make, model, and year.", 
                        Alert.AlertType.ERROR);
                event.consume();
            } else {
                try {
                    Integer.parseInt(yearField.getText());
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Year", 
                            "Year must be a valid number.", 
                            Alert.AlertType.ERROR);
                    event.consume();
                }
            }
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                vehicle.setMake(makeField.getText());
                vehicle.setModel(modelField.getText());
                vehicle.setModelYear(Integer.parseInt(yearField.getText()));
                vehicle.setLicensePlate(licensePlateField.getText());
                vehicle.setVin(vinField.getText());
                vehicle.setColor(colorField.getText());
                
                // Update vehicle in database
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                return vehicleDao.update(vehicle);
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show a confirmation dialog
     * @param title Dialog title
     * @param message Dialog message
     * @return true if confirmed
     */
    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private void createServicesTab() {
        Tab tab = new Tab("Services");

        // Create a BorderPane to be managed by ServiceController
        BorderPane serviceContentPane = new BorderPane(); 

        if (this.serviceController != null) {
            System.out.println("[MainView] Calling serviceController.initialize()...");
            this.serviceController.initialize(serviceContentPane, this.stage); // Initialize ServiceController
        } else {
            System.err.println("[MainView] ERROR: serviceController is null in createServicesTab!");
            serviceContentPane.setCenter(new Label("Error: ServiceController not initialized."));
        }

        tab.setContent(serviceContentPane);
        tabPane.getTabs().add(tab);
    }
    
    /**
     * Create the appointments tab
     */
    private void createAppointmentsTab() {
        Tab tab = new Tab("Appointments");

        // Create a BorderPane to be managed by AppointmentController
        BorderPane appointmentContentPane = new BorderPane();

        // Ensure appointmentController is not null (it's initialized in initializeControllers)
        if (this.appointmentController != null) {
            // Let the AppointmentController set up its own UI within this pane
            System.out.println("[MainView] Calling appointmentController.initialize()...");
            this.appointmentController.initialize(appointmentContentPane, this.stage);
        } else {
            System.err.println("[MainView] ERROR: appointmentController is null in createAppointmentsTab!");
            appointmentContentPane.setCenter(new Label("Error: AppointmentController not initialized."));
        }

        tab.setContent(appointmentContentPane); // Set the controller-managed pane as the tab's content
        tabPane.getTabs().add(tab);
    }
    
    /**
     * Show a dialog to create a new appointment
     * @param date Selected date for the appointment
     */
    private void showNewAppointmentDialog(LocalDate date) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Appointment");
        dialog.setHeaderText("Schedule a new appointment");
        dialog.initOwner(stage);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Fields
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(3);
        
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Select Customer (Optional)");
        
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPromptText("Select Vehicle (Optional)");
        vehicleComboBox.setDisable(true);
        
        ComboBox<Technician> technicianComboBox = new ComboBox<>();
        technicianComboBox.setPromptText("Select Technician (Optional)");
        
        DatePicker appointmentDate = new DatePicker(date);
        
        // Time picker controls
        ComboBox<LocalTime> startTimeComboBox = new ComboBox<>();
        ComboBox<LocalTime> endTimeComboBox = new ComboBox<>();
        
        // Populate time options (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue; // Skip after 6:00 PM
                startTimeComboBox.getItems().add(LocalTime.of(hour, minute));
                endTimeComboBox.getItems().add(LocalTime.of(hour, minute));
            }
        }
        
        startTimeComboBox.setValue(LocalTime.of(9, 0)); // Default to 9:00 AM
        endTimeComboBox.setValue(LocalTime.of(10, 0)); // Default to 10:00 AM
        
        // Format the time display
        StringConverter<LocalTime> timeConverter = new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime time) {
                if (time == null) return "";
                return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
            }
            
            @Override
            public LocalTime fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                return LocalTime.parse(string, DateTimeFormatter.ofPattern("hh:mm a"));
            }
        };
        
        startTimeComboBox.setConverter(timeConverter);
        endTimeComboBox.setConverter(timeConverter);
        
        // Status selector
        ComboBox<AppointmentStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(AppointmentStatus.values());
        statusComboBox.setValue(AppointmentStatus.SCHEDULED);
        
        // Load dummy data (in a real app, this would come from the database)
        Customer john = new Customer();
        john.setId(1L);
        john.setFirstName("John");
        john.setLastName("Doe");
        
        Customer jane = new Customer();
        jane.setId(2L);
        jane.setFirstName("Jane");
        jane.setLastName("Smith");
        
        customerComboBox.getItems().addAll(john, jane);
        
        // Customer converter
        customerComboBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                if (customer == null) return "";
                return customer.getFirstName() + " " + customer.getLastName();
            }
            
            @Override
            public Customer fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // When customer is selected, update vehicles
        customerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                vehicleComboBox.setDisable(false);
                
                // In a real app, we'd load this customer's vehicles
                Vehicle vehicle = new Vehicle();
                vehicle.setId(1L);
                vehicle.setMake("Honda");
                vehicle.setModel("Accord");
                vehicle.setModelYear(2018);
                vehicleComboBox.getItems().clear();
                vehicleComboBox.getItems().add(vehicle);
            } else {
                vehicleComboBox.setDisable(true);
                vehicleComboBox.getItems().clear();
            }
        });
        
        // Vehicle converter
        vehicleComboBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                if (vehicle == null) return "";
                return vehicle.getModelYear() + " " + vehicle.getMake() + " " + vehicle.getModel();
            }
            
            @Override
            public Vehicle fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // Load technicians (dummy data)
        Technician tech1 = new Technician();
        tech1.setId(1L);
        tech1.setFirstName("Mike");
        tech1.setLastName("Wilson");
        
        Technician tech2 = new Technician();
        tech2.setId(2L);
        tech2.setFirstName("Sarah");
        tech2.setLastName("Johnson");
        
        technicianComboBox.getItems().addAll(tech1, tech2);
        
        // Technician converter
        technicianComboBox.setConverter(new StringConverter<Technician>() {
            @Override
            public String toString(Technician tech) {
                if (tech == null) return "";
                return tech.getFirstName() + " " + tech.getLastName();
            }
            
            @Override
            public Technician fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // Status converter
        statusComboBox.setConverter(new StringConverter<AppointmentStatus>() {
            @Override
            public String toString(AppointmentStatus status) {
                if (status == null) return "";
                return status.getDisplayName();
            }
            
            @Override
            public AppointmentStatus fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // Add fields to grid
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        
        grid.add(new Label("Customer:"), 0, 2);
        grid.add(customerComboBox, 1, 2);
        
        grid.add(new Label("Vehicle:"), 0, 3);
        grid.add(vehicleComboBox, 1, 3);
        
        grid.add(new Label("Technician:"), 0, 4);
        grid.add(technicianComboBox, 1, 4);
        
        grid.add(new Label("Date:"), 0, 5);
        grid.add(appointmentDate, 1, 5);
        
        grid.add(new Label("Start Time:"), 0, 6);
        grid.add(startTimeComboBox, 1, 6);
        
        grid.add(new Label("End Time:"), 0, 7);
        grid.add(endTimeComboBox, 1, 7);
        
        grid.add(new Label("Status:"), 0, 8);
        grid.add(statusComboBox, 1, 8);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on title field by default
        Platform.runLater(titleField::requestFocus);
        
        // Add validation for form fields
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            // Validate required fields
            if (titleField.getText().isEmpty()) {
                showAlert("Missing Information", "Please enter a title for the appointment.", Alert.AlertType.ERROR);
                event.consume();
            } else if (startTimeComboBox.getValue() == null || endTimeComboBox.getValue() == null) {
                showAlert("Missing Information", "Please select start and end times.", Alert.AlertType.ERROR);
                event.consume();
            } else if (startTimeComboBox.getValue().isAfter(endTimeComboBox.getValue())) {
                showAlert("Invalid Times", "End time must be after start time.", Alert.AlertType.ERROR);
                event.consume();
            }
        });
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            // Create and save appointment
            Appointment appointment = new Appointment();
            appointment.setTitle(titleField.getText());
            appointment.setDescription(descriptionArea.getText());
            appointment.setCustomer(customerComboBox.getValue());
            appointment.setVehicle(vehicleComboBox.getValue());
            appointment.setTechnician(technicianComboBox.getValue());
            appointment.setStartTime(appointmentDate.getValue().atTime(startTimeComboBox.getValue()));
            appointment.setEndTime(appointmentDate.getValue().atTime(endTimeComboBox.getValue()));
            appointment.setStatus(statusComboBox.getValue());
            
            // In a real application, we would save this appointment to the database
            
            showAlert("Appointment Created", 
                    "New appointment created for " + appointment.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")), 
                    Alert.AlertType.INFORMATION);
            
            // Refresh the calendar view
            // In a real application, we would reload the appointments from the database
        }
    }
    
    /**
     * Create a day view for the calendar
     * @return ScrollPane with day view contents
     */
    private ScrollPane createDayView() {
        VBox dayView = new VBox(5);
        dayView.setPadding(new Insets(10));
        
        // Create time slots (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue; // Skip after 6:00 PM
                
                String time = String.format("%02d:%02d %s", 
                        hour > 12 ? hour - 12 : hour, 
                        minute, 
                        hour >= 12 ? "PM" : "AM");
                
                HBox timeSlot = new HBox(10);
                timeSlot.setPadding(new Insets(5));
                timeSlot.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 0 1 0;");
                
                Label timeLabel = new Label(time);
                timeLabel.setPrefWidth(80);
                
                // This would be filled with actual appointments
                Label appointmentLabel = new Label("No appointments");
                appointmentLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                
                timeSlot.getChildren().addAll(timeLabel, appointmentLabel);
                dayView.getChildren().add(timeSlot);
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(dayView);
        scrollPane.setFitToWidth(true);
        
        return scrollPane;
    }
    
    /**
     * Show an alert dialog
     * @param title Dialog title
     * @param message Dialog message
     * @param alertType Alert type
     */
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void createAdminSettingsTab() {
        Tab tab = new Tab("Admin Settings");
        BorderPane adminContentPane = new BorderPane();

        if (this.adminController != null) {
            System.out.println("[MainView] Calling adminController.initialize()...");
            this.adminController.initialize(adminContentPane, this.stage);
        } else {
            System.err.println("[MainView] ERROR: adminController is null in createAdminSettingsTab!");
            adminContentPane.setCenter(new Label("Error: AdminController not initialized."));
        }

        tab.setContent(adminContentPane);
        tabPane.getTabs().add(tab);
    }
    
    private void createReportsTab() {
        Tab tab = new Tab("Reports");
        BorderPane reportContentPane = new BorderPane();

        if (this.reportController != null) {
            System.out.println("[MainView] Calling reportController.initialize()...");
            this.reportController.initialize(reportContentPane, this.stage);
        } else {
            System.err.println("[MainView] ERROR: reportController is null in createReportsTab!");
            reportContentPane.setCenter(new Label("Error: ReportController not initialized."));
        }
        tab.setContent(reportContentPane);
        tabPane.getTabs().add(tab);
    }
} 