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
import com.tireshop.dao.ServiceRecordDao;
import com.tireshop.model.Customer;
import com.tireshop.model.Product;
import com.tireshop.model.Sale;
import com.tireshop.model.SaleItem;
import com.tireshop.model.Appointment;
import com.tireshop.model.Vehicle;
import com.tireshop.model.Technician;
import com.tireshop.model.AppointmentStatus;
import com.tireshop.model.ServiceRecord;
import com.tireshop.model.dto.SalesSummaryData;
import com.tireshop.service.InventoryService;
import com.tireshop.service.SalesService;
import com.tireshop.service.AppointmentService;
import com.tireshop.util.DatabaseManager;
import com.tireshop.util.SettingsService;
import com.tireshop.util.PrinterService;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.layout.ColumnConstraints;
import com.tireshop.service.UserService;
import com.tireshop.model.User;
import com.tireshop.model.TimeEntry;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Set;
import com.tireshop.view.ClockInOutCredentialDialog;
import com.tireshop.view.TimeClockDialog;
import com.tireshop.util.SettingsService.QuickLink;
import java.awt.Desktop;
import java.net.URI;
import java.io.IOException;
import java.net.URISyntaxException;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import com.tireshop.service.VehicleServiceHistoryService;
import com.tireshop.service.EmailService;
import com.tireshop.service.AutoRefreshService;
import com.tireshop.util.ResponsiveUIManager;
import com.tireshop.model.dto.ProductSalesReportItem;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;


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
    private UserService userService;
    private VehicleServiceHistoryService vehicleServiceHistoryService;
    private EmailService emailService;
    private PrinterService printerService;
    private com.tireshop.service.DailyReportService dailyReportService;
    private User currentUser;
    
    // Dashboard components that need updating
    private VBox inventoryAlertsContent;
    private VBox tiresSoldTodayContent;
    
    // Add new fields for tab management
    private Tab inventoryTab;
    private Tab salesTab;
    private Tab customersTab;
    private Tab servicesTab;
    private Tab appointmentsTab;
    private Tab adminTab;
    private Tab reportsTab;
    
    private MenuItem loginMenuItem;
    private MenuItem clockInOutMenuItem;
    private Button statusBarClockInOutButton; // New button for status bar
    
    private Label dateTimeLabel = new Label();
    private Label dbStatusLabel; // Database connection status
    private javafx.scene.shape.Circle dbIndicator; // Database connection indicator
    private Label connectionWarningLabel; // Warning banner for connection issues
    
    private FlowPane quickLinksFlowPane;
    
    // Customer table references for external refresh
    private TableView<Customer> customerTable;
    private TableView<Vehicle> vehicleTable;
    private TextField customerSearchField;
    
    /**
     * Initialize the main view
     * @param primaryStage JavaFX primary stage
     */
    public void initialize(Stage primaryStage) {
        this.stage = primaryStage;
        
        // Initialize controllers
        initializeControllers();
        
        BorderPane root = new BorderPane();
        
        // --- Create Top Bar Area (MenuBar + QuickLinks) ---
        MenuBar menuBar = createMenuBar();
        FlowPane topBarQuickLinks = createTopBarQuickLinks(); // New method to create HBox for quick links
        
        VBox topBarContainer = new VBox(); // VBox to hold MenuBar and QuickLinks HBox
        topBarContainer.getChildren().addAll(menuBar, topBarQuickLinks);
        root.setTop(topBarContainer);
        // --- End of Top Bar Area ---
        
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        root.setCenter(tabPane);
        
        createAllTabs();
        showOnlyDashboard();
        
        Scene scene = new Scene(root, 1200, 800);
        
        // Load modern CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/styles/modern.css").toExternalForm());
        
        // Set minimum window size for optimal display
        double[] minSize = ResponsiveUIManager.getMinimumWindowSize();
        primaryStage.setMinWidth(minSize[0]);
        primaryStage.setMinHeight(minSize[1]);
        
        primaryStage.setTitle("Tire Shop POS");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        
        // Initialize responsive UI behavior
        ResponsiveUIManager.initializeResponsiveUI(primaryStage);

        // Show debug info for scaling (can be removed in production)
        Platform.runLater(() -> {
            System.out.println("=== Dynamic Scaling Debug Info ===");
            System.out.println(ResponsiveUIManager.getSystemScalingInfo());
            System.out.println("===================================");
        });

        // Show login dialog on startup
        Platform.runLater(() -> {
            showLoginDialog();
            updateQuickLinksTopBar(); // Initial update after login possibility
        });
    }
    
    /**
     * Initialize controllers
     */
    private void initializeControllers() {
        this.mainViewInstance = this;
        SettingsService settingsService = SettingsService.getInstance();

        // Create DAOs
        ProductDao productDao = new ProductDao(DatabaseManager.getSessionFactory());
        CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
        SaleDao saleDao = new SaleDao(DatabaseManager.getSessionFactory());
        SaleItemDao saleItemDao = new SaleItemDao(DatabaseManager.getSessionFactory());
        com.tireshop.dao.SalePaymentDao salePaymentDao = new com.tireshop.dao.SalePaymentDao(DatabaseManager.getSessionFactory());
        com.tireshop.dao.ChargeAccountPaymentDao chargeAccountPaymentDao = new com.tireshop.dao.ChargeAccountPaymentDao(DatabaseManager.getSessionFactory());
        VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
        ServiceDao serviceDao = new ServiceDao(DatabaseManager.getSessionFactory());
        TechnicianDao technicianDao = new TechnicianDao(DatabaseManager.getSessionFactory());
        AppointmentDao appointmentDao = new AppointmentDao(DatabaseManager.getSessionFactory());
        ServiceRecordDao serviceRecordDao = new ServiceRecordDao(DatabaseManager.getSessionFactory());

        // Create services
        this.inventoryService = new InventoryService(productDao, settingsService);
        this.emailService = new EmailService(settingsService);
        this.vehicleServiceHistoryService = new VehicleServiceHistoryService(serviceRecordDao, this.emailService, settingsService);
        this.salesService = new SalesService(
                saleDao, saleItemDao, salePaymentDao, customerDao, vehicleDao, serviceDao, technicianDao, chargeAccountPaymentDao, this.inventoryService, settingsService, this.vehicleServiceHistoryService);

        // Daily owner email report (scheduled + manual from Admin settings)
        this.dailyReportService = new com.tireshop.service.DailyReportService(
                settingsService, saleDao, salePaymentDao, customerDao, this.emailService);
        this.dailyReportService.start();
        
        // Create controllers
        inventoryController = new InventoryController(this.inventoryService);
        inventoryController.setMainView(this); // Set MainView reference for dashboard refreshing
        
        salesController = new SalesController(this.salesService, this.inventoryService, 
                this.mainViewInstance, settingsService);
        appointmentController = new AppointmentController(appointmentDao, customerDao, vehicleDao, technicianDao);
        serviceController = new ServiceController(serviceDao, technicianDao);
        this.userService = new UserService();
        // Share this UserService with AdminController so role-permission edits
        // take effect immediately instead of requiring a restart
        adminController = new AdminController(this.userService);
        reportController = new ReportController(this.salesService, this.inventoryService);
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().add(exitItem);
        
        // Time Clock menu
        Menu timeClockMenu = new Menu("Time Clock");
        loginMenuItem = new MenuItem("Login");
        clockInOutMenuItem = new MenuItem("Clock In/Out");
        clockInOutMenuItem.setDisable(true); // Disabled until user logs in
        
        loginMenuItem.setOnAction(e -> {
            if (currentUser == null) {
                showLoginDialog();
            } else {
                logout();
            }
        });
        
        clockInOutMenuItem.setOnAction(e -> handleClockInOut()); // Always calls handleClockInOut
        
        timeClockMenu.getItems().addAll(loginMenuItem, clockInOutMenuItem);
        
        menuBar.getMenus().addAll(fileMenu, timeClockMenu);
        return menuBar;
    }
    
    // This method might be simplified or its purpose re-evaluated if button text is static.
    // For now, it primarily handles enabling/disabling based on main login.
    private void updateTimeClockMenu() { 
        if (currentUser != null) {
            if (clockInOutMenuItem != null) {
                clockInOutMenuItem.setDisable(false); 
            }
             if (statusBarClockInOutButton != null) {
                statusBarClockInOutButton.setDisable(false);
            }
        } else { 
            if (clockInOutMenuItem != null) {
                clockInOutMenuItem.setDisable(true);
            }
            if (statusBarClockInOutButton != null) {
                statusBarClockInOutButton.setDisable(true);
            }
        }
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setStyle("-fx-background-color: #f1f1f1; -fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;");
        
        // New Clock In/Out button for status bar
        statusBarClockInOutButton = new Button("Clock In/Out");
        statusBarClockInOutButton.setOnAction(e -> handleClockInOut());
        statusBarClockInOutButton.setDisable(true); // Initially disabled until a user logs into the main app
        
        HBox statusBox = new HBox(5);
        statusBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.shape.Circle statusIndicator = new javafx.scene.shape.Circle(5);
        statusIndicator.setFill(javafx.scene.paint.Color.rgb(76, 175, 80));
        statusIndicator.setStroke(javafx.scene.paint.Color.rgb(46, 125, 50));
        statusIndicator.setStrokeWidth(1);
        Label statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("success");
        statusBox.getChildren().addAll(statusIndicator, statusLabel);
        
        HBox dbBox = new HBox(5);
        dbBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dbIndicator = new javafx.scene.shape.Circle(5); // Store reference
        dbIndicator.setFill(javafx.scene.paint.Color.rgb(76, 175, 80));
        dbIndicator.setStroke(javafx.scene.paint.Color.rgb(46, 125, 50));
        dbIndicator.setStrokeWidth(1);
        dbStatusLabel = new Label("Database: Connected"); // Store reference
        dbBox.getChildren().addAll(dbIndicator, dbStatusLabel);
        
        HBox userBox = new HBox(5);
        userBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label userLabel = new Label("User: (Not Logged In)"); // Default text
        userBox.getChildren().addAll(userLabel);
        
        // Logic to update userLabel when currentUser changes is in showLoginDialog and logout
        
        dateTimeLabel.getStyleClass().add("info");
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
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Add new button to the left, then other status items
        statusBar.getChildren().addAll(statusBarClockInOutButton, statusBox, dbBox, userBox, spacer, dateTimeLabel);
        return statusBar;
    }
    
    /**
     * Shuts down resources used by the MainView, such as controllers.
     */
    public void shutdownResources() {
        if (inventoryController != null) {
            inventoryController.shutdown(); // This will stop the ScannerServer
            System.out.println("[MainView] InventoryController shutdown initiated.");
        }
        if (dailyReportService != null) {
            dailyReportService.stop();
            System.out.println("[MainView] DailyReportService shutdown.");
        }
    }

    public com.tireshop.service.DailyReportService getDailyReportService() {
        return dailyReportService;
    }
    
    private void createAllTabs() {
        // Create dashboard tab
        Tab dashboardTab = new Tab("Dashboard");
        createDashboardTab(dashboardTab);
        tabPane.getTabs().add(dashboardTab);

        // Create other tabs but don't add them to tabPane yet
        inventoryTab = new Tab("Inventory");
        setupInventoryTab(inventoryTab);
        
        salesTab = new Tab("Sales");
        setupSalesTab(salesTab);
        
        customersTab = new Tab("Customers");
        setupCustomersTab(customersTab);
        
        servicesTab = new Tab("Services");
        setupServicesTab(servicesTab);
        
        appointmentsTab = new Tab("Appointments");
        setupAppointmentsTab(appointmentsTab);
        
        adminTab = new Tab("Admin Settings");
        setupAdminTab(adminTab);
        
        reportsTab = new Tab("Reports");
        setupReportsTab(reportsTab);
        
        // Add tab selection listeners for auto-refresh
        setupTabSelectionListeners();
        
        // Start auto-refresh service for multi-machine synchronization
        startAutoRefreshService();
    }
    
    /**
     * Start the auto-refresh service for multi-machine synchronization
     */
    private void startAutoRefreshService() {
        try {
            AutoRefreshService autoRefreshService = AutoRefreshService.getInstance();
            autoRefreshService.setMainView(this);
            autoRefreshService.start();
            System.out.println("✅ Auto-refresh service started for multi-machine synchronization");
        } catch (Exception e) {
            System.err.println("❌ Failed to start auto-refresh service: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Set up tab selection listeners to auto-refresh when tabs become active
     */
    private void setupTabSelectionListeners() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null) {
                String tabName = newTab.getText();
                System.out.println("[MainView] Tab selected: " + tabName);
                
                // Auto-refresh the selected tab
                switch (tabName) {
                    case "Dashboard":
                        refreshDashboard();
                        break;
                    case "Inventory":
                        if (inventoryController != null) {
                            inventoryController.refreshProducts();
                        }
                        break;
                    case "Sales":
                        if (salesController != null) {
                            salesController.refreshSales();
                        }
                        break;
                    case "Customers":
                        refreshCustomerSection();
                        break;
                    case "Services":
                        if (serviceController != null) {
                            serviceController.refreshServicesTable();
                        }
                        break;
                    case "Appointments":
                        if (appointmentController != null) {
                            appointmentController.refreshAppointments();
                        }
                        break;
                    case "Admin Settings":
                        if (adminController != null) {
                            adminController.refreshDashboardStats();
                        }
                        break;
                    case "Reports":
                        if (reportController != null) {
                            reportController.refreshAnalyticsMetrics();
                        }
                        break;
                }
            }
        });
    }

    private void showOnlyDashboard() {
        tabPane.getTabs().clear();
        Tab dashboardTab = new Tab("Dashboard");
        createDashboardTab(dashboardTab);
        tabPane.getTabs().add(dashboardTab);
    }

    private void updateTabsBasedOnRole(String roleName) {
        Tab dashboardTab = null;
        if (!tabPane.getTabs().isEmpty()) {
            dashboardTab = tabPane.getTabs().get(0); // Preserve dashboard tab if it exists
        }
        tabPane.getTabs().clear();
        if (dashboardTab != null && "Dashboard".equals(dashboardTab.getText())) {
            tabPane.getTabs().add(dashboardTab); // Always re-add dashboard first
        } else { // Should not happen if dashboard is always first, but as a fallback:
            Tab newDashboardTab = new Tab("Dashboard");
            createDashboardTab(newDashboardTab);
            tabPane.getTabs().add(newDashboardTab);
            System.err.println("[MainView] Dashboard tab was missing or not first, recreated it.");
        }

        System.out.println("[MainView] Updating UI based on permissions for role: " + roleName);
        Set<String> permittedTabs = userService.getTabPermissionsForRole(roleName);
        System.out.println("[MainView] Permitted tabs for " + roleName + ": " + permittedTabs);

        // Add tabs based on permissions
        // Ensure tab instances (inventoryTab, salesTab etc.) are not null (created in createAllTabs)
        if (permittedTabs.contains(UserService.TAB_INVENTORY) && inventoryTab != null) tabPane.getTabs().add(inventoryTab);
        if (permittedTabs.contains(UserService.TAB_SALES) && salesTab != null) tabPane.getTabs().add(salesTab);
        if (permittedTabs.contains(UserService.TAB_CUSTOMERS) && customersTab != null) tabPane.getTabs().add(customersTab);
        if (permittedTabs.contains(UserService.TAB_SERVICES) && servicesTab != null) tabPane.getTabs().add(servicesTab);
        if (permittedTabs.contains(UserService.TAB_APPOINTMENTS) && appointmentsTab != null) tabPane.getTabs().add(appointmentsTab);
        if (permittedTabs.contains(UserService.TAB_ADMIN_SETTINGS) && adminTab != null) tabPane.getTabs().add(adminTab);
        if (permittedTabs.contains(UserService.TAB_REPORTS) && reportsTab != null) tabPane.getTabs().add(reportsTab);

        System.out.println("[MainView] Total tabs after update for role " + roleName + ": " + tabPane.getTabs().size());
        tabPane.getSelectionModel().selectFirst(); // Select dashboard after update
    }

    private void showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog(userService);
        if (loginDialog.showAndWaitForLogin()) {
            currentUser = loginDialog.getLoggedInUser();
            if (currentUser != null) {
                loginMenuItem.setText("Logout (" + currentUser.getUsername() + ")");
                // Update userLabel in status bar
                HBox statusBar = (HBox) stage.getScene().getRoot().lookup(".status-bar"); // Assuming BorderPane's bottom is HBox
                if (statusBar != null) {
                    Label userLabel = (Label) statusBar.getChildren().stream()
                                           .filter(node -> node instanceof HBox && ((HBox)node).getChildren().stream().anyMatch(child -> child instanceof Label && ((Label)child).getText().startsWith("User:")))
                                           .findFirst().map(node -> ((HBox)node).getChildren().get(0)).orElse(null);
                    if(userLabel instanceof Label) { // Check if it's actually the label
                         ((Label)userLabel).setText("User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                    } else { // Fallback to find by style or ID if specific structure fails
                        Label foundUserLabel = (Label)statusBar.lookup("#userStatusLabel"); // Assuming you add an ID
                        if(foundUserLabel != null) foundUserLabel.setText("User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                    }
                }

                updateTimeClockMenu(); // This will enable the clock in/out options
                updateTabsBasedOnRole(currentUser.getRole());
                showAlert("Login Successful", "Welcome " + currentUser.getUsername() + "!", Alert.AlertType.INFORMATION);
            }
        }
    }

    private void logout() {
        // Removed auto clock-out functionality - employees remain clocked in until they manually clock out
        currentUser = null;
        loginMenuItem.setText("Login");
        
        // Update userLabel in status bar
        HBox statusBar = (HBox) stage.getScene().getRoot().lookup(".status-bar");
         if (statusBar != null) {
            Label userLabel = (Label) statusBar.getChildren().stream()
                                   .filter(node -> node instanceof HBox && ((HBox)node).getChildren().stream().anyMatch(child -> child instanceof Label && ((Label)child).getText().startsWith("User:")))
                                   .findFirst().map(node -> ((HBox)node).getChildren().get(0)).orElse(null);
            if(userLabel instanceof Label) {
                 ((Label)userLabel).setText("User: (Not Logged In)");
            } else {
                Label foundUserLabel = (Label)statusBar.lookup("#userStatusLabel");
                if(foundUserLabel != null) foundUserLabel.setText("User: (Not Logged In)");
            }
        }

        updateTimeClockMenu(); // This will disable clock in/out options
        showOnlyDashboard();
        showAlert("Logout", "You have been logged out.", Alert.AlertType.INFORMATION);
    }
    
    private void createDashboardTab(Tab tab) {
        BorderPane mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(0)); // Remove padding for better space utilization
        mainContainer.getStyleClass().add("main-dashboard-container");
        
        // Enhanced header with stats overview - compact responsive layout
        VBox headerSection = new VBox(10);
        headerSection.getStyleClass().add("dashboard-header-section");
        headerSection.setPadding(new Insets(15, 20, 15, 20));
        headerSection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 0 0 2px 0;");
        
        // Add quick links at the TOP as requested
        FlowPane quickLinksPane = createTopBarQuickLinks();
        quickLinksPane.setStyle("-fx-background-color: #e8f4fd; -fx-padding: 10px; -fx-border-color: #2196F3; -fx-border-width: 0 0 2px 0;");
        updateQuickLinksTopBar();
        
        // Main title with welcome message
        HBox titleBox = new HBox(20);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox titleContent = new VBox(5);
        Label titleLabel = new Label("🏪 Tire Shop Dashboard");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label welcomeLabel = new Label("Welcome back, " + (currentUser != null ? currentUser.getUsername() : "User"));
        welcomeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        
        titleContent.getChildren().addAll(titleLabel, welcomeLabel);
        
        // Date and time display
        VBox dateTimeBox = new VBox(5);
        dateTimeBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Label currentDateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        currentDateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label currentTimeLabel = new Label();
        currentTimeLabel.setId("currentTimeLabel");
        currentTimeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        updateCurrentTime(currentTimeLabel);
        
        dateTimeBox.getChildren().addAll(currentDateLabel, currentTimeLabel);
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        titleBox.getChildren().addAll(titleContent, spacer, dateTimeBox);
        
        // ENHANCED VISIBLE STATS - Professional cards that stand out!
        HBox quickStatsBox = new HBox(25);
        quickStatsBox.setAlignment(javafx.geometry.Pos.CENTER);
        quickStatsBox.setPrefHeight(140);
        quickStatsBox.setMinHeight(130);
        quickStatsBox.setPadding(new Insets(25, 15, 25, 15));
        quickStatsBox.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef); -fx-border-color: #dee2e6; -fx-border-width: 1px; -fx-background-radius: 10px; -fx-border-radius: 10px;");
        
        // Create simple, visible stat boxes - will be updated with real data
        VBox salesBox = createSimpleStatBox("💰 Sales", "Loading...", "#2196F3", "dashboard-sales");
        VBox appointmentsBox = createSimpleStatBox("📅 Appointments", "Loading...", "#FF5722", "dashboard-appointments");
        VBox stockBox = createSimpleStatBox("⚠️ Low Stock", "Loading...", "#FF9800", "dashboard-stock");
        VBox tiresBox = createSimpleStatBox("🚗 Tires Sold", "Loading...", "#4CAF50", "dashboard-tires");
        
        quickStatsBox.getChildren().addAll(salesBox, appointmentsBox, stockBox, tiresBox);
        
        // Quick actions bar
        HBox quickActionsBox = new HBox(10);
        quickActionsBox.setAlignment(javafx.geometry.Pos.CENTER);
        quickActionsBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button newSaleBtn = new Button("🛒 New Sale");
        newSaleBtn.getStyleClass().addAll("success", "dashboard-action-btn");
        newSaleBtn.setOnAction(e -> {
            if (tabPane.getTabs().contains(salesTab)) tabPane.getSelectionModel().select(salesTab);
            else showAlert("Access Denied", "You do not have access to Sales.", Alert.AlertType.WARNING);
        });
        
        Button newAppointmentBtn = new Button("📅 New Appointment");
        newAppointmentBtn.getStyleClass().addAll("primary", "dashboard-action-btn");
        newAppointmentBtn.setOnAction(e -> {
            if (tabPane.getTabs().contains(appointmentsTab)) {
                tabPane.getSelectionModel().select(appointmentsTab);
                appointmentController.showNewAppointmentDialog(stage);
            } else showAlert("Access Denied", "You do not have access to Appointments.", Alert.AlertType.WARNING);
        });
        
        Button addCustomerBtn = new Button("👤 Add Customer");
        addCustomerBtn.getStyleClass().addAll("secondary", "dashboard-action-btn");
        addCustomerBtn.setOnAction(e -> {
            if (tabPane.getTabs().contains(customersTab)) {
                showAddCustomerDialog();
                refreshCustomerData();
            } else showAlert("Access Denied", "You do not have access to Customers.", Alert.AlertType.WARNING);
        });
        
        Button viewReportsBtn = new Button("📊 View Reports");
        viewReportsBtn.getStyleClass().addAll("warning", "dashboard-action-btn");
        viewReportsBtn.setOnAction(e -> {
            if (tabPane.getTabs().contains(reportsTab)) tabPane.getSelectionModel().select(reportsTab);
            else showAlert("Access Denied", "You do not have access to Reports.", Alert.AlertType.WARNING);
        });
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("secondary", "dashboard-action-btn");
        refreshBtn.setOnAction(e -> refreshDashboard());
        
        quickActionsBox.getChildren().addAll(newSaleBtn, newAppointmentBtn, addCustomerBtn, viewReportsBtn, refreshBtn);
        
        // Add quick links first, then everything else
        headerSection.getChildren().addAll(quickLinksPane, titleBox, quickStatsBox, quickActionsBox);
        mainContainer.setTop(headerSection);
        
        // Enhanced main content with BIGGER cards for less scrolling
        GridPane dashboardContentLayout = new GridPane();
        dashboardContentLayout.setHgap(15);
        dashboardContentLayout.setVgap(15);
        dashboardContentLayout.setPadding(new Insets(5, 15, 15, 15)); // Less top padding
        dashboardContentLayout.getStyleClass().add("dashboard-content-grid");
        
        ColumnConstraints col1 = new ColumnConstraints(); 
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); 
        col2.setPercentWidth(50);
        dashboardContentLayout.getColumnConstraints().addAll(col1, col2);
        
        RowConstraints rowMain = new RowConstraints(); 
        rowMain.setVgrow(Priority.ALWAYS);
        dashboardContentLayout.getRowConstraints().addAll(rowMain);

        // Left column: BIGGER Inventory Alerts taking full column space
        inventoryAlertsContent = new VBox(5); 
        inventoryAlertsContent.getStyleClass().add("spacing-10");
        TitledPane inventoryStatsPane = new TitledPane("⚠️ Inventory Alerts", inventoryAlertsContent);
        inventoryStatsPane.setCollapsible(false); 
        inventoryStatsPane.getStyleClass().addAll("dashboard-tile", "enhanced-tile");
        inventoryStatsPane.setPrefHeight(700); // Make it MUCH BIGGER - full column height
        inventoryStatsPane.setMinHeight(650);
        inventoryStatsPane.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        VBox.setVgrow(inventoryStatsPane, Priority.ALWAYS);
        
        dashboardContentLayout.add(inventoryStatsPane, 0, 0);
        
        // Right column: BIGGER Tires Sold taking full column space
        VBox tiresSoldTodayContent = new VBox(5);
        tiresSoldTodayContent.getStyleClass().add("spacing-10");
        this.tiresSoldTodayContent = tiresSoldTodayContent;
        TitledPane tiresSoldTodayPane = new TitledPane("🚗 Tires Sold Today", tiresSoldTodayContent);
        tiresSoldTodayPane.setCollapsible(false); 
        tiresSoldTodayPane.getStyleClass().addAll("dashboard-tile", "enhanced-tile");
        tiresSoldTodayPane.setPrefHeight(700); // Make it MUCH BIGGER - full column height
        tiresSoldTodayPane.setMinHeight(650);
        tiresSoldTodayPane.setStyle("-fx-background-color: #f3e5f5; -fx-border-color: #9C27B0; -fx-border-width: 2px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        VBox.setVgrow(tiresSoldTodayPane, Priority.ALWAYS);
        
        dashboardContentLayout.add(tiresSoldTodayPane, 1, 0);

        // Initialize data
        updateInventoryAlerts(); 
        updateTiresSoldToday();
        updateQuickStats();
        
        mainContainer.setCenter(dashboardContentLayout);
        tab.setContent(mainContainer);

        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected()) {
                refreshDashboard();
            }
        });
    }
    
    private VBox createQuickStatCard(String icon, String title, String value, String id, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("quick-stat-card");
        // Make them BIGGER and more visible
        card.setPrefWidth(200);
        card.setPrefHeight(130);
        card.setMinWidth(180);
        card.setMinHeight(120);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e1e8ed; -fx-border-width: 1px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center; -fx-font-weight: normal;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        
        Label valueLabel = new Label(value);
        valueLabel.setId(id);
        valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);
        
        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-cursor: hand; -fx-scale-x: 1.05; -fx-scale-y: 1.05; -fx-background-color: #f8f9fa; -fx-border-color: #e1e8ed; -fx-border-width: 1px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 12, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-cursor: default; -fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-background-color: white; -fx-border-color: #e1e8ed; -fx-border-width: 1px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);"));
        
        return card;
    }
    
    private VBox createSimpleStatBox(String title, String value, String color, String id) {
        VBox box = new VBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPadding(new Insets(25, 20, 25, 20));
        box.setPrefWidth(280);
        box.setPrefHeight(120);
        box.setMinWidth(250);
        box.setMinHeight(110);
        box.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3px; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #444; -fx-text-alignment: center;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(240);
        
        Label valueLabel = new Label(value);
        valueLabel.setId(id); // Set the ID for updating later
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-text-alignment: center;");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(240);
        
        box.getChildren().addAll(titleLabel, valueLabel);
        
        // Enhanced hover effect with scaling
        box.setOnMouseEntered(e -> {
            box.setStyle("-fx-background-color: " + color + "; -fx-border-color: " + color + "; -fx-border-width: 3px; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 5); -fx-cursor: hand;");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-text-alignment: center;");
            valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; -fx-text-alignment: center;");
            box.setScaleX(1.05);
            box.setScaleY(1.05);
        });
        box.setOnMouseExited(e -> {
            box.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3px; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3); -fx-cursor: default;");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #444; -fx-text-alignment: center;");
            valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-text-alignment: center;");
            box.setScaleX(1.0);
            box.setScaleY(1.0);
        });
        
        return box;
    }
    
    private void updateCurrentTime(Label timeLabel) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    private void updateQuickStats() {
        System.out.println("[MainView] Updating quick stats for date: " + LocalDate.now());
        CompletableFuture.runAsync(() -> {
            try {
                // Get today's sales data
                SalesSummaryData todaySummary = salesService.getSalesSummaryForDate(LocalDate.now());
                String salesValue = "$" + String.format("%.0f", todaySummary.getTotalRevenue());
                System.out.println("[MainView] Sales today: " + salesValue);
                
                // Get appointments count
                AppointmentDao appointmentDao = new AppointmentDao(DatabaseManager.getSessionFactory());
                List<Appointment> todaysAppointments = appointmentDao.findByDate(LocalDate.now());
                String appointmentsValue = String.valueOf(todaysAppointments.size());
                System.out.println("[MainView] Appointments today: " + appointmentsValue);
                
                // Get low stock count
                List<Product> lowStockProducts = inventoryService.getLowStockProducts();
                String lowStockValue = String.valueOf(lowStockProducts.size());
                System.out.println("[MainView] Low stock items: " + lowStockValue);
                
                // Get tires sold today - count actual tire quantities (EXCLUDING VOIDED SALES!)
                SaleItemDao saleItemDao = new SaleItemDao(DatabaseManager.getSessionFactory());
                List<SaleItem> todaySaleItems = saleItemDao.findProductItemsBySaleDateRange(LocalDate.now(), LocalDate.now());
                int totalTiresSold = todaySaleItems.stream()
                    .filter(item -> item.getSale() != null && !item.getSale().isVoided()) // Exclude voided sales!
                    .filter(item -> item.getProduct() != null && 
                            ((item.getProduct().getName() != null && item.getProduct().getName().toLowerCase().contains("tire")) || 
                            (item.getProduct().getCategory() != null && item.getProduct().getCategory().toLowerCase().contains("tire"))))
                    .mapToInt(SaleItem::getQuantity)
                    .sum();
                String tiresValue = String.valueOf(totalTiresSold);
                System.out.println("[MainView] Tires sold today: " + tiresValue + " (excluding voided sales)");
                
                Platform.runLater(() -> {
                    updateQuickStatCard("dashboard-sales", salesValue);
                    updateQuickStatCard("dashboard-appointments", appointmentsValue);
                    updateQuickStatCard("dashboard-stock", lowStockValue);
                    updateQuickStatCard("dashboard-tires", tiresValue);
                });
                
            } catch (Exception e) {
                System.err.println("[MainView] Error updating quick stats: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    updateQuickStatCard("dashboard-sales", "Error");
                    updateQuickStatCard("dashboard-appointments", "Error");
                    updateQuickStatCard("dashboard-stock", "Error");
                    updateQuickStatCard("dashboard-tires", "Error");
                });
            }
        });
    }
    
    private void updateQuickStatCard(String id, String value) {
        Label statLabel = (Label) stage.getScene().lookup("#" + id);
        if (statLabel != null) {
            System.out.println("[MainView] Updating stat " + id + " to: " + value);
            statLabel.setText(value);
        } else {
            System.err.println("[MainView] Could not find label with ID: " + id);
        }
    }
    
    private FlowPane createTopBarQuickLinks() {
        quickLinksFlowPane = new FlowPane(); // Use the field
        quickLinksFlowPane.setHgap(8);
        quickLinksFlowPane.setVgap(5); 
        quickLinksFlowPane.setPadding(new Insets(5, 10, 5, 10));
        quickLinksFlowPane.setStyle("-fx-background-color: #e0e0e0;"); // Style similar to a toolbar or menu bar background
        return quickLinksFlowPane;
    }

    private void updateQuickLinksTopBar() {
        if (quickLinksFlowPane == null) return;
        quickLinksFlowPane.getChildren().clear();
        SettingsService settingsService = SettingsService.getInstance();
        List<QuickLink> quickLinks = settingsService.getQuickLinks();

        if (!quickLinks.isEmpty()) {
            for (QuickLink ql : quickLinks) {
                Button linkButton = new Button(ql.getName());
                linkButton.getStyleClass().add("quick-link-button"); // Use a specific style if needed
                linkButton.setOnAction(e -> {
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI(ql.getUrl()));
                        }
                    } catch (IOException | URISyntaxException ex) {
                        showAlert("Error Opening Link", "Could not open URL: " + ql.getUrl() + "\n" + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                });
                quickLinksFlowPane.getChildren().add(linkButton);
            }
        }
        // If empty, the FlowPane will just be an empty bar, which is fine.
    }
    
    public void refreshDashboard() {
        updateInventoryAlerts();
        updateTiresSoldToday();
        updateQuickStats(); // Update the new quick stats cards
        updateQuickLinksTopBar(); // Refresh quick links in the top bar
        
        // Refresh analytics in reports section
        if (reportController != null) {
            reportController.refreshAnalyticsMetrics();
        }
        
        // Refresh responsive styles for the dashboard
        ResponsiveUIManager.refreshResponsiveStyles(stage.getScene());
    }
    
    /**
     * Check whether any secondary window (dialog, alert, etc.) is currently open.
     * Auto-refresh uses this to avoid wiping screens while the user is mid-task.
     */
    public boolean isDialogOpen() {
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof javafx.stage.Stage && window != stage && window.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refresh only the currently active tab (better performance)
     */
    public void refreshCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabName = selectedTab.getText();
            switch (tabName) {
                case "Dashboard":
                    refreshDashboard();
                    break;
                case "Inventory":
                    if (inventoryController != null) {
                        inventoryController.refreshProducts();
                    }
                    break;
                case "Sales":
                    if (salesController != null) {
                        salesController.refreshSales();
                    }
                    break;
                case "Customers":
                    refreshCustomerSection();
                    break;
                case "Services":
                    if (serviceController != null) {
                        serviceController.refreshServicesTable();
                    }
                    break;
                case "Appointments":
                    if (appointmentController != null) {
                        appointmentController.refreshAppointments();
                    }
                    break;
                case "Admin Settings":
                    if (adminController != null) {
                        adminController.refreshDashboardStats();
                    }
                    break;
                case "Reports":
                    if (reportController != null) {
                        reportController.refreshAnalyticsMetrics();
                    }
                    break;
            }
        }
    }
    
    /**
     * Comprehensive refresh system for all tabs
     */
    public void refreshAllTabs() {
        System.out.println("[MainView] Refreshing all tabs...");
        
        // Refresh dashboard
        refreshDashboard();
        
        // Refresh inventory
        if (inventoryController != null) {
            inventoryController.refreshProducts();
        }
        
        // Refresh sales
        if (salesController != null) {
            salesController.refreshSales();
        }
        
        // Refresh customers
        refreshCustomerSection();
        
        // Refresh appointments
        if (appointmentController != null) {
            appointmentController.refreshAppointments();
        }
        
        // Refresh services
        if (serviceController != null) {
            serviceController.refreshServicesTable();
        }
        
        // Refresh admin dashboard
        if (adminController != null) {
            adminController.refreshDashboardStats();
        }
        
        // Refresh reports
        if (reportController != null) {
            reportController.refreshAnalyticsMetrics();
        }
        
        System.out.println("[MainView] All tabs refreshed successfully");
    }
    
    /**
     * Get the sales service for external access
     */
    public SalesService getSalesService() {
        return salesService;
    }
    
    /**
     * Refresh specific tab based on data changes - ALWAYS refreshes immediately
     */
    public void refreshTabsForDataChange(String dataType) {
        System.out.println("[MainView] Immediate refresh for data change: " + dataType);
        
        // Always refresh dashboard for any data change (shows stats)
        javafx.application.Platform.runLater(this::refreshDashboard);
        
        switch (dataType.toLowerCase()) {
            case "product":
            case "inventory":
                // Refresh inventory tab immediately
                if (inventoryController != null) {
                    javafx.application.Platform.runLater(() -> inventoryController.refreshProducts());
                }
                break;
                
            case "sale":
            case "sales":
                // Refresh sales and reports immediately
                if (salesController != null) {
                    javafx.application.Platform.runLater(() -> salesController.refreshSales());
                }
                if (reportController != null) {
                    javafx.application.Platform.runLater(() -> reportController.refreshAnalyticsMetrics());
                }
                break;
                
            case "customer":
            case "customers":
                // Refresh customers tab immediately
                javafx.application.Platform.runLater(this::refreshCustomerSection);
                break;
                
            case "appointment":
            case "appointments":
                // Refresh appointments immediately
                if (appointmentController != null) {
                    javafx.application.Platform.runLater(() -> appointmentController.refreshAppointments());
                }
                break;
                
            case "service":
            case "services":
                // Refresh services tab immediately
                if (serviceController != null) {
                    javafx.application.Platform.runLater(() -> serviceController.refreshServicesTable());
                }
                break;
                
            case "all":
            default:
                // Refresh everything immediately
                javafx.application.Platform.runLater(this::refreshAllTabs);
                break;
        }
    }
    

    
    private void setupInventoryTab(Tab tab) {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: #f8f9fa;");
        
        TableView<Product> productTable = new TableView<>();
        if (inventoryController != null) {
            inventoryController.setProductTable(productTable); 
            inventoryController.initialize(); 
        } else {
            content.setCenter(new Label("Error: InventoryController not initialized."));
            tab.setContent(content);
            return;
        }
        
        // Enhanced header section
        VBox topPanel = new VBox(15);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Header title
        Label headerTitle = new Label("📦 Inventory Management");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        
        // Enhanced action buttons with modern styling
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button addBtn = new Button("➕ Add Product");
        addBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button editBtn = new Button("✏️ Edit Product");
        editBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button deleteBtn = new Button("🗑️ Delete Product");
        deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button checkAvailabilityBtn = new Button("🔍 Check Availability");
        checkAvailabilityBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button scanBtn = new Button("📱 Mobile Scanner");
        scanBtn.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        actionButtons.getChildren().addAll(addBtn, editBtn, deleteBtn, checkAvailabilityBtn, refreshBtn, scanBtn);
        // Enhanced filter controls section with better styling
        HBox filterControls = new HBox(15);
        filterControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterControls.setPadding(new Insets(10, 0, 0, 0));
        
        if (inventoryController.getSearchField() != null) {
            TextField searchField = inventoryController.getSearchField();
            searchField.setStyle("-fx-padding: 8 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px; -fx-font-size: 14px;");
            searchField.setPrefWidth(200);
            filterControls.getChildren().add(searchField);
        }
        
        Label sortLabel = new Label("Sort by:");
        sortLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        ComboBox<String> sortCombo = inventoryController.getSortByComboBox();
        sortCombo.setStyle("-fx-padding: 4 8; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px;");
        
        CheckBox ascendingCheck = inventoryController.getAscendingCheckBox();
        ascendingCheck.setStyle("-fx-text-fill: #495057;");
        
        Label filterLabel = new Label("Filter:");
        filterLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        ComboBox<String> filterCombo = inventoryController.getFilterByComboBox();
        filterCombo.setStyle("-fx-padding: 4 8; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px;");
        
        filterControls.getChildren().addAll(sortLabel, sortCombo, ascendingCheck, filterLabel, filterCombo);
        topPanel.getChildren().addAll(headerTitle, actionButtons, filterControls);
        content.setTop(topPanel);
        
        // Enhanced main content area
        VBox mainContentWrapper = new VBox(15);
        mainContentWrapper.setPadding(new Insets(15, 0, 0, 0));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.7);
        splitPane.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Enhanced product table styling
        productTable.setStyle("-fx-background-color: white; -fx-table-cell-border-color: #e9ecef; -fx-font-size: 13px;");
        splitPane.getItems().add(productTable); 
        
        // Enhanced details pane
        ScrollPane detailsScrollPane = new ScrollPane(inventoryController.getDetailsPane());
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.setPrefWidth(320);
        detailsScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        splitPane.getItems().add(detailsScrollPane);
        
        mainContentWrapper.getChildren().add(splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        content.setCenter(mainContentWrapper);
        addBtn.setOnAction(e -> inventoryController.showAddProductDialog(stage));
        refreshBtn.setOnAction(e -> inventoryController.refreshProducts());
        scanBtn.setOnAction(e -> {
            try {
                if (inventoryController.getScannerServer() == null || !inventoryController.getScannerServer().isRunning()) {
                    inventoryController.startScannerServer();
                    String scannerUrl = inventoryController.getScannerServer().getScanUrl();
                    showAlert("Mobile Scanner", "Mobile scanner server started.\n\nAccess from your phone at:\n" + scannerUrl + "\n\nMake sure your phone is on the same WiFi network.", Alert.AlertType.INFORMATION);
                } else {
                    String scannerUrl = inventoryController.getScannerServer().getScanUrl();
                    showAlert("Mobile Scanner", "Mobile scanner is already running.\n\nAccess from your phone at:\n" + scannerUrl + "\n\nMake sure your phone is on the same WiFi network.", Alert.AlertType.INFORMATION);
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to start mobile scanner: " + ex.getMessage(), Alert.AlertType.ERROR);
                ex.printStackTrace(); 
            }
        });
        editBtn.setOnAction(e -> {
            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) inventoryController.showAdjustPriceDialog(selectedProduct, stage); 
            else showAlert("No Selection", "Please select a product to edit.", Alert.AlertType.WARNING);
        });
        deleteBtn.setOnAction(e -> {
            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                if (showConfirmationDialog("Delete Product", "Are you sure you want to delete " + selectedProduct.getName() + "?")) {
                    inventoryController.deleteProduct(selectedProduct);
                }
            } else showAlert("No Selection", "Please select a product to delete.", Alert.AlertType.WARNING);
        });
        checkAvailabilityBtn.setOnAction(e -> {
            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                inventoryController.showSupplierAvailabilityDialog(selectedProduct, stage);
            } else {
                showAlert("No Selection", "Please select a product to check availability.", Alert.AlertType.WARNING);
            }
        });
        tab.setContent(content);
    }
    
    private void setupSalesTab(Tab tab) {
        BorderPane content = new BorderPane();
        salesController.initialize(content, stage);
        tab.setContent(content);
    }
    
    private void setupCustomersTab(Tab tab) {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: #f8f9fa;");
        
        // Enhanced header section
        VBox headerSection = new VBox(15);
        headerSection.setPadding(new Insets(20));
        headerSection.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Header title
        Label headerTitle = new Label("👥 Customer Management");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        
        // Enhanced controls with modern styling
        HBox controls = new HBox(12);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button addCustomerBtn = new Button("➕ Add Customer");
        addCustomerBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button editCustomerBtn = new Button("✏️ Edit Customer");
        editCustomerBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button deleteCustomerBtn = new Button("🗑️ Delete Customer");
        deleteCustomerBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        TextField searchCustomerField = new TextField();
        searchCustomerField.setPromptText("🔍 Search Customers...");
        searchCustomerField.setStyle("-fx-padding: 8 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px; -fx-font-size: 14px;");
        searchCustomerField.setPrefWidth(200);
        this.customerSearchField = searchCustomerField;

        Button searchCustomerBtn = new Button("🔍 Search");
        searchCustomerBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");

        Button chargePaymentBtn = new Button("💵 Record Charge Payment");
        chargePaymentBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");

        Button chargeHistoryBtn = new Button("📜 Charge History");
        chargeHistoryBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");

        controls.getChildren().addAll(addCustomerBtn, editCustomerBtn, deleteCustomerBtn, chargePaymentBtn, chargeHistoryBtn, searchCustomerField, searchCustomerBtn);
        headerSection.getChildren().addAll(headerTitle, controls);
        content.setTop(headerSection);
        // Enhanced main content area
        VBox mainContentWrapper = new VBox(15);
        mainContentWrapper.setPadding(new Insets(15, 0, 0, 0));
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Enhanced customer table with section styling
        this.customerTable = new TableView<>();
        this.customerTable.setStyle("-fx-background-color: white; -fx-table-cell-border-color: #e9ecef; -fx-font-size: 13px;");
        TableColumn<Customer, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        idCol.setPrefWidth(50);
        TableColumn<Customer, String> firstNameCol = new TableColumn<>("First Name");
        firstNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFirstName()));
        TableColumn<Customer, String> lastNameCol = new TableColumn<>("Last Name");
        lastNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastName()));
        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        TableColumn<Customer, String> chargeBalanceCol = new TableColumn<>("Charge Balance");
        chargeBalanceCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                String.format("$%.2f", cellData.getValue().getChargeBalance())));
        chargeBalanceCol.setCellFactory(column -> new javafx.scene.control.TableCell<Customer, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Customer customer = getTableView().getItems().get(getIndex());
                    if (customer != null && customer.getChargeBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });
        this.customerTable.getColumns().addAll(idCol, firstNameCol, lastNameCol, phoneCol, emailCol, chargeBalanceCol);
        VBox.setVgrow(this.customerTable, javafx.scene.layout.Priority.ALWAYS);
        
        // Enhanced vehicle table
        this.vehicleTable = new TableView<>();
        this.vehicleTable.setStyle("-fx-background-color: white; -fx-table-cell-border-color: #e9ecef; -fx-font-size: 13px;");
        TableColumn<Vehicle, String> makeCol = new TableColumn<>("Make");
        makeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMake()));
        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getModel()));
        TableColumn<Vehicle, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getModelYear()));
        TableColumn<Vehicle, String> plateCol = new TableColumn<>("License Plate");
        plateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLicensePlate()));
        this.vehicleTable.getColumns().addAll(makeCol, modelCol, yearCol, plateCol);
        VBox.setVgrow(this.vehicleTable, javafx.scene.layout.Priority.ALWAYS);
        
        // Enhanced vehicle controls
        HBox vehicleControls = new HBox(12);
        vehicleControls.setPadding(new Insets(10));
        vehicleControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button addVehicleBtn = new Button("➕ Add Vehicle");
        addVehicleBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button editVehicleBtn = new Button("✏️ Edit Vehicle");
        editVehicleBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button deleteVehicleBtn = new Button("🗑️ Delete Vehicle");
        deleteVehicleBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button serviceHistoryBtn = new Button("📋 Service History");
        serviceHistoryBtn.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        vehicleControls.getChildren().addAll(addVehicleBtn, editVehicleBtn, deleteVehicleBtn, serviceHistoryBtn);
        addVehicleBtn.setDisable(true); editVehicleBtn.setDisable(true); deleteVehicleBtn.setDisable(true); serviceHistoryBtn.setDisable(true);
        
        // Enhanced section styling
        VBox customerSection = new VBox(10);
        customerSection.setPadding(new Insets(15));
        Label customerSectionLabel = new Label("👥 Customers");
        customerSectionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #495057; -fx-padding: 0 0 5 0;");
        customerSection.getChildren().addAll(customerSectionLabel, this.customerTable);
        
        VBox vehicleSection = new VBox(10);
        vehicleSection.setPadding(new Insets(15));
        Label vehicleSectionLabel = new Label("🚗 Vehicles for Selected Customer");
        vehicleSectionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #495057; -fx-padding: 0 0 5 0;");
        vehicleSection.getChildren().addAll(vehicleSectionLabel, vehicleControls, this.vehicleTable);
        
        splitPane.getItems().addAll(customerSection, vehicleSection);
        splitPane.setDividerPositions(0.5);
        mainContentWrapper.getChildren().add(splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        content.setCenter(mainContentWrapper);
        CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
        VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
        refreshCustomerTable(this.customerTable, customerDao);
        this.customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                refreshVehicleTable(this.vehicleTable, vehicleDao, newSelection.getId());
                addVehicleBtn.setDisable(false); editVehicleBtn.setDisable(false); deleteVehicleBtn.setDisable(false); serviceHistoryBtn.setDisable(false);
            } else {
                this.vehicleTable.getItems().clear();
                addVehicleBtn.setDisable(true); editVehicleBtn.setDisable(true); deleteVehicleBtn.setDisable(true); serviceHistoryBtn.setDisable(true);
            }
        });

        // Record a payment against the selected customer's store charge balance
        chargePaymentBtn.setOnAction(e -> {
            Customer selected = this.customerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No Selection", "Please select a customer to record a charge payment.", Alert.AlertType.WARNING);
                return;
            }
            if (selected.getChargeBalance() == null || selected.getChargeBalance().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                showAlert("No Balance Due", selected.getFullName() + " has no outstanding charge balance.", Alert.AlertType.INFORMATION);
                return;
            }
            showChargeAccountPaymentDialog(selected);
            refreshCustomerTable(this.customerTable, customerDao);
        });
        chargeHistoryBtn.setOnAction(e -> {
            Customer selected = this.customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showChargeHistoryDialog(selected);
            } else showAlert("No Selection", "Please select a customer to view charge history.", Alert.AlertType.WARNING);
        });
        addCustomerBtn.setOnAction(e -> {
            showAddCustomerDialog(); 
            refreshCustomerTable(this.customerTable, customerDao);
        });
        editCustomerBtn.setOnAction(e -> {
            Customer selected = this.customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditCustomerDialog(selected); 
                refreshCustomerTable(this.customerTable, customerDao);
            } else showAlert("No Selection", "Please select a customer to edit.", Alert.AlertType.WARNING);
        });
        deleteCustomerBtn.setOnAction(e -> {
            Customer selected = this.customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmationDialog("Delete Customer", "Are you sure you want to delete " + selected.getFullName() + "? This will also delete their vehicles.")) {
                    try {
                        customerDao.delete(selected); 
                        refreshCustomerTable(this.customerTable, customerDao);
                        this.vehicleTable.getItems().clear();
                        refreshTabsForDataChange("customer");
                        showAlert("Success", "Customer deleted successfully.", Alert.AlertType.INFORMATION);
                    } catch (Exception ex) {
                        System.err.println("Error deleting customer: " + ex.getMessage());
                        
                        // Check if it's a constraint violation (customer has sales or other records)
                        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException ||
                            ex.getMessage().contains("constraint") || 
                            ex.getMessage().contains("foreign key") ||
                            ex.getMessage().contains("23503")) {
                            showAlert("Cannot Delete Customer", 
                                     "This customer cannot be deleted because they have sales records, appointments, or other data in the system. " +
                                     "Customers with transaction history must remain in the system for record-keeping and audit purposes.\n\n" +
                                     "If you need to deactivate this customer, please contact your system administrator.", 
                                     Alert.AlertType.ERROR);
                        } else {
                            showAlert("Delete Failed", 
                                     "An unexpected error occurred while deleting the customer: " + ex.getMessage(), 
                                     Alert.AlertType.ERROR);
                        }
                    }
                }
            } else showAlert("No Selection", "Please select a customer to delete.", Alert.AlertType.WARNING);
        });
        searchCustomerBtn.setOnAction(e -> {
            String searchTerm = searchCustomerField.getText();
            if (searchTerm == null || searchTerm.trim().isEmpty()) refreshCustomerTable(this.customerTable, customerDao);
            else this.customerTable.setItems(FXCollections.observableArrayList(customerDao.search(searchTerm.trim())));
        });
        addVehicleBtn.setOnAction(e -> {
            Customer selectedCustomer = this.customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                showAddVehicleDialog(selectedCustomer);
                refreshVehicleTable(this.vehicleTable, vehicleDao, selectedCustomer.getId());
            }
        });
        editVehicleBtn.setOnAction(e -> {
            Vehicle selectedVehicle = this.vehicleTable.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null) {
                showEditVehicleDialog(selectedVehicle);
                 Customer selectedCustomer = this.customerTable.getSelectionModel().getSelectedItem();
                 if(selectedCustomer != null) refreshVehicleTable(this.vehicleTable, vehicleDao, selectedCustomer.getId());
            } else {
                showAlert("No Selection", "Please select a vehicle to edit.", Alert.AlertType.WARNING);
            }
        });
        deleteVehicleBtn.setOnAction(e -> {
            Vehicle selectedVehicle = this.vehicleTable.getSelectionModel().getSelectedItem();
            Customer selectedCustomer = this.customerTable.getSelectionModel().getSelectedItem();
            if (selectedVehicle != null && selectedCustomer != null) {
                if (showConfirmationDialog("Delete Vehicle", "Are you sure you want to delete this vehicle?")) {
                    try {
                        vehicleDao.delete(selectedVehicle);
                        refreshVehicleTable(this.vehicleTable, vehicleDao, selectedCustomer.getId());
                        refreshTabsForDataChange("customer");
                        showAlert("Success", "Vehicle deleted successfully.", Alert.AlertType.INFORMATION);
                    } catch (Exception ex) {
                        System.err.println("Error deleting vehicle: " + ex.getMessage());
                        
                        // Check if it's a constraint violation (vehicle has service records or other data)
                        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException ||
                            ex.getMessage().contains("constraint") || 
                            ex.getMessage().contains("foreign key") ||
                            ex.getMessage().contains("23503")) {
                            showAlert("Cannot Delete Vehicle", 
                                     "This vehicle cannot be deleted because it has service records, appointments, or other data in the system. " +
                                     "Vehicles with service history must remain in the system for record-keeping and warranty tracking.\n\n" +
                                     "If you need to deactivate this vehicle, please contact your system administrator.", 
                                     Alert.AlertType.ERROR);
                        } else {
                            showAlert("Delete Failed", 
                                     "An unexpected error occurred while deleting the vehicle: " + ex.getMessage(), 
                                     Alert.AlertType.ERROR);
                        }
                    }
                }
            } else {
                showAlert("No Selection", "Please select a vehicle to delete.", Alert.AlertType.WARNING);
            }
        });
        serviceHistoryBtn.setOnAction(e -> {
            Customer selectedCustomer = this.customerTable.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                showServiceHistoryDialog(selectedCustomer);
            } else {
                showAlert("No Selection", "Please select a customer to view service history.", Alert.AlertType.WARNING);
            }
        });
        tab.setContent(content);
    }
    
    private void refreshCustomerTable(TableView<Customer> table, CustomerDao dao) {
        table.setItems(FXCollections.observableArrayList(dao.findAll()));
    }

    private void refreshVehicleTable(TableView<Vehicle> table, VehicleDao dao, Long customerId) {
        table.setItems(FXCollections.observableArrayList(dao.findByCustomerId(customerId)));
    }

    public void showEditCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");
        dialog.setHeaderText("Edit customer information");
        dialog.initOwner(stage);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField firstNameField = new TextField(customer.getFirstName());
        TextField lastNameField = new TextField(customer.getLastName());
        TextField phoneField = new TextField(customer.getPhone());
        TextField emailField = new TextField(customer.getEmail());
        TextField addressField = new TextField(customer.getAddress());
        CheckBox taxExemptCheckBox = new CheckBox("Tax Exempt");
        taxExemptCheckBox.setSelected(customer.isTaxExempt());
        grid.add(new Label("First Name:"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Address:"), 0, 4); grid.add(addressField, 1, 4);
        grid.add(new Label("Tax Status:"), 0, 5); grid.add(taxExemptCheckBox, 1, 5);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(firstNameField::requestFocus);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty() || phoneField.getText().trim().isEmpty()) {
                showAlert("Missing Information", "First name, last name, and phone number are required.", Alert.AlertType.ERROR);
                event.consume();
            }
        });
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                customer.setFirstName(firstNameField.getText().trim());
                customer.setLastName(lastNameField.getText().trim());
                customer.setPhone(phoneField.getText().trim());
                customer.setEmail(emailField.getText().trim());
                customer.setAddress(addressField.getText().trim());
                customer.setTaxExempt(taxExemptCheckBox.isSelected());
                CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
                return customerDao.update(customer);
            }
            return null;
        });
        dialog.showAndWait();
    }

    public void showEditVehicleDialog(Vehicle vehicle) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Edit Vehicle");
        dialog.setHeaderText("Edit vehicle information");
        dialog.initOwner(stage);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField makeField = new TextField(vehicle.getMake());
        TextField modelField = new TextField(vehicle.getModel());
        TextField yearField = new TextField(String.valueOf(vehicle.getModelYear()));
        TextField licensePlateField = new TextField(vehicle.getLicensePlate());
        TextField vinField = new TextField(vehicle.getVin());
        TextField colorField = new TextField(vehicle.getColor());
        grid.add(new Label("Make:"), 0, 0); grid.add(makeField, 1, 0);
        grid.add(new Label("Model:"), 0, 1); grid.add(modelField, 1, 1);
        grid.add(new Label("Year:"), 0, 2); grid.add(yearField, 1, 2);
        grid.add(new Label("License Plate:"), 0, 3); grid.add(licensePlateField, 1, 3);
        grid.add(new Label("VIN:"), 0, 4); grid.add(vinField, 1, 4);
        grid.add(new Label("Color:"), 0, 5); grid.add(colorField, 1, 5);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(makeField::requestFocus);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (makeField.getText().trim().isEmpty() || modelField.getText().trim().isEmpty() || yearField.getText().trim().isEmpty()) {
                showAlert("Missing Information", "Make, model, and year are required.", Alert.AlertType.ERROR);
                event.consume();
            } else {
                try { Integer.parseInt(yearField.getText().trim()); } 
                catch (NumberFormatException ex) {
                    showAlert("Invalid Year", "Year must be a valid number.", Alert.AlertType.ERROR);
                    event.consume();
                }
            }
        });
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                vehicle.setMake(makeField.getText().trim());
                vehicle.setModel(modelField.getText().trim());
                vehicle.setModelYear(Integer.parseInt(yearField.getText().trim()));
                vehicle.setLicensePlate(licensePlateField.getText().trim());
                vehicle.setVin(vinField.getText().trim());
                vehicle.setColor(colorField.getText().trim());
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                return vehicleDao.update(vehicle);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void setupServicesTab(Tab tab) {
        BorderPane serviceContentPane = new BorderPane();
        if (this.serviceController != null) this.serviceController.initialize(serviceContentPane, this.stage);
        else serviceContentPane.setCenter(new Label("Error: ServiceController not initialized."));
        tab.setContent(serviceContentPane);
    }
    
    private void setupAppointmentsTab(Tab tab) {
        BorderPane appointmentContentPane = new BorderPane();
        if (this.appointmentController != null) this.appointmentController.initialize(appointmentContentPane, this.stage);
        else appointmentContentPane.setCenter(new Label("Error: AppointmentController not initialized."));
        tab.setContent(appointmentContentPane);
    }
    
    private void setupAdminTab(Tab tab) {
        BorderPane adminContentPane = new BorderPane();
        if (this.adminController != null) this.adminController.initialize(adminContentPane, this.stage);
        else adminContentPane.setCenter(new Label("Error: AdminController not initialized."));
        tab.setContent(adminContentPane);
    }
    
    private void setupReportsTab(Tab tab) {
        BorderPane reportContentPane = new BorderPane();
        if (this.reportController != null) this.reportController.initialize(reportContentPane, this.stage);
        else reportContentPane.setCenter(new Label("Error: ReportController not initialized."));
        tab.setContent(reportContentPane);
    }
    
    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.initOwner(stage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    public void showAddCustomerDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add Customer"); dialog.setHeaderText("Create a new customer"); dialog.initOwner(stage);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField firstNameField = new TextField(); firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField(); lastNameField.setPromptText("Last Name");
        TextField phoneField = new TextField(); phoneField.setPromptText("Phone");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField addressField = new TextField(); addressField.setPromptText("Address");
        CheckBox taxExemptCheckBox = new CheckBox("Tax Exempt");
        grid.add(new Label("First Name:"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Address:"), 0, 4); grid.add(addressField, 1, 4);
        grid.add(new Label("Tax Status:"), 0, 5); grid.add(taxExemptCheckBox, 1, 5);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(firstNameField::requestFocus);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (firstNameField.getText().isEmpty()) {
                showAlert("Missing Information", "Please enter at least a first name.", Alert.AlertType.ERROR);
                event.consume();
            }
        });
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Customer customer = new Customer();
                customer.setFirstName(firstNameField.getText()); customer.setLastName(lastNameField.getText());
                customer.setPhone(phoneField.getText()); customer.setEmail(emailField.getText()); customer.setAddress(addressField.getText());
                customer.setTaxExempt(taxExemptCheckBox.isSelected());
                CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());
                return customerDao.save(customer);
            }
            return null;
        });
        dialog.showAndWait();
    }

    /**
     * Dialog to record a payment against a customer's store charge account balance
     */
    private void showChargeAccountPaymentDialog(Customer customer) {
        Dialog<java.math.BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Charge Account Payment");
        dialog.setHeaderText("Record payment from " + customer.getFullName());
        dialog.initOwner(stage);
        ButtonType saveButtonType = new ButtonType("Record Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        java.math.BigDecimal balance = customer.getChargeBalance();
        Label balanceLabel = new Label(String.format("$%.2f", balance));
        balanceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c0392b;");

        TextField amountField = new TextField();
        amountField.setPromptText("Payment amount");
        Label changeLabel = new Label("");
        changeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");

        // Live change-due display when the customer overpays
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(newVal.trim());
                if (amount.compareTo(balance) > 0) {
                    changeLabel.setText("Change due: $" + amount.subtract(balance).setScale(2, java.math.RoundingMode.HALF_UP));
                } else {
                    changeLabel.setText("");
                }
            } catch (NumberFormatException ex) {
                changeLabel.setText("");
            }
        });

        grid.add(new Label("Current Balance:"), 0, 0); grid.add(balanceLabel, 1, 0);
        grid.add(new Label("Payment Amount:"), 0, 1); grid.add(amountField, 1, 1);
        grid.add(changeLabel, 1, 2);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(amountField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new java.math.BigDecimal(amountField.getText().trim());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        Optional<java.math.BigDecimal> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            Optional<com.tireshop.model.ChargeAccountPayment> paymentRecord =
                    salesService.recordChargeAccountPaymentDetailed(customer.getId(), amount);
            if (paymentRecord.isPresent()) {
                com.tireshop.model.ChargeAccountPayment payment = paymentRecord.get();
                java.math.BigDecimal newBalance = payment.getBalanceAfter() != null
                        ? payment.getBalanceAfter() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal changeDue = amount.compareTo(balance) > 0
                        ? amount.subtract(balance) : null;
                String message = String.format("Payment of $%.2f recorded.\nNew balance: $%.2f", amount.min(balance), newBalance);
                if (changeDue != null) {
                    message += String.format("\n\nChange due to customer: $%.2f", changeDue);
                }
                showAlert("Payment Recorded", message, Alert.AlertType.INFORMATION);
                // Offer to print or save a receipt for the payment
                showChargePaymentReceiptOptions(payment, customer);
            } else {
                showAlert("Invalid Payment", "Please enter a valid payment amount greater than $0.", Alert.AlertType.WARNING);
            }
        });
    }

    /**
     * Lazily create the printer service (needs a SettingsService for company info).
     */
    private PrinterService getPrinterService() {
        if (printerService == null) {
            printerService = new PrinterService(new SettingsService());
        }
        return printerService;
    }

    /**
     * After a charge account payment, let the cashier print a receipt
     * or save it as a PDF for the customer.
     */
    private void showChargePaymentReceiptOptions(com.tireshop.model.ChargeAccountPayment payment, Customer customer) {
        Dialog<Void> receiptDialog = new Dialog<>();
        receiptDialog.setTitle("Payment Receipt");
        receiptDialog.setHeaderText("Print a receipt for this payment?");
        receiptDialog.initOwner(stage);

        ButtonType printButtonType = new ButtonType("🖨 Print Receipt", ButtonBar.ButtonData.OK_DONE);
        ButtonType pdfButtonType = new ButtonType("💾 Save PDF", ButtonBar.ButtonData.APPLY);
        receiptDialog.getDialogPane().getButtonTypes().addAll(printButtonType, pdfButtonType, ButtonType.CLOSE);

        String summary = String.format(
                "Customer: %s%nPayment: $%.2f%nBalance After: $%.2f%nDate: %s",
                customer.getFullName(),
                payment.getAmount() != null ? payment.getAmount() : java.math.BigDecimal.ZERO,
                payment.getBalanceAfter() != null ? payment.getBalanceAfter() : java.math.BigDecimal.ZERO,
                payment.getPaymentTimestamp() != null
                        ? payment.getPaymentTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")) : "");
        Label summaryLabel = new Label(summary);
        summaryLabel.setStyle("-fx-font-size: 13px; -fx-padding: 10;");
        receiptDialog.getDialogPane().setContent(summaryLabel);

        // Keep the dialog open after Print/Save so the cashier can do both
        Button printButton = (Button) receiptDialog.getDialogPane().lookupButton(printButtonType);
        printButton.addEventFilter(ActionEvent.ACTION, event -> {
            boolean success = getPrinterService().printChargePaymentReceipt(payment, customer, null);
            showAlert(success ? "Print Sent" : "Print Failed",
                    success ? "The payment receipt was sent to the printer."
                            : "Could not print the receipt. Check the printer connection.",
                    success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            event.consume(); // keep dialog open
        });
        Button pdfButton = (Button) receiptDialog.getDialogPane().lookupButton(pdfButtonType);
        pdfButton.addEventFilter(ActionEvent.ACTION, event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Payment Receipt PDF");
            fileChooser.setInitialFileName("payment-receipt-" + (payment.getId() != null ? payment.getId() : "receipt") + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                boolean success = getPrinterService().generateChargePaymentPdf(payment, customer, file.getAbsolutePath());
                showAlert(success ? "PDF Saved" : "Save Failed",
                        success ? "Receipt saved to:\n" + file.getAbsolutePath()
                                : "Could not save the PDF. Check the file path and try again.",
                        success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            }
            event.consume(); // keep dialog open
        });

        receiptDialog.showAndWait();
    }

    /**
     * Dialog showing a customer's full store charge account activity:
     * charges made against the account and payments made towards it.
     */
    private void showChargeHistoryDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Charge Account History");
        dialog.setHeaderText(customer.getFullName() + " - Store Charge Activity"
                + String.format("   (Current Balance: $%.2f)", customer.getChargeBalance()));
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        SalesService.ChargeAccountActivity activity = salesService.getChargeAccountActivity(customer.getId());

        // Merge charges and payments into one dated activity list
        List<String[]> rows = new ArrayList<>();
        // Parallel list: payment record for each row (null for charge rows)
        List<com.tireshop.model.ChargeAccountPayment> rowPayments = new ArrayList<>();
        for (com.tireshop.model.SalePayment charge : activity.charges) {
            String invoice = charge.getSale() != null && charge.getSale().getInvoiceNumber() != null
                    ? charge.getSale().getInvoiceNumber() : "?";
            rows.add(new String[]{
                    charge.getPaymentTimestamp() != null
                            ? charge.getPaymentTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                    "Charge - Invoice " + invoice,
                    String.format("$%.2f", charge.getAmount()), ""});
            rowPayments.add(null);
        }
        for (com.tireshop.model.ChargeAccountPayment payment : activity.payments) {
            String description = payment.getNotes() != null && !payment.getNotes().trim().isEmpty()
                    ? payment.getNotes() : "Payment Received";
            java.math.BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : java.math.BigDecimal.ZERO;
            String amountText = (amount.signum() >= 0 ? "-$" : "+$")
                    + String.format("%.2f", amount.abs());
            rows.add(new String[]{
                    payment.getPaymentTimestamp() != null
                            ? payment.getPaymentTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                    description,
                    amountText,
                    payment.getBalanceAfter() != null ? String.format("$%.2f", payment.getBalanceAfter()) : ""});
            rowPayments.add(payment);
        }
        // Sort newest first, keeping the payment records aligned with their rows
        final List<String[]> rowsForSort = rows;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rowsForSort.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> rowsForSort.get(b)[0].compareTo(rowsForSort.get(a)[0]));
        List<String[]> sortedRows = new ArrayList<>();
        List<com.tireshop.model.ChargeAccountPayment> sortedPayments = new ArrayList<>();
        for (int idx : order) {
            sortedRows.add(rowsForSort.get(idx));
            sortedPayments.add(rowPayments.get(idx));
        }
        rows = sortedRows;
        rowPayments = sortedPayments;

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(560);
        content.setPrefHeight(380);

        if (rows.isEmpty()) {
            Label emptyLabel = new Label("No store charge activity for this customer yet.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
            content.getChildren().add(emptyLabel);
        } else {
            TableView<String[]> table = new TableView<>();
            TableColumn<String[], String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));
            dateCol.setPrefWidth(140);
            TableColumn<String[], String> descCol = new TableColumn<>("Description");
            descCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
            descCol.setPrefWidth(220);
            TableColumn<String[], String> amountCol = new TableColumn<>("Amount");
            amountCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[2]));
            amountCol.setPrefWidth(90);
            TableColumn<String[], String> balCol = new TableColumn<>("Balance After");
            balCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[3]));
            balCol.setPrefWidth(100);
            table.getColumns().addAll(dateCol, descCol, amountCol, balCol);
            table.setItems(FXCollections.observableArrayList(rows));
            table.setStyle("-fx-font-size: 12px;");
            content.getChildren().add(table);

            // Reprint a receipt for a selected payment row
            List<com.tireshop.model.ChargeAccountPayment> finalRowPayments = rowPayments;
            Button printPaymentBtn = new Button("🖨 Print Payment Receipt");
            printPaymentBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
            printPaymentBtn.setDisable(true);
            printPaymentBtn.setOnAction(e -> {
                int idx = table.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < finalRowPayments.size() && finalRowPayments.get(idx) != null) {
                    showChargePaymentReceiptOptions(finalRowPayments.get(idx), customer);
                }
            });
            table.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                int idx = newIdx.intValue();
                printPaymentBtn.setDisable(idx < 0 || idx >= finalRowPayments.size() || finalRowPayments.get(idx) == null);
            });
            HBox tableButtons = new HBox(10);
            tableButtons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tableButtons.getChildren().add(printPaymentBtn);
            Label printHint = new Label("Select a payment row to reprint its receipt.");
            printHint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            tableButtons.getChildren().add(printHint);
            content.getChildren().add(tableButtons);
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    public void showAddVehicleDialog(Customer customer) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Add Vehicle"); dialog.setHeaderText("Add a new vehicle for " + customer.getFullName()); dialog.initOwner(stage);
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField makeField = new TextField(); makeField.setPromptText("Make");
        TextField modelField = new TextField(); modelField.setPromptText("Model");
        TextField yearField = new TextField(); yearField.setPromptText("Year");
        TextField licensePlateField = new TextField(); licensePlateField.setPromptText("License Plate");
        TextField vinField = new TextField(); vinField.setPromptText("VIN");
        TextField colorField = new TextField(); colorField.setPromptText("Color");
        grid.add(new Label("Make:"), 0, 0); grid.add(makeField, 1, 0);
        grid.add(new Label("Model:"), 0, 1); grid.add(modelField, 1, 1);
        grid.add(new Label("Year:"), 0, 2); grid.add(yearField, 1, 2);
        grid.add(new Label("License Plate:"), 0, 3); grid.add(licensePlateField, 1, 3);
        grid.add(new Label("VIN:"), 0, 4); grid.add(vinField, 1, 4);
        grid.add(new Label("Color:"), 0, 5); grid.add(colorField, 1, 5);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(makeField::requestFocus);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (makeField.getText().isEmpty() || modelField.getText().isEmpty() || yearField.getText().isEmpty()) {
                showAlert("Missing Information", "Please enter make, model, and year.", Alert.AlertType.ERROR);
                event.consume();
            } else {
                try { Integer.parseInt(yearField.getText()); } 
                catch (NumberFormatException ex) {
                    showAlert("Invalid Year", "Year must be a valid number.", Alert.AlertType.ERROR);
                    event.consume();
                }
            }
        });
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Vehicle vehicle = new Vehicle();
                vehicle.setMake(makeField.getText()); vehicle.setModel(modelField.getText());
                vehicle.setModelYear(Integer.parseInt(yearField.getText()));
                vehicle.setLicensePlate(licensePlateField.getText()); vehicle.setVin(vinField.getText()); vehicle.setColor(colorField.getText());
                vehicle.setCustomer(customer);
                VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
                return vehicleDao.save(vehicle);
            }
            return null;
        });
        dialog.showAndWait();
    }

    // Centralized method to handle Clock In/Out logic
    private void handleClockInOut() {
        ClockInOutCredentialDialog credentialDialog = new ClockInOutCredentialDialog(userService);
        Optional<User> clockingUserOptional = credentialDialog.showAndWait();

        if (clockingUserOptional.isPresent()) {
            User clockingUser = clockingUserOptional.get();
            if (clockingUser != null) { 
                if (userService.isUserClockedIn(clockingUser.getId())) {
                    TimeEntry entry = TimeClockDialog.showClockOutDialog(userService, clockingUser);
                    if (entry != null) {
                        showAlert("Clock Out Successful", "User '" + clockingUser.getUsername() + "' clocked out.", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Clock Out Cancelled", "Clock out process was not completed for user '" + clockingUser.getUsername() + "'.", Alert.AlertType.WARNING);
                    }
                } else {
                    TimeEntry entry = TimeClockDialog.showClockInDialog(userService, clockingUser);
                    if (entry != null) {
                        showAlert("Clock In Successful", "User '" + clockingUser.getUsername() + "' clocked in.", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Clock In Cancelled", "Clock in process was not completed for user '" + clockingUser.getUsername() + "'.", Alert.AlertType.WARNING);
                    }
                }
                // This update is for the menu item and new status bar button, tied to currentUser's login state for enablement.
                // The actual text of these buttons will be static "Clock In/Out".
                updateTimeClockMenu(); 
            } else {
                showAlert("Authentication Failed", "Invalid credentials for clock in/out.", Alert.AlertType.ERROR);
            }
        } else {
            System.out.println("[MainView] Clock In/Out credential dialog cancelled.");
        }
    }


    
    // Enhanced updateInventoryAlerts method with better fitting content
    private void updateInventoryAlerts() {
        if (inventoryService == null) {
            System.err.println("[MainView] inventoryService is null in updateInventoryAlerts. Cannot update.");
            if(inventoryAlertsContent != null) inventoryAlertsContent.getChildren().setAll(new Label("Error: Inventory service not available."));
            return;
        }
        if (inventoryAlertsContent == null) {
            System.err.println("[MainView] inventoryAlertsContent is null in updateInventoryAlerts. Cannot update UI.");
            return;
        }

        inventoryAlertsContent.getChildren().clear();
        inventoryAlertsContent.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-padding: 15px;");
        
        List<Product> lowStockProducts = this.inventoryService.getLowStockProducts();
        
        // Header with count and action button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        Label countLabel = new Label(lowStockProducts.size() + " Products Need Attention");
        countLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F57C00;");
        
        Button orderButton = new Button("Go to Inventory");
        orderButton.setStyle("-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-color: #FF9800; -fx-text-fill: white;");
        orderButton.setOnAction(e -> {
            if (tabPane.getTabs().contains(inventoryTab)) tabPane.getSelectionModel().select(inventoryTab);
            else showAlert("Access Denied", "You do not have access to Inventory.", Alert.AlertType.WARNING);
        });
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(countLabel, spacer, orderButton);
        
        if (lowStockProducts.isEmpty()) {
            Label noAlertsLabel = new Label("✅ All products are well stocked!");
            noAlertsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50; -fx-padding: 30px 20px; -fx-wrap-text: true;");
            inventoryAlertsContent.getChildren().addAll(headerBox, noAlertsLabel);
        } else {
            TableView<Product> lowStockTable = new TableView<>();
            lowStockTable.setItems(FXCollections.observableArrayList(lowStockProducts));
            lowStockTable.setStyle("-fx-font-size: 12px;");
            
            TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name")); 
            nameCol.setPrefWidth(160);
            
            TableColumn<Product, String> sizeCol = new TableColumn<>("Size");
            sizeCol.setCellValueFactory(new PropertyValueFactory<>("size")); 
            sizeCol.setPrefWidth(80);
            
            TableColumn<Product, Integer> stockCol = new TableColumn<>("Stock");
            stockCol.setCellValueFactory(new PropertyValueFactory<>("quantityInStock")); 
            stockCol.setPrefWidth(60);
            
            TableColumn<Product, Integer> reorderCol = new TableColumn<>("Reorder");
            reorderCol.setCellValueFactory(new PropertyValueFactory<>("reorderLevel")); 
            reorderCol.setPrefWidth(70);
            
            lowStockTable.getColumns().addAll(nameCol, sizeCol, stockCol, reorderCol);
            lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            // Add scroll pane for table that can handle much more content - full column height
            ScrollPane inventoryScrollPane = new ScrollPane(lowStockTable);
            inventoryScrollPane.setFitToWidth(true);
            inventoryScrollPane.setFitToHeight(true);
            inventoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            inventoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            inventoryScrollPane.setPrefHeight(580); // Much bigger - full column
            inventoryScrollPane.setMaxHeight(620);
            inventoryScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
            VBox.setVgrow(inventoryScrollPane, Priority.ALWAYS);
            
            inventoryAlertsContent.getChildren().addAll(headerBox, inventoryScrollPane);
        }
    }
    
    private void updateTiresSoldToday() {
        if (salesService == null) {
            System.err.println("[MainView] salesService is null in updateTiresSoldToday. Cannot update.");
            if (tiresSoldTodayContent != null) tiresSoldTodayContent.getChildren().setAll(new Label("Error: Sales service not available."));
            return;
        }
        if (tiresSoldTodayContent == null) {
            System.err.println("[MainView] tiresSoldTodayContent is null in updateTiresSoldToday. Cannot update UI.");
            return;
        }

        System.out.println("[DEBUG] updateTiresSoldToday called for date: " + LocalDate.now());
        
        // Get individual sale items for today instead of aggregated data
        SaleItemDao saleItemDao = new SaleItemDao(DatabaseManager.getSessionFactory());
        List<SaleItem> todaySaleItems = saleItemDao.findProductItemsBySaleDateRange(LocalDate.now(), LocalDate.now());
        
        System.out.println("[DEBUG] Total sale items today: " + todaySaleItems.size());
        
        // Filter for tire-related products (EXCLUDING VOIDED SALES!)
        List<SaleItem> tireSaleItems = todaySaleItems.stream()
            .filter(item -> item.getSale() != null && !item.getSale().isVoided()) // Exclude voided sales!
            .filter(item -> {
                if (item.getProduct() == null) return false;
                
                // Check category first
                if (item.getProduct().getCategory() != null && 
                   (item.getProduct().getCategory().toLowerCase().contains("tire") || 
                    item.getProduct().getCategory().toLowerCase().contains("tires"))) {
                    return true;
                }
                
                // Fallback: Check product name for tire-related keywords
                if (item.getProduct().getName() != null) {
                    String productName = item.getProduct().getName().toLowerCase();
                    return productName.contains("tire") || 
                           productName.contains("tires") ||
                           productName.contains("all-terrain") ||
                           productName.contains("performance") ||
                           productName.contains("winter") ||
                           productName.contains("summer") ||
                           productName.contains("all season") ||
                           productName.contains("mud terrain") ||
                           productName.contains("pilot sport") ||
                           productName.contains("michelin") ||
                           productName.contains("goodyear") ||
                           productName.contains("bridgestone") ||
                           productName.contains("continental") ||
                           productName.contains("mastercraft") ||
                           productName.contains("open country") ||
                           productName.contains("a/t") ||
                           productName.contains("m/t") ||
                           productName.contains("h/t") ||
                           productName.contains("courser") ||
                           productName.contains("crosswind");
                }
                
                return false;
            })
            .collect(java.util.stream.Collectors.toList());
        
        System.out.println("[DEBUG] Tire sale items found: " + tireSaleItems.size());
        
        tiresSoldTodayContent.getChildren().clear();
        tiresSoldTodayContent.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-padding: 15px;");
        
        // Calculate total tire count and revenue
        int totalTireCount = tireSaleItems.stream().mapToInt(item -> item.getQuantity()).sum();
        java.math.BigDecimal totalTireRevenue = tireSaleItems.stream()
            .map(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        System.out.println("[DEBUG] Total tire count: " + totalTireCount + ", Total tire revenue: " + totalTireRevenue);
        
        // Header with tire count and revenue
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        Label countLabel = new Label(totalTireCount + " Tires Sold Today");
        countLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9C27B0;");
        
        Button viewAllBtn = new Button("View Sales");
        viewAllBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-color: #9C27B0; -fx-text-fill: white;");
        viewAllBtn.setOnAction(e -> {
            if (tabPane.getTabs().contains(salesTab)) {
                tabPane.getSelectionModel().select(salesTab);
            } else {
                showAlert("Access Denied", "You do not have access to Sales.", Alert.AlertType.WARNING);
            }
        });
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(countLabel, spacer, viewAllBtn);
        
        if (tireSaleItems.isEmpty()) {
            Label noTiresLabel = new Label("🚗 No tires sold today yet.");
            noTiresLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 40px 20px;");
            tiresSoldTodayContent.getChildren().addAll(headerBox, noTiresLabel);
        } else {
            // Create scrollable content area for tire metrics and sales list - full column height
            ScrollPane tiresScrollPane = new ScrollPane();
            tiresScrollPane.setFitToWidth(true);
            tiresScrollPane.setFitToHeight(true);
            tiresScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            tiresScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            tiresScrollPane.setPrefHeight(580); // Much bigger - full column
            tiresScrollPane.setMaxHeight(620);
            tiresScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
            
            VBox scrollableContent = new VBox(10);
            scrollableContent.setPadding(new Insets(10));
            
            // Small, compact text metrics at the top 
            HBox compactMetricsBox = new HBox(30);
            compactMetricsBox.setAlignment(javafx.geometry.Pos.CENTER);
            compactMetricsBox.setPadding(new Insets(10, 20, 15, 20));
            compactMetricsBox.setStyle("-fx-background-color: #F8F0FF; -fx-background-radius: 8px; -fx-border-color: #9C27B0; -fx-border-width: 1px; -fx-border-radius: 8px;");
            
            // Compact tire count text
            HBox countText = new HBox(5);
            countText.setAlignment(javafx.geometry.Pos.CENTER);
            Label tiresLabel = new Label("Tires Sold:");
            tiresLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-weight: normal;");
            Label tiresValue = new Label(String.valueOf(totalTireCount));
            tiresValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #9C27B0;");
            countText.getChildren().addAll(tiresLabel, tiresValue);
            
            // Compact revenue text
            HBox revenueText = new HBox(5);
            revenueText.setAlignment(javafx.geometry.Pos.CENTER);
            Label revLabel = new Label("Revenue:");
            revLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-weight: normal;");
            Label revValue = new Label(String.format("$%.0f", totalTireRevenue));
            revValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #673AB7;");
            revenueText.getChildren().addAll(revLabel, revValue);
            
            compactMetricsBox.getChildren().addAll(countText, revenueText);
            scrollableContent.getChildren().add(compactMetricsBox);
            
            // Expanded tire sales list - show many more sales since we have space
            if (tireSaleItems.size() > 0) {
                Label recentSalesLabel = new Label("Today's Tire Sales (" + tireSaleItems.size() + " total):");
                recentSalesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9C27B0; -fx-padding: 10 0 8 0;");
                
                VBox recentSalesList = new VBox(3);
                recentSalesList.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8px; -fx-padding: 15px;");
                
                // Sort by sale time (most recent first) and show more items (up to 20)
                tireSaleItems.sort((a, b) -> b.getSale().getTimestamp().compareTo(a.getSale().getTimestamp()));
                List<SaleItem> recentItems = tireSaleItems.stream().limit(20).collect(java.util.stream.Collectors.toList());
                
                for (SaleItem item : recentItems) {
                    HBox saleRow = new HBox(8);
                    saleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    saleRow.setStyle("-fx-padding: 2px 5px; -fx-background-color: white; -fx-background-radius: 4px;");
                    
                    String time = item.getSale().getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String productName = item.getProduct().getName();
                    if (productName.length() > 35) {
                        productName = productName.substring(0, 32) + "...";
                    }
                    String qty = "×" + item.getQuantity();
                    String total = String.format("$%.0f", item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                    
                    Label timeLabel = new Label(time);
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-min-width: 35px;");
                    
                    Label productLabel = new Label(productName);
                    productLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
                    HBox.setHgrow(productLabel, Priority.ALWAYS);
                    
                    Label qtyLabel = new Label(qty);
                    qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9C27B0; -fx-font-weight: bold; -fx-min-width: 25px;");
                    
                    Label totalLabel = new Label(total);
                    totalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-min-width: 45px; -fx-text-alignment: right;");
                    
                    saleRow.getChildren().addAll(timeLabel, productLabel, qtyLabel, totalLabel);
                    recentSalesList.getChildren().add(saleRow);
                }
                
                scrollableContent.getChildren().addAll(recentSalesLabel, recentSalesList);
            }
            
            tiresScrollPane.setContent(scrollableContent);
            tiresSoldTodayContent.getChildren().addAll(headerBox, tiresScrollPane);
            VBox.setVgrow(tiresScrollPane, Priority.ALWAYS);
        }
    }
    
    private void showServiceHistoryDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Service History");
        dialog.setHeaderText("Service History for " + customer.getFullName());
        dialog.initOwner(stage);
        dialog.getDialogPane().setPrefSize(1000, 700);
        
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Get all vehicles for this customer
        VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
        List<Vehicle> vehicles = vehicleDao.findByCustomerId(customer.getId());
        
        if (vehicles.isEmpty()) {
            content.getChildren().add(new Label("No vehicles found for this customer."));
        } else {
            TabPane vehicleTabPane = new TabPane();
            vehicleTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            
            for (Vehicle vehicle : vehicles) {
                Tab vehicleTab = new Tab(vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getModelYear() + ")");
                
                VBox vehicleContent = new VBox(10);
                vehicleContent.setPadding(new Insets(10));
                
                // Vehicle info
                GridPane vehicleInfo = new GridPane();
                vehicleInfo.setHgap(10);
                vehicleInfo.setVgap(5);
                vehicleInfo.add(new Label("License Plate:"), 0, 0);
                vehicleInfo.add(new Label(vehicle.getLicensePlate() != null ? vehicle.getLicensePlate() : "N/A"), 1, 0);
                vehicleInfo.add(new Label("VIN:"), 0, 1);
                vehicleInfo.add(new Label(vehicle.getVin() != null ? vehicle.getVin() : "N/A"), 1, 1);
                
                TitledPane vehicleInfoPane = new TitledPane("Vehicle Information", vehicleInfo);
                vehicleInfoPane.setCollapsible(false);
                
                // Service history table
                TableView<ServiceRecord> serviceTable = new TableView<>();
                
                TableColumn<ServiceRecord, String> dateCol = new TableColumn<>("Date");
                dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getServiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
                dateCol.setPrefWidth(100);
                
                TableColumn<ServiceRecord, String> typeCol = new TableColumn<>("Service Type");
                typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getServiceType()));
                typeCol.setPrefWidth(150);
                
                TableColumn<ServiceRecord, String> descriptionCol = new TableColumn<>("Description");
                descriptionCol.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getDescription() != null ? data.getValue().getDescription() : ""));
                descriptionCol.setPrefWidth(300);
                
                TableColumn<ServiceRecord, Integer> mileageCol = new TableColumn<>("Mileage");
                mileageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(
                    data.getValue().getMileage()));
                mileageCol.setPrefWidth(80);
                
                TableColumn<ServiceRecord, String> technicianCol = new TableColumn<>("Technician");
                technicianCol.setCellValueFactory(data -> {
                    Technician tech = data.getValue().getTechnician();
                    return new SimpleStringProperty(tech != null ? 
                        tech.getFirstName() + " " + tech.getLastName() : "N/A");
                });
                technicianCol.setPrefWidth(150);
                
                TableColumn<ServiceRecord, String> costCol = new TableColumn<>("Total Cost");
                costCol.setCellValueFactory(data -> new SimpleStringProperty(
                    "$" + String.format("%.2f", data.getValue().getTotalCost())));
                costCol.setPrefWidth(100);
                
                TableColumn<ServiceRecord, String> nextServiceCol = new TableColumn<>("Next Service");
                nextServiceCol.setCellValueFactory(data -> {
                    LocalDateTime nextDate = data.getValue().getNextServiceDate();
                    Integer nextMileage = data.getValue().getNextServiceMileage();
                    String nextService = "";
                    if (nextDate != null) {
                        nextService = nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    if (nextMileage != null) {
                        nextService += (nextService.isEmpty() ? "" : " or ") + nextMileage + " miles";
                    }
                    return new SimpleStringProperty(nextService);
                });
                nextServiceCol.setPrefWidth(150);
                
                serviceTable.getColumns().addAll(dateCol, typeCol, descriptionCol, mileageCol, 
                                               technicianCol, costCol, nextServiceCol);
                
                // Load service history
                List<ServiceRecord> serviceHistory = vehicleServiceHistoryService.getVehicleServiceHistory(vehicle.getId());
                serviceTable.setItems(FXCollections.observableArrayList(serviceHistory));
                
                // Service recommendations
                HBox recommendationBox = new HBox(10);
                recommendationBox.setPadding(new Insets(10, 0, 0, 0));
                Button checkRecommendationsBtn = new Button("Check Service Recommendations");
                checkRecommendationsBtn.setOnAction(evt -> {
                    TextInputDialog mileageDialog = new TextInputDialog();
                    mileageDialog.setTitle("Current Mileage");
                    mileageDialog.setHeaderText("Enter current vehicle mileage");
                    mileageDialog.setContentText("Mileage:");
                    
                    Optional<String> mileageResult = mileageDialog.showAndWait();
                    if (mileageResult.isPresent()) {
                        try {
                            Integer currentMileage = Integer.parseInt(mileageResult.get());
                            showServiceRecommendations(vehicle, currentMileage);
                        } catch (NumberFormatException ex) {
                            showAlert("Invalid Input", "Please enter a valid mileage number.", Alert.AlertType.ERROR);
                        }
                    }
                });
                
                recommendationBox.getChildren().add(checkRecommendationsBtn);
                
                // Add selected service details pane
                TitledPane detailsPane = new TitledPane("Service Details", new Label("Select a service record to view details"));
                detailsPane.setCollapsible(true);
                detailsPane.setExpanded(false);
                
                serviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        VBox detailsContent = new VBox(5);
                        detailsContent.setPadding(new Insets(10));
                        
                        if (newSelection.getNotes() != null && !newSelection.getNotes().isEmpty()) {
                            detailsContent.getChildren().add(new Label("Notes: " + newSelection.getNotes()));
                        }
                        
                        if (newSelection.getWarrantyInfo() != null && !newSelection.getWarrantyInfo().isEmpty()) {
                            detailsContent.getChildren().add(new Label("Warranty: " + newSelection.getWarrantyInfo()));
                        }
                        
                        if (!newSelection.getServiceItems().isEmpty()) {
                            Label itemsLabel = new Label("Service Items:");
                            itemsLabel.setStyle("-fx-font-weight: bold;");
                            detailsContent.getChildren().add(itemsLabel);
                            
                            for (ServiceRecord.ServiceItem item : newSelection.getServiceItems()) {
                                String itemText = "• " + item.getItemName() + 
                                    " (Qty: " + item.getQuantity() + ") - $" + 
                                    String.format("%.2f", item.getUnitPrice());
                                if (item.getPartNumber() != null) {
                                    itemText += " [Part #: " + item.getPartNumber() + "]";
                                }
                                detailsContent.getChildren().add(new Label(itemText));
                            }
                        }
                        
                        detailsPane.setContent(detailsContent);
                        detailsPane.setExpanded(true);
                    }
                });
                
                vehicleContent.getChildren().addAll(vehicleInfoPane, serviceTable, recommendationBox, detailsPane);
                vehicleTab.setContent(vehicleContent);
                vehicleTabPane.getTabs().add(vehicleTab);
            }
            
            content.getChildren().add(vehicleTabPane);
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);
        
        dialog.showAndWait();
    }
    
    private void showServiceRecommendations(Vehicle vehicle, Integer currentMileage) {
        List<VehicleServiceHistoryService.ServiceRecommendation> recommendations = 
            vehicleServiceHistoryService.getServiceRecommendations(vehicle.getId(), currentMileage);
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Service Recommendations");
        dialog.setHeaderText("Recommended Services for " + vehicle.toString());
        dialog.initOwner(stage);
        
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        if (recommendations.isEmpty()) {
            content.getChildren().add(new Label("No service recommendations at this time."));
        } else {
            TableView<VehicleServiceHistoryService.ServiceRecommendation> recTable = new TableView<>();
            
            TableColumn<VehicleServiceHistoryService.ServiceRecommendation, String> typeCol = new TableColumn<>("Service Type");
            typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getServiceType()));
            typeCol.setPrefWidth(200);
            
            TableColumn<VehicleServiceHistoryService.ServiceRecommendation, String> reasonCol = new TableColumn<>("Reason");
            reasonCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReason()));
            reasonCol.setPrefWidth(150);
            
            TableColumn<VehicleServiceHistoryService.ServiceRecommendation, String> urgencyCol = new TableColumn<>("Urgency");
            urgencyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUrgency()));
            urgencyCol.setCellFactory(col -> new TableCell<VehicleServiceHistoryService.ServiceRecommendation, String>() {
                @Override
                protected void updateItem(String urgency, boolean empty) {
                    super.updateItem(urgency, empty);
                    if (empty || urgency == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(urgency);
                        if ("HIGH".equals(urgency)) {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        } else if ("MEDIUM".equals(urgency)) {
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: green;");
                        }
                    }
                }
            });
            urgencyCol.setPrefWidth(100);
            
            TableColumn<VehicleServiceHistoryService.ServiceRecommendation, String> lastServiceCol = new TableColumn<>("Last Service");
            lastServiceCol.setCellValueFactory(data -> {
                LocalDateTime lastDate = data.getValue().getLastServiceDate();
                return new SimpleStringProperty(lastDate != null ? 
                    lastDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Never");
            });
            lastServiceCol.setPrefWidth(120);
            
            recTable.getColumns().addAll(typeCol, reasonCol, urgencyCol, lastServiceCol);
            recTable.setItems(FXCollections.observableArrayList(recommendations));
            
            content.getChildren().addAll(
                new Label("Based on current mileage: " + currentMileage),
                recTable
            );
            
            // Add button to schedule appointment
            Button scheduleBtn = new Button("Schedule Appointment");
            scheduleBtn.setOnAction(e -> {
                if (tabPane.getTabs().contains(appointmentsTab)) {
                    dialog.close();
                    tabPane.getSelectionModel().select(appointmentsTab);
                } else {
                    showAlert("Access Denied", "You do not have access to Appointments.", Alert.AlertType.WARNING);
                }
            });
            content.getChildren().add(scheduleBtn);
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(600, 400);
        dialog.showAndWait();
    }
    
    /**
     * Refresh customer data across the application
     * This method can be called from external sources like SalesController
     * to update customer tables after adding new customers
     */
    public void refreshCustomerData() {
        // Refresh the customer table in the customers tab if it exists
        if (customerTable != null) {
            CustomerDao customerDao = new CustomerDao(DatabaseManager.getSessionFactory());

            // Remember current selection so we can restore it after reload
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            Long selectedId = selected != null ? selected.getId() : null;

            // Re-apply the active search filter instead of wiping it
            String searchTerm = customerSearchField != null ? customerSearchField.getText() : null;
            List<Customer> customers;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                customers = customerDao.search(searchTerm.trim());
            } else {
                customers = customerDao.findAll();
            }
            customerTable.setItems(FXCollections.observableArrayList(customers));

            // Restore selection (the selection listener reloads vehicles + buttons)
            if (selectedId != null) {
                for (Customer c : customerTable.getItems()) {
                    if (c.getId() != null && c.getId().equals(selectedId)) {
                        customerTable.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Refresh vehicle data for a specific customer
     * This method can be called from external sources like SalesController
     * to update vehicle tables after adding new vehicles
     */
    public void refreshVehicleData(Customer customer) {
        // Refresh the vehicle table in the customers tab if it exists and customer is selected
        if (vehicleTable != null && customer != null) {
            VehicleDao vehicleDao = new VehicleDao(DatabaseManager.getSessionFactory());
            List<Vehicle> vehicles = vehicleDao.findByCustomerId(customer.getId());
            vehicleTable.setItems(FXCollections.observableArrayList(vehicles));
        }
    }
    
    /**
     * Refresh both customer and vehicle data
     * Convenience method for comprehensive customer section refresh
     */
    public void refreshCustomerSection() {
        refreshCustomerData();
        // The selection listener keeps the vehicle table in sync; only clear it
        // if no customer ended up selected after the refresh.
        if (vehicleTable != null) {
            Customer selected = customerTable != null
                    ? customerTable.getSelectionModel().getSelectedItem() : null;
            if (selected == null) {
                vehicleTable.getItems().clear();
            }
        }
    }
    
    /**
     * Show/hide connection warning banner
     */
    public void showConnectionWarning(boolean show) {
        if (show) {
            // Update status bar indicator
            if (dbIndicator != null) {
                dbIndicator.setFill(javafx.scene.paint.Color.rgb(244, 67, 54)); // Red
                dbIndicator.setStroke(javafx.scene.paint.Color.rgb(198, 40, 40));
            }
            if (dbStatusLabel != null) {
                dbStatusLabel.setText("Database: DISCONNECTED - Reconnecting...");
                dbStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
            }
            
            // Show warning alert (only once)
            if (connectionWarningLabel == null || !connectionWarningLabel.isVisible()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Database Connection Lost");
                alert.setHeaderText("Connection to database server lost!");
                alert.setContentText("The application will automatically attempt to reconnect.\n\n" +
                                   "You can continue working, but changes may not save until connection is restored.\n\n" +
                                   "Check that the host computer is running and network is connected.");
                alert.show();
            }
        } else {
            // Update status bar to connected
            if (dbIndicator != null) {
                dbIndicator.setFill(javafx.scene.paint.Color.rgb(76, 175, 80)); // Green
                dbIndicator.setStroke(javafx.scene.paint.Color.rgb(46, 125, 50));
            }
            if (dbStatusLabel != null) {
                dbStatusLabel.setText("Database: Connected");
                dbStatusLabel.setStyle("-fx-text-fill: #2c3e50;");
            }
            
            // Show success notification
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Database Connection Restored");
            alert.setHeaderText("Connection restored!");
            alert.setContentText("Database connection has been re-established.\n" +
                               "All features are now fully operational.");
            alert.show();
            
            // Refresh all data
            refreshAllTabs();
        }
    }

} 