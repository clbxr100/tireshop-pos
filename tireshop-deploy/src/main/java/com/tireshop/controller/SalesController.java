package com.tireshop.controller;

import com.tireshop.dao.CustomerDao;
import com.tireshop.dao.ProductDao;
import com.tireshop.dao.SaleDao;
import com.tireshop.dao.SaleItemDao;
import com.tireshop.model.*;
import com.tireshop.service.InventoryService;
import com.tireshop.service.SalesService;
import com.tireshop.util.PrinterService;
import com.tireshop.util.SettingsService;
import com.tireshop.view.MainView;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for sales operations
 */
public class SalesController {
    
    private final SalesService salesService;
    private final InventoryService inventoryService;
    private final PaymentController paymentController;
    private final MainView mainView;
    private final SettingsService settingsService;
    private PrinterService printerService;
    private TableView<Sale> salesTable;
    private ObservableList<Sale> salesList;
    private BorderPane salesPane;
    
    public SalesController(SalesService salesService, 
                          InventoryService inventoryService,
                          MainView mainView,
                          SettingsService settingsService) {
        this.salesService = salesService;
        this.inventoryService = inventoryService;
        this.mainView = mainView;
        this.settingsService = settingsService;
        this.printerService = new PrinterService(this.settingsService);
        this.paymentController = new PaymentController(this.salesService, this.settingsService);
    }
    
    /**
     * Initialize the sales view
     * @param salesPane The main sales pane
     * @param stage The application stage
     */
    public void initialize(BorderPane salesPane, Stage stage) {
        this.salesPane = salesPane;
        // Create TabPane for sales views
        TabPane salesTabPane = new TabPane();
        salesTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Load sales-specific CSS
        salesPane.getStylesheets().add(getClass().getResource("/styles/sales.css").toExternalForm());
        
        // Create tabs
        Tab salesListTab = createSalesListTab(stage);
        Tab customTab = createCustomTab(stage);
        
        // Add tabs to the TabPane
        salesTabPane.getTabs().addAll(salesListTab, customTab);
        
        // Add the TabPane to the main salesPane
        salesPane.setCenter(salesTabPane);
    }
    
    /**
     * Create the sales list tab
     * @param stage The application stage
     * @return Tab containing sales list view
     */
    private Tab createSalesListTab(Stage stage) {
        Tab tab = new Tab("Sales List");
        
        BorderPane content = new BorderPane();
        
        // Create top controls
        HBox topControls = createTopControls(stage);
        content.setTop(topControls);
        
        // Create sales table
        salesTable = createSalesTable();
        content.setCenter(salesTable);
        
        // Load sales data
        refreshSales();
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * Create the custom details tab
     * @param stage The application stage
     * @return Tab containing custom details view
     */
    private Tab createCustomTab(Stage stage) {
        Tab tab = new Tab("Details");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        // Title section
        Label titleLabel = new Label("Sale Details");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Declare ALL buttons used within this tab upfront to ensure they are in scope for lambdas
        Button addProductBtn = new Button("Add Product");
        Button addServiceBtn = new Button("Add Service");
        Button removeItemBtn = new Button("Remove Item");
        Button saveNotesBtn = new Button("Save Notes");
        Button completePaymentBtn = new Button("Complete Payment");
        Button printReceiptBtn = new Button("Print Receipt");
        Button cancelSaleBtn = new Button("Cancel Sale");
        Button refreshSaleComboBtn = new Button("Refresh List"); // For the sale ComboBox

        // Current sale selection
        ComboBox<Sale> saleComboBox = new ComboBox<>();
        saleComboBox.setPromptText("Select a sale to view details");
        saleComboBox.setPrefWidth(300);
        
        // Sale information panel
        TitledPane saleInfoPane = new TitledPane("Sale Information", new VBox());
        saleInfoPane.setCollapsible(false);
        saleInfoPane.setPrefHeight(200);
        
        GridPane saleInfoGrid = new GridPane();
        saleInfoGrid.setHgap(15);
        saleInfoGrid.setVgap(10);
        saleInfoGrid.setPadding(new Insets(10));
        
        // Sale info fields
        Label invoiceLabel = new Label();
        Label dateLabel = new Label();
        Label customerLabel = new Label();
        Label vehicleLabel = new Label();
        Label totalLabelValue = new Label();
        Label statusLabel = new Label();
        
        saleInfoGrid.add(new Label("Invoice #:"), 0, 0);
        saleInfoGrid.add(invoiceLabel, 1, 0);
        saleInfoGrid.add(new Label("Date:"), 0, 1);
        saleInfoGrid.add(dateLabel, 1, 1);
        saleInfoGrid.add(new Label("Customer:"), 0, 2);
        saleInfoGrid.add(customerLabel, 1, 2);
        saleInfoGrid.add(new Label("Vehicle:"), 0, 3);
        saleInfoGrid.add(vehicleLabel, 1, 3);
        saleInfoGrid.add(new Label("Total:"), 0, 4);
        saleInfoGrid.add(totalLabelValue, 1, 4);
        saleInfoGrid.add(new Label("Status:"), 0, 5);
        saleInfoGrid.add(statusLabel, 1, 5);
        
        ((VBox) saleInfoPane.getContent()).getChildren().add(saleInfoGrid);
        
        // Items section
        TitledPane itemsPane = new TitledPane("Sale Items", new VBox());
        itemsPane.setCollapsible(false);
        
        TableView<SaleItem> itemsTable = new TableView<>();
        
        // Create item columns
        TableColumn<SaleItem, String> itemTypeColumn = new TableColumn<>("Type");
        itemTypeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getItemType()));
                
        TableColumn<SaleItem, String> itemNameColumn = new TableColumn<>("Item");
        itemNameColumn.setCellValueFactory(cellData -> {
            SaleItem item = cellData.getValue();
            return new SimpleStringProperty(item.getItemName());
        });
        
        TableColumn<SaleItem, String> itemPriceColumn = new TableColumn<>("Price");
        itemPriceColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getUnitPrice()));
                
        TableColumn<SaleItem, Number> qtyColumn = new TableColumn<>("Quantity");
        qtyColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getQuantity()));
                
        TableColumn<SaleItem, String> itemSubtotalColumn = new TableColumn<>("Subtotal");
        itemSubtotalColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getSubtotal()));
        
        itemsTable.getColumns().addAll(itemTypeColumn, itemNameColumn, itemPriceColumn, qtyColumn, itemSubtotalColumn);
        
        // Add and remove buttons for items
        HBox itemButtons = new HBox(10);
        itemButtons.getChildren().addAll(addProductBtn, addServiceBtn, removeItemBtn);
        itemButtons.setPadding(new Insets(5));
        
        ((VBox) itemsPane.getContent()).getChildren().addAll(itemsTable, itemButtons);
        
        // Notes section
        TitledPane notesPane = new TitledPane("Notes", new VBox());
        notesPane.setCollapsible(false);
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter notes about this sale here...");
        notesArea.setWrapText(true);
        
        ((VBox) notesPane.getContent()).getChildren().addAll(notesArea, saveNotesBtn);
        ((VBox) notesPane.getContent()).setPadding(new Insets(10));
        
        refreshSaleComboBtn.setOnAction(e -> {
            Sale currentSelection = saleComboBox.getValue();
            Long currentSelectionId = currentSelection != null ? currentSelection.getId() : null;
            List<Sale> sales = salesService.getAllSales();
            saleComboBox.setItems(FXCollections.observableArrayList(sales));
            if (currentSelectionId != null) {
                sales.stream().filter(s -> s.getId().equals(currentSelectionId)).findFirst().ifPresent(saleComboBox::setValue);
            } else if (!sales.isEmpty()) {
                saleComboBox.getSelectionModel().selectFirst(); // Select first if nothing was selected
            }
        });

        saleComboBox.setOnAction(e -> {
            Sale selectedSaleFromComboBox = saleComboBox.getValue();
            if (selectedSaleFromComboBox != null) {
                salesService.getSaleById(selectedSaleFromComboBox.getId()).ifPresentOrElse(freshSale -> {
                    invoiceLabel.setText(freshSale.getInvoiceNumber());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    dateLabel.setText(freshSale.getTimestamp().format(formatter));
                    Customer customer = freshSale.getCustomer();
                    customerLabel.setText(customer != null ? customer.getFirstName() + " " + customer.getLastName() : "Not specified");
                    Vehicle vehicle = freshSale.getVehicle();
                    vehicleLabel.setText(vehicle != null ? vehicle.toString() : "Not specified");
                    totalLabelValue.setText("$" + freshSale.getTotal());
                    statusLabel.setText(freshSale.isPaid() ? "Paid" : "Pending");
                    itemsTable.setItems(FXCollections.observableArrayList(freshSale.getItems()));
                    notesArea.setText(freshSale.getNotes() != null ? freshSale.getNotes() : "");

                    boolean isPaid = freshSale.isPaid();
                    addProductBtn.setDisable(isPaid); addServiceBtn.setDisable(isPaid); removeItemBtn.setDisable(isPaid); saveNotesBtn.setDisable(isPaid);
                    completePaymentBtn.setDisable(isPaid);
                    printReceiptBtn.setDisable(!isPaid);
                    cancelSaleBtn.setDisable(isPaid);
                }, () -> {
                    showAlert("Error", "Selected sale not found. Please refresh.", Alert.AlertType.ERROR);
                    invoiceLabel.setText(""); dateLabel.setText(""); customerLabel.setText(""); vehicleLabel.setText("");
                    totalLabelValue.setText(""); statusLabel.setText(""); itemsTable.getItems().clear(); notesArea.setText("");
                    addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
                    completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); cancelSaleBtn.setDisable(true);
                });
            } else {
                invoiceLabel.setText(""); dateLabel.setText(""); customerLabel.setText(""); vehicleLabel.setText("");
                totalLabelValue.setText(""); statusLabel.setText(""); itemsTable.getItems().clear(); notesArea.setText("");
                addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
                completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); cancelSaleBtn.setDisable(true);
            }
        });

        addProductBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && !selectedSale.isPaid()) {
                showAddProductDialog(selectedSale, stage);
                salesService.getSaleById(selectedSale.getId()).ifPresent(saleComboBox::setValue); // Refresh details by re-triggering combo box action
            }
        });
        addServiceBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && !selectedSale.isPaid()) {
                showAddServiceDialog(selectedSale, stage);
                 salesService.getSaleById(selectedSale.getId()).ifPresent(saleComboBox::setValue);
            }
        });
        
        // THIS IS THE PROBLEMATIC HANDLER - FIX IT HERE
        removeItemBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            SaleItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null && !selectedSale.isPaid() && selectedItem != null) {
                salesService.removeItemFromSale(selectedSale.getId(), selectedItem.getId()).ifPresent(updatedSale -> {
                    refreshSales(); 
                    saleComboBox.setValue(updatedSale);
                    
                    // Refresh dashboard after inventory changes
                    if (mainView != null) {
                        mainView.refreshDashboard();
                    }
                });
            } else if (selectedItem == null) {
                showAlert("No Selection", "Please select an item to remove.", Alert.AlertType.WARNING);
            } else if (selectedSale != null && selectedSale.isPaid()) {
                 showAlert("Sale Paid", "Cannot modify items on a paid sale.", Alert.AlertType.WARNING);
            }
        });
        
        saveNotesBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && !selectedSale.isPaid()) {
                salesService.updateSaleNotes(selectedSale.getId(), notesArea.getText()).ifPresent(updatedSale -> {
                    showAlert("Notes Saved", "Sale notes have been updated.", Alert.AlertType.INFORMATION);
                    refreshSales(); 
                    saleComboBox.setValue(null);
                    saleComboBox.setValue(updatedSale); 
                });
            }  else if (selectedSale != null && selectedSale.isPaid()) {
                 showAlert("Sale Paid", "Cannot modify notes on a paid sale.", Alert.AlertType.WARNING);
            }
        });

        HBox actionButtons = new HBox(10);
        actionButtons.getChildren().addAll(completePaymentBtn, printReceiptBtn, cancelSaleBtn);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        completePaymentBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null) {
                 salesService.getSaleById(selectedSale.getId()).ifPresent(freshSale -> {
                    if (freshSale.isPaid()) {
                        showAlert("Already Paid", "This sale has already been paid.", Alert.AlertType.INFORMATION);
                    } else {
                        processSalePayment(freshSale, stage); 
                        refreshSaleComboBtn.fire(); 
                    }
                });
            } else {
                showAlert("No Selection", "Please select a sale to complete payment.", Alert.AlertType.WARNING);
            }
        });
        printReceiptBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    printReceipt(selectedSale, stage);
                } else {
                    showAlert("Not Paid", "This sale has not been paid yet.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("No Selection", "Please select a sale to print receipt for.", Alert.AlertType.WARNING);
            }
        });
        cancelSaleBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && !selectedSale.isPaid()) {
                if (showConfirmationDialog("Cancel Sale", 
                        "Are you sure you want to cancel this sale? All items will be returned to inventory.",
                        stage)) {
                    boolean canceled = salesService.cancelSale(selectedSale.getId());
                    if (canceled) {
                        refreshSaleComboBtn.fire(); 
                        saleComboBox.setValue(null);
                        
                        // Refresh dashboard after inventory changes
                        if (mainView != null) {
                            mainView.refreshDashboard();
                        }
                    } else {
                        showAlert("Error", "Could not cancel the sale.", Alert.AlertType.ERROR);
                    }
                }
            } else if (selectedSale != null && selectedSale.isPaid()) {
                 showAlert("Sale Paid", "Cannot cancel a paid sale.", Alert.AlertType.WARNING);
            }
        });

        // Initial button states
        addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
        completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); cancelSaleBtn.setDisable(true);

        HBox selectionControls = new HBox(10);
        selectionControls.getChildren().addAll(new Label("Select Sale:"), saleComboBox, refreshSaleComboBtn);
        
        content.getChildren().addAll(
                titleLabel,
                selectionControls,
                saleInfoPane,
                itemsPane,
                notesPane,
                actionButtons
        );
        
        refreshSaleComboBtn.fire(); // Initial loading of sales into ComboBox
        
        tab.setContent(new ScrollPane(content));
        return tab;
    }
    
    // Add minimal implementations of required methods
    
    private HBox createTopControls(Stage stage) {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.getStyleClass().add("spacing-10");
        
        Button newSaleBtn = new Button("New Sale");
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("secondary");
        
        Button completeBtn = new Button("Complete Sale");
        completeBtn.getStyleClass().add("complete-sale-button");
        
        Button cancelBtn = new Button("Cancel Sale");
        cancelBtn.getStyleClass().add("cancel-sale-button");
        
        Button printBtn = new Button("Print Receipt");
        printBtn.getStyleClass().add("print-button");
        
        Button settingsBtn = new Button("Printer Settings");
        settingsBtn.getStyleClass().add("secondary");
        
        // New Sale button action
        newSaleBtn.setOnAction(e -> showNewSaleDialog(stage));
        
        // View Details button action
        viewBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                // Find the Details Tab (assuming it's the second tab, index 1)
                if (salesPane.getCenter() instanceof TabPane) {
                    TabPane mainSalesTabs = (TabPane) salesPane.getCenter();
                    if (mainSalesTabs.getTabs().size() > 1) {
                        mainSalesTabs.getSelectionModel().select(1); // Select Details Tab
                        // Now, find the ComboBox in that tab and set its value
                        ComboBox<Sale> detailsSaleComboBox = findSaleComboBoxInDetailsTab(mainSalesTabs.getTabs().get(1));
                        if (detailsSaleComboBox != null) {
                            detailsSaleComboBox.setValue(selectedSale);
                        }
                    }
                }
            } else {
                showAlert("No Selection", "Please select a sale to view.", Alert.AlertType.WARNING);
            }
        });
        
        // Complete Sale button action
        completeBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    showAlert("Already Paid", "This sale has already been paid.", Alert.AlertType.INFORMATION);
                } else {
                    processSalePayment(selectedSale, stage);
                }
            } else {
                showAlert("No Selection", "Please select a sale to complete.", Alert.AlertType.WARNING);
            }
        });
        
        // Cancel Sale button action
        cancelBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (showConfirmationDialog("Cancel Sale", 
                        "Are you sure you want to cancel this sale? All items will be returned to inventory.",
                        stage)) {
                    boolean canceled = salesService.cancelSale(selectedSale.getId());
                    if (canceled) {
                        refreshSales();
                        
                        // Refresh dashboard after inventory changes
                        if (mainView != null) {
                            mainView.refreshDashboard();
                        }
                    } else {
                        showAlert("Error", "Could not cancel the sale.", Alert.AlertType.ERROR);
                    }
                }
            } else {
                showAlert("No Selection", "Please select a sale to cancel.", Alert.AlertType.WARNING);
            }
        });
        
        // Print Receipt button action
        printBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    printReceipt(selectedSale, stage);
                } else {
                    showAlert("Not Paid", "This sale has not been paid yet.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("No Selection", "Please select a sale to print receipt for.", Alert.AlertType.WARNING);
            }
        });
        
        // Printer Settings button action
        settingsBtn.setOnAction(e -> showPrinterSettingsDialog(stage));
        
        controls.getChildren().addAll(newSaleBtn, viewBtn, completeBtn, cancelBtn, printBtn, settingsBtn);
        return controls;
    }
    
    private TableView<Sale> createSalesTable() {
        TableView<Sale> table = new TableView<>();
        table.getStyleClass().add("sales-table");
        
        // ID Column
        TableColumn<Sale, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getId()));
        idColumn.setPrefWidth(50);
        
        // Invoice Number Column
        TableColumn<Sale, String> invoiceColumn = new TableColumn<>("Invoice #");
        invoiceColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getInvoiceNumber()));
        invoiceColumn.setPrefWidth(150);
        
        // Date Column
        TableColumn<Sale, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return new SimpleStringProperty(
                    cellData.getValue().getTimestamp().format(formatter));
        });
        dateColumn.setPrefWidth(140);
        
        // Customer Column
        TableColumn<Sale, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? 
                    customer.getFirstName() + " " + customer.getLastName() : "");
        });
        customerColumn.setPrefWidth(150);
        
        // Total Column
        TableColumn<Sale, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getTotal();
            return new SimpleStringProperty(total != null ? "$" + total : "");
        });
        totalColumn.setPrefWidth(100);
        
        // Payment Method Column
        TableColumn<Sale, String> paymentColumn = new TableColumn<>("Payment");
        paymentColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getPaymentMethod()));
        paymentColumn.setPrefWidth(120);
        
        // Status Column with styled cells
        TableColumn<Sale, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().isPaid() ? "Paid" : "Pending"));
        statusColumn.setPrefWidth(80);
        
        // Use custom cell factory for status column to apply styles
        statusColumn.setCellFactory(column -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-cell", "paid", "pending");
                } else {
                    setText(item);
                    getStyleClass().removeAll("status-cell", "paid", "pending");
                    getStyleClass().add("status-cell");
                    if ("Paid".equals(item)) {
                        getStyleClass().add("paid");
                    } else if ("Pending".equals(item)) {
                        getStyleClass().add("pending");
                    }
                }
            }
        });
        
        table.getColumns().addAll(idColumn, invoiceColumn, dateColumn, customerColumn, 
                totalColumn, paymentColumn, statusColumn);
        
        return table;
    }
    
    public void refreshSales() {
        List<Sale> sales = salesService.getAllSales();
        salesList = FXCollections.observableArrayList(sales);
        if (salesTable != null) {
            salesTable.setItems(salesList);
        }
    }
    
    /**
     * Process payment for a sale
     * @param sale The sale to process payment for
     * @param owner The owner window
     */
    private void processSalePayment(Sale sale, Stage owner) {
        boolean success = paymentController.showPaymentDialog(sale, owner);
        if (success) {
            refreshSales();
            showAlert("Payment Successful", "The sale has been completed successfully.", Alert.AlertType.INFORMATION);
            
            // Refresh dashboard with updated sales data
            if (mainView != null) {
                mainView.refreshDashboard();
            }
        }
    }
    
    /**
     * Show a confirmation dialog
     * @param title The dialog title
     * @param message The dialog message
     * @param owner The owner window
     * @return true if confirmed, false otherwise
     */
    private boolean showConfirmationDialog(String title, String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Show an alert dialog
     * @param title The dialog title
     * @param message The dialog message
     * @param type The alert type
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Add stubs for methods called in the event handlers
    
    private void showAddProductDialog(Sale sale, Stage owner) {
        // This would show a dialog to select a product and quantity
        // Then add it to the sale
        // For demo, we'll use a placeholder product
        
        Dialog<ProductSelection> dialog = new Dialog<>();
        dialog.setTitle("Add Product");
        dialog.setHeaderText("Select Product and Quantity");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Add to Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<Product> productComboBox = new ComboBox<>();
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 100, 1);
        
        // Load products
        List<Product> products = inventoryService.getAllProducts();
        productComboBox.setItems(FXCollections.observableArrayList(products));
        
        // Set converter to display product name
        productComboBox.setConverter(new javafx.util.StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }
            
            @Override
            public Product fromString(String string) {
                return productComboBox.getItems().stream()
                        .filter(product -> product.getName().equals(string))
                        .findFirst().orElse(null);
            }
        });
        
        grid.add(new Label("Product:"), 0, 0);
        grid.add(productComboBox, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(quantitySpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Product selectedProduct = productComboBox.getValue();
                Integer quantity = quantitySpinner.getValue();
                
                if (selectedProduct != null && quantity != null) {
                    return new ProductSelection(selectedProduct.getId(), quantity);
                }
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<ProductSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Optional<Sale> updatedSale = salesService.addProductToSale(
                    sale.getId(), selection.productId, selection.quantity);
            
            if (updatedSale.isEmpty()) {
                showAlert("Error", "Could not add product to sale. Check inventory.", Alert.AlertType.ERROR);
            } else {
                // Refresh dashboard after inventory change
                if (mainView != null) {
                    mainView.refreshDashboard();
                }
            }
        });
    }
    
    private void showAddServiceDialog(Sale sale, Stage owner) {
        Dialog<ServiceSelection> dialog = new Dialog<>();
        dialog.setTitle("Add Service");
        dialog.setHeaderText("Select Service and Technician");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Add to Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<Service> serviceComboBox = new ComboBox<>();
        ComboBox<Technician> technicianComboBox = new ComboBox<>();
        
        // Load services
        List<Service> services = salesService.getAllServices();
        if (services.isEmpty()) {
            // If no services found, show a message and return
            dialog.close();
            showAlert("No Services", "No services available. Please add services first.", Alert.AlertType.WARNING);
            return;
        }
        
        serviceComboBox.setItems(FXCollections.observableArrayList(services));
        
        // Load technicians
        List<Technician> technicians = salesService.getAllTechnicians();
        if (technicians.isEmpty()) {
            // If no technicians found, show a message and return
            dialog.close();
            showAlert("No Technicians", "No technicians available. Please add technicians first.", Alert.AlertType.WARNING);
            return;
        }
        
        technicianComboBox.setItems(FXCollections.observableArrayList(technicians));
        
        // Set converter to display service name
        serviceComboBox.setConverter(new javafx.util.StringConverter<Service>() {
            @Override
            public String toString(Service service) {
                return service != null ? service.getName() + " - $" + service.getPrice() : "";
            }
            
            @Override
            public Service fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // Set converter to display technician name
        technicianComboBox.setConverter(new javafx.util.StringConverter<Technician>() {
            @Override
            public String toString(Technician technician) {
                return technician != null ? technician.getFirstName() + " " + technician.getLastName() : "";
            }
            
            @Override
            public Technician fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        grid.add(new Label("Service:"), 0, 0);
        grid.add(serviceComboBox, 1, 0);
        grid.add(new Label("Technician:"), 0, 1);
        grid.add(technicianComboBox, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on service combobox by default
        Platform.runLater(serviceComboBox::requestFocus);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Service selectedService = serviceComboBox.getValue();
                Technician selectedTechnician = technicianComboBox.getValue();
                
                if (selectedService != null && selectedTechnician != null) {
                    return new ServiceSelection(selectedService.getId(), selectedTechnician.getId());
                }
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<ServiceSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Optional<Sale> updatedSale = salesService.addServiceToSale(
                    sale.getId(), selection.serviceId, selection.technicianId);
            
            if (updatedSale.isEmpty()) {
                showAlert("Error", "Could not add service to sale.", Alert.AlertType.ERROR);
            } else {
                // Refresh dashboard after adding service
                if (mainView != null) {
                    mainView.refreshDashboard();
                }
            }
        });
    }
    
    private void printReceipt(Sale sale, Stage owner) {
        // Create print options dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Print Receipt");
        dialog.setHeaderText("Receipt Printing Options");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType printButtonType = new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(printButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        ComboBox<String> printerComboBox = new ComboBox<>();
        printerComboBox.getItems().add("Default Printer");
        
        // Add available printers
        List<String> printers = printerService.getAvailablePrinters();
        if (!printers.isEmpty()) {
            printerComboBox.getItems().addAll(printers);
        }
        printerComboBox.setValue("Default Printer");
        
        content.getChildren().addAll(
                new Label("Select Printer:"),
                printerComboBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> dialogButton == printButtonType);
        
        // Show dialog and process result
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            String printerNameFromDialog = printerComboBox.getValue();
            if ("Default Printer".equals(printerNameFromDialog)) {
                printerNameFromDialog = null; // Let PrinterService use its default or system default
            }
            
            boolean printed = printerService.printReceipt(sale, printerNameFromDialog);
            if (printed) {
                showAlert("Receipt Printed", 
                        "Receipt for invoice " + sale.getInvoiceNumber() + " has been sent to printer.", 
                        Alert.AlertType.INFORMATION);
            } else {
                showAlert("Print Error", "Failed to print receipt. Please check printer connection.", 
                        Alert.AlertType.ERROR);
            }
        }
    }
    
    // Needed helper classes
    
    /**
     * Helper class for product selections
     */
    private static class ProductSelection {
        private final Long productId;
        private final int quantity;
        
        public ProductSelection(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
    
    /**
     * Helper class for service selections
     */
    private static class ServiceSelection {
        private final Long serviceId;
        private final Long technicianId;
        
        public ServiceSelection(Long serviceId, Long technicianId) {
            this.serviceId = serviceId;
            this.technicianId = technicianId;
        }
    }
    
    /**
     * Helper class for custom item selections
     */
    private static class CustomItemSelection {
        private final String name;
        private final BigDecimal price;
        private final int quantity;
        
        public CustomItemSelection(String name, BigDecimal price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
    
    /**
     * Helper class for customer selections
     */
    private static class CustomerSelection {
        private final Long customerId;
        private final Long vehicleId;
        
        public CustomerSelection(Long customerId, Long vehicleId) {
            this.customerId = customerId;
            this.vehicleId = vehicleId;
        }
    }

    /**
     * Show printer settings dialog
     * @param owner The owner window
     */
    private void showPrinterSettingsDialog(Stage owner) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Printer Settings");
        dialog.setHeaderText("Configure Receipt Printer");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Get available printers
        List<String> printers = printerService.getAvailablePrinters();
        ComboBox<String> printerComboBox = new ComboBox<>();
        
        // Add "Default" option
        printerComboBox.getItems().add("Default Printer");
        
        // Add available printers
        if (!printers.isEmpty()) {
            printerComboBox.getItems().addAll(printers);
            printerComboBox.setValue(printers.get(0));
        } else {
            printerComboBox.setValue("Default Printer");
        }
        
        // Test print button
        Button testBtn = new Button("Print Test Receipt");
        testBtn.setOnAction(e -> {
            String printer = printerComboBox.getValue();
            if ("Default Printer".equals(printer)) {
                printer = null;
            }
            
            // For testing, create a dummy sale
            Sale dummySale = new Sale();
            dummySale.setInvoiceNumber("TEST-123456");
            dummySale.setPaymentType(PaymentType.CASH);
            
            // Use settingsService to get company info for the dummy sale for a more realistic test
            dummySale.getCustomer(); // This is just to avoid a null if PrinterService expects it.
                                    // A better dummy sale would be constructed.

            if (printerService.printReceipt(dummySale, printer)) {
                showAlert("Test Print", "Test receipt sent to printer.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to print test receipt.", Alert.AlertType.ERROR);
            }
        });
        
        content.getChildren().addAll(
                new Label("Select Receipt Printer:"),
                printerComboBox,
                testBtn
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String printer = printerComboBox.getValue();
                if ("Default Printer".equals(printer)) {
                    return null;
                }
                return printer;
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(printerName -> {
            printerService.setDefaultPrinterName(printerName);
            showAlert("Settings Saved", "Printer settings have been saved.", Alert.AlertType.INFORMATION);
        });
    }
    
    /**
     * Show dialog to create a new sale
     * @param owner The owner window
     */
    private void showNewSaleDialog(Stage owner) {
        Dialog<CustomerSelection> dialog = new Dialog<>();
        dialog.setTitle("New Sale");
        dialog.setHeaderText("Create New Sale");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType createButtonType = new ButtonType("Create Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox contentVBox = new VBox(10);
        contentVBox.setPadding(new Insets(10));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Customer selection (optional)
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Select Customer (Optional)");
        
        // Add "None" option
        Button addCustomerBtn = new Button("+");
        addCustomerBtn.setTooltip(new Tooltip("Add New Customer"));
        
        // Vehicle selection (optional)
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPromptText("Select Vehicle (Optional)");
        vehicleComboBox.setDisable(true); // Initially disabled
        
        Button addVehicleBtn = new Button("+");
        addVehicleBtn.setTooltip(new Tooltip("Add New Vehicle"));
        addVehicleBtn.setDisable(true); // Initially disabled
        
        // Load customers from database
        List<Customer> customers = salesService.getAllCustomers();
        customerComboBox.getItems().addAll(customers);
        
        // Set converter to display customer name
        customerComboBox.setConverter(new javafx.util.StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer != null ? customer.getFirstName() + " " + customer.getLastName() : "";
            }
            
            @Override
            public Customer fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // When customer is selected, load their vehicles
        customerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            vehicleComboBox.getItems().clear();
            
            if (newVal != null) {
                vehicleComboBox.setDisable(false);
                addVehicleBtn.setDisable(false);
                
                // Load vehicles for selected customer
                List<Vehicle> vehicles = salesService.getVehiclesForCustomer(newVal.getId());
                vehicleComboBox.getItems().addAll(vehicles);
            } else {
                vehicleComboBox.setDisable(true);
                addVehicleBtn.setDisable(true);
            }
        });
        
        // Set converter to display vehicle info
        vehicleComboBox.setConverter(new javafx.util.StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                return vehicle != null ? vehicle.toString() : "";
            }
            
            @Override
            public Vehicle fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        // Create layout for customer selection with add button
        HBox customerBox = new HBox(5);
        customerBox.getChildren().addAll(customerComboBox, addCustomerBtn);
        HBox.setHgrow(customerComboBox, javafx.scene.layout.Priority.ALWAYS);
        
        // Create layout for vehicle selection with add button
        HBox vehicleBox = new HBox(5);
        vehicleBox.getChildren().addAll(vehicleComboBox, addVehicleBtn);
        HBox.setHgrow(vehicleComboBox, javafx.scene.layout.Priority.ALWAYS);
        
        grid.add(new Label("Customer:"), 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(new Label("Vehicle:"), 0, 1);
        grid.add(vehicleBox, 1, 1);
        
        // Add customer button action
        addCustomerBtn.setOnAction(e -> {
            if (mainView != null) {
                mainView.showAddCustomerDialog();
                // Refresh customer ComboBox after adding
                List<Customer> updatedCustomers = salesService.getAllCustomers();
                Customer lastAddedCustomer = updatedCustomers.isEmpty() ? null : updatedCustomers.get(updatedCustomers.size() - 1);
                customerComboBox.setItems(FXCollections.observableArrayList(updatedCustomers));
                if (lastAddedCustomer != null) {
                    customerComboBox.setValue(lastAddedCustomer);
                }
            } else {
                 showAlert("Error", "MainView reference not available.", Alert.AlertType.ERROR);
            }
        });
        
        // Add vehicle button action
        addVehicleBtn.setOnAction(e -> {
            Customer selectedCustomer = customerComboBox.getValue();
            if (selectedCustomer == null) {
                showAlert("No Customer Selected", "Please select a customer before adding a vehicle.", Alert.AlertType.WARNING);
                return;
            }
            if (mainView != null) {
                mainView.showAddVehicleDialog(selectedCustomer);
                // Refresh vehicle ComboBox after adding
                List<Vehicle> updatedVehicles = salesService.getVehiclesForCustomer(selectedCustomer.getId());
                Vehicle lastAddedVehicle = updatedVehicles.isEmpty() ? null : updatedVehicles.get(updatedVehicles.size() -1);
                vehicleComboBox.setItems(FXCollections.observableArrayList(updatedVehicles));
                if (lastAddedVehicle != null) {
                    vehicleComboBox.setValue(lastAddedVehicle);
                }
            } else {
                showAlert("Error", "MainView reference not available.", Alert.AlertType.ERROR);
            }
        });
        
        // Add grid to content
        contentVBox.getChildren().add(grid);
        
        // Add separator
        contentVBox.getChildren().add(new Separator());
        
        // Add a label for items section
        Label itemsLabel = new Label("Items will appear here after creating the sale");
        itemsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        contentVBox.getChildren().add(itemsLabel);
        
        dialog.getDialogPane().setContent(contentVBox);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Customer selectedCustomer = customerComboBox.getValue();
                Vehicle selectedVehicle = vehicleComboBox.getValue();
                
                Long customerId = selectedCustomer != null ? selectedCustomer.getId() : null;
                Long vehicleId = selectedVehicle != null ? selectedVehicle.getId() : null;
                
                return new CustomerSelection(customerId, vehicleId);
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<CustomerSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Sale newSale = salesService.createSale(selection.customerId, selection.vehicleId);
            if (newSale != null) {
                refreshSales();
                
                // Auto-select the new sale
                salesTable.getSelectionModel().select(newSale);
                
                // Show sale details dialog to add items
                showSaleDetailsDialog(newSale, owner);
            } else {
                showAlert("Error", "Could not create a new sale.", Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Show dialog with sale details
     * @param sale The sale to show details for
     * @param owner The owner window
     */
    private void showSaleDetailsDialog(Sale sale, Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sale Details");
        dialog.setHeaderText("Invoice: " + sale.getInvoiceNumber());
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        // Make dialog resizable
        dialog.setResizable(true);
        
        // Set dialog size
        dialog.getDialogPane().setPrefSize(800, 600);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        // Customer info
        GridPane customerGrid = new GridPane();
        customerGrid.setHgap(10);
        customerGrid.setVgap(5);
        
        Customer customer = sale.getCustomer();
        if (customer != null) {
            customerGrid.add(new Label("Customer:"), 0, 0);
            customerGrid.add(new Label(customer.getFirstName() + " " + customer.getLastName()), 1, 0);
            customerGrid.add(new Label("Phone:"), 0, 1);
            customerGrid.add(new Label(customer.getPhone()), 1, 1);
        }
        
        Vehicle vehicle = sale.getVehicle();
        if (vehicle != null) {
            customerGrid.add(new Label("Vehicle:"), 0, 2);
            customerGrid.add(new Label(vehicle.toString()), 1, 2);
        }
        
        // Items table
        TableView<SaleItem> itemsTable = new TableView<>();
        
        TableColumn<SaleItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getItemType()));
        
        TableColumn<SaleItem, String> nameColumn = new TableColumn<>("Item");
        nameColumn.setCellValueFactory(cellData -> {
            SaleItem item = cellData.getValue();
            return new SimpleStringProperty(item.getItemName());
        });
        
        TableColumn<SaleItem, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getUnitPrice();
            return new SimpleStringProperty("$" + price);
        });
        
        TableColumn<SaleItem, Number> qtyColumn = new TableColumn<>("Qty");
        qtyColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getQuantity()));
        
        TableColumn<SaleItem, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getSubtotal();
            return new SimpleStringProperty("$" + total);
        });
        
        itemsTable.getColumns().addAll(typeColumn, nameColumn, priceColumn, qtyColumn, totalColumn);
        
        // Add items to table
        ObservableList<SaleItem> items = FXCollections.observableArrayList(sale.getItems());
        itemsTable.setItems(items);
        
        // Totals
        GridPane totalsGrid = new GridPane();
        totalsGrid.setHgap(10);
        totalsGrid.setVgap(5);
        
        Label subtotalLabel = new Label("$" + sale.getSubtotal());
        Label taxLabel = new Label("$" + sale.getTax());
        Label ccFeeLabel = new Label(); // For Credit Card Fee
        Label totalLabelValue = new Label("$" + sale.getTotal()); // Renamed from totalLabel
        
        int rowIndex = 0;
        totalsGrid.add(new Label("Subtotal:"), 0, rowIndex); totalsGrid.add(subtotalLabel, 1, rowIndex++);
        totalsGrid.add(new Label("Tax:"), 0, rowIndex); totalsGrid.add(taxLabel, 1, rowIndex++);
        
        // Credit Card Fee row (conditionally visible)
        Label ccFeeTextLabel = new Label("Credit Card Fee:");
        if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            ccFeeLabel.setText("$" + String.format("%.2f", sale.getCreditCardFeeAmount()));
            totalsGrid.add(ccFeeTextLabel, 0, rowIndex); totalsGrid.add(ccFeeLabel, 1, rowIndex++);
        } else {
            ccFeeLabel.setText("$0.00"); // Keep consistent structure or hide row
        }
        totalsGrid.add(new Label("Total:"), 0, rowIndex); totalsGrid.add(totalLabelValue, 1, rowIndex++);
        
        // Add/remove buttons
        HBox itemControls = new HBox(10);
        Button addProductBtn = new Button("Add Product");
        Button addCustomItemBtn = new Button("Add Custom Item");
        Button addServiceBtn = new Button("Add Service");
        Button removeItemBtn = new Button("Remove Item");
        
        addProductBtn.setOnAction(e -> {
            // Show dialog to add product
            showAddProductDialog(sale, owner);
            
            // Refresh items list with fresh data from database
            Sale refreshedSale = salesService.getSaleById(sale.getId()).orElse(sale);
            ObservableList<SaleItem> updatedItems = FXCollections.observableArrayList(refreshedSale.getItems());
            itemsTable.setItems(updatedItems);
            
            // Update totals with refreshed sale
            updateTotals(refreshedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
        });
        
        addCustomItemBtn.setOnAction(e -> {
            // Show dialog to add custom item
            showAddCustomItemDialog(sale, owner);
            
            // Refresh items list
            ObservableList<SaleItem> updatedItems = FXCollections.observableArrayList(
                salesService.getSaleById(sale.getId()).orElse(sale).getItems());
            itemsTable.setItems(updatedItems);
            
            // Update totals
            updateTotals(salesService.getSaleById(sale.getId()).orElse(sale), subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
        });
        
        addServiceBtn.setOnAction(e -> {
            // Show dialog to add service
            showAddServiceDialog(sale, owner);
            
            // Refresh items list with fresh data from database
            Sale refreshedSale = salesService.getSaleById(sale.getId()).orElse(sale);
            ObservableList<SaleItem> updatedItems = FXCollections.observableArrayList(refreshedSale.getItems());
            itemsTable.setItems(updatedItems);
            
            // Update totals with refreshed sale
            updateTotals(refreshedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
        });
        
        removeItemBtn.setOnAction(e -> {
            SaleItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                Optional<Sale> updatedSaleOpt = salesService.removeItemFromSale(sale.getId(), selectedItem.getId());
                if (updatedSaleOpt.isPresent()) {
                    Sale updatedSale = updatedSaleOpt.get();
                    // Refresh items list with the updated sale
                    itemsTable.setItems(FXCollections.observableArrayList(updatedSale.getItems()));
                    
                    // Update the sale in the list
                    refreshSales();
                    
                    // Update totals
                    updateTotals(updatedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
                    
                    // Refresh dashboard after inventory changes
                    if (mainView != null) {
                        mainView.refreshDashboard();
                    }
                }
            } else {
                showAlert("No Selection", "Please select an item to remove.", Alert.AlertType.WARNING);
            }
        });
        
        itemControls.getChildren().addAll(addProductBtn, addCustomItemBtn, addServiceBtn, removeItemBtn);
        
        // Payment info
        GridPane paymentGrid = new GridPane();
        paymentGrid.setHgap(10);
        paymentGrid.setVgap(5);
        
        paymentGrid.add(new Label("Payment Status:"), 0, 0);
        paymentGrid.add(new Label(sale.isPaid() ? "Paid" : "Pending"), 1, 0);
        
        if (sale.isPaid()) {
            paymentGrid.add(new Label("Payment Method:"), 0, 1);
            paymentGrid.add(new Label(sale.getPaymentMethod()), 1, 1);
            
            // Add payment details based on type
            if (sale.getPaymentType() == PaymentType.CREDIT_CARD || 
                    sale.getPaymentType() == PaymentType.DEBIT_CARD) {
                paymentGrid.add(new Label("Card Type:"), 0, 2);
                paymentGrid.add(new Label(sale.getCardType() != null ? sale.getCardType() : ""), 1, 2);
                
                paymentGrid.add(new Label("Card Number:"), 0, 3);
                paymentGrid.add(new Label(sale.getCardLastFour() != null ? 
                        "XXXX-XXXX-XXXX-" + sale.getCardLastFour() : ""), 1, 3);
            } else if (sale.getPaymentType() == PaymentType.CHECK) {
                paymentGrid.add(new Label("Check Number:"), 0, 2);
                paymentGrid.add(new Label(sale.getCheckNumber() != null ? sale.getCheckNumber() : ""), 1, 2);
            }
        }
        
        // Add all components to content
        content.getChildren().addAll(
                new Label("Customer Information:"),
                customerGrid,
                new Separator(),
                new Label("Sale Items:"),
                itemsTable,
                itemControls,
                new Separator(),
                new Label("Totals:"),
                totalsGrid,
                new Separator(),
                new Label("Payment Information:"),
                paymentGrid
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        
        // Set content
        dialog.getDialogPane().setContent(scrollPane);
        
        // Add buttons
        ButtonType doneButton = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(doneButton);
        
        if (!sale.isPaid()) {
            ButtonType payButton = new ButtonType("Process Payment", ButtonBar.ButtonData.APPLY);
            dialog.getDialogPane().getButtonTypes().add(payButton);
            
            // Handle payment button
            Button payBtn = (Button) dialog.getDialogPane().lookupButton(payButton);
            payBtn.setOnAction(e -> {
                dialog.close();
                processSalePayment(sale, owner);
            });
        }
        
        // Show dialog
        dialog.showAndWait();
        
        // Refresh sales list after dialog is closed
        refreshSales();
    }
    
    /**
     * Show dialog to add a custom item to a sale
     * @param sale The sale to add custom item to
     * @param owner The owner window
     */
    private void showAddCustomItemDialog(Sale sale, Stage owner) {
        Dialog<CustomItemSelection> dialog = new Dialog<>();
        dialog.setTitle("Add Custom Item");
        dialog.setHeaderText("Add a custom item to the sale");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Add to Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Item Description");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Unit Price");
        
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 100, 1);
        
        grid.add(new Label("Description:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Unit Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantitySpinner, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default
        Platform.runLater(nameField::requestFocus);
        
        // Validate input
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Missing Information", "Please enter a description for the item.", Alert.AlertType.ERROR);
                event.consume();
            } else {
                try {
                    new BigDecimal(priceField.getText());
                } catch (NumberFormatException e) {
                    showAlert("Invalid Price", "Please enter a valid price.", Alert.AlertType.ERROR);
                    event.consume();
                }
            }
        });
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String name = nameField.getText();
                BigDecimal price = new BigDecimal(priceField.getText());
                Integer quantity = quantitySpinner.getValue();
                
                return new CustomItemSelection(name, price, quantity);
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<CustomItemSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Optional<Sale> updatedSale = salesService.addCustomItemToSale(
                    sale.getId(), selection.name, selection.price, selection.quantity);
            
            if (updatedSale.isEmpty()) {
                showAlert("Error", "Could not add custom item to sale.", Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Update totals in the UI
     */
    private void updateTotals(Sale sale, Label subtotalLabel, Label taxLabel, Label ccFeeLabel, Label totalLabelValue) {
        subtotalLabel.setText("$" + String.format("%.2f", sale.getSubtotal()));
        taxLabel.setText("$" + String.format("%.2f", sale.getTax()));
        if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            ccFeeLabel.setText("$" + String.format("%.2f", sale.getCreditCardFeeAmount()));
            // Make sure the row is visible if it was hidden
            // This requires ccFeeTextLabel to be accessible or manage visibility differently
        } else {
            ccFeeLabel.setText("$0.00");
        }
        totalLabelValue.setText("$" + String.format("%.2f", sale.getTotal()));
    }
    
    /**
     * Find the sale combo box in the details tab
     * @param detailsTab The details tab
     * @return The sale combo box, or null if not found
     */
    private ComboBox<Sale> findSaleComboBoxInDetailsTab(Tab detailsTab) {
        if (detailsTab != null && detailsTab.getContent() instanceof ScrollPane) {
            ScrollPane scrollPane = (ScrollPane) detailsTab.getContent();
            if (scrollPane.getContent() instanceof VBox) {
                VBox tabContentVBox = (VBox) scrollPane.getContent();
                if (!tabContentVBox.getChildren().isEmpty() && tabContentVBox.getChildren().get(1) instanceof HBox) {
                    HBox selectionControls = (HBox) tabContentVBox.getChildren().get(1);
                    if (!selectionControls.getChildren().isEmpty() && selectionControls.getChildren().get(1) instanceof ComboBox) {
                        // This is brittle, relies on exact layout structure.
                        @SuppressWarnings("unchecked")
                        ComboBox<Sale> comboBox = (ComboBox<Sale>) selectionControls.getChildren().get(1);
                        return comboBox;
                    }
                }
            }
        }
        return null;
    }
} 