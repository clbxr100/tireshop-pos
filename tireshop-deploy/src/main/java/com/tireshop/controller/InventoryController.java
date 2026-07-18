package com.tireshop.controller;

import com.tireshop.model.Product;
import com.tireshop.service.InventoryService;
import com.tireshop.util.ScannerServer;
import com.tireshop.view.MainView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for inventory management screen
 */
public class InventoryController {
    
    private static final Logger LOGGER = Logger.getLogger(InventoryController.class.getName());
    
    private InventoryService inventoryService;
    private TableView<Product> productTable;
    private MainView mainView;
    private ScannerServer scannerServer;
    
    /**
     * Constructor with InventoryService (should be the one used by MainView)
     * @param inventoryService The inventory service to use
     */
    public InventoryController(InventoryService inventoryService) {
        System.out.println("[CONSTRUCTOR] InventoryController instance created with injected InventoryService.");
        this.inventoryService = inventoryService;
    }
    
    /**
     * Set MainView reference for dashboard refreshing
     * @param mainView The MainView instance
     */
    public void setMainView(MainView mainView) {
        this.mainView = mainView;
    }
    
    /**
     * Initialize the controller with a TableView
     * @param productTable The TableView to populate with product data
     */
    public void initialize(TableView<Product> productTable) {
        this.productTable = productTable;
        
        // Set up the product table columns
        TableColumn<Product, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getId()));
        
        TableColumn<Product, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getName()));
        nameColumn.setPrefWidth(200);
        
        TableColumn<Product, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            
            // First check if size field is populated
            if (product.getSize() != null && !product.getSize().isEmpty()) {
                return new SimpleStringProperty(product.getSize());
            }
            
            // Then extract from name/description if product is a tire
            if (product.getCategory() != null && product.getCategory().equals("Tire")) {
                String name = product.getName();
                String description = product.getDescription();
                
                // Look for pattern like 205/55R16 in name first
                if (name != null) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{2,3})/(\\d{2})R(\\d{2})");
                    java.util.regex.Matcher matcher = pattern.matcher(name);
                    if (matcher.find()) {
                        return new SimpleStringProperty(matcher.group(0));
                    }
                }
                
                // Then try description
                if (description != null) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{2,3})/(\\d{2})R(\\d{2})");
                    java.util.regex.Matcher matcher = pattern.matcher(description);
                    if (matcher.find()) {
                        return new SimpleStringProperty(matcher.group(0));
                    }
                }
            }
            return new SimpleStringProperty("");
        });
        sizeColumn.setPrefWidth(80);
        
        TableColumn<Product, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getDescription()));
        descriptionColumn.setPrefWidth(300);
        
        TableColumn<Product, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getPrice()));
        
        TableColumn<Product, Number> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(cellData -> 
                new SimpleIntegerProperty(cellData.getValue().getQuantityInStock()));
        
        TableColumn<Product, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            if (product.getQuantityInStock() <= 0) {
                return new SimpleStringProperty("Out of Stock");
            } else if (product.getQuantityInStock() <= product.getReorderLevel()) {
                return new SimpleStringProperty("Low Stock");
            } else {
                return new SimpleStringProperty("In Stock");
            }
        });
        
        // Add columns to the table
        productTable.getColumns().addAll(idColumn, nameColumn, sizeColumn, descriptionColumn, 
                priceColumn, quantityColumn, statusColumn);
        
        // Load products
        refreshProducts();
    }
    
    /**
     * Refresh the product list from the database
     */
    public void refreshProducts() {
        ObservableList<Product> products = FXCollections.observableArrayList(
                inventoryService.getAllProducts());
        productTable.setItems(products);
    }
    
    /**
     * Search products by name or description
     * @param keyword Keyword to search for
     */
    public void searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            refreshProducts();
        } else {
            ObservableList<Product> products = FXCollections.observableArrayList(
                    inventoryService.searchProducts(keyword));
            productTable.setItems(products);
        }
    }
    
    /**
     * Filter products by category
     * @param category The category to filter by
     */
    public void filterByCategory(String category) {
        if (category == null || category.trim().isEmpty() || category.equals("All")) {
            refreshProducts();
        } else {
            ObservableList<Product> products = FXCollections.observableArrayList(
                    inventoryService.getProductsByCategory(category));
            productTable.setItems(products);
        }
    }
    
    /**
     * Get the currently selected product
     * @return The selected product or null if none selected
     */
    public Product getSelectedProduct() {
        return productTable.getSelectionModel().getSelectedItem();
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
            
            // Refresh dashboard after inventory change
            if (mainView != null) {
                mainView.refreshDashboard();
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
        
        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Form fields
        TextField nameField = new TextField(product.getName());
        TextArea descriptionArea = new TextArea(product.getDescription());
        TextField priceField = new TextField(product.getPrice().toString());
        TextField quantityField = new TextField(String.valueOf(product.getQuantityInStock()));
        TextField reorderLevelField = new TextField(String.valueOf(product.getReorderLevel()));
        TextField skuField = new TextField(product.getSku());
        TextField barcodeField = new TextField(product.getBarcode());
        TextField categoryField = new TextField(product.getCategory());
        TextField sizeField = new TextField(product.getSize() != null ? product.getSize() : "");
        
        // Add labels and fields to grid
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Size:"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(new Label("Price:"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Quantity:"), 0, 5);
        grid.add(quantityField, 1, 5);
        grid.add(new Label("Reorder Level:"), 0, 6);
        grid.add(reorderLevelField, 1, 6);
        grid.add(new Label("SKU:"), 0, 7);
        grid.add(skuField, 1, 7);
        grid.add(new Label("Barcode:"), 0, 8);
        grid.add(barcodeField, 1, 8);
        
        dialog.getDialogPane().setContent(grid);
        
        // Set focus to price field
        javafx.application.Platform.runLater(priceField::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    product.setName(nameField.getText());
                    product.setDescription(descriptionArea.getText());
                    product.setPrice(new BigDecimal(priceField.getText()));
                    product.setQuantityInStock(Integer.parseInt(quantityField.getText()));
                    product.setReorderLevel(Integer.parseInt(reorderLevelField.getText()));
                    product.setSku(skuField.getText());
                    product.setBarcode(barcodeField.getText());
                    product.setCategory(categoryField.getText());
                    product.setSize(sizeField.getText());
                    
                    return inventoryService.updateProduct(product);
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Please enter valid numbers for price, quantity, and reorder level.");
                    alert.showAndWait();
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
        
        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Form fields
        TextField nameField = new TextField();
        TextArea descriptionArea = new TextArea();
        TextField priceField = new TextField();
        TextField quantityField = new TextField();
        TextField reorderLevelField = new TextField();
        TextField skuField = new TextField();
        TextField barcodeField = new TextField();
        TextField categoryField = new TextField();
        TextField sizeField = new TextField();
        
        // Add labels and fields to grid
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Size:"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(new Label("Price:"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Quantity:"), 0, 5);
        grid.add(quantityField, 1, 5);
        grid.add(new Label("Reorder Level:"), 0, 6);
        grid.add(reorderLevelField, 1, 6);
        grid.add(new Label("SKU:"), 0, 7);
        grid.add(skuField, 1, 7);
        grid.add(new Label("Barcode:"), 0, 8);
        grid.add(barcodeField, 1, 8);
        
        dialog.getDialogPane().setContent(grid);
        
        // Set focus to name field
        javafx.application.Platform.runLater(nameField::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Product product = new Product();
                    product.setName(nameField.getText());
                    product.setDescription(descriptionArea.getText());
                    product.setPrice(new BigDecimal(priceField.getText()));
                    product.setQuantityInStock(Integer.parseInt(quantityField.getText()));
                    product.setReorderLevel(Integer.parseInt(reorderLevelField.getText()));
                    product.setSku(skuField.getText());
                    product.setBarcode(barcodeField.getText());
                    product.setCategory(categoryField.getText());
                    product.setSize(sizeField.getText());
                    
                    return inventoryService.addProduct(product);
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Please enter valid numbers for price, quantity, and reorder level.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        
        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            refreshProducts();
            
            // Refresh dashboard
            if (mainView != null) {
                mainView.refreshDashboard();
            }
        });
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
            inventoryService.deleteProduct(product.getId());
            refreshProducts();
            
            // Refresh dashboard
            if (mainView != null) {
                mainView.refreshDashboard();
            }
        }
    }
    
    /**
     * Start the mobile scanner server
     * @throws IOException If server fails to start
     */
    public void startScannerServer() throws IOException {
        if (scannerServer == null) {
            scannerServer = new ScannerServer(inventoryService);
            scannerServer.start();
            LOGGER.info("Mobile scanner server started");
        }
    }
    
    /**
     * Stop the mobile scanner server
     */
    public void stopScannerServer() {
        if (scannerServer != null) {
            scannerServer.stop();
            scannerServer = null;
            LOGGER.info("Mobile scanner server stopped");
        }
    }
    
    /**
     * Get the scanner server instance
     * @return ScannerServer instance or null if not started
     */
    public ScannerServer getScannerServer() {
        return scannerServer;
    }
} 