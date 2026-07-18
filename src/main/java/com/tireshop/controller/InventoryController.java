package com.tireshop.controller;

import com.tireshop.model.Product;
import com.tireshop.service.InventoryService;
import com.tireshop.service.TireService;
import com.tireshop.dao.ProductDao;
import com.tireshop.util.DatabaseManager;
import com.tireshop.util.ScannerServer;
import com.tireshop.view.MainView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for inventory management screen
 */
public class InventoryController {
    
    private static final Logger LOGGER = Logger.getLogger(InventoryController.class.getName());
    
    private InventoryService inventoryService;
    private TireService tireService;
    private ObservableList<Product> productList;
    private TableView<Product> productTable;
    private MainView mainView;
    private ScannerServer scannerServer;
    private com.tireshop.service.SupplierCatalogService supplierCatalogService;
    private com.tireshop.util.SettingsService settingsService;
    
    private ComboBox<String> sortByComboBox;
    private ComboBox<String> filterByComboBox;
    private CheckBox ascendingCheckBox;
    private TextField searchField;
    private VBox detailsPane;
    private ImageView tireImageView;
    private Label nameLabel;
    private Label manufacturerLabel;
    private Label sizeLabel;
    private Label typeLabel;
    private Label speedRatingLabel;
    private Label loadRatingLabel;
    private Label treadwearLabel;
    private Label tractionLabel;
    private Label temperatureLabel;
    private Label runFlatLabel;
    private Label warrantyLabel;
    private Label priceLabel;
    private Label stockLabel;
    
    private Button scanWithPhoneButton;
    private Label scannerUrlLabel;
    
    private Stage stage;
    
    /**
     * Constructor with InventoryService (should be the one used by MainView)
     * @param inventoryService The inventory service to use
     */
    public InventoryController(InventoryService inventoryService) {
        System.out.println("[CONSTRUCTOR] InventoryController instance created with injected InventoryService.");
        this.inventoryService = inventoryService;
        this.tireService = new TireService(this.inventoryService);
        this.settingsService = new com.tireshop.util.SettingsService();
        this.supplierCatalogService = new com.tireshop.service.SupplierCatalogService(this.settingsService);
    }
    
    /**
     * Set MainView reference for dashboard refreshing
     * @param mainView The MainView instance
     */
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }
    
    /**
     * Set the product table reference
     * @param table The TableView to use
     */
    public void setProductTable(TableView<Product> table) {
        this.productTable = table;
    }
    
    /**
     * Initialize the controller
     */
    public void initialize() {
        if (productTable == null) {
            LOGGER.warning("Product table not set before initialization!");
            return;
        }
        
        // Create UI controls
        createControls();
        
        // Set up components
        setupTable();
        setupSortingControls();
        setupFilteringControls();
        setupSearchField();
        setupDetailsPane();
        
        // Load initial data
        refreshProducts();
    }
    
    /**
     * Create UI controls
     */
    private void createControls() {
        sortByComboBox = new ComboBox<>();
        filterByComboBox = new ComboBox<>();
        ascendingCheckBox = new CheckBox("Ascending");
        searchField = new TextField();
        detailsPane = new VBox(10);
        tireImageView = new ImageView();
        
        nameLabel = new Label();
        manufacturerLabel = new Label();
        sizeLabel = new Label();
        typeLabel = new Label();
        speedRatingLabel = new Label();
        loadRatingLabel = new Label();
        treadwearLabel = new Label();
        tractionLabel = new Label();
        temperatureLabel = new Label();
        runFlatLabel = new Label();
        warrantyLabel = new Label();
        priceLabel = new Label();
        stockLabel = new Label();
        
        scanWithPhoneButton = new Button("Scan with Phone");
        scannerUrlLabel = new Label("Scanner URL: (Not Active)");
        scannerUrlLabel.setVisible(false);
    }
    
    /**
     * Set up the TableView columns
     */
    private void setupTable() {
        // Clear existing columns
        productTable.getColumns().clear();
        
        // Basic columns
        TableColumn<Product, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getId()));
        idColumn.setPrefWidth(50);
        
        TableColumn<Product, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getName()));
        nameColumn.setPrefWidth(200);
        
        TableColumn<Product, String> manufacturerColumn = new TableColumn<>("Brand");
        manufacturerColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getManufacturer()));
        manufacturerColumn.setPrefWidth(100);
        
        TableColumn<Product, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSize()));
        sizeColumn.setPrefWidth(100);
        
        // UTQG Ratings Group
        TableColumn<Product, String> utqgColumn = new TableColumn<>("UTQG Ratings");
        
        TableColumn<Product, Number> treadwearColumn = new TableColumn<>("Treadwear");
        treadwearColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getUtqgTreadwear()));
        treadwearColumn.setPrefWidth(80);
        treadwearColumn.setCellFactory(column -> new TableCell<Product, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Color code treadwear ratings
                    int value = item.intValue();
                    if (value >= 600) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;"); // Dark green for excellent
                    } else if (value >= 400) {
                        setStyle("-fx-text-fill: #388e3c;"); // Green for good
                    } else if (value >= 300) {
                        setStyle("-fx-text-fill: #f57c00;"); // Orange for average
                    } else {
                        setStyle("-fx-text-fill: #d32f2f;"); // Red for poor
                    }
                }
            }
        });
        
        TableColumn<Product, String> tractionColumn = new TableColumn<>("Traction");
        tractionColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getUtqgTraction()));
        tractionColumn.setPrefWidth(70);
        tractionColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color code traction ratings
                    switch (item) {
                        case "AA":
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        case "A":
                            setStyle("-fx-text-fill: #388e3c;");
                            break;
                        case "B":
                            setStyle("-fx-text-fill: #f57c00;");
                            break;
                        case "C":
                            setStyle("-fx-text-fill: #d32f2f;");
                            break;
                    }
                }
            }
        });
        
        TableColumn<Product, String> temperatureColumn = new TableColumn<>("Temp");
        temperatureColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getUtqgTemperature()));
        temperatureColumn.setPrefWidth(60);
        temperatureColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color code temperature ratings
                    switch (item) {
                        case "A":
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        case "B":
                            setStyle("-fx-text-fill: #f57c00;");
                            break;
                        case "C":
                            setStyle("-fx-text-fill: #d32f2f;");
                            break;
                    }
                }
            }
        });
        
        utqgColumn.getColumns().addAll(treadwearColumn, tractionColumn, temperatureColumn);
        
        // Performance columns
        TableColumn<Product, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTireType()));
        typeColumn.setPrefWidth(100);
        
        TableColumn<Product, String> speedRatingColumn = new TableColumn<>("Speed");
        speedRatingColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSpeedRating()));
        speedRatingColumn.setPrefWidth(60);
        speedRatingColumn.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<Product, String> loadRatingColumn = new TableColumn<>("Load");
        loadRatingColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getLoadRating()));
        loadRatingColumn.setPrefWidth(60);
        loadRatingColumn.setStyle("-fx-alignment: CENTER;");
        
        // Overall Rating Column (calculated)
        TableColumn<Product, String> ratingColumn = new TableColumn<>("Overall Rating");
        ratingColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            double rating = calculateOverallRating(p);
            return new SimpleStringProperty(String.format("%.1f★", rating));
        });
        ratingColumn.setPrefWidth(100);
        ratingColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    double rating = Double.parseDouble(item.replace("★", ""));
                    if (rating >= 4.5) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 14px;");
                    } else if (rating >= 3.5) {
                        setStyle("-fx-text-fill: #388e3c; -fx-font-size: 13px;");
                    } else if (rating >= 2.5) {
                        setStyle("-fx-text-fill: #f57c00; -fx-font-size: 12px;");
                    } else {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
                    }
                }
            }
        });
        
        TableColumn<Product, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getSellingPrice();
            return new SimpleStringProperty(price != null ? "$" + price : "");
        });
        priceColumn.setPrefWidth(80);
        
        TableColumn<Product, Number> stockColumn = new TableColumn<>("Stock");
        stockColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getQuantityInStock()));
        stockColumn.setPrefWidth(60);
        stockColumn.setCellFactory(column -> new TableCell<Product, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Set tire quantity text to black for better readability
                    setStyle("-fx-text-fill: black;");
                }
            }
        });
        
        // Add columns to table
        productTable.getColumns().addAll(
            idColumn, nameColumn, manufacturerColumn, sizeColumn, typeColumn,
            utqgColumn, speedRatingColumn, loadRatingColumn, ratingColumn, 
            priceColumn, stockColumn
        );
        
        // Enable multi-selection for comparison
        productTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Add selection listener
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (productTable.getSelectionModel().getSelectedItems().size() == 1) {
                    showProductDetails(newValue);
                }
            });
            
        // Add right-click context menu for quick sale creation
        setupContextMenu();
    }
    
    /**
     * Set up context menu for right-click functionality
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        // Add to New Sale menu item
        MenuItem addToNewSaleItem = new MenuItem("🛒 Add to New Sale");
        addToNewSaleItem.setOnAction(e -> {
            Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
            if (selectedProduct != null) {
                showAddToNewSaleDialog(selectedProduct);
            }
        });
        
        // Add to Existing Sale menu item (for future enhancement)
        MenuItem addToExistingSaleItem = new MenuItem("📋 Add to Existing Sale");
        addToExistingSaleItem.setOnAction(e -> {
            // TODO: Implement add to existing sale functionality
            showAlert("Coming Soon", "Add to existing sale functionality will be available soon!", Alert.AlertType.INFORMATION);
        });
        
        contextMenu.getItems().addAll(addToNewSaleItem, addToExistingSaleItem);
        
        // Set context menu on the table
        productTable.setContextMenu(contextMenu);
    }
    
    /**
     * Show dialog to add product to a new sale
     */
    private void showAddToNewSaleDialog(Product product) {
        // Block up front: products with no stock or no price cannot be sold
        if (product.getQuantityInStock() <= 0) {
            showAlert("Out of Stock", product.getName() + " has no quantity in stock. "
                + "Receive inventory before selling it.", Alert.AlertType.WARNING);
            return;
        }
        if (product.getSellingPrice() == null) {
            showAlert("No Price Set", product.getName() + " has no selling price. "
                + "Edit the product to set a price before selling it.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Add to New Sale");
        dialog.setHeaderText("Add " + product.getName() + " to New Sale");
        dialog.setResizable(true);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Create Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Product info
        Label productLabel = new Label("Product:");
        Label productNameLabel = new Label(product.getName() + " - " + product.getSize());
        productNameLabel.setStyle("-fx-font-weight: bold;");
        
        Label priceLabel = new Label("Price:");
        Label priceValueLabel = new Label("$" + (product.getSellingPrice() != null ? product.getSellingPrice() : "0.00"));
        priceValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        
        Label stockLabel = new Label("Available:");
        Label stockValueLabel = new Label(String.valueOf(product.getQuantityInStock()));
        if (product.getQuantityInStock() <= 0) {
            stockValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        } else if (product.getQuantityInStock() <= 5) {
            stockValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f57c00;");
        } else {
            stockValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        }
        
        // Quantity input
        Label quantityLabel = new Label("Quantity:");
        Spinner<Integer> quantitySpinner = new Spinner<>(1, Math.max(1, product.getQuantityInStock()), 1);
        quantitySpinner.setPrefWidth(100);
        
        // Total calculation
        Label totalLabel = new Label("Total:");
        Label totalValueLabel = new Label();
        totalValueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");
        
        // Update total when quantity changes
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (product.getSellingPrice() != null && newVal != null) {
                BigDecimal total = product.getSellingPrice().multiply(BigDecimal.valueOf(newVal));
                totalValueLabel.setText("$" + total);
            }
        });
        
        // Set initial total
        if (product.getSellingPrice() != null) {
            BigDecimal total = product.getSellingPrice().multiply(BigDecimal.valueOf(quantitySpinner.getValue()));
            totalValueLabel.setText("$" + total);
        }
        
        // Add to grid
        grid.add(productLabel, 0, 0);
        grid.add(productNameLabel, 1, 0);
        grid.add(priceLabel, 0, 1);
        grid.add(priceValueLabel, 1, 1);
        grid.add(stockLabel, 0, 2);
        grid.add(stockValueLabel, 1, 2);
        grid.add(quantityLabel, 0, 3);
        grid.add(quantitySpinner, 1, 3);
        grid.add(totalLabel, 0, 4);
        grid.add(totalValueLabel, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Focus on quantity spinner
        Platform.runLater(quantitySpinner::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(quantity -> {
            if (quantity > 0 && quantity <= product.getQuantityInStock()) {
                // Create new sale with the selected product
                createNewSaleWithProduct(product, quantity);
            } else {
                showAlert("Invalid Quantity", 
                    "Quantity must be between 1 and " + product.getQuantityInStock() + ".", 
                    Alert.AlertType.WARNING);
            }
        });
    }
    
    /**
     * Create a new sale with the specified product
     */
    private void createNewSaleWithProduct(Product product, int quantity) {
        try {
            // Get the main view to access sales controller
            if (mainView != null) {
                // Use the sales service to create a new sale
                com.tireshop.service.SalesService salesService = mainView.getSalesService();
                if (salesService != null) {
                    // Create sale without customer initially
                    com.tireshop.model.Sale newSale = salesService.createSale(null, null);
                    if (newSale != null) {
                        // Add the product to the sale
                        salesService.addProductToSale(newSale.getId(), product.getId(), quantity);
                        
                        // Refresh the main view to show the new sale
                        mainView.refreshAllTabs();
                        
                        // Show success message
                        showAlert("Success", 
                            "New sale created with " + product.getName() + " (Qty: " + quantity + ")", 
                            Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Could not create new sale.", Alert.AlertType.ERROR);
                    }
                } else {
                    showAlert("Error", "Sales service not available.", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Error", "Main view not available.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating new sale with product", e);
            showAlert("Error", "Failed to create sale: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Show an alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Calculate overall rating based on UTQG and other factors
     */
    private double calculateOverallRating(Product product) {
        double rating = 2.5; // Base rating
        
        // Treadwear rating (0-2 points)
        if (product.getUtqgTreadwear() != null) {
            int treadwear = product.getUtqgTreadwear();
            if (treadwear >= 600) rating += 2.0;
            else if (treadwear >= 500) rating += 1.5;
            else if (treadwear >= 400) rating += 1.0;
            else if (treadwear >= 300) rating += 0.5;
        }
        
        // Traction rating (0-1.5 points)
        if (product.getUtqgTraction() != null) {
            switch (product.getUtqgTraction()) {
                case "AA": rating += 1.5; break;
                case "A": rating += 1.0; break;
                case "B": rating += 0.5; break;
            }
        }
        
        // Temperature rating (0-1 point)
        if (product.getUtqgTemperature() != null) {
            switch (product.getUtqgTemperature()) {
                case "A": rating += 1.0; break;
                case "B": rating += 0.5; break;
            }
        }
        
        // Cap at 5.0
        return Math.min(rating, 5.0);
    }
    
    /**
     * Set up sorting controls
     */
    private void setupSortingControls() {
        // Initialize sort options
        sortByComboBox.setItems(FXCollections.observableArrayList(
            "Name", "Price", "Manufacturer", "Size", "Type", 
            "Speed Rating", "Treadwear", "Stock", "Overall Rating",
            "Best Value", "Longest Lasting", "Best Performance"
        ));
        sortByComboBox.setValue("Name");
        
        // Add listeners
        sortByComboBox.setOnAction(e -> applySorting());
        ascendingCheckBox.setOnAction(e -> applySorting());
        ascendingCheckBox.setSelected(true);
    }
    
    /**
     * Set up filtering controls
     */
    private void setupFilteringControls() {
        // Initialize filter options
        filterByComboBox.setItems(FXCollections.observableArrayList(
            "All", "All-Season", "Winter", "Summer", "All-Terrain",
            "Performance", "Touring", "Highway"
        ));
        filterByComboBox.setValue("All");
        
        // Add listener
        filterByComboBox.setOnAction(e -> applyFilters());
    }
    
    /**
     * Set up search field
     */
    private void setupSearchField() {
        searchField.setPromptText("Search by name, size (275/65), brand...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() >= 2 || newValue.isEmpty()) {
                searchProducts(newValue);
            }
        });
    }
    
    /**
     * Set up details pane
     */
    private void setupDetailsPane() {
        detailsPane.setVisible(false);
        tireImageView.setFitWidth(200);
        tireImageView.setFitHeight(200);
        tireImageView.setPreserveRatio(true);
    }
    
    /**
     * Apply current sorting settings
     */
    private void applySorting() {
        String sortBy = sortByComboBox.getValue();
        boolean ascending = ascendingCheckBox.isSelected();
        
        List<Product> products = new ArrayList<>(productList);
        
        switch (sortBy) {
            case "Name":
                products.sort((a, b) -> ascending ? 
                    a.getName().compareToIgnoreCase(b.getName()) : 
                    b.getName().compareToIgnoreCase(a.getName()));
                break;
            case "Price":
                products.sort((a, b) -> {
                    BigDecimal priceA = a.getSellingPrice() != null ? a.getSellingPrice() : BigDecimal.ZERO;
                    BigDecimal priceB = b.getSellingPrice() != null ? b.getSellingPrice() : BigDecimal.ZERO;
                    return ascending ? priceA.compareTo(priceB) : priceB.compareTo(priceA);
                });
                break;
            case "Manufacturer":
                products.sort((a, b) -> {
                    String mfgA = a.getManufacturer() != null ? a.getManufacturer() : "";
                    String mfgB = b.getManufacturer() != null ? b.getManufacturer() : "";
                    return ascending ? mfgA.compareToIgnoreCase(mfgB) : mfgB.compareToIgnoreCase(mfgA);
                });
                break;
            case "Size":
                products.sort((a, b) -> {
                    String sizeA = a.getSize() != null ? a.getSize() : "";
                    String sizeB = b.getSize() != null ? b.getSize() : "";
                    return ascending ? sizeA.compareToIgnoreCase(sizeB) : sizeB.compareToIgnoreCase(sizeA);
                });
                break;
            case "Type":
                products.sort((a, b) -> {
                    String typeA = a.getTireType() != null ? a.getTireType() : "";
                    String typeB = b.getTireType() != null ? b.getTireType() : "";
                    return ascending ? typeA.compareToIgnoreCase(typeB) : typeB.compareToIgnoreCase(typeA);
                });
                break;
            case "Speed Rating":
                products.sort((a, b) -> {
                    String speedA = a.getSpeedRating() != null ? a.getSpeedRating() : "";
                    String speedB = b.getSpeedRating() != null ? b.getSpeedRating() : "";
                    return ascending ? speedA.compareToIgnoreCase(speedB) : speedB.compareToIgnoreCase(speedA);
                });
                break;
            case "Treadwear":
                products.sort((a, b) -> {
                    Integer wearA = a.getUtqgTreadwear() != null ? a.getUtqgTreadwear() : 0;
                    Integer wearB = b.getUtqgTreadwear() != null ? b.getUtqgTreadwear() : 0;
                    return ascending ? wearA.compareTo(wearB) : wearB.compareTo(wearA);
                });
                break;
            case "Stock":
                products.sort((a, b) -> ascending ? 
                    Integer.compare(a.getQuantityInStock(), b.getQuantityInStock()) : 
                    Integer.compare(b.getQuantityInStock(), a.getQuantityInStock()));
                break;
            case "Overall Rating":
                products.sort((a, b) -> {
                    double ratingA = calculateOverallRating(a);
                    double ratingB = calculateOverallRating(b);
                    return ascending ? Double.compare(ratingA, ratingB) : Double.compare(ratingB, ratingA);
                });
                break;
            case "Best Value":
                // Sort by rating/price ratio (best rated for the price)
                products.sort((a, b) -> {
                    double valueA = calculateValueScore(a);
                    double valueB = calculateValueScore(b);
                    return Double.compare(valueB, valueA); // Always descending for "best"
                });
                break;
            case "Longest Lasting":
                // Sort by treadwear rating
                products.sort((a, b) -> {
                    Integer wearA = a.getUtqgTreadwear() != null ? a.getUtqgTreadwear() : 0;
                    Integer wearB = b.getUtqgTreadwear() != null ? b.getUtqgTreadwear() : 0;
                    return wearB.compareTo(wearA); // Always descending for "best"
                });
                break;
            case "Best Performance":
                // Sort by combination of speed rating, traction, and temperature
                products.sort((a, b) -> {
                    double perfA = calculatePerformanceScore(a);
                    double perfB = calculatePerformanceScore(b);
                    return Double.compare(perfB, perfA); // Always descending for "best"
                });
                break;
        }
        
        productTable.setItems(FXCollections.observableArrayList(products));
    }
    
    /**
     * Calculate value score (rating per dollar)
     */
    private double calculateValueScore(Product product) {
        double rating = calculateOverallRating(product);
        BigDecimal price = product.getSellingPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        // Return rating per $100
        return rating / (price.doubleValue() / 100.0);
    }
    
    /**
     * Calculate performance score based on speed, traction, and temperature ratings
     */
    private double calculatePerformanceScore(Product product) {
        double score = 0;
        
        // Speed rating score
        if (product.getSpeedRating() != null) {
            switch (product.getSpeedRating()) {
                case "Y": score += 3.0; break;
                case "W": score += 2.5; break;
                case "V": score += 2.0; break;
                case "H": score += 1.5; break;
                case "T": score += 1.0; break;
                case "S": score += 0.5; break;
            }
        }
        
        // Traction score
        if (product.getUtqgTraction() != null) {
            switch (product.getUtqgTraction()) {
                case "AA": score += 2.0; break;
                case "A": score += 1.5; break;
                case "B": score += 1.0; break;
                case "C": score += 0.5; break;
            }
        }
        
        // Temperature score
        if (product.getUtqgTemperature() != null) {
            switch (product.getUtqgTemperature()) {
                case "A": score += 1.0; break;
                case "B": score += 0.5; break;
            }
        }
        
        return score;
    }
    
    /**
     * Apply current filters
     */
    private void applyFilters() {
        String filterValue = filterByComboBox.getValue();
        if (filterValue.equals("All")) {
            refreshProducts();
        } else {
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", filterValue);
            List<Product> filteredProducts = tireService.filterTires(filters);
            productTable.setItems(FXCollections.observableArrayList(filteredProducts));
        }
    }
    
    /**
     * Show detailed information for a selected product
     */
    private void showProductDetails(Product product) {
        if (product == null) {
            detailsPane.setVisible(false);
            return;
        }
        
        detailsPane.setVisible(true);
        detailsPane.getChildren().clear();
        detailsPane.setPadding(new Insets(15));
        detailsPane.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px;");
        
        // Title section
        Label titleLabel = new Label(product.getName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label mfgLabel = new Label(product.getManufacturer() + " - " + product.getSize());
        mfgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        
        // Overall rating
        double rating = calculateOverallRating(product);
        Label ratingLabel = new Label(String.format("Overall Rating: %.1f★", rating));
        ratingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " + 
            (rating >= 4.0 ? "-fx-text-fill: #2e7d32;" : 
             rating >= 3.0 ? "-fx-text-fill: #388e3c;" : 
             rating >= 2.0 ? "-fx-text-fill: #f57c00;" : "-fx-text-fill: #d32f2f;"));
        
        detailsPane.getChildren().addAll(titleLabel, mfgLabel, ratingLabel, new Separator());
        
        // UTQG Ratings section
        Label utqgHeader = new Label("UTQG Ratings");
        utqgHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        detailsPane.getChildren().add(utqgHeader);
        
        GridPane utqgGrid = new GridPane();
        utqgGrid.setHgap(20);
        utqgGrid.setVgap(5);
        
        // Treadwear
        Label treadwearLabel = new Label("Treadwear:");
        Label treadwearValue = new Label(product.getUtqgTreadwear() != null ? 
            product.getUtqgTreadwear().toString() : "N/A");
        if (product.getUtqgTreadwear() != null) {
            String treadwearDesc = getTreadwearDescription(product.getUtqgTreadwear());
            treadwearValue.setText(product.getUtqgTreadwear() + " - " + treadwearDesc);
            treadwearValue.setStyle(getTreadwearStyle(product.getUtqgTreadwear()));
        }
        
        // Traction
        Label tractionLabel = new Label("Traction:");
        Label tractionValue = new Label(product.getUtqgTraction() != null ? 
            product.getUtqgTraction() : "N/A");
        if (product.getUtqgTraction() != null) {
            String tractionDesc = getTractionDescription(product.getUtqgTraction());
            tractionValue.setText(product.getUtqgTraction() + " - " + tractionDesc);
            tractionValue.setStyle(getTractionStyle(product.getUtqgTraction()));
        }
        
        // Temperature
        Label tempLabel = new Label("Temperature:");
        Label tempValue = new Label(product.getUtqgTemperature() != null ? 
            product.getUtqgTemperature() : "N/A");
        if (product.getUtqgTemperature() != null) {
            String tempDesc = getTemperatureDescription(product.getUtqgTemperature());
            tempValue.setText(product.getUtqgTemperature() + " - " + tempDesc);
            tempValue.setStyle(getTemperatureStyle(product.getUtqgTemperature()));
        }
        
        utqgGrid.add(treadwearLabel, 0, 0);
        utqgGrid.add(treadwearValue, 1, 0);
        utqgGrid.add(tractionLabel, 0, 1);
        utqgGrid.add(tractionValue, 1, 1);
        utqgGrid.add(tempLabel, 0, 2);
        utqgGrid.add(tempValue, 1, 2);
        
        detailsPane.getChildren().addAll(utqgGrid, new Separator());
        
        // Performance specs
        Label perfHeader = new Label("Performance Specifications");
        perfHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        detailsPane.getChildren().add(perfHeader);
        
        GridPane perfGrid = new GridPane();
        perfGrid.setHgap(20);
        perfGrid.setVgap(5);
        
        perfGrid.add(new Label("Type:"), 0, 0);
        perfGrid.add(new Label(product.getTireType() != null ? product.getTireType() : "N/A"), 1, 0);
        
        perfGrid.add(new Label("Speed Rating:"), 0, 1);
        String speedDesc = product.getSpeedRating() != null ? 
            product.getSpeedRating() + " - " + getSpeedRatingDescription(product.getSpeedRating()) : "N/A";
        perfGrid.add(new Label(speedDesc), 1, 1);
        
        perfGrid.add(new Label("Load Rating:"), 0, 2);
        perfGrid.add(new Label(product.getLoadRating() != null ? product.getLoadRating() : "N/A"), 1, 2);
        
        perfGrid.add(new Label("Run Flat:"), 0, 3);
        perfGrid.add(new Label(product.getRunFlat() != null ? 
            (product.getRunFlat() ? "Yes" : "No") : "N/A"), 1, 3);
        
        detailsPane.getChildren().addAll(perfGrid, new Separator());
        
        // Pricing and stock
        Label priceHeader = new Label("Pricing & Availability");
        priceHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        detailsPane.getChildren().add(priceHeader);
        
        GridPane priceGrid = new GridPane();
        priceGrid.setHgap(20);
        priceGrid.setVgap(5);
        
        priceGrid.add(new Label("Price:"), 0, 0);
        Label priceValueLabel = new Label(product.getSellingPrice() != null ? 
            String.format("$%.2f", product.getSellingPrice()) : "N/A");
        priceValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        priceGrid.add(priceValueLabel, 1, 0);
        
        priceGrid.add(new Label("In Stock:"), 0, 1);
        Label stockValueLabel = new Label(String.valueOf(product.getQuantityInStock()));
        // Set tire quantity text to black for better readability
        stockValueLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        priceGrid.add(stockValueLabel, 1, 1);
        
        // Value score
        double valueScore = calculateValueScore(product);
        priceGrid.add(new Label("Value Score:"), 0, 2);
        Label valueLabel = new Label(String.format("%.1f/5.0", Math.min(valueScore, 5.0)));
        valueLabel.setStyle("-fx-font-weight: bold;");
        priceGrid.add(valueLabel, 1, 2);
        
        detailsPane.getChildren().addAll(priceGrid, new Separator());
        
        // Recommendations
        Label recHeader = new Label("Recommendations");
        recHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        detailsPane.getChildren().add(recHeader);
        
        VBox recBox = new VBox(5);
        recBox.setPadding(new Insets(5));
        recBox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 3px;");
        
        // Generate recommendations based on tire characteristics
        List<String> recommendations = generateRecommendations(product);
        for (String rec : recommendations) {
            Label recLabel = new Label("• " + rec);
            recLabel.setWrapText(true);
            recLabel.setStyle("-fx-font-size: 12px;");
            recBox.getChildren().add(recLabel);
        }
        
        detailsPane.getChildren().add(recBox);
    }
    
    private String getTreadwearDescription(int treadwear) {
        if (treadwear >= 600) return "Excellent longevity";
        if (treadwear >= 500) return "Very good longevity";
        if (treadwear >= 400) return "Good longevity";
        if (treadwear >= 300) return "Average longevity";
        return "Below average longevity";
    }
    
    private String getTreadwearStyle(int treadwear) {
        if (treadwear >= 600) return "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
        if (treadwear >= 400) return "-fx-text-fill: #388e3c;";
        if (treadwear >= 300) return "-fx-text-fill: #f57c00;";
        return "-fx-text-fill: #d32f2f;";
    }
    
    private String getTractionDescription(String traction) {
        switch (traction) {
            case "AA": return "Superior wet traction";
            case "A": return "Excellent wet traction";
            case "B": return "Good wet traction";
            case "C": return "Marginal wet traction";
            default: return "";
        }
    }
    
    private String getTractionStyle(String traction) {
        switch (traction) {
            case "AA": return "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
            case "A": return "-fx-text-fill: #388e3c;";
            case "B": return "-fx-text-fill: #f57c00;";
            case "C": return "-fx-text-fill: #d32f2f;";
            default: return "";
        }
    }
    
    private String getTemperatureDescription(String temp) {
        switch (temp) {
            case "A": return "Excellent heat resistance";
            case "B": return "Good heat resistance";
            case "C": return "Marginal heat resistance";
            default: return "";
        }
    }
    
    private String getTemperatureStyle(String temp) {
        switch (temp) {
            case "A": return "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
            case "B": return "-fx-text-fill: #f57c00;";
            case "C": return "-fx-text-fill: #d32f2f;";
            default: return "";
        }
    }
    
    private String getSpeedRatingDescription(String speedRating) {
        switch (speedRating) {
            case "Y": return "186+ mph";
            case "W": return "168 mph";
            case "V": return "149 mph";
            case "H": return "130 mph";
            case "T": return "118 mph";
            case "S": return "112 mph";
            case "R": return "106 mph";
            case "Q": return "99 mph";
            default: return "";
        }
    }
    
    private List<String> generateRecommendations(Product product) {
        List<String> recommendations = new ArrayList<>();
        
        // Based on treadwear
        if (product.getUtqgTreadwear() != null) {
            if (product.getUtqgTreadwear() >= 600) {
                recommendations.add("Excellent choice for high-mileage drivers");
            } else if (product.getUtqgTreadwear() < 300) {
                recommendations.add("Consider for performance driving, but expect shorter lifespan");
            }
        }
        
        // Based on tire type
        if (product.getTireType() != null) {
            switch (product.getTireType()) {
                case "All-Season":
                    recommendations.add("Good for year-round use in moderate climates");
                    break;
                case "Winter":
                    recommendations.add("Essential for snow and ice conditions");
                    break;
                case "Summer":
                    recommendations.add("Best performance in warm, dry conditions");
                    break;
                case "All-Terrain":
                    recommendations.add("Ideal for mixed on/off-road driving");
                    break;
            }
        }
        
        // Based on ratings
        double rating = calculateOverallRating(product);
        if (rating >= 4.5) {
            recommendations.add("Top-rated tire with excellent overall performance");
        } else if (rating >= 3.5) {
            recommendations.add("Well-balanced tire for most driving needs");
        }
        
        // Based on value
        double valueScore = calculateValueScore(product);
        if (valueScore >= 3.0) {
            recommendations.add("Great value for the price");
        }
        
        return recommendations;
    }
    
    /**
     * Refresh the product list
     * PRESERVES active search filter to prevent auto-refresh from clearing searches
     */
    public void refreshProducts() {
        // Check if there's an active search filter
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            // Re-apply the search instead of showing all products
            String currentSearch = searchField.getText();
            System.out.println("[InventoryController] Refresh with active search: " + currentSearch);
            searchProducts(currentSearch);
        } else {
            // No search active, show all products
            productList = FXCollections.observableArrayList(inventoryService.getAllProducts());
            productTable.setItems(productList);
        }
    }
    
    /**
     * Search products by name or description
     */
    public void searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Only load all products if search is actually empty
            productList = FXCollections.observableArrayList(inventoryService.getAllProducts());
            productTable.setItems(productList);
        } else {
            // Search and display filtered results
            productList = FXCollections.observableArrayList(
                    inventoryService.searchProducts(searchTerm));
            productTable.setItems(productList);
            System.out.println("[InventoryController] Search results: " + productList.size() + " products for '" + searchTerm + "'");
        }
    }
    
    /**
     * Compare selected tires
     */
    @FXML
    private void compareSelectedTires() {
        ObservableList<Product> selectedProducts = productTable.getSelectionModel().getSelectedItems();
        if (selectedProducts.size() < 2) {
            showAlert(Alert.AlertType.INFORMATION, "Selection Needed", "Please select at least two tires to compare");
            return;
        }
        if (selectedProducts.size() > 4) {
            showAlert(Alert.AlertType.INFORMATION, "Too Many Selected", "Please select up to 4 tires to compare");
            return;
        }
        
        showComparisonDialog(new ArrayList<>(selectedProducts));
    }
    
    /**
     * Show tire comparison dialog
     */
    private void showComparisonDialog(List<Product> products) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tire Comparison");
        dialog.setHeaderText("Compare Selected Tires");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(800);
        scrollPane.setPrefViewportHeight(600);
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");
        
        // Header row
        int col = 0;
        Label headerLabel = new Label("Feature");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(headerLabel, col++, 0);
        
        for (Product product : products) {
            VBox productHeader = new VBox(5);
            Label nameLabel = new Label(product.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(150);
            
            Label sizeLabel = new Label(product.getSize());
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            
            productHeader.getChildren().addAll(nameLabel, sizeLabel);
            grid.add(productHeader, col++, 0);
        }
        
        int row = 1;
        
        // Add separator
        for (int i = 0; i <= products.size(); i++) {
            Separator sep = new Separator();
            grid.add(sep, i, row);
        }
        row++;
        
        // Basic Information Section
        addComparisonSectionHeader(grid, "Basic Information", row++, products.size() + 1);
        
        // Manufacturer
        addComparisonRow(grid, "Brand", row++, products, p -> p.getManufacturer());
        
        // Type
        addComparisonRow(grid, "Type", row++, products, p -> p.getTireType());
        
        // Price
        addComparisonRow(grid, "Price", row++, products, p -> 
            p.getSellingPrice() != null ? String.format("$%.2f", p.getSellingPrice()) : "N/A",
            (label, product) -> {
                if (product.getSellingPrice() != null) {
                    // Find lowest price
                    BigDecimal lowestPrice = products.stream()
                        .map(Product::getSellingPrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                    
                    if (product.getSellingPrice().equals(lowestPrice)) {
                        label.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    }
                }
            });
        
        // Stock
        addComparisonRow(grid, "In Stock", row++, products, p -> String.valueOf(p.getQuantityInStock()),
            (label, product) -> {
                // Set tire quantity text to black for better readability
                label.setStyle("-fx-text-fill: black;");
            });
        
        // UTQG Ratings Section
        row++;
        addComparisonSectionHeader(grid, "UTQG Ratings", row++, products.size() + 1);
        
        // Treadwear
        addComparisonRow(grid, "Treadwear", row++, products, p -> 
            p.getUtqgTreadwear() != null ? p.getUtqgTreadwear().toString() : "N/A",
            (label, product) -> {
                if (product.getUtqgTreadwear() != null) {
                    label.setStyle(getTreadwearStyle(product.getUtqgTreadwear()));
                }
            });
        
        // Traction
        addComparisonRow(grid, "Traction", row++, products, p -> 
            p.getUtqgTraction() != null ? p.getUtqgTraction() : "N/A",
            (label, product) -> {
                if (product.getUtqgTraction() != null) {
                    label.setStyle(getTractionStyle(product.getUtqgTraction()));
                }
            });
        
        // Temperature
        addComparisonRow(grid, "Temperature", row++, products, p -> 
            p.getUtqgTemperature() != null ? p.getUtqgTemperature() : "N/A",
            (label, product) -> {
                if (product.getUtqgTemperature() != null) {
                    label.setStyle(getTemperatureStyle(product.getUtqgTemperature()));
                }
            });
        
        // Performance Section
        row++;
        addComparisonSectionHeader(grid, "Performance", row++, products.size() + 1);
        
        // Speed Rating
        addComparisonRow(grid, "Speed Rating", row++, products, p -> {
            if (p.getSpeedRating() != null) {
                return p.getSpeedRating() + " - " + getSpeedRatingDescription(p.getSpeedRating());
            }
            return "N/A";
        });
        
        // Load Rating
        addComparisonRow(grid, "Load Rating", row++, products, p -> 
            p.getLoadRating() != null ? p.getLoadRating() : "N/A");
        
        // Run Flat
        addComparisonRow(grid, "Run Flat", row++, products, p -> 
            p.getRunFlat() != null ? (p.getRunFlat() ? "Yes" : "No") : "N/A");
        
        // Ratings Section
        row++;
        addComparisonSectionHeader(grid, "Ratings & Scores", row++, products.size() + 1);
        
        // Overall Rating
        addComparisonRow(grid, "Overall Rating", row++, products, p -> {
            double rating = calculateOverallRating(p);
            return String.format("%.1f★", rating);
        }, (label, product) -> {
            double rating = calculateOverallRating(product);
            if (rating >= 4.0) {
                label.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else if (rating >= 3.0) {
                label.setStyle("-fx-text-fill: #388e3c; -fx-font-size: 13px;");
            } else {
                label.setStyle("-fx-text-fill: #f57c00; -fx-font-size: 12px;");
            }
        });
        
        // Value Score
        addComparisonRow(grid, "Value Score", row++, products, p -> {
            double value = calculateValueScore(p);
            return String.format("%.1f/5.0", Math.min(value, 5.0));
        });
        
        // Performance Score
        addComparisonRow(grid, "Performance Score", row++, products, p -> {
            double perf = calculatePerformanceScore(p);
            return String.format("%.1f/6.0", perf);
        });
        
        // Winner badges
        row++;
        Separator finalSep = new Separator();
        GridPane.setColumnSpan(finalSep, products.size() + 1);
        grid.add(finalSep, 0, row++);
        
        Label winnerLabel = new Label("Best Choice:");
        winnerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(winnerLabel, 0, row);
        
        // Determine winners
        col = 1;
        for (Product product : products) {
            VBox badges = new VBox(3);
            
            // Best overall
            if (isBestOverall(product, products)) {
                Label badge = new Label("🏆 Best Overall");
                badge.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
                badges.getChildren().add(badge);
            }
            
            // Best value
            if (isBestValue(product, products)) {
                Label badge = new Label("💰 Best Value");
                badge.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                badges.getChildren().add(badge);
            }
            
            // Longest lasting
            if (isLongestLasting(product, products)) {
                Label badge = new Label("⏱️ Longest Lasting");
                badge.setStyle("-fx-font-weight: bold; -fx-text-fill: #9C27B0;");
                badges.getChildren().add(badge);
            }
            
            grid.add(badges, col++, row);
        }
        
        scrollPane.setContent(grid);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(850, 650);
        
        dialog.showAndWait();
    }
    
    private void addComparisonSectionHeader(GridPane grid, String title, int row, int colspan) {
        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1976D2; -fx-padding: 10 0 5 0;");
        GridPane.setColumnSpan(sectionLabel, colspan);
        grid.add(sectionLabel, 0, row);
    }
    
    private void addComparisonRow(GridPane grid, String feature, int row, List<Product> products, 
                                 java.util.function.Function<Product, String> valueExtractor) {
        addComparisonRow(grid, feature, row, products, valueExtractor, null);
    }
    
    private void addComparisonRow(GridPane grid, String feature, int row, List<Product> products, 
                                 java.util.function.Function<Product, String> valueExtractor,
                                 java.util.function.BiConsumer<Label, Product> styleApplier) {
        Label featureLabel = new Label(feature + ":");
        featureLabel.setStyle("-fx-font-weight: bold;");
        grid.add(featureLabel, 0, row);
        
        int col = 1;
        for (Product product : products) {
            Label valueLabel = new Label(valueExtractor.apply(product));
            if (styleApplier != null) {
                styleApplier.accept(valueLabel, product);
            }
            grid.add(valueLabel, col++, row);
        }
    }
    
    private boolean isBestOverall(Product product, List<Product> products) {
        double productRating = calculateOverallRating(product);
        return products.stream()
            .mapToDouble(this::calculateOverallRating)
            .max()
            .orElse(0) == productRating;
    }
    
    private boolean isBestValue(Product product, List<Product> products) {
        double productValue = calculateValueScore(product);
        return products.stream()
            .mapToDouble(this::calculateValueScore)
            .max()
            .orElse(0) == productValue;
    }
    
    private boolean isLongestLasting(Product product, List<Product> products) {
        if (product.getUtqgTreadwear() == null) return false;
        int productTreadwear = product.getUtqgTreadwear();
        return products.stream()
            .map(Product::getUtqgTreadwear)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0) == productTreadwear;
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        if (this.stage != null) {
            alert.initOwner(this.stage);
        }
        alert.showAndWait();
    }
    
    /**
     * Show dialog to add stock to inventory
     * @param product The product to add stock to
     * @param owner The owner window
     */
    public void showAddStockDialog(Product product, Stage owner) {
        if (product == null) return;
        
        // Create dialog
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Add Stock");
        dialog.setHeaderText("Add inventory for: " + product.getName());
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 1000, 1);
        quantitySpinner.setEditable(true);
        
        grid.add(new Label("Current stock:"), 0, 0);
        grid.add(new Label(String.valueOf(product.getQuantityInStock())), 1, 0);
        grid.add(new Label("Quantity to add:"), 0, 1);
        grid.add(quantitySpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return quantitySpinner.getValue();
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(quantity -> {
            inventoryService.addInventory(product.getId(), quantity);
            refreshProducts();
            
            // Refresh all relevant tabs after inventory change
            if (mainView != null) {
                mainView.refreshTabsForDataChange("product");
            }
        });
    }
    
    /**
     * Show dialog to adjust product price
     * @param product The product to adjust price for
     * @param owner The owner window
     */
    public void showAdjustPriceDialog(Product product, Stage owner) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");
        dialog.setHeaderText("Edit " + product.getName());
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create tabbed pane for better organization
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Basic Info Tab
        Tab basicTab = new Tab("Basic Information");
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(10);
        basicGrid.setVgap(10);
        basicGrid.setPadding(new javafx.geometry.Insets(20));
        
        // Basic fields
        TextField nameField = new TextField(product.getName());
        TextField manufacturerField = new TextField(product.getManufacturer() != null ? product.getManufacturer() : "");
        TextArea descriptionArea = new TextArea(product.getDescription());
        descriptionArea.setPrefRowCount(3);
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Tire", "Rim", "Service", "Other");
        categoryCombo.setValue(product.getCategory() != null ? product.getCategory() : "Tire");
        TextField sizeField = new TextField(product.getSize() != null ? product.getSize() : "");
        TextField modelNumberField = new TextField(product.getModelNumber() != null ? product.getModelNumber() : "");
        
        // Add to basic grid
        int row = 0;
        basicGrid.add(new Label("Product Name:"), 0, row);
        basicGrid.add(nameField, 1, row++);
        basicGrid.add(new Label("Manufacturer:"), 0, row);
        basicGrid.add(manufacturerField, 1, row++);
        basicGrid.add(new Label("Description:"), 0, row);
        basicGrid.add(descriptionArea, 1, row++);
        basicGrid.add(new Label("Category:"), 0, row);
        basicGrid.add(categoryCombo, 1, row++);
        basicGrid.add(new Label("Size:"), 0, row);
        basicGrid.add(sizeField, 1, row++);
        basicGrid.add(new Label("Model Number:"), 0, row);
        basicGrid.add(modelNumberField, 1, row++);
        
        basicTab.setContent(basicGrid);
        
        // Inventory Tab
        Tab inventoryTab = new Tab("Inventory & Pricing");
        GridPane inventoryGrid = new GridPane();
        inventoryGrid.setHgap(10);
        inventoryGrid.setVgap(10);
        inventoryGrid.setPadding(new javafx.geometry.Insets(20));
        
        TextField priceField = new TextField(product.getPrice() != null ? product.getPrice().toString() : "0");
        TextField quantityField = new TextField(String.valueOf(product.getQuantityInStock()));
        TextField reorderLevelField = new TextField(String.valueOf(product.getReorderLevel()));
        TextField skuField = new TextField(product.getSku() != null ? product.getSku() : "");
        TextField barcodeField = new TextField(product.getBarcode() != null ? product.getBarcode() : "");
        TextField locationField = new TextField(product.getLocation() != null ? product.getLocation() : "");
        
        row = 0;
        inventoryGrid.add(new Label("Selling Price:"), 0, row);
        inventoryGrid.add(priceField, 1, row++);
        inventoryGrid.add(new Label("Quantity in Stock:"), 0, row);
        inventoryGrid.add(quantityField, 1, row++);
        inventoryGrid.add(new Label("Reorder Level:"), 0, row);
        inventoryGrid.add(reorderLevelField, 1, row++);
        inventoryGrid.add(new Label("SKU:"), 0, row);
        inventoryGrid.add(skuField, 1, row++);
        inventoryGrid.add(new Label("Barcode:"), 0, row);
        inventoryGrid.add(barcodeField, 1, row++);
        inventoryGrid.add(new Label("Location:"), 0, row);
        inventoryGrid.add(locationField, 1, row++);
        
        inventoryTab.setContent(inventoryGrid);
        
        // Tire Specifications Tab (only for tires)
        Tab specsTab = new Tab("Tire Specifications");
        GridPane specsGrid = new GridPane();
        specsGrid.setHgap(10);
        specsGrid.setVgap(10);
        specsGrid.setPadding(new javafx.geometry.Insets(20));
        
        // Tire specific fields
        ComboBox<String> tireTypeCombo = new ComboBox<>();
        tireTypeCombo.getItems().addAll("All-Season", "Summer", "Winter", "All-Terrain", "Mud-Terrain", "Touring", "Performance");
        tireTypeCombo.setValue(product.getTireType() != null ? product.getTireType() : "All-Season");
        
        ComboBox<String> speedRatingCombo = new ComboBox<>();
        speedRatingCombo.getItems().addAll("L", "M", "N", "P", "Q", "R", "S", "T", "U", "H", "V", "W", "Y", "Z");
        speedRatingCombo.setValue(product.getSpeedRating() != null ? product.getSpeedRating() : "H");
        
        TextField loadRatingField = new TextField(product.getLoadRating() != null ? product.getLoadRating() : "");
        TextField treadwearField = new TextField(product.getUtqgTreadwear() != null ? product.getUtqgTreadwear().toString() : "");
        
        ComboBox<String> tractionCombo = new ComboBox<>();
        tractionCombo.getItems().addAll("AA", "A", "B", "C");
        tractionCombo.setValue(product.getUtqgTraction() != null ? product.getUtqgTraction() : "A");
        
        ComboBox<String> temperatureCombo = new ComboBox<>();
        temperatureCombo.getItems().addAll("A", "B", "C");
        temperatureCombo.setValue(product.getUtqgTemperature() != null ? product.getUtqgTemperature() : "A");
        
        TextField treadDepthField = new TextField(product.getTreadDepth() != null ? product.getTreadDepth().toString() : "10");
        CheckBox runFlatCheckBox = new CheckBox("Run-Flat");
        runFlatCheckBox.setSelected(product.getRunFlat() != null && product.getRunFlat());
        
        TextField warrantyField = new TextField(product.getWarranty() != null ? product.getWarranty() : "");
        
        row = 0;
        specsGrid.add(new Label("Tire Type:"), 0, row);
        specsGrid.add(tireTypeCombo, 1, row++);
        specsGrid.add(new Label("Speed Rating:"), 0, row);
        specsGrid.add(speedRatingCombo, 1, row++);
        specsGrid.add(new Label("Load Rating:"), 0, row);
        specsGrid.add(loadRatingField, 1, row++);
        specsGrid.add(new Label("UTQG Treadwear:"), 0, row);
        specsGrid.add(treadwearField, 1, row++);
        specsGrid.add(new Label("UTQG Traction:"), 0, row);
        specsGrid.add(tractionCombo, 1, row++);
        specsGrid.add(new Label("UTQG Temperature:"), 0, row);
        specsGrid.add(temperatureCombo, 1, row++);
        specsGrid.add(new Label("Tread Depth (32nds):"), 0, row);
        specsGrid.add(treadDepthField, 1, row++);
        specsGrid.add(new Label("Features:"), 0, row);
        specsGrid.add(runFlatCheckBox, 1, row++);
        specsGrid.add(new Label("Warranty:"), 0, row);
        specsGrid.add(warrantyField, 1, row++);
        
        specsTab.setContent(specsGrid);
        
        // Add tabs
        tabPane.getTabs().addAll(basicTab, inventoryTab);
        
        // Only add specs tab if category is Tire
        if ("Tire".equals(product.getCategory())) {
            tabPane.getTabs().add(specsTab);
        }
        
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Tire".equals(newVal)) {
                if (!tabPane.getTabs().contains(specsTab)) {
                    tabPane.getTabs().add(specsTab);
                }
            } else {
                tabPane.getTabs().remove(specsTab);
            }
        });
        
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().setPrefWidth(500);
        
        // Set focus to name field
        javafx.application.Platform.runLater(nameField::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    product.setName(nameField.getText());
                    product.setManufacturer(manufacturerField.getText());
                    product.setDescription(descriptionArea.getText());
                    product.setCategory(categoryCombo.getValue());
                    product.setSize(sizeField.getText());
                    product.setModelNumber(modelNumberField.getText());
                    product.setPrice(new BigDecimal(priceField.getText().isEmpty() ? "0" : priceField.getText()));
                    product.setQuantityInStock(Integer.parseInt(quantityField.getText()));
                    product.setReorderLevel(Integer.parseInt(reorderLevelField.getText()));
                    product.setSku(skuField.getText());
                    // Set barcode to null if empty to avoid unique constraint violations
                    String barcodeText = barcodeField.getText();
                    product.setBarcode(barcodeText == null || barcodeText.trim().isEmpty() ? null : barcodeText.trim());
                    product.setLocation(locationField.getText());
                    
                    // Set tire-specific fields if it's a tire
                    if ("Tire".equals(categoryCombo.getValue())) {
                        product.setTireType(tireTypeCombo.getValue());
                        product.setSpeedRating(speedRatingCombo.getValue());
                        product.setLoadRating(loadRatingField.getText());
                        if (!treadwearField.getText().isEmpty()) {
                            product.setUtqgTreadwear(Integer.parseInt(treadwearField.getText()));
                        }
                        product.setUtqgTraction(tractionCombo.getValue());
                        product.setUtqgTemperature(temperatureCombo.getValue());
                        product.setTreadDepth(Integer.parseInt(treadDepthField.getText()));
                        product.setRunFlat(runFlatCheckBox.isSelected());
                        product.setWarranty(warrantyField.getText());
                        product.setConstruction("Radial"); // Default
                        product.setSeasonality(mapTireTypeToSeasonality(tireTypeCombo.getValue()));
                    }
                    
                    return inventoryService.updateProduct(product);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for numeric fields.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(updatedProduct -> {
            refreshProducts();
            
            // Refresh dashboard
            if (mainView != null) {
                mainView.refreshDashboard();
            }
        });
    }
    
    /**
     * Show dialog to add a new product
     * @param owner The owner window
     */
    public void showAddProductDialog(Stage owner) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add Product");
        dialog.setHeaderText("Create a new product");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create tabbed pane for better organization
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Basic Info Tab
        Tab basicTab = new Tab("Basic Information");
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(10);
        basicGrid.setVgap(10);
        basicGrid.setPadding(new javafx.geometry.Insets(20));
        
        // Basic fields
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Michelin Pilot Sport 4S");
        TextField manufacturerField = new TextField();
        manufacturerField.setPromptText("e.g., Michelin");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPromptText("Product description...");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Tire", "Rim", "Service", "Other");
        categoryCombo.setValue("Tire");
        TextField sizeField = new TextField();
        sizeField.setPromptText("e.g., 245/45R18");
        TextField modelNumberField = new TextField();
        modelNumberField.setPromptText("Model number");
        
        // Add to basic grid
        int row = 0;
        basicGrid.add(new Label("Product Name:"), 0, row);
        basicGrid.add(nameField, 1, row++);
        basicGrid.add(new Label("Manufacturer:"), 0, row);
        basicGrid.add(manufacturerField, 1, row++);
        basicGrid.add(new Label("Description:"), 0, row);
        basicGrid.add(descriptionArea, 1, row++);
        basicGrid.add(new Label("Category:"), 0, row);
        basicGrid.add(categoryCombo, 1, row++);
        basicGrid.add(new Label("Size:"), 0, row);
        basicGrid.add(sizeField, 1, row++);
        basicGrid.add(new Label("Model Number:"), 0, row);
        basicGrid.add(modelNumberField, 1, row++);
        
        basicTab.setContent(basicGrid);
        
        // Inventory Tab
        Tab inventoryTab = new Tab("Inventory & Pricing");
        GridPane inventoryGrid = new GridPane();
        inventoryGrid.setHgap(10);
        inventoryGrid.setVgap(10);
        inventoryGrid.setPadding(new javafx.geometry.Insets(20));
        
        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        TextField quantityField = new TextField("0");
        TextField reorderLevelField = new TextField("5");
        TextField skuField = new TextField();
        TextField barcodeField = new TextField();
        TextField locationField = new TextField();
        locationField.setPromptText("e.g., A1-B2");
        
        row = 0;
        inventoryGrid.add(new Label("Selling Price:"), 0, row);
        inventoryGrid.add(priceField, 1, row++);
        inventoryGrid.add(new Label("Quantity in Stock:"), 0, row);
        inventoryGrid.add(quantityField, 1, row++);
        inventoryGrid.add(new Label("Reorder Level:"), 0, row);
        inventoryGrid.add(reorderLevelField, 1, row++);
        inventoryGrid.add(new Label("SKU:"), 0, row);
        inventoryGrid.add(skuField, 1, row++);
        inventoryGrid.add(new Label("Barcode:"), 0, row);
        inventoryGrid.add(barcodeField, 1, row++);
        inventoryGrid.add(new Label("Location:"), 0, row);
        inventoryGrid.add(locationField, 1, row++);
        
        inventoryTab.setContent(inventoryGrid);
        
        // Tire Specifications Tab (only for tires)
        Tab specsTab = new Tab("Tire Specifications");
        GridPane specsGrid = new GridPane();
        specsGrid.setHgap(10);
        specsGrid.setVgap(10);
        specsGrid.setPadding(new javafx.geometry.Insets(20));
        
        // Tire specific fields
        ComboBox<String> tireTypeCombo = new ComboBox<>();
        tireTypeCombo.getItems().addAll("All-Season", "Summer", "Winter", "All-Terrain", "Mud-Terrain", "Touring", "Performance");
        tireTypeCombo.setValue("All-Season"); // Default value, but user can change this
        
        ComboBox<String> speedRatingCombo = new ComboBox<>();
        speedRatingCombo.getItems().addAll("L", "M", "N", "P", "Q", "R", "S", "T", "U", "H", "V", "W", "Y", "Z");
        speedRatingCombo.setValue("H");
        
        TextField loadRatingField = new TextField();
        loadRatingField.setPromptText("e.g., 94");
        
        TextField treadwearField = new TextField();
        treadwearField.setPromptText("e.g., 500");
        
        ComboBox<String> tractionCombo = new ComboBox<>();
        tractionCombo.getItems().addAll("AA", "A", "B", "C");
        tractionCombo.setValue("A");
        
        ComboBox<String> temperatureCombo = new ComboBox<>();
        temperatureCombo.getItems().addAll("A", "B", "C");
        temperatureCombo.setValue("A");
        
        TextField treadDepthField = new TextField("10");
        treadDepthField.setPromptText("in 32nds of inch");
        
        CheckBox runFlatCheckBox = new CheckBox("Run-Flat");
        
        TextField warrantyField = new TextField();
        warrantyField.setPromptText("e.g., 50,000 miles");
        
        row = 0;
        specsGrid.add(new Label("Tire Type:"), 0, row);
        specsGrid.add(tireTypeCombo, 1, row++);
        specsGrid.add(new Label("Speed Rating:"), 0, row);
        specsGrid.add(speedRatingCombo, 1, row++);
        specsGrid.add(new Label("Load Rating:"), 0, row);
        specsGrid.add(loadRatingField, 1, row++);
        specsGrid.add(new Label("UTQG Treadwear:"), 0, row);
        specsGrid.add(treadwearField, 1, row++);
        specsGrid.add(new Label("UTQG Traction:"), 0, row);
        specsGrid.add(tractionCombo, 1, row++);
        specsGrid.add(new Label("UTQG Temperature:"), 0, row);
        specsGrid.add(temperatureCombo, 1, row++);
        specsGrid.add(new Label("Tread Depth (32nds):"), 0, row);
        specsGrid.add(treadDepthField, 1, row++);
        specsGrid.add(new Label("Features:"), 0, row);
        specsGrid.add(runFlatCheckBox, 1, row++);
        specsGrid.add(new Label("Warranty:"), 0, row);
        specsGrid.add(warrantyField, 1, row++);
        
        specsTab.setContent(specsGrid);
        
        // Add tabs
        tabPane.getTabs().addAll(basicTab, inventoryTab);
        
        // Only add specs tab if category is Tire
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Tire".equals(newVal)) {
                if (!tabPane.getTabs().contains(specsTab)) {
                    tabPane.getTabs().add(specsTab);
                }
            } else {
                tabPane.getTabs().remove(specsTab);
            }
        });
        
        // Add specs tab initially since default is Tire
        tabPane.getTabs().add(specsTab);
        
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().setPrefWidth(500);
        
        // Set focus to name field
        javafx.application.Platform.runLater(nameField::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Product product = new Product();
                    product.setName(nameField.getText());
                    product.setManufacturer(manufacturerField.getText());
                    product.setDescription(descriptionArea.getText());
                    product.setCategory(categoryCombo.getValue());
                    product.setSize(sizeField.getText());
                    product.setModelNumber(modelNumberField.getText());
                    product.setPrice(new BigDecimal(priceField.getText().isEmpty() ? "0" : priceField.getText()));
                    product.setQuantityInStock(Integer.parseInt(quantityField.getText()));
                    product.setReorderLevel(Integer.parseInt(reorderLevelField.getText()));
                    product.setSku(skuField.getText());
                    // Set barcode to null if empty to avoid unique constraint violations
                    String barcodeText = barcodeField.getText();
                    product.setBarcode(barcodeText == null || barcodeText.trim().isEmpty() ? null : barcodeText.trim());
                    product.setLocation(locationField.getText());
                    
                    // Set tire-specific fields if it's a tire
                    if ("Tire".equals(categoryCombo.getValue())) {
                        product.setTireType(tireTypeCombo.getValue());
                        product.setSpeedRating(speedRatingCombo.getValue());
                        product.setLoadRating(loadRatingField.getText());
                        if (!treadwearField.getText().isEmpty()) {
                            product.setUtqgTreadwear(Integer.parseInt(treadwearField.getText()));
                        }
                        product.setUtqgTraction(tractionCombo.getValue());
                        product.setUtqgTemperature(temperatureCombo.getValue());
                        product.setTreadDepth(Integer.parseInt(treadDepthField.getText()));
                        product.setRunFlat(runFlatCheckBox.isSelected());
                        product.setWarranty(warrantyField.getText());
                        product.setConstruction("Radial"); // Default
                        product.setSeasonality(mapTireTypeToSeasonality(tireTypeCombo.getValue()));
                    }
                    
                    return inventoryService.addProduct(product);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for numeric fields.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            refreshProducts();
            
            // Refresh all relevant tabs after inventory change
            if (mainView != null) {
                mainView.refreshTabsForDataChange("product");
            }
        });
    }
    
    private String mapTireTypeToSeasonality(String tireType) {
        switch (tireType) {
            case "Winter":
                return "Winter";
            case "Summer":
                return "Summer";
            default:
                return "All-Season";
        }
    }
    
    /**
     * Show confirmation dialog to delete a product
     * @param product The product to delete
     * @param owner The owner window
     */
    public void showDeleteConfirmation(Product product, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete " + product.getName());
        alert.setContentText("Are you sure you want to delete this product?");
        alert.initOwner(owner);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = inventoryService.deleteProduct(product.getId());
            if (deleted) {
                refreshProducts();
                
                // Refresh all relevant tabs after inventory change
                if (mainView != null) {
                    mainView.refreshTabsForDataChange("product");
                }
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Delete Failed");
                errorAlert.setHeaderText("Cannot Delete Product");
                errorAlert.setContentText("This product cannot be deleted because it has been sold or is referenced in other records. Products that have been sold must remain in the system for record-keeping purposes.");
                errorAlert.initOwner(owner);
                errorAlert.showAndWait();
            }
        }
    }
    
    /**
     * Delete a product from the inventory
     * @param product The product to delete
     */
    public void deleteProduct(Product product) {
        try {
            boolean deleted = inventoryService.deleteProduct(product.getId());
            if (deleted) {
                refreshProducts();
                
                // Refresh all relevant tabs after inventory change
                if (mainView != null) {
                    mainView.refreshTabsForDataChange("product");
                }
                LOGGER.info("Successfully deleted product: " + product.getName());
            } else {
                LOGGER.info("Cannot delete product " + product.getName() + " - it's referenced by sales records");
                showAlert(Alert.AlertType.ERROR, "Delete Failed", 
                         "This product cannot be deleted because it has been sold or is referenced in other records. Products that have been sold must remain in the system for record-keeping purposes.");
            }
        } catch (Exception e) {
            LOGGER.severe("Unexpected error deleting product: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Delete Failed", 
                     "An unexpected error occurred while deleting the product: " + e.getMessage());
        }
    }
    
    /**
     * Show supplier availability dialog for a product
     * @param product The product to check availability for
     * @param owner The owner window
     */
    public void showSupplierAvailabilityDialog(Product product, Stage owner) {
        if (product == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a product to check availability.");
            return;
        }
        
        // Create loading dialog
        Dialog<Void> loadingDialog = new Dialog<>();
        loadingDialog.setTitle("Checking Availability");
        loadingDialog.setContentText("Checking supplier availability...");
        loadingDialog.initOwner(owner);
        loadingDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        // Show loading dialog
        loadingDialog.show();
        
        // Run availability check in background
        new Thread(() -> {
            try {
                String sku = product.getSku() != null ? product.getSku() : product.getBarcode();
                if (sku == null || sku.isEmpty()) {
                    Platform.runLater(() -> {
                        loadingDialog.close();
                        showAlert(Alert.AlertType.ERROR, "No SKU", "This product does not have a SKU or barcode to check.");
                    });
                    return;
                }
                
                List<com.tireshop.service.SupplierCatalogService.SupplierInventory> availability = 
                    supplierCatalogService.checkAllSuppliers(sku);
                
                Platform.runLater(() -> {
                    loadingDialog.close();
                    showSupplierAvailabilityResults(product, availability, owner);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingDialog.close();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to check availability: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Show supplier availability results
     */
    private void showSupplierAvailabilityResults(Product product, 
                                                 List<com.tireshop.service.SupplierCatalogService.SupplierInventory> results, 
                                                 Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Supplier Availability");
        dialog.setHeaderText("Availability for: " + product.getName());
        dialog.initOwner(owner);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        if (results.isEmpty()) {
            Label noResultsLabel = new Label("No availability information found.");
            noResultsLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            content.getChildren().add(noResultsLabel);
        } else {
            // Create table for results
            TableView<com.tireshop.service.SupplierCatalogService.SupplierInventory> table = new TableView<>();
            
            TableColumn<com.tireshop.service.SupplierCatalogService.SupplierInventory, String> supplierCol = new TableColumn<>("Supplier");
            supplierCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getSupplier().getDisplayName()));
            supplierCol.setPrefWidth(150);
            
            TableColumn<com.tireshop.service.SupplierCatalogService.SupplierInventory, String> priceCol = new TableColumn<>("Price");
            priceCol.setCellValueFactory(cellData -> {
                BigDecimal price = cellData.getValue().getPrice();
                return new SimpleStringProperty(price != null ? String.format("$%.2f", price) : "N/A");
            });
            priceCol.setPrefWidth(80);
            
            TableColumn<com.tireshop.service.SupplierCatalogService.SupplierInventory, Number> qtyCol = new TableColumn<>("Available");
            qtyCol.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getQuantityAvailable()));
            qtyCol.setPrefWidth(80);
            
            TableColumn<com.tireshop.service.SupplierCatalogService.SupplierInventory, String> warehouseCol = new TableColumn<>("Warehouse");
            warehouseCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getWarehouse()));
            warehouseCol.setPrefWidth(100);
            
            TableColumn<com.tireshop.service.SupplierCatalogService.SupplierInventory, String> deliveryCol = new TableColumn<>("Delivery");
            deliveryCol.setCellValueFactory(cellData -> {
                Integer days = cellData.getValue().getEstimatedDeliveryDays();
                return new SimpleStringProperty(days != null ? days + " days" : "N/A");
            });
            deliveryCol.setPrefWidth(80);
            
            table.getColumns().addAll(supplierCol, priceCol, qtyCol, warehouseCol, deliveryCol);
            table.setItems(FXCollections.observableArrayList(results));
            table.setPrefHeight(200);
            
            content.getChildren().add(table);
            
            // Add place order button
            Button placeOrderBtn = new Button("Place Order");
            placeOrderBtn.setDisable(true);
            
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                placeOrderBtn.setDisable(newVal == null || newVal.getQuantityAvailable() <= 0);
            });
            
            placeOrderBtn.setOnAction(e -> {
                com.tireshop.service.SupplierCatalogService.SupplierInventory selected = 
                    table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showPlaceOrderDialog(product, selected, owner);
                    dialog.close();
                }
            });
            
            content.getChildren().add(placeOrderBtn);
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(600);
        
        dialog.showAndWait();
    }
    
    /**
     * Show place order dialog
     */
    private void showPlaceOrderDialog(Product product, 
                                     com.tireshop.service.SupplierCatalogService.SupplierInventory inventory,
                                     Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Place Order");
        dialog.setHeaderText("Order from " + inventory.getSupplier().getDisplayName());
        dialog.initOwner(owner);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Product info
        grid.add(new Label("Product:"), 0, 0);
        grid.add(new Label(product.getName()), 1, 0);
        
        grid.add(new Label("Unit Price:"), 0, 1);
        grid.add(new Label(String.format("$%.2f", inventory.getPrice())), 1, 1);
        
        grid.add(new Label("Available:"), 0, 2);
        grid.add(new Label(String.valueOf(inventory.getQuantityAvailable())), 1, 2);
        
        // Order quantity
        grid.add(new Label("Order Quantity:"), 0, 3);
        Spinner<Integer> quantitySpinner = new Spinner<>(1, inventory.getQuantityAvailable(), 1);
        quantitySpinner.setEditable(true);
        grid.add(quantitySpinner, 1, 3);
        
        // Total
        Label totalLabel = new Label(String.format("$%.2f", inventory.getPrice()));
        grid.add(new Label("Total:"), 0, 4);
        grid.add(totalLabel, 1, 4);
        
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            BigDecimal total = inventory.getPrice().multiply(BigDecimal.valueOf(newVal));
            totalLabel.setText(String.format("$%.2f", total));
        });
        
        // PO Number
        grid.add(new Label("PO Number:"), 0, 5);
        TextField poField = new TextField();
        poField.setText("PO-" + System.currentTimeMillis());
        grid.add(poField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType orderButtonType = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(orderButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == orderButtonType) {
                try {
                    List<com.tireshop.service.SupplierCatalogService.OrderItem> items = Arrays.asList(
                        new com.tireshop.service.SupplierCatalogService.OrderItem(
                            inventory.getSku(), 
                            quantitySpinner.getValue(), 
                            inventory.getPrice()
                        )
                    );
                    
                    com.tireshop.service.SupplierCatalogService.SupplierOrder order = 
                        supplierCatalogService.placeOrder(inventory.getSupplier(), items, poField.getText());
                    
                    showAlert(Alert.AlertType.INFORMATION, "Order Placed", 
                             "Order " + order.getPoNumber() + " has been submitted to " + 
                             inventory.getSupplier().getDisplayName());
                    
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Order Failed", 
                             "Failed to place order: " + e.getMessage());
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Get the search field
     * @return The search field
     */
    public TextField getSearchField() {
        return searchField;
    }
    
    /**
     * Get the sort by combo box
     * @return The sort by combo box
     */
    public ComboBox<String> getSortByComboBox() {
        return sortByComboBox;
    }
    
    /**
     * Get the ascending checkbox
     * @return The ascending checkbox
     */
    public CheckBox getAscendingCheckBox() {
        return ascendingCheckBox;
    }
    
    /**
     * Get the filter by combo box
     * @return The filter by combo box
     */
    public ComboBox<String> getFilterByComboBox() {
        return filterByComboBox;
    }
    
    /**
     * Get the details pane
     * @return The details pane
     */
    public VBox getDetailsPane() {
        return detailsPane;
    }
    
    // Added public method to get the ScannerServer instance
    public ScannerServer getScannerServer() {
        return this.scannerServer;
    }

    // Added public method to get the scanner port
    public int getScannerPort() {
        if (this.scannerServer != null && this.scannerServer.isRunning()) {
            return this.scannerServer.getPort(); // Assuming ScannerServer has a getPort() method
        }
        return -1; // Or throw an exception if server not running
    }

    // Added public method to start the scanner server
    public void startScannerServer() {
        if (this.scannerServer == null || !this.scannerServer.isRunning()) {
            try {
                Consumer<String> barcodeHandler = barcode -> {
                    Platform.runLater(() -> {
                        // Ensure searchField is not null if it's accessed here
                        if (this.searchField != null) {
                           this.searchField.setText(barcode);
                        } else {
                            LOGGER.warning("SearchField is null in barcodeHandler");
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Barcode Scanned", "Barcode: " + barcode + "\nProduct search updated.");
                    });
                };
                
                // Ensure inventoryService is not null
                if (this.inventoryService == null) {
                    LOGGER.severe("InventoryService is null, cannot start ScannerServer.");
                    showAlert(Alert.AlertType.ERROR, "Scanner Error", "Internal error: InventoryService not available.");
                    return;
                }

                this.scannerServer = new ScannerServer(this.inventoryService, barcodeHandler); 
                this.scannerServer.start();
                String url = this.scannerServer.getScanUrl(); 
                
                if (this.scannerUrlLabel != null) {
                    this.scannerUrlLabel.setText("Scan URL: " + url);
                    this.scannerUrlLabel.setVisible(true);
                }
                if (this.scanWithPhoneButton != null) {
                    this.scanWithPhoneButton.setText("Stop Scanner");
                }
                LOGGER.info("Mobile scanner server started. URL: " + url);

            } catch (RuntimeException e) { 
                LOGGER.log(Level.SEVERE, "Failed to start scanner server (RuntimeException)", e);
                showAlert(Alert.AlertType.ERROR, "Scanner Error", "Could not start phone scanner server: " + e.getMessage());
            } catch (Exception e) { // Catching generic Exception to see if there are other issues
                LOGGER.log(Level.SEVERE, "Unexpected error starting scanner server", e);
                showAlert(Alert.AlertType.ERROR, "Scanner Error", "An unexpected error occurred: " + e.getMessage());
            }
        } else {
            LOGGER.info("Scanner server is already running.");
            // Optionally, update UI to show it's already running
             if (this.scannerServer != null && this.scannerUrlLabel != null && this.scanWithPhoneButton != null) {
                this.scannerUrlLabel.setText("Scan URL: " + this.scannerServer.getScanUrl());
                this.scannerUrlLabel.setVisible(true);
                this.scanWithPhoneButton.setText("Stop Scanner");
            }
        }
    }

    private void toggleScannerServer() {
        if (this.scannerServer == null || !this.scannerServer.isRunning()) {
            try {
                Consumer<String> barcodeHandler = barcode -> {
                    Platform.runLater(() -> {
                        this.searchField.setText(barcode);
                        showAlert(Alert.AlertType.INFORMATION, "Barcode Scanned", "Barcode: " + barcode + "\nProduct search updated.");
                    });
                };
                
                this.scannerServer = new ScannerServer(this.inventoryService, barcodeHandler); 
                this.scannerServer.start();
                String url = this.scannerServer.getScanUrl(); 
                
                if (this.scannerUrlLabel != null) {
                    this.scannerUrlLabel.setText("Scan URL: " + url);
                    this.scannerUrlLabel.setVisible(true);
                }
                if (this.scanWithPhoneButton != null) {
                    this.scanWithPhoneButton.setText("Stop Scanner");
                }
                LOGGER.info("Mobile scanner server started. URL: " + url);

            } catch (RuntimeException e) { 
                LOGGER.log(Level.SEVERE, "Failed to start scanner server (RuntimeException)", e);
                showAlert(Alert.AlertType.ERROR, "Scanner Error", "Could not start phone scanner server: " + e.getMessage());
            } catch (Exception e) { 
                LOGGER.log(Level.SEVERE, "Unexpected error starting scanner server", e);
                showAlert(Alert.AlertType.ERROR, "Scanner Error", "An unexpected error occurred: " + e.getMessage());
            }
        } else {
            if (this.scannerServer != null) {
                this.scannerServer.stop();
            }
            if (this.scannerUrlLabel != null) {
                this.scannerUrlLabel.setText("Scanner URL: (Not Active)");
                this.scannerUrlLabel.setVisible(false);
            }
            if (this.scanWithPhoneButton != null) {
                this.scanWithPhoneButton.setText("Scan with Phone");
            }
            LOGGER.info("Mobile scanner server stopped.");
        }
    }
    
    public void shutdown() {
        if (scannerServer != null && scannerServer.isRunning()) {
            scannerServer.stop();
            LOGGER.info("ScannerServer stopped on InventoryController shutdown.");
        }
    }

    private HBox createInventoryTopControls() {
        HBox controlsContainer = new HBox(10);
        controlsContainer.setPadding(new Insets(0, 0, 10, 0)); 
        controlsContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search by Name, Size (275/65), Barcode, SKU...");
        searchField.setMinWidth(250); // Adjusted min width for longer prompt
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.length() >= 2 || newV.isEmpty()) { 
                searchProducts(newV);
            }
        });

        sortByComboBox = new ComboBox<>();
        // Ensure items are added to sortByComboBox in setupSortingControls()

        ascendingCheckBox = new CheckBox("Ascending");
        ascendingCheckBox.setSelected(true); // Default sort order

        filterByComboBox = new ComboBox<>(); 
        // Ensure items are added to filterByComboBox in setupFilteringControls()

        scanWithPhoneButton = new Button("Scan with Phone");
        scanWithPhoneButton.setOnAction(e -> toggleScannerServer());

        scannerUrlLabel = new Label("Scanner URL: (Not Active)");
        scannerUrlLabel.setVisible(false); // Initially hidden
        // Optional: Add some style to make URL label more noticeable when visible
        // scannerUrlLabel.setStyle("-fx-text-fill: blue; -fx-font-style: italic;");

        controlsContainer.getChildren().addAll(
            new Label("Search:"), searchField, 
            new Separator(javafx.geometry.Orientation.VERTICAL),
            new Label("Sort By:"), sortByComboBox, ascendingCheckBox,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            new Label("Filter By:"), filterByComboBox,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            scanWithPhoneButton, scannerUrlLabel
        );
        return controlsContainer;
    }
} 