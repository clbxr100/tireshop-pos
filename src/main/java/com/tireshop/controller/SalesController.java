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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for sales operations
 */
public class SalesController {
    
    // Helper class for item edit results
    private static class ItemEditResult {
        public final BigDecimal price;
        public final String description;
        
        public ItemEditResult(BigDecimal price, String description) {
            this.price = price;
            this.description = description;
        }
    }
    
    private final SalesService salesService;
    private final InventoryService inventoryService;
    private final PaymentController paymentController;
    private final MainView mainView;
    private final SettingsService settingsService;
    private PrinterService printerService;
    private TableView<Sale> salesTable;
    private ObservableList<Sale> salesList;
    private BorderPane salesPane;
    private Label countLabel; // Add count label as a field

    // Pagination fields
    private int currentPage = 0;
    private static final int PAGE_SIZE = 50;
    private int totalSales = 0;
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Label pageLabel;

    private static final Logger LOGGER = Logger.getLogger(SalesController.class.getName());
    
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
        
        // Add listener to update count when table items change
        if (countLabel != null) {
            salesTable.itemsProperty().addListener((obs, oldItems, newItems) -> {
                if (newItems != null) {
                    countLabel.setText(newItems.size() + " sales shown");
                }
            });
        }
        
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
        Button returnSaleBtn = new Button("Return/Void Sale");
        Button partialReturnBtn = new Button("Partial Return");
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
        itemsTable.setPrefHeight(250);
        
        // Create enhanced item columns
        TableColumn<SaleItem, String> itemTypeColumn = new TableColumn<>("Type");
        itemTypeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getItemType()));
        itemTypeColumn.setPrefWidth(80);
                
        TableColumn<SaleItem, String> itemNameColumn = new TableColumn<>("Item");
        itemNameColumn.setCellValueFactory(cellData -> {
            SaleItem item = cellData.getValue();
            String name = item.getItemName();
            
            // For products, try to get more details
            if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                Product product = item.getProduct();
                // Include size and brand in the display
                name = product.getName();
                if (product.getSize() != null) {
                    name += " - " + product.getSize();
                }
                if (product.getManufacturer() != null) {
                    name += " (" + product.getManufacturer() + ")";
                }
            }
            
            return new SimpleStringProperty(name);
        });
        itemNameColumn.setPrefWidth(300);
        
        // Add details column for products
        TableColumn<SaleItem, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(cellData -> {
            SaleItem item = cellData.getValue();
            StringBuilder details = new StringBuilder();
            
            if ("PRODUCT".equals(item.getItemType()) && item.getProduct() != null) {
                Product product = item.getProduct();
                if (product.getTireType() != null) {
                    details.append(product.getTireType());
                }
                if (product.getUtqgTreadwear() != null) {
                    if (details.length() > 0) details.append(" | ");
                    details.append("TW: ").append(product.getUtqgTreadwear());
                }
                if (product.getSpeedRating() != null) {
                    if (details.length() > 0) details.append(" | ");
                    details.append("Speed: ").append(product.getSpeedRating());
                }
            } else if ("SERVICE".equals(item.getItemType())) {
                // For services, show technician if available
                if (item.getTechnician() != null) {
                    Technician tech = item.getTechnician();
                    details.append("Tech: ").append(tech.getFirstName()).append(" ").append(tech.getLastName());
                }
            }
            
            return new SimpleStringProperty(details.toString());
        });
        detailsColumn.setPrefWidth(200);
        
        TableColumn<SaleItem, String> itemPriceColumn = new TableColumn<>("Unit Price");
        itemPriceColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getUnitPrice()));
        itemPriceColumn.setPrefWidth(80);
        
        // Make price column editable for pending sales
        itemPriceColumn.setCellFactory(column -> new TableCell<SaleItem, String>() {
            private TextField textField;
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    SaleItem saleItem = getTableRow().getItem();
                    Sale currentSale = saleComboBox.getValue();
                    
                    // Only make editable if sale is pending (not paid)
                    if (saleItem != null && currentSale != null && !currentSale.isPaid()) {
                        setText(null);
                        setGraphic(createEditableField(saleItem, currentSale));
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
            
            private TextField createEditableField(SaleItem saleItem, Sale sale) {
                if (textField == null) {
                    textField = new TextField();
                    textField.setStyle("-fx-text-fill: black;");
                    textField.setPrefWidth(70);
                }
                
                textField.setText(saleItem.getUnitPrice().toString());
                
                textField.setOnAction(e -> updateItemPrice(saleItem, sale, textField.getText()));
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        updateItemPrice(saleItem, sale, textField.getText());
                    }
                });
                
                return textField;
            }
            
            private void updateItemPrice(SaleItem saleItem, Sale sale, String newPriceText) {
                try {
                    BigDecimal newPrice = new BigDecimal(newPriceText);
                    if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                        salesService.updateSaleItemPrice(sale.getId(), saleItem.getId(), newPrice).ifPresentOrElse(
                            updatedSale -> {
                                // Refresh the sale display
                                saleComboBox.setValue(updatedSale);
                                
                                // Refresh dashboard after price change
                                if (mainView != null) {
                                    mainView.refreshDashboard();
                                }
                            },
                            () -> showAlert("Error", "Could not update item price.", Alert.AlertType.ERROR)
                        );
                    } else {
                        showAlert("Invalid Price", "Price must be greater than or equal to zero.", Alert.AlertType.WARNING);
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Invalid Price", "Please enter a valid price.", Alert.AlertType.WARNING);
                }
            }
        });
                
        TableColumn<SaleItem, Number> qtyColumn = new TableColumn<>("Qty");
        qtyColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getQuantity()));
        qtyColumn.setPrefWidth(50);
        qtyColumn.setCellFactory(column -> new TableCell<SaleItem, Number>() {
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
                
        TableColumn<SaleItem, String> itemSubtotalColumn = new TableColumn<>("Subtotal");
        itemSubtotalColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getSubtotal()));
        itemSubtotalColumn.setPrefWidth(80);
        itemSubtotalColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        itemsTable.getColumns().addAll(itemTypeColumn, itemNameColumn, detailsColumn, 
                                      itemPriceColumn, qtyColumn, itemSubtotalColumn);
        
        // Add and remove buttons for items
        Button editPriceBtn = new Button("Edit Price");
        editPriceBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        editPriceBtn.setDisable(true);

        // Military discount toggle button
        Button militaryDiscountBtn = new Button("🎖 Apply Military Discount");
        militaryDiscountBtn.setStyle("-fx-background-color: #5d6d7e; -fx-text-fill: white;");
        militaryDiscountBtn.setDisable(true);

        HBox itemButtons = new HBox(10);
        itemButtons.getChildren().addAll(addProductBtn, addServiceBtn, removeItemBtn, editPriceBtn, militaryDiscountBtn);
        itemButtons.setPadding(new Insets(5));
        
        ((VBox) itemsPane.getContent()).getChildren().addAll(itemsTable, itemButtons);
        
        // Notes & PO section
        TitledPane notesPane = new TitledPane("PO # / Notes", new VBox(10));
        notesPane.setCollapsible(false);

        // PO Number field
        HBox poBox = new HBox(10);
        Label poLabel = new Label("PO #:");
        TextField poField = new TextField();
        poField.setPromptText("Purchase Order Number");
        poField.setPrefWidth(200);
        Button savePoBtn = new Button("Save PO");
        savePoBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        poBox.getChildren().addAll(poLabel, poField, savePoBtn);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter notes about this sale here...");
        notesArea.setWrapText(true);
        notesArea.setPrefHeight(80);

        ((VBox) notesPane.getContent()).getChildren().addAll(poBox, notesArea, saveNotesBtn);
        ((VBox) notesPane.getContent()).setPadding(new Insets(10));

        // Save PO button action
        savePoBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && !selectedSale.isPaid()) {
                selectedSale.setPoNumber(poField.getText());
                salesService.updateSale(selectedSale);
                showAlert("PO Saved", "Purchase Order number has been saved.", Alert.AlertType.INFORMATION);
                refreshSales();
            }
        });
        
        // Military discount toggle handler
        militaryDiscountBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale == null || selectedSale.isPaid()) {
                return;
            }

            boolean hasDiscount = selectedSale.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
            if (hasDiscount) {
                // Remove the discount
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Remove Discount");
                confirm.setHeaderText("Remove the military discount from this sale?");
                confirm.initOwner(stage);
                Optional<ButtonType> confirmResult = confirm.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    salesService.removeDiscount(selectedSale.getId());
                    salesService.getSaleById(selectedSale.getId()).ifPresent(saleComboBox::setValue);
                }
            } else {
                // Apply the discount
                java.math.BigDecimal percent = settingsService.getMilitaryDiscountPercent();
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Military Discount");
                confirm.setHeaderText("Apply " + percent.stripTrailingZeros().toPlainString()
                        + "% military discount to this sale?");
                confirm.setContentText("Thank you for your service! 🇺🇸\n\nThe discount can be changed in Admin settings.");
                confirm.initOwner(stage);
                Optional<ButtonType> confirmResult = confirm.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    Optional<Sale> discounted = salesService.applyMilitaryDiscount(selectedSale.getId());
                    if (discounted.isPresent()) {
                        salesService.getSaleById(selectedSale.getId()).ifPresent(saleComboBox::setValue);
                    } else {
                        showAlert("Discount Unavailable",
                                "Could not apply the discount. Check the percentage in Admin settings.",
                                Alert.AlertType.WARNING);
                    }
                }
            }
        });

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
                // Load full sale details (customer, vehicle, items)
                Sale freshSale = salesService.getSaleByIdWithDetails(selectedSaleFromComboBox.getId());
                if (freshSale != null) {
                    String invoiceText = freshSale.getInvoiceNumber();
                    if (freshSale.hasPartialReturn()) {
                        invoiceText += " [PARTIAL RETURN]";
                    }
                    invoiceLabel.setText(invoiceText);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
                    dateLabel.setText(freshSale.getTimestamp().format(formatter));
                    Customer customer = freshSale.getCustomer();
                    customerLabel.setText(customer != null ? customer.getFullName() : "Not specified");
                    Vehicle vehicle = freshSale.getVehicle();
                    vehicleLabel.setText(vehicle != null ? vehicle.toString() : "Not specified");
                    java.math.BigDecimal discountAmt = freshSale.getDiscountAmount();
                    if (discountAmt.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        totalLabelValue.setText("$" + freshSale.getTotal() + "  (" + freshSale.getDiscountReason() + " discount: -$" + discountAmt + ")");
                    } else {
                        totalLabelValue.setText("$" + freshSale.getTotal());
                    }
                    statusLabel.setText(freshSale.isPaid() ? "Paid" : "Pending");
                    itemsTable.setItems(FXCollections.observableArrayList(freshSale.getItems()));
                    notesArea.setText(freshSale.getNotes() != null ? freshSale.getNotes() : "");
                    poField.setText(freshSale.getPoNumber() != null ? freshSale.getPoNumber() : "");

                    boolean isPaid = freshSale.isPaid();
                    boolean isVoided = freshSale.isVoided();
                    addProductBtn.setDisable(isPaid); addServiceBtn.setDisable(isPaid); removeItemBtn.setDisable(isPaid); saveNotesBtn.setDisable(isPaid);
                    poField.setDisable(isPaid); savePoBtn.setDisable(isPaid);
                    editPriceBtn.setDisable(isPaid || itemsTable.getSelectionModel().getSelectedItem() == null);
                    militaryDiscountBtn.setDisable(isPaid);
                    if (discountAmt.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        militaryDiscountBtn.setText("🎖 Remove Military Discount");
                        militaryDiscountBtn.setStyle("-fx-background-color: #922b21; -fx-text-fill: white;");
                    } else {
                        militaryDiscountBtn.setText("🎖 Apply Military Discount");
                        militaryDiscountBtn.setStyle("-fx-background-color: #5d6d7e; -fx-text-fill: white;");
                    }
                    completePaymentBtn.setDisable(isPaid);
                    printReceiptBtn.setDisable(!isPaid);
                    partialReturnBtn.setDisable(!isPaid || isVoided); // Partial return for paid, non-voided sales
                    returnSaleBtn.setDisable(!isPaid || isVoided); // Full return for paid, non-voided sales
                    cancelSaleBtn.setDisable(isPaid); // Cancel only for unpaid sales
                } else {
                    showAlert("Error", "Selected sale not found. Please refresh.", Alert.AlertType.ERROR);
                    invoiceLabel.setText(""); dateLabel.setText(""); customerLabel.setText(""); vehicleLabel.setText("");
                    totalLabelValue.setText(""); statusLabel.setText(""); itemsTable.getItems().clear(); notesArea.setText(""); poField.setText("");
                    addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
                    poField.setDisable(true); savePoBtn.setDisable(true);
                    militaryDiscountBtn.setDisable(true);
                    completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); partialReturnBtn.setDisable(true); returnSaleBtn.setDisable(true); cancelSaleBtn.setDisable(true);
                }
            } else {
                invoiceLabel.setText(""); dateLabel.setText(""); customerLabel.setText(""); vehicleLabel.setText("");
                totalLabelValue.setText(""); statusLabel.setText(""); itemsTable.getItems().clear(); notesArea.setText(""); poField.setText("");
                addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
                poField.setDisable(true); savePoBtn.setDisable(true);
                militaryDiscountBtn.setDisable(true);
                completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); returnSaleBtn.setDisable(true); cancelSaleBtn.setDisable(true);
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
        
        // Edit Price button handler
        editPriceBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            SaleItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null && !selectedSale.isPaid() && selectedItem != null) {
                showEditPriceDialog(selectedSale, selectedItem);
            } else if (selectedItem == null) {
                showAlert("No Selection", "Please select an item to edit price.", Alert.AlertType.WARNING);
            } else if (selectedSale != null && selectedSale.isPaid()) {
                showAlert("Sale Paid", "Cannot modify prices on a paid sale.", Alert.AlertType.WARNING);
            }
        });
        
        // Add table selection listener to enable/disable edit price button
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            Sale currentSale = saleComboBox.getValue();
            boolean canEdit = currentSale != null && !currentSale.isPaid() && newSelection != null;
            editPriceBtn.setDisable(!canEdit);
        });

        // Style the return buttons
        returnSaleBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        partialReturnBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        
        HBox actionButtons = new HBox(10);
        actionButtons.getChildren().addAll(completePaymentBtn, printReceiptBtn, partialReturnBtn, returnSaleBtn, cancelSaleBtn);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        completePaymentBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null) {
                Sale freshSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                if (freshSale != null) {
                    if (freshSale.isPaid()) {
                        showAlert("Already Paid", "This sale has already been paid.", Alert.AlertType.INFORMATION);
                    } else {
                        processSalePayment(freshSale, stage);
                        refreshSaleComboBtn.fire();
                    }
                }
            } else {
                showAlert("No Selection", "Please select a sale to complete payment.", Alert.AlertType.WARNING);
            }
        });
        printReceiptBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    // Reload with full details for printing
                    Sale fullSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                    if (fullSale != null) {
                        printReceipt(fullSale, stage);
                    }
                } else {
                    showAlert("Not Paid", "This sale has not been paid yet.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("No Selection", "Please select a sale to print receipt for.", Alert.AlertType.WARNING);
            }
        });
        partialReturnBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && selectedSale.isPaid() && !selectedSale.isVoided()) {
                processPartialReturn(selectedSale, stage);
            }
        });
        
        returnSaleBtn.setOnAction(e -> {
            Sale selectedSale = saleComboBox.getValue();
            if (selectedSale != null && selectedSale.isPaid() && !selectedSale.isVoided()) {
                processReturnSale(selectedSale, stage);
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
                        
                        // Refresh all relevant tabs after inventory changes
                        if (mainView != null) {
                            mainView.refreshTabsForDataChange("sale");
                        }
                        showAlert("Sale Canceled", "The sale has been canceled and items returned to inventory.", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Cancel Failed", "Could not cancel the sale. Please check the system logs for details.", Alert.AlertType.ERROR);
                    }
                }
            } else if (selectedSale != null && selectedSale.isPaid()) {
                 showAlert("Sale Paid", "Cannot cancel a paid sale. Use Return/Void instead.", Alert.AlertType.WARNING);
            }
        });

        // Initial button states
        addProductBtn.setDisable(true); addServiceBtn.setDisable(true); removeItemBtn.setDisable(true); saveNotesBtn.setDisable(true);
        poField.setDisable(true); savePoBtn.setDisable(true);
        completePaymentBtn.setDisable(true); printReceiptBtn.setDisable(true); partialReturnBtn.setDisable(true); returnSaleBtn.setDisable(true); cancelSaleBtn.setDisable(true);

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
        VBox controlsContainer = new VBox(10);
        controlsContainer.setPadding(new Insets(10));
        controlsContainer.getStyleClass().add("spacing-10");
        
        // First row: Search and filters
        HBox searchAndFilters = new HBox(10);
        searchAndFilters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search by invoice #, customer name...");
        searchField.setPrefWidth(200);
        
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("secondary");
        
        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary");
        
        // Date range filters
        Label dateLabel = new Label("Date Range:");
        DatePicker fromDate = new DatePicker();
        fromDate.setPromptText("From");
        fromDate.setPrefWidth(120);
        
        DatePicker toDate = new DatePicker();
        toDate.setPromptText("To");
        toDate.setPrefWidth(120);
        
        Button filterBtn = new Button("Filter");
        filterBtn.getStyleClass().add("secondary");
        
        // Quick filter buttons
        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().addAll("secondary", "quick-filter");
        
        Button weekBtn = new Button("This Week");
        weekBtn.getStyleClass().addAll("secondary", "quick-filter");
        
        Button allBtn = new Button("All Sales");
        allBtn.getStyleClass().addAll("secondary", "quick-filter");
        
        searchAndFilters.getChildren().addAll(
            searchField, searchBtn, clearBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            dateLabel, fromDate, toDate, filterBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            todayBtn, weekBtn, allBtn
        );
        
        // Second row: Action buttons (organized by function)
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Sale creation group
        VBox saleCreationGroup = new VBox(5);
        Label creationLabel = new Label("New Sale:");
        creationLabel.getStyleClass().add("button-group-label");
        Button newSaleBtn = new Button("New Sale");
        newSaleBtn.getStyleClass().add("primary");
        saleCreationGroup.getChildren().addAll(creationLabel, newSaleBtn);
        
        // Sale management group
        VBox managementGroup = new VBox(5);
        Label managementLabel = new Label("Manage:");
        managementLabel.getStyleClass().add("button-group-label");
        HBox managementButtons = new HBox(5);
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("secondary");
        Button completeBtn = new Button("Complete Payment");
        completeBtn.getStyleClass().add("complete-sale-button");
        Button cancelBtn = new Button("Cancel Sale");
        cancelBtn.getStyleClass().add("cancel-sale-button");
        managementButtons.getChildren().addAll(viewBtn, completeBtn, cancelBtn);
        managementGroup.getChildren().addAll(managementLabel, managementButtons);
        
        // Printing group
        VBox printingGroup = new VBox(5);
        Label printingLabel = new Label("Print:");
        printingLabel.getStyleClass().add("button-group-label");
        HBox printingButtons = new HBox(5);
        Button printBtn = new Button("Print Receipt");
        printBtn.getStyleClass().add("print-button");
        Button settingsBtn = new Button("Printer Settings");
        settingsBtn.getStyleClass().add("secondary");
        printingButtons.getChildren().addAll(printBtn, settingsBtn);
        printingGroup.getChildren().addAll(printingLabel, printingButtons);
        
        actionButtons.getChildren().addAll(saleCreationGroup, managementGroup, printingGroup);
        
        // Status display row
        HBox statusRow = new HBox(15);
        statusRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusRow.getStyleClass().add("status-bar");
        
        Label statusLabel = new Label("All Sales");
        statusLabel.getStyleClass().add("status-label");
        
        this.countLabel = new Label("0 sales shown");
        this.countLabel.getStyleClass().add("count-label");

        // Pagination controls
        this.prevPageBtn = new Button("◄ Previous");
        this.prevPageBtn.getStyleClass().add("secondary");
        this.prevPageBtn.setDisable(true);

        this.pageLabel = new Label("Page 1");
        this.pageLabel.getStyleClass().add("count-label");

        this.nextPageBtn = new Button("Next ►");
        this.nextPageBtn.getStyleClass().add("secondary");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");

        statusRow.getChildren().addAll(statusLabel, this.countLabel, spacer,
            prevPageBtn, pageLabel, nextPageBtn, refreshBtn);
        
        controlsContainer.getChildren().addAll(searchAndFilters, actionButtons, statusRow);
        
        // Search functionality
        searchBtn.setOnAction(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                List<Sale> allSales = salesService.getAllSales();
                List<Sale> filteredSales = allSales.stream()
                    .filter(sale -> 
                        (sale.getInvoiceNumber() != null && sale.getInvoiceNumber().toLowerCase().contains(searchTerm.toLowerCase())) ||
                        (sale.getCustomer() != null && 
                         sale.getCustomer().getFullName().toLowerCase().contains(searchTerm.toLowerCase()))
                    )
                    .collect(java.util.stream.Collectors.toList());
                
                salesList = FXCollections.observableArrayList(filteredSales);
                salesTable.setItems(salesList);
                statusLabel.setText("Search: \"" + searchTerm + "\"");
                this.countLabel.setText(filteredSales.size() + " sales found");
            }
        });
        
        clearBtn.setOnAction(e -> {
            searchField.clear();
            fromDate.setValue(null);
            toDate.setValue(null);
            refreshSales();
            statusLabel.setText("All Sales");
        });
        
        // Date filtering
        filterBtn.setOnAction(e -> {
            if (fromDate.getValue() != null || toDate.getValue() != null) {
                List<Sale> allSales = salesService.getAllSales();
                List<Sale> filteredSales = allSales.stream()
                    .filter(sale -> {
                        LocalDate saleDate = sale.getTimestamp().toLocalDate();
                        boolean afterFrom = fromDate.getValue() == null || !saleDate.isBefore(fromDate.getValue());
                        boolean beforeTo = toDate.getValue() == null || !saleDate.isAfter(toDate.getValue());
                        return afterFrom && beforeTo;
                    })
                    .collect(java.util.stream.Collectors.toList());
                
                salesList = FXCollections.observableArrayList(filteredSales);
                salesTable.setItems(salesList);
                
                String dateRange = "";
                if (fromDate.getValue() != null && toDate.getValue() != null) {
                    dateRange = fromDate.getValue() + " to " + toDate.getValue();
                } else if (fromDate.getValue() != null) {
                    dateRange = "from " + fromDate.getValue();
                } else {
                    dateRange = "to " + toDate.getValue();
                }
                statusLabel.setText("Filtered: " + dateRange);
                this.countLabel.setText(filteredSales.size() + " sales found");
            }
        });
        
        // Quick filters
        todayBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromDate.setValue(today);
            toDate.setValue(today);
            filterBtn.fire();
        });
        
        weekBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromDate.setValue(today.minusDays(7));
            toDate.setValue(today);
            filterBtn.fire();
        });
        
        allBtn.setOnAction(e -> {
            clearBtn.fire();
        });
        
        refreshBtn.setOnAction(e -> {
            currentPage = 0;
            refreshSales();
            statusLabel.setText("All Sales");
        });

        // Pagination button handlers
        prevPageBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshSales();
            }
        });

        nextPageBtn.setOnAction(e -> {
            int maxPage = (int) Math.ceil((double) totalSales / PAGE_SIZE) - 1;
            if (currentPage < maxPage) {
                currentPage++;
                refreshSales();
            }
        });
        
        // Action button handlers (keeping existing functionality)
        newSaleBtn.setOnAction(e -> showNewSaleDialog(stage));
        
        viewBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (salesPane.getCenter() instanceof TabPane) {
                    TabPane mainSalesTabs = (TabPane) salesPane.getCenter();
                    if (mainSalesTabs.getTabs().size() > 1) {
                        mainSalesTabs.getSelectionModel().select(1);
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
        
        completeBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    showAlert("Already Paid", "This sale has already been paid.", Alert.AlertType.INFORMATION);
                } else {
                    // Reload sale with full details
                    Sale fullSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                    if (fullSale != null) {
                        processSalePayment(fullSale, stage);
                    }
                }
            } else {
                showAlert("No Selection", "Please select a sale to complete.", Alert.AlertType.WARNING);
            }
        });

        cancelBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (showConfirmationDialog("Cancel Sale",
                        "Are you sure you want to cancel this sale? All items will be returned to inventory.",
                        stage)) {
                    boolean canceled = salesService.cancelSale(selectedSale.getId());
                    if (canceled) {
                        refreshSales();
                        if (mainView != null) {
                            mainView.refreshTabsForDataChange("sale");
                        }
                        showAlert("Sale Canceled", "The sale has been canceled and items returned to inventory.", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Cancel Failed", "Could not cancel the sale. Please check the system logs for details.", Alert.AlertType.ERROR);
                    }
                }
            } else {
                showAlert("No Selection", "Please select a sale to cancel.", Alert.AlertType.WARNING);
            }
        });

        printBtn.setOnAction(e -> {
            Sale selectedSale = salesTable.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                if (selectedSale.isPaid()) {
                    // Reload sale with full details for printing
                    Sale fullSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                    if (fullSale != null) {
                        printReceipt(fullSale, stage);
                    }
                } else {
                    showAlert("Not Paid", "This sale has not been paid yet.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("No Selection", "Please select a sale to print receipt for.", Alert.AlertType.WARNING);
            }
        });
        
        settingsBtn.setOnAction(e -> showPrinterSettingsDialog(stage));
        
        // Return just the VBox container wrapped in an HBox to maintain interface compatibility
        HBox wrapper = new HBox();
        wrapper.getChildren().add(controlsContainer);
        return wrapper;
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
        invoiceColumn.setCellValueFactory(cellData -> {
            Sale sale = cellData.getValue();
            String invoiceText = sale.getInvoiceNumber();
            if (sale.hasPartialReturn()) {
                invoiceText += " [PARTIAL RETURN]";
            }
            return new SimpleStringProperty(invoiceText);
        });
        invoiceColumn.setPrefWidth(200);
        
        // Date Column
        TableColumn<Sale, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            return new SimpleStringProperty(
                    cellData.getValue().getTimestamp().format(formatter));
        });
        dateColumn.setPrefWidth(140);
        
        // Customer Column
        TableColumn<Sale, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? 
                    customer.getFullName() : "");
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
        
        // Actions Column with Edit Items, Edit Customer, and Add Vehicle buttons
        TableColumn<Sale, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(column -> new TableCell<Sale, Void>() {
            private final Button editItemsBtn = new Button("Edit Items");
            private final Button editCustomerBtn = new Button("Edit Customer");
            private final Button addVehicleBtn = new Button("Add Vehicle");
            private final HBox buttonContainer = new HBox(5);

            {
                editItemsBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
                editItemsBtn.setOnAction(e -> {
                    Sale sale = getTableRow().getItem();
                    if (sale != null && !sale.isPaid()) {
                        // Reload sale with full details
                        Sale fullSale = salesService.getSaleByIdWithDetails(sale.getId());
                        if (fullSale != null) {
                            showSaleItemsEditDialog(fullSale);
                        }
                    }
                });

                editCustomerBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
                editCustomerBtn.setOnAction(e -> {
                    Sale sale = getTableRow().getItem();
                    if (sale != null && !sale.isPaid()) {
                        // Reload sale with full details
                        Sale fullSale = salesService.getSaleByIdWithDetails(sale.getId());
                        if (fullSale != null) {
                            Stage ownerStage = (Stage) getTableView().getScene().getWindow();
                            showCustomerEditDialog(fullSale, ownerStage);
                        }
                    }
                });

                addVehicleBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");
                addVehicleBtn.setOnAction(e -> {
                    Sale sale = getTableRow().getItem();
                    if (sale != null && !sale.isPaid()) {
                        // Reload sale with full details
                        Sale fullSale = salesService.getSaleByIdWithDetails(sale.getId());
                        if (fullSale != null) {
                            Stage ownerStage = (Stage) getTableView().getScene().getWindow();
                            showVehicleEditDialog(fullSale, ownerStage);
                        }
                    }
                });

                buttonContainer.getChildren().addAll(editItemsBtn, editCustomerBtn, addVehicleBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Sale sale = getTableRow().getItem();
                    if (sale != null && !sale.isPaid()) {
                        editItemsBtn.setDisable(false);
                        editCustomerBtn.setDisable(false);
                        addVehicleBtn.setDisable(false);
                        setGraphic(buttonContainer);
                    } else {
                        editItemsBtn.setDisable(true);
                        editCustomerBtn.setDisable(true);
                        addVehicleBtn.setDisable(true);
                        setGraphic(buttonContainer);
                    }
                }
            }
        });
        actionsColumn.setPrefWidth(220);
        
        table.getColumns().addAll(idColumn, invoiceColumn, dateColumn, customerColumn,
                totalColumn, paymentColumn, statusColumn, actionsColumn);

        // Add right-click context menu for printing invoice
        ContextMenu contextMenu = new ContextMenu();

        MenuItem printInvoiceItem = new MenuItem("Print Invoice");
        printInvoiceItem.setOnAction(e -> {
            Sale selectedSale = table.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                // Reload sale with full details for printing
                Sale fullSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                if (fullSale != null) {
                    printInvoiceForSale(fullSale);
                }
            }
        });

        MenuItem viewDetailsItem = new MenuItem("View Details");
        viewDetailsItem.setOnAction(e -> {
            Sale selectedSale = table.getSelectionModel().getSelectedItem();
            if (selectedSale != null) {
                // Reload sale with full details (customer, vehicle, items)
                Sale fullSale = salesService.getSaleByIdWithDetails(selectedSale.getId());
                if (fullSale != null) {
                    Stage owner = (Stage) table.getScene().getWindow();
                    showSaleDetailsDialog(fullSale, owner);
                }
            }
        });

        contextMenu.getItems().addAll(printInvoiceItem, viewDetailsItem);
        table.setContextMenu(contextMenu);

        return table;
    }

    /**
     * Print invoice for a sale (works for both paid and unpaid sales)
     * Uses the same print dialog as the rest of the system
     */
    private void printInvoiceForSale(Sale sale) {
        if (sale == null) return;

        Stage owner = (Stage) salesTable.getScene().getWindow();
        printReceipt(sale, owner);
    }
    
    /**
     * Refresh the sales list
     */
    public void refreshSales() {
        // Remember the current selection so auto-refresh doesn't lose it
        Sale selected = salesTable != null ? salesTable.getSelectionModel().getSelectedItem() : null;
        Long selectedId = selected != null ? selected.getId() : null;

        // Get total count for pagination
        totalSales = salesService.getTotalSalesCount();

        // Calculate pagination values
        int maxPage = (int) Math.ceil((double) totalSales / PAGE_SIZE) - 1;
        if (maxPage < 0) maxPage = 0;

        // Fetch paginated sales
        List<Sale> sales = salesService.getSalesPaginated(currentPage, PAGE_SIZE);
        salesList = FXCollections.observableArrayList(sales);
        salesTable.setItems(salesList);

        // Restore selection if the sale is still on this page
        if (selectedId != null) {
            for (Sale s : salesList) {
                if (s.getId() != null && s.getId().equals(selectedId)) {
                    salesTable.getSelectionModel().select(s);
                    break;
                }
            }
        }

        // Update pagination UI
        updatePaginationControls(maxPage);
    }

    private void updatePaginationControls(int maxPage) {
        // Update page label
        pageLabel.setText("Page " + (currentPage + 1) + " of " + (maxPage + 1));

        // Update count label
        int startIndex = currentPage * PAGE_SIZE + 1;
        int endIndex = Math.min((currentPage + 1) * PAGE_SIZE, totalSales);
        if (totalSales == 0) {
            countLabel.setText("0 sales shown");
        } else {
            countLabel.setText("Showing " + startIndex + "-" + endIndex + " of " + totalSales + " sales");
        }

        // Enable/disable pagination buttons
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= maxPage);
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
            
            // Refresh all relevant tabs after sales change
            if (mainView != null) {
                mainView.refreshTabsForDataChange("sale");
            }
            
            // Automatically show the enhanced print dialog with copy options
            try {
                // Get the updated sale from database to ensure we have the latest data
                Optional<Sale> updatedSaleOpt = salesService.getSaleById(sale.getId());
                if (updatedSaleOpt.isPresent()) {
                    Sale updatedSale = updatedSaleOpt.get();
                    printReceipt(updatedSale, owner);
                } else {
                    // Fallback to original sale if we can't get updated one
                    printReceipt(sale, owner);
                }
            } catch (Exception e) {
                System.err.println("Print failed: " + e.getMessage());
                showAlert("Print Error", "Failed to print receipt: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Show dialog to edit sale items and their prices
     * @param sale The sale to edit items for
     */
    private void showSaleItemsEditDialog(Sale sale) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Sale Items");
        dialog.setHeaderText("Edit items for Sale #" + sale.getInvoiceNumber());
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(800, 600);
        
        // Set buttons
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Sale info label that will be updated
        final Label saleInfoLabel = new Label("Sale #" + sale.getInvoiceNumber() + " - " + 
                                  (sale.getCustomer() != null ? sale.getCustomer().getFullName() : "No Customer") +
                                  " - Total: $" + sale.getTotal());
        saleInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Add buttons for adding more items to the work order
        HBox addItemsControls = new HBox(10);
        addItemsControls.setPadding(new Insets(10, 0, 10, 0));
        
        Button addProductBtn = new Button("Add Product");
        Button addCustomItemBtn = new Button("Add Custom Item");
        Button addServiceBtn = new Button("Add Service");
        Button removeItemBtn = new Button("Remove Selected");

        addProductBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addCustomItemBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        addServiceBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        removeItemBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        
        // Items table - declare before button handlers
        TableView<SaleItem> itemsTable = new TableView<>();
        
        // Get the stage for dialog ownership
        Stage ownerStage = (Stage) salesTable.getScene().getWindow();
        
        addProductBtn.setOnAction(e -> {
            showAddProductDialog(sale, ownerStage, () -> {
                // Refresh the items table after adding
                refreshItemsTable(itemsTable, sale);
                updateSaleInfoLabel(saleInfoLabel, sale);
            });
        });
        
        addCustomItemBtn.setOnAction(e -> {
            showAddCustomItemDialog(sale, ownerStage, () -> {
                // Refresh the items table after adding
                refreshItemsTable(itemsTable, sale);
                updateSaleInfoLabel(saleInfoLabel, sale);
            });
        });
        
        addServiceBtn.setOnAction(e -> {
            showAddServiceDialog(sale, ownerStage, () -> {
                // Refresh the items table after adding
                refreshItemsTable(itemsTable, sale);
                updateSaleInfoLabel(saleInfoLabel, sale);
            });
        });

        removeItemBtn.setOnAction(e -> {
            System.out.println("[RemoveItem] Button clicked");
            SaleItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            System.out.println("[RemoveItem] Selected item: " + (selectedItem != null ? selectedItem.getItemName() + " (ID: " + selectedItem.getId() + ")" : "null"));

            if (selectedItem != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Remove Item");
                confirm.setHeaderText("Remove " + selectedItem.getItemName() + "?");
                confirm.setContentText("Are you sure you want to remove this item from the sale?");
                confirm.initOwner(ownerStage);

                Optional<ButtonType> confirmResult = confirm.showAndWait();
                System.out.println("[RemoveItem] Confirm result: " + confirmResult.orElse(ButtonType.CANCEL));

                if (confirmResult.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    Long itemId = selectedItem.getId();
                    Long saleId = sale.getId();
                    System.out.println("[RemoveItem] Removing item ID: " + itemId + " from sale ID: " + saleId);

                    try {
                        Optional<Sale> result = salesService.removeItemFromSale(saleId, itemId);
                        System.out.println("[RemoveItem] Service result present: " + result.isPresent());

                        if (result.isPresent()) {
                            System.out.println("[RemoveItem] Item removed, refreshing table...");
                            // Force refresh from database
                            Optional<Sale> freshSale = salesService.getSaleById(saleId);
                            if (freshSale.isPresent()) {
                                System.out.println("[RemoveItem] Fresh sale has " + freshSale.get().getItems().size() + " items");
                                itemsTable.getItems().clear();
                                itemsTable.getItems().addAll(freshSale.get().getItems());
                                itemsTable.refresh();
                                updateSaleInfoLabel(saleInfoLabel, sale);
                            }
                            if (mainView != null) {
                                mainView.refreshDashboard();
                            }
                            showAlert("Item Removed", "Item has been removed from the sale.", Alert.AlertType.INFORMATION);
                        } else {
                            System.out.println("[RemoveItem] Service returned empty - removal failed");
                            showAlert("Error", "Could not remove item. Item may have already been removed.", Alert.AlertType.ERROR);
                        }
                    } catch (Exception ex) {
                        System.out.println("[RemoveItem] Exception: " + ex.getMessage());
                        ex.printStackTrace();
                        showAlert("Error", "Error removing item: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            } else {
                showAlert("No Selection", "Please select an item to remove.", Alert.AlertType.WARNING);
            }
        });

        addItemsControls.getChildren().addAll(addProductBtn, addCustomItemBtn, addServiceBtn, removeItemBtn);
        itemsTable.setPrefHeight(300);
        
        // Item Type Column
        TableColumn<SaleItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getItemType()));
        typeColumn.setPrefWidth(80);
        
        // Item Name Column - enhanced to show custom descriptions
        TableColumn<SaleItem, String> nameColumn = new TableColumn<>("Item");
        nameColumn.setCellValueFactory(cellData -> {
            SaleItem item = cellData.getValue();
            String displayName = item.getItemName();
            if (item.getItemType().equals("CUSTOM") && item.getCustomItemName() != null) {
                displayName = item.getCustomItemName();
            }
            return new SimpleStringProperty(displayName);
        });
        nameColumn.setPrefWidth(200);
        
        // Unit Price Column - shows price with Edit button
        TableColumn<SaleItem, SaleItem> priceColumn = new TableColumn<>("Unit Price");
        priceColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        priceColumn.setPrefWidth(150);
        priceColumn.setCellFactory(column -> new TableCell<SaleItem, SaleItem>() {
            private final Button editBtn = new Button("Edit");
            private final HBox cellBox = new HBox(5);
            private final Label priceLabel = new Label();
            
            {
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 2 6;");
                cellBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                priceLabel.setStyle("-fx-text-fill: black;");
                cellBox.getChildren().addAll(priceLabel, editBtn);
            }
            
            @Override
            protected void updateItem(SaleItem saleItem, boolean empty) {
                super.updateItem(saleItem, empty);
                if (empty || saleItem == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    priceLabel.setText("$" + saleItem.getUnitPrice());
                    editBtn.setOnAction(e -> showQuickEditPriceDialog(saleItem, itemsTable, saleInfoLabel));
                    setText(null);
                    setGraphic(cellBox);
                }
            }
        });
        
        // Quantity Column - editable
        TableColumn<SaleItem, Number> qtyColumn = new TableColumn<>("Qty");
        qtyColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getQuantity()));
        qtyColumn.setPrefWidth(80);
        qtyColumn.setCellFactory(column -> new TableCell<SaleItem, Number>() {
            private TextField textField;
            
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null || getTableRow() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    SaleItem saleItem = getTableRow().getItem();
                    if (saleItem != null && itemsTable != null && saleInfoLabel != null) {
                        setText(null);
                        setGraphic(createEditableQuantityField(saleItem, itemsTable, saleInfoLabel));
                    } else {
                        setText(item != null ? item.toString() : "");
                        setGraphic(null);
                    }
                }
            }
            
            private TextField createEditableQuantityField(SaleItem saleItem, TableView<SaleItem> itemsTable, Label saleInfoLabel) {
                if (textField == null) {
                    textField = new TextField();
                    textField.setStyle("-fx-text-fill: black;");
                    textField.setPrefWidth(50);
                }
                
                textField.setText(String.valueOf(saleItem.getQuantity()));
                
                textField.setOnAction(e -> updateItemQuantity(saleItem, textField.getText(), itemsTable, saleInfoLabel));
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        updateItemQuantity(saleItem, textField.getText(), itemsTable, saleInfoLabel);
                    }
                });
                
                return textField;
            }
            
            private void updateItemQuantity(SaleItem saleItem, String newQuantityText, TableView<SaleItem> itemsTable, Label saleInfoLabel) {
                try {
                    int newQuantity = Integer.parseInt(newQuantityText);
                    if (newQuantity > 0) {
                        // Get the sale from userData
                        Object[] userData = (Object[]) itemsTable.getUserData();
                        if (userData != null && userData.length >= 2) {
                            Sale sale = (Sale) userData[0];
                            
                            // Use the service method which handles inventory adjustments
                            salesService.updateSaleItemQuantity(sale.getId(), saleItem.getId(), newQuantity).ifPresentOrElse(
                                updatedSale -> {
                                    System.out.println("[SalesController] Quantity updated successfully");
                                    
                                    // Update the sale reference in userData
                                    userData[0] = updatedSale;
                                    
                                    // Update the existing items in the table without recreating the list
                                    ObservableList<SaleItem> currentItems = itemsTable.getItems();
                                    currentItems.clear();
                                    currentItems.addAll(updatedSale.getItems());
                                    
                                    // Update the info label
                                    saleInfoLabel.setText("Sale #" + updatedSale.getInvoiceNumber() + " - " + 
                                              (updatedSale.getCustomer() != null ? updatedSale.getCustomer().getFullName() : "No Customer") +
                                              " - Total: $" + updatedSale.getTotal());
                                    
                                    // Refresh the table to show updated subtotals
                                    itemsTable.refresh();
                                    
                                    // Refresh dashboard and inventory
                                    if (mainView != null) {
                                        mainView.refreshDashboard();
                                    }
                                },
                                () -> {
                                    showAlert("Error", "Could not update quantity. Check inventory availability.", Alert.AlertType.ERROR);
                                    textField.setText(String.valueOf(saleItem.getQuantity()));
                                }
                            );
                        }
                    } else {
                        showAlert("Invalid Quantity", "Quantity must be greater than 0.", Alert.AlertType.WARNING);
                        textField.setText(String.valueOf(saleItem.getQuantity()));
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid number.", Alert.AlertType.WARNING);
                    textField.setText(String.valueOf(saleItem.getQuantity()));
                }
            }
        });
        
        // Subtotal Column
        TableColumn<SaleItem, String> subtotalColumn = new TableColumn<>("Subtotal");
        subtotalColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty("$" + cellData.getValue().getSubtotal()));
        subtotalColumn.setPrefWidth(100);
        
        itemsTable.getColumns().addAll(typeColumn, nameColumn, priceColumn, qtyColumn, subtotalColumn);
        itemsTable.setItems(FXCollections.observableArrayList(sale.getItems()));
        
        // Instructions
        Label instructions = new Label("Click on Unit Price to edit prices, Quantity to edit quantities, or use the buttons above to add more items to this work order. Press ENTER or click elsewhere to save.");
        instructions.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        
        // Store references for updating
        itemsTable.setUserData(new Object[] {sale, saleInfoLabel});
        
        content.getChildren().addAll(saleInfoLabel, addItemsControls, instructions, itemsTable);
        dialog.getDialogPane().setContent(content);
        
        dialog.showAndWait();
        
        // After dialog closes, refresh main sales table to show updated totals
        refreshSales();
    }
    
    /**
     * Show quick edit price dialog from table cell
     */
    private void showQuickEditPriceDialog(SaleItem saleItem, TableView<SaleItem> itemsTable, Label saleInfoLabel) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Edit Price");
        dialog.setHeaderText(saleItem.getItemName());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField priceField = new TextField(saleItem.getUnitPrice().toString());
        priceField.setPromptText("Enter new price");
        priceField.setStyle("-fx-text-fill: black;");
        
        grid.add(new Label("Current: $" + saleItem.getUnitPrice()), 0, 0);
        grid.add(new Label("New Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(priceField::requestFocus);
        
        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                try {
                    return new BigDecimal(priceField.getText());
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newPrice -> {
            if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                Object[] userData = (Object[]) itemsTable.getUserData();
                if (userData != null && userData.length >= 1) {
                    Sale sale = (Sale) userData[0];
                    
                    salesService.updateSaleItemPrice(sale.getId(), saleItem.getId(), newPrice).ifPresentOrElse(
                        updatedSale -> {
                            System.out.println("[SalesController] Price updated successfully in dialog");
                            // Update the sale reference in userData
                            userData[0] = updatedSale;
                            
                            // Update the existing items in the table without recreating the list
                            // This preserves the state of other editable fields like quantity
                            ObservableList<SaleItem> currentItems = itemsTable.getItems();
                            currentItems.clear();
                            currentItems.addAll(updatedSale.getItems());
                            
                            // Refresh table to update display
                            itemsTable.refresh();
                            
                            // Update sale info label
                            saleInfoLabel.setText("Sale #" + updatedSale.getInvoiceNumber() + " - " + 
                                                  (updatedSale.getCustomer() != null ? updatedSale.getCustomer().getFullName() : "No Customer") +
                                                  " - Total: $" + updatedSale.getTotal());
                        },
                        () -> showAlert("Error", "Could not update price.", Alert.AlertType.ERROR)
                    );
                }
            } else {
                showAlert("Invalid Price", "Price cannot be negative.", Alert.AlertType.WARNING);
            }
        });
    }
    
    /**
     * Update item price in the edit dialog
     */
    private void updateItemPriceInDialog(Sale sale, SaleItem saleItem, String newPriceText, TableView<SaleItem> itemsTable) {
        try {
            BigDecimal newPrice = new BigDecimal(newPriceText);
            if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                // Don't update if price hasn't changed
                if (newPrice.compareTo(saleItem.getUnitPrice()) == 0) {
                    return;
                }
                
                salesService.updateSaleItemPrice(sale.getId(), saleItem.getId(), newPrice).ifPresentOrElse(
                    updatedSale -> {
                        System.out.println("[SalesController] Price update successful, refreshing UI...");
                        
                        // Get the stored references
                        Object[] userData = (Object[]) itemsTable.getUserData();
                        if (userData != null && userData.length == 2) {
                            Label saleInfoLabel = (Label) userData[1];
                            
                            // Get fresh sale from database
                            salesService.getSaleById(sale.getId()).ifPresent(freshSale -> {
                                System.out.println("[SalesController] Fresh sale loaded - Total: $" + freshSale.getTotal());
                                
                                // Update the items table
                                itemsTable.setItems(FXCollections.observableArrayList(freshSale.getItems()));
                                itemsTable.refresh();
                                
                                // Update the sale info label with new total
                                saleInfoLabel.setText("Sale #" + freshSale.getInvoiceNumber() + " - " + 
                                                      (freshSale.getCustomer() != null ? freshSale.getCustomer().getFullName() : "No Customer") +
                                                      " - Total: $" + freshSale.getTotal());
                                
                                System.out.println("[SalesController] UI refreshed with new values");
                            });
                        }
                        
                        // Refresh dashboard
                        if (mainView != null) {
                            mainView.refreshDashboard();
                        }
                    },
                    () -> showAlert("Error", "Could not update item price.", Alert.AlertType.ERROR)
                );
            } else {
                showAlert("Invalid Price", "Price must be greater than or equal to zero.", Alert.AlertType.WARNING);
            }
        } catch (NumberFormatException ex) {
            showAlert("Invalid Price", "Please enter a valid price.", Alert.AlertType.WARNING);
        }
    }
    
    /**
     * Show edit dialog for a sale item (price and description for custom items)
     * @param sale The sale containing the item
     * @param saleItem The sale item to edit
     */
    private void showEditPriceDialog(Sale sale, SaleItem saleItem) {
        Dialog<ItemEditResult> dialog = new Dialog<>();
        dialog.setTitle("Edit Item");
        dialog.setHeaderText("Edit item: " + saleItem.getItemName());
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Price field
        TextField priceField = new TextField();
        priceField.setText(saleItem.getUnitPrice().toString());
        priceField.setPromptText("Enter new price");
        
        Label currentPriceLabel = new Label("Current Price: $" + saleItem.getUnitPrice());
        currentPriceLabel.setStyle("-fx-font-weight: bold;");
        
        grid.add(currentPriceLabel, 0, 0, 2, 1);
        grid.add(new Label("New Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        
        // Description field (only for custom items)
        if (saleItem.getItemType().equals("CUSTOM")) {
            TextField descriptionField = new TextField();
            descriptionField.setText(saleItem.getCustomItemName());
            descriptionField.setPromptText("Enter item description");
            
            grid.add(new Label("Description:"), 0, 2);
            grid.add(descriptionField, 1, 2);
            
            dialog.getDialogPane().setContent(grid);
            Platform.runLater(descriptionField::requestFocus);
            
            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        BigDecimal newPrice = new BigDecimal(priceField.getText());
                        String newDescription = descriptionField.getText().trim();
                        if (newPrice.compareTo(BigDecimal.ZERO) >= 0 && !newDescription.isEmpty()) {
                            return new ItemEditResult(newPrice, newDescription);
                        } else {
                            showAlert("Invalid Input", "Price must be >= 0 and description cannot be empty.", Alert.AlertType.WARNING);
                        }
                    } catch (NumberFormatException ex) {
                        showAlert("Invalid Price", "Please enter a valid price.", Alert.AlertType.WARNING);
                    }
                }
                return null;
            });
        } else {
            dialog.getDialogPane().setContent(grid);
            Platform.runLater(priceField::requestFocus);
            
            // Convert result for non-custom items
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        BigDecimal newPrice = new BigDecimal(priceField.getText());
                        if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                            return new ItemEditResult(newPrice, saleItem.getCustomItemName());
                        } else {
                            showAlert("Invalid Price", "Price must be greater than or equal to zero.", Alert.AlertType.WARNING);
                        }
                    } catch (NumberFormatException ex) {
                        showAlert("Invalid Price", "Please enter a valid price.", Alert.AlertType.WARNING);
                    }
                }
                return null;
            });
        }
        
        // Show dialog and process result
        Optional<ItemEditResult> result = dialog.showAndWait();
        result.ifPresent(editResult -> {
            // Update price and description
            salesService.updateSaleItemPriceAndDescription(sale.getId(), saleItem.getId(), editResult.price, editResult.description).ifPresentOrElse(
                updatedSale -> {
                    System.out.println("[SalesController] Price updated in Details tab");
                    
                    // Refresh dashboard after price change
                    if (mainView != null) {
                        mainView.refreshDashboard();
                    }
                    
                    // Refresh sales list
                    refreshSales();
                    
                    showAlert("Price Updated", "Item price has been updated successfully.", Alert.AlertType.INFORMATION);
                },
                () -> showAlert("Error", "Could not update item price.", Alert.AlertType.ERROR)
            );
        });
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
    
    private void showAddProductDialog(Sale sale, Stage owner, Runnable onSuccess) {
        Dialog<ProductSelection> dialog = new Dialog<>();
        dialog.setTitle("Add Product to Sale");
        dialog.setHeaderText("Search and Select Product");
        dialog.initOwner(owner);
        dialog.setResizable(true);
        
        // Set buttons
        ButtonType addButtonType = new ButtonType("Add to Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create main content
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.setPrefWidth(700);
        mainContent.setPrefHeight(500);
        
        // Search section
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, size (275/65R16), brand, or type...");
        searchField.setPrefWidth(320);
        
        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        categoryFilter.getItems().addAll("All", "All-Season", "Winter", "Summer", "Performance", "All-Terrain");
        categoryFilter.setValue("All");
        
        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("Clear");
        
        searchBox.getChildren().addAll(
            new Label("Search:"), searchField, 
            new Label("Type:"), categoryFilter,
            searchBtn, clearBtn
        );
        
        // Product table
        TableView<Product> productTable = new TableView<>();
        productTable.setPrefHeight(300);
        
        // Table columns with comprehensive tire information
        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(200);
        
        TableColumn<Product, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSize()));
        sizeCol.setPrefWidth(100);
        
        TableColumn<Product, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getManufacturer()));
        brandCol.setPrefWidth(100);
        
        TableColumn<Product, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTireType()));
        typeCol.setPrefWidth(100);
        
        TableColumn<Product, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getSellingPrice();
            return new SimpleStringProperty(price != null ? "$" + price : "N/A");
        });
        priceCol.setPrefWidth(80);
        
        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getQuantityInStock()));
        stockCol.setPrefWidth(60);
        stockCol.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
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
        
        // UTQG Ratings column group
        TableColumn<Product, String> utqgCol = new TableColumn<>("UTQG");
        
        TableColumn<Product, Integer> treadwearCol = new TableColumn<>("Treadwear");
        treadwearCol.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getUtqgTreadwear()));
        treadwearCol.setPrefWidth(70);
        
        TableColumn<Product, String> tractionCol = new TableColumn<>("Traction");
        tractionCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getUtqgTraction()));
        tractionCol.setPrefWidth(60);
        
        TableColumn<Product, String> tempCol = new TableColumn<>("Temp");
        tempCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getUtqgTemperature()));
        tempCol.setPrefWidth(50);
        
        utqgCol.getColumns().addAll(treadwearCol, tractionCol, tempCol);
        
        productTable.getColumns().addAll(nameCol, sizeCol, brandCol, typeCol, priceCol, stockCol, utqgCol);
        
        // Selected product details
        VBox detailsBox = new VBox(5);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px;");
        
        Label detailsLabel = new Label("Selected Product Details:");
        detailsLabel.setStyle("-fx-font-weight: bold;");
        
        Label selectedProductInfo = new Label("No product selected");
        selectedProductInfo.setWrapText(true);
        
        detailsBox.getChildren().addAll(detailsLabel, selectedProductInfo);
        
        // Quantity selection
        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label qtyLabel = new Label("Quantity:");
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 100, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(80);
        
        Label totalPriceLabel = new Label("Total: $0.00");
        totalPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        quantityBox.getChildren().addAll(qtyLabel, quantitySpinner, new Label("   "), totalPriceLabel);
        
        // Load initial products
        List<Product> allProducts = inventoryService.getAllProducts();
        ObservableList<Product> productList = FXCollections.observableArrayList(allProducts);
        productTable.setItems(productList);
        
        // Search functionality
        Runnable searchProducts = () -> {
            String searchTerm = searchField.getText().toLowerCase();
            String category = categoryFilter.getValue();
            
            List<Product> filtered = allProducts.stream()
                .filter(p -> {
                    // Category filter
                    if (!"All".equals(category) && !category.equals(p.getTireType())) {
                        return false;
                    }
                    
                    // Search filter
                    if (searchTerm.isEmpty()) {
                        return true;
                    }
                    
                    return (p.getName() != null && p.getName().toLowerCase().contains(searchTerm)) ||
                           (p.getSize() != null && p.getSize().toLowerCase().contains(searchTerm)) ||
                           (p.getManufacturer() != null && p.getManufacturer().toLowerCase().contains(searchTerm)) ||
                           (p.getTireType() != null && p.getTireType().toLowerCase().contains(searchTerm));
                })
                .collect(Collectors.toList());
            
            productTable.setItems(FXCollections.observableArrayList(filtered));
        };
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> searchProducts.run());
        categoryFilter.setOnAction(e -> searchProducts.run());
        searchBtn.setOnAction(e -> searchProducts.run());
        clearBtn.setOnAction(e -> {
            searchField.clear();
            categoryFilter.setValue("All");
            searchProducts.run();
        });
        
        // Selection listener
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Update details
                StringBuilder details = new StringBuilder();
                details.append("Name: ").append(newVal.getName()).append("\n");
                details.append("Size: ").append(newVal.getSize()).append("\n");
                details.append("Brand: ").append(newVal.getManufacturer()).append("\n");
                details.append("Type: ").append(newVal.getTireType()).append("\n");
                
                if (newVal.getUtqgTreadwear() != null) {
                    details.append("UTQG: ").append(newVal.getUtqgTreadwear())
                           .append(" ").append(newVal.getUtqgTraction())
                           .append(" ").append(newVal.getUtqgTemperature()).append("\n");
                }
                
                if (newVal.getSpeedRating() != null) {
                    details.append("Speed Rating: ").append(newVal.getSpeedRating()).append("\n");
                }
                
                if (newVal.getLoadRating() != null) {
                    details.append("Load Rating: ").append(newVal.getLoadRating()).append("\n");
                }
                
                details.append("Price: $").append(newVal.getSellingPrice()).append("\n");
                details.append("In Stock: ").append(newVal.getQuantityInStock());
                
                selectedProductInfo.setText(details.toString());
                
                // Update total price
                updateTotalPrice(newVal, quantitySpinner.getValue() != null ? quantitySpinner.getValue() : 1, totalPriceLabel);

                // Disable add button if out of stock OR if the product has no price set
                // (auto-scraped tires often have no price - selling them crashes downstream math)
                Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
                boolean noPrice = newVal.getSellingPrice() == null;
                addButton.setDisable(newVal.getQuantityInStock() == 0 || noPrice);
                if (noPrice) {
                    selectedProductInfo.setText(selectedProductInfo.getText() + "\n\n⚠ No price set - edit this product in Inventory before selling it.");
                }
            } else {
                selectedProductInfo.setText("No product selected");
                totalPriceLabel.setText("Total: $0.00");
            }
        });
        
        // Quantity change listener
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Editable spinners can deliver null (e.g. field cleared) - ignore those events
            if (newVal == null) {
                return;
            }
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateTotalPrice(selected, newVal, totalPriceLabel);

                // Check stock availability
                if (newVal > selected.getQuantityInStock()) {
                    quantitySpinner.getValueFactory().setValue(selected.getQuantityInStock());
                    // Use Platform.runLater to avoid "showAndWait during animation" error
                    Platform.runLater(() -> {
                        showAlert("Stock Limit", "Only " + selected.getQuantityInStock() + " items in stock.", 
                                 Alert.AlertType.WARNING);
                    });
                }
            }
        });
        
        // Assemble main content
        mainContent.getChildren().addAll(
            searchBox,
            new Separator(),
            productTable,
            detailsBox,
            quantityBox
        );
        
        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().setPrefSize(1000, 700);
        
        // Disable add button initially
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
                Integer quantity = quantitySpinner.getValue();
                
                if (selectedProduct != null && quantity != null && quantity > 0) {
                    return new ProductSelection(selectedProduct.getId(), quantity);
                }
            }
            return null;
        });
        
        // Focus search field
        Platform.runLater(searchField::requestFocus);
        
        // Show dialog and process result
        Optional<ProductSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Optional<Sale> updatedSale;
            try {
                updatedSale = salesService.addProductToSale(
                        sale.getId(), selection.productId, selection.quantity);
            } catch (Exception ex) {
                // Never let a data problem crash the whole POS - show the error instead
                ex.printStackTrace();
                showAlert("Error", "Could not add product to sale: " + ex.getMessage(), Alert.AlertType.ERROR);
                return;
            }

            if (updatedSale.isEmpty()) {
                showAlert("Error", "Could not add product to sale. Check inventory and that a price is set.", Alert.AlertType.ERROR);
            } else {
                // Call success callback to refresh parent dialog
                if (onSuccess != null) {
                    onSuccess.run();
                }
                // Refresh dashboard after inventory change
                if (mainView != null) {
                    mainView.refreshDashboard();
                }
                showAlert("Success", "Product added to sale successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }
    
    // Overloaded method for backward compatibility
    private void showAddProductDialog(Sale sale, Stage owner) {
        showAddProductDialog(sale, owner, null);
    }
    
    private void updateTotalPrice(Product product, int quantity, Label totalLabel) {
        if (product != null && product.getSellingPrice() != null) {
            BigDecimal total = product.getSellingPrice().multiply(BigDecimal.valueOf(quantity));
            totalLabel.setText(String.format("Total: $%.2f", total));
        } else {
            totalLabel.setText("Total: $0.00");
        }
    }
    
    private void showAddServiceDialog(Sale sale, Stage owner, Runnable onSuccess) {
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
                // Call success callback to refresh parent dialog
                if (onSuccess != null) {
                    onSuccess.run();
                }
                // Refresh dashboard after adding service
                if (mainView != null) {
                    mainView.refreshDashboard();
                }
                showAlert("Success", "Service added to sale successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }
    
    // Overloaded method for backward compatibility
    private void showAddServiceDialog(Sale sale, Stage owner) {
        showAddServiceDialog(sale, owner, null);
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
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Receipt format selection
        Label formatLabel = new Label("Receipt Format:");
        formatLabel.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton thermalReceipt = new RadioButton("Thermal Receipt (40 columns - traditional format)");
        RadioButton fullPageReceipt = new RadioButton("Full Page Receipt (80 columns - includes all vehicle details)");
        
        thermalReceipt.setToggleGroup(formatGroup);
        fullPageReceipt.setToggleGroup(formatGroup);
        fullPageReceipt.setSelected(true); // Default to full-page format
        
        VBox formatOptions = new VBox(8);
        formatOptions.getChildren().addAll(thermalReceipt, fullPageReceipt);
        
        // Number of copies selection
        Label copiesLabel = new Label("Number of Copies:");
        copiesLabel.setStyle("-fx-font-weight: bold;");
        
        Spinner<Integer> copiesSpinner = new Spinner<>(1, 10, 2); // Default to 2 copies
        copiesSpinner.setEditable(true);
        copiesSpinner.setPrefWidth(80);
        
        HBox copiesBox = new HBox(10);
        copiesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        copiesBox.getChildren().addAll(copiesLabel, copiesSpinner);
        
        // Action selection
        Label actionLabel = new Label("Action:");
        actionLabel.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup actionGroup = new ToggleGroup();
        RadioButton printAction = new RadioButton("Print to Printer");
        RadioButton pdfPreviewAction = new RadioButton("Generate PDF Preview");
        
        printAction.setToggleGroup(actionGroup);
        pdfPreviewAction.setToggleGroup(actionGroup);
        printAction.setSelected(true); // Default to printing
        
        VBox actionOptions = new VBox(8);
        actionOptions.getChildren().addAll(printAction, pdfPreviewAction);
        
        // Printer selection (only show when printing)
        Label printerLabel = new Label("Select Printer:");
        printerLabel.setStyle("-fx-font-weight: bold;");
        
        ComboBox<String> printerComboBox = new ComboBox<>();
        printerComboBox.getItems().add("Default Printer");
        
        // Add available printers
        List<String> printers = printerService.getAvailablePrinters();
        if (!printers.isEmpty()) {
            printerComboBox.getItems().addAll(printers);
        }
        printerComboBox.setValue("Default Printer");
        
        // Show/hide printer selection and copies based on action
        VBox printerSection = new VBox(8);
        printerSection.getChildren().addAll(printerLabel, printerComboBox);
        
        printAction.setOnAction(e -> {
            printerSection.setVisible(true);
            copiesBox.setVisible(true);
        });
        pdfPreviewAction.setOnAction(e -> {
            printerSection.setVisible(false);
            copiesBox.setVisible(false);
        });
        
        content.getChildren().addAll(
                formatLabel,
                formatOptions,
                new Separator(),
                copiesBox,
                new Separator(),
                actionLabel,
                actionOptions,
                new Separator(),
                printerSection
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> dialogButton == printButtonType);
        
        // Show dialog and process result
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            
            if (pdfPreviewAction.isSelected()) {
                // Generate PDF preview
                String fileName = "receipt_" + sale.getInvoiceNumber() + "_" + 
                                 java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
                String filePath = System.getProperty("user.home") + "/Desktop/" + fileName;
                
                boolean pdfGenerated;
                String receiptType;
                
                if (fullPageReceipt.isSelected()) {
                    pdfGenerated = printerService.generateWorkOrderPdf(sale, filePath);
                    receiptType = "work order PDF";
                } else {
                    // For thermal format, we'll use the old PDF method or create a simple text-based PDF
                    pdfGenerated = printerService.generateReceiptPdf(sale, filePath);
                    receiptType = "thermal receipt PDF";
                }
                
                if (pdfGenerated) {
                    showAlert("PDF Generated", 
                            "The " + receiptType + " has been saved to your Desktop as: " + fileName, 
                            Alert.AlertType.INFORMATION);
                    
                    // Try to open the PDF
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Could not open PDF file: " + e.getMessage());
                    }
                } else {
                    showAlert("PDF Error", "Failed to generate " + receiptType + ".", Alert.AlertType.ERROR);
                }
                
            } else {
                // Print to printer
                String printerNameFromDialog = printerComboBox.getValue();
                if ("Default Printer".equals(printerNameFromDialog)) {
                    printerNameFromDialog = null; // Let PrinterService use its default or system default
                }

                Integer copiesValue = copiesSpinner.getValue();
                int numberOfCopies = copiesValue != null && copiesValue > 0 ? copiesValue : 1;
                boolean allPrinted = true;
                String receiptType;
                
                // Choose printing method based on selected format
                if (fullPageReceipt.isSelected()) {
                    receiptType = "full-page receipt";
                } else {
                    receiptType = "thermal receipt";
                }
                
                // Print multiple copies
                for (int i = 1; i <= numberOfCopies; i++) {
                    boolean printed;
                    
                    if (fullPageReceipt.isSelected()) {
                        printed = printerService.printFullPageReceipt(sale, printerNameFromDialog);
                    } else {
                        printed = printerService.printReceipt(sale, printerNameFromDialog);
                    }
                    
                    if (!printed) {
                        allPrinted = false;
                        showAlert("Print Error", "Failed to print copy " + i + " of " + numberOfCopies + ". Please check printer connection.", 
                                Alert.AlertType.ERROR);
                        break; // Stop printing if one fails
                    }
                    
                    // Small delay between copies to prevent printer issues
                    if (i < numberOfCopies) {
                        try {
                            Thread.sleep(500); // 500ms delay between copies
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                if (allPrinted) {
                    String message = numberOfCopies == 1 ? 
                        "The " + receiptType + " for invoice " + sale.getInvoiceNumber() + " has been sent to printer." :
                        numberOfCopies + " copies of the " + receiptType + " for invoice " + sale.getInvoiceNumber() + " have been sent to printer.";
                    showAlert("Receipt Printed", message, Alert.AlertType.INFORMATION);
                }
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
            
            // Create a proper test sale with realistic data
            Sale dummySale = new Sale();
            dummySale.setInvoiceNumber("TEST-" + System.currentTimeMillis());
            dummySale.setTimestamp(java.time.LocalDateTime.now());
            dummySale.setPaymentMethod("CASH");
            dummySale.setPaymentType(PaymentType.CASH);
            
            // Add a test item to make the receipt realistic
            try {
                SaleItem testItem = new SaleItem();
                testItem.setItemType("CUSTOM");
                testItem.setCustomItemName("Test Print Item");
                testItem.setQuantity(1);
                testItem.setUnitPrice(new BigDecimal("1.00"));
                testItem.setSubtotal(new BigDecimal("1.00"));
                
                // Add to sale
                java.util.List<SaleItem> items = new java.util.ArrayList<>();
                items.add(testItem);
                dummySale.setItems(items);
                
                // Set totals using current tax rate
                dummySale.setSubtotal(new BigDecimal("1.00"));
                BigDecimal testTax = new BigDecimal("1.00").multiply(settingsService.getSalesTaxRate());
                dummySale.setTax(testTax);
                dummySale.setTotal(new BigDecimal("1.00").add(testTax));
                
            } catch (Exception ex) {
                // Continue with basic sale if item creation fails
                dummySale.setSubtotal(new BigDecimal("0.00"));
                dummySale.setTax(new BigDecimal("0.00"));
                dummySale.setTotal(new BigDecimal("0.00"));
            }

            if (printerService.printReceipt(dummySale, printer)) {
                showAlert("Test Print", "Test receipt sent to printer successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Print Error", "Failed to print test receipt. Check printer connection and settings.", Alert.AlertType.ERROR);
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
        
        // Customer selection
        Label customerLabel = new Label("Customer:");
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Select customer");
        customerComboBox.setPrefWidth(200);
        
        Button addCustomerBtn = new Button("Add New Customer");
        addCustomerBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        HBox customerBox = new HBox(10);
        customerBox.getChildren().addAll(customerComboBox, addCustomerBtn);
        
        // Load customers with their vehicles using the SalesService
        List<Customer> customers = salesService.getAllCustomersWithVehicles();
        customerComboBox.setItems(FXCollections.observableArrayList(customers));
        
        // Vehicle selection
        Label vehicleLabel = new Label("Vehicle:");
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPromptText("Select vehicle");
        vehicleComboBox.setPrefWidth(200);
        vehicleComboBox.setDisable(true); // Disable until a customer is selected
        
        Button addVehicleBtn = new Button("Add New Vehicle");
        addVehicleBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addVehicleBtn.setDisable(true); // Disable until a customer is selected
        
        HBox vehicleBox = new HBox(10);
        vehicleBox.getChildren().addAll(vehicleComboBox, addVehicleBtn);
        
        // Set converters for display
        customerComboBox.setConverter(new javafx.util.StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                // Display just the customer name without phone number
                return customer != null ? customer.getFullName() : "";
            }
            
            @Override
            public Customer fromString(String string) {
                return customers.stream()
                        .filter(c -> c.getFullName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        
        // When customer is selected, update vehicle dropdown
        customerComboBox.setOnAction(e -> {
            try {
                Customer selectedCustomer = customerComboBox.getValue();
                if (selectedCustomer != null && selectedCustomer.getId() != null) {
                    vehicleComboBox.setDisable(false);
                    addVehicleBtn.setDisable(false);
                    List<Vehicle> vehicles = selectedCustomer.getVehicles();
                    if (vehicles != null) {
                        vehicleComboBox.setItems(FXCollections.observableArrayList(vehicles));
                    } else {
                        vehicleComboBox.setItems(FXCollections.observableArrayList());
                    }
                } else {
                    vehicleComboBox.getItems().clear();
                    vehicleComboBox.setDisable(true);
                    addVehicleBtn.setDisable(true);
                }
            } catch (Exception ex) {
                System.err.println("Error updating vehicle dropdown: " + ex.getMessage());
                vehicleComboBox.getItems().clear();
                vehicleComboBox.setDisable(true);
                addVehicleBtn.setDisable(true);
            }
        });
        
        // Add Customer button action
        addCustomerBtn.setOnAction(e -> {
            showAddCustomerDialog(owner, customers, customerComboBox);
        });
        
        // Add Vehicle button action
        addVehicleBtn.setOnAction(e -> {
            Customer selectedCustomer = customerComboBox.getValue();
            if (selectedCustomer != null) {
                showAddVehicleDialog(owner, selectedCustomer, vehicleComboBox);
            }
        });
        
        vehicleComboBox.setConverter(new javafx.util.StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                // Use the Vehicle's toString method which handles null values properly
                return vehicle != null ? vehicle.toString() : "";
            }
            
            @Override
            public Vehicle fromString(String string) {
                return null;
            }
        });
        
        grid.add(customerLabel, 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(vehicleLabel, 0, 1);
        grid.add(vehicleBox, 1, 1);
        
        // (The rest of the method remains the same)
        contentVBox.getChildren().add(grid);
        contentVBox.getChildren().add(new Separator());
        Label itemsLabel = new Label("Items will appear here after creating the sale");
        itemsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        contentVBox.getChildren().add(itemsLabel);
        dialog.getDialogPane().setContent(contentVBox);
        
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

        Optional<CustomerSelection> result = dialog.showAndWait();
        result.ifPresent(selection -> {
            Sale newSale = salesService.createSale(selection.customerId, selection.vehicleId);
            if (newSale != null) {
                refreshSales();
                salesTable.getSelectionModel().select(newSale);
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
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(1200, 800);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        GridPane customerGrid = new GridPane();
        customerGrid.setHgap(10);
        customerGrid.setVgap(5);
        Customer customer = sale.getCustomer();
        if (customer != null) {
            customerGrid.add(new Label("Customer:"), 0, 0);
            customerGrid.add(new Label(customer.getFullName()), 1, 0);
            customerGrid.add(new Label("Phone:"), 0, 1);
            customerGrid.add(new Label(customer.getPhone()), 1, 1);
        }
        Vehicle vehicle = sale.getVehicle();
        if (vehicle != null) {
            customerGrid.add(new Label("Vehicle:"), 0, 2);
            customerGrid.add(new Label(vehicle.toString()), 1, 2);
        }

        TableView<SaleItem> itemsTable = new TableView<>();
        TableColumn<SaleItem, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemType()));
        TableColumn<SaleItem, String> nameColumn = new TableColumn<>("Item");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItemName()));
        TableColumn<SaleItem, String> priceColumn = new TableColumn<>("Unit Price");
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getUnitPrice()));
        TableColumn<SaleItem, Number> qtyColumn = new TableColumn<>("Qty");
        qtyColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getQuantity()));
        qtyColumn.setCellFactory(column -> new TableCell<SaleItem, Number>() {
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
        TableColumn<SaleItem, String> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getSubtotal()));
        itemsTable.getColumns().addAll(typeColumn, nameColumn, priceColumn, qtyColumn, totalColumn);
        itemsTable.setItems(FXCollections.observableArrayList(sale.getItems()));

        GridPane totalsGrid = new GridPane();
        totalsGrid.setHgap(10);
        totalsGrid.setVgap(5);
        Label subtotalLabel = new Label("$" + sale.getSubtotal());
        Label taxLabel = new Label("$" + sale.getTax());
        Label ccFeeLabel = new Label();
        Label totalLabelValue = new Label("$" + sale.getTotal());
        int rowIndex = 0;
        totalsGrid.add(new Label("Subtotal:"), 0, rowIndex); totalsGrid.add(subtotalLabel, 1, rowIndex++);
        totalsGrid.add(new Label("Tax:"), 0, rowIndex); totalsGrid.add(taxLabel, 1, rowIndex++);
        Label ccFeeTextLabel = new Label("Credit Card Fee:");
        if (sale.getCreditCardFeeAmount() != null && sale.getCreditCardFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            ccFeeLabel.setText("$" + String.format("%.2f", sale.getCreditCardFeeAmount()));
            totalsGrid.add(ccFeeTextLabel, 0, rowIndex); totalsGrid.add(ccFeeLabel, 1, rowIndex++);
        } else {
            ccFeeLabel.setText("$0.00");
        }
        totalsGrid.add(new Label("Total:"), 0, rowIndex); totalsGrid.add(totalLabelValue, 1, rowIndex++);

        HBox itemControls = new HBox(10);
        Button addProductBtn = new Button("Add Product");
        Button addCustomItemBtn = new Button("Add Custom Item");
        Button addServiceBtn = new Button("Add Service");
        Button removeItemBtn = new Button("Remove Item");
        itemControls.getChildren().addAll(addProductBtn, addCustomItemBtn, addServiceBtn, removeItemBtn);

        GridPane paymentGrid = new GridPane();
        paymentGrid.setHgap(10);
        paymentGrid.setVgap(5);
        paymentGrid.add(new Label("Payment Status:"), 0, 0);
        paymentGrid.add(new Label(sale.isPaid() ? "Paid" : "Pending"), 1, 0);
        if (sale.isPaid()) {
            paymentGrid.add(new Label("Payment Method:"), 0, 1);
            paymentGrid.add(new Label(sale.getPaymentMethod()), 1, 1);
            if (sale.getPaymentType() == PaymentType.CREDIT_CARD || sale.getPaymentType() == PaymentType.DEBIT_CARD) {
                paymentGrid.add(new Label("Card Type:"), 0, 2);
                paymentGrid.add(new Label(sale.getCardType() != null ? sale.getCardType() : ""), 1, 2);
                paymentGrid.add(new Label("Card Number:"), 0, 3);
                paymentGrid.add(new Label(sale.getCardLastFour() != null ? "XXXX-XXXX-XXXX-" + sale.getCardLastFour() : ""), 1, 3);
            } else if (sale.getPaymentType() == PaymentType.CHECK) {
                paymentGrid.add(new Label("Check Number:"), 0, 2);
                paymentGrid.add(new Label(sale.getCheckNumber() != null ? sale.getCheckNumber() : ""), 1, 2);
            }
        }
        
        content.getChildren().addAll(
                new Label("Customer Information:"), customerGrid, new Separator(),
                new Label("Sale Items:"), itemsTable, itemControls, new Separator(),
                new Label("Totals:"), totalsGrid, new Separator(),
                new Label("Payment Information:"), paymentGrid
        );

        addProductBtn.setOnAction(e -> {
            showAddProductDialog(sale, owner);
            Sale refreshedSale = salesService.getSaleByIdWithDetails(sale.getId());
            if (refreshedSale != null) {
                itemsTable.setItems(FXCollections.observableArrayList(refreshedSale.getItems()));
                updateTotals(refreshedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
            }
        });
        addCustomItemBtn.setOnAction(e -> {
            showAddCustomItemDialog(sale, owner);
            Sale refreshedSale = salesService.getSaleByIdWithDetails(sale.getId());
            if (refreshedSale != null) {
                itemsTable.setItems(FXCollections.observableArrayList(refreshedSale.getItems()));
                updateTotals(refreshedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
            }
        });
        addServiceBtn.setOnAction(e -> {
            showAddServiceDialog(sale, owner);
            Sale refreshedSale = salesService.getSaleByIdWithDetails(sale.getId());
            if (refreshedSale != null) {
                itemsTable.setItems(FXCollections.observableArrayList(refreshedSale.getItems()));
                updateTotals(refreshedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
            }
        });
        removeItemBtn.setOnAction(e -> {
            SaleItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                salesService.removeItemFromSale(sale.getId(), selectedItem.getId()).ifPresent(updatedSale -> {
                    itemsTable.setItems(FXCollections.observableArrayList(updatedSale.getItems()));
                    refreshSales();
                    updateTotals(updatedSale, subtotalLabel, taxLabel, ccFeeLabel, totalLabelValue);
                    if (mainView != null) mainView.refreshTabsForDataChange("sale");
                });
            } else {
                showAlert("No Selection", "Please select an item to remove.", Alert.AlertType.WARNING);
            }
        });
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);
        
        ButtonType doneButton = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(doneButton);
        
        if (!sale.isPaid()) {
            ButtonType payButton = new ButtonType("Process Payment", ButtonBar.ButtonData.APPLY);
            dialog.getDialogPane().getButtonTypes().add(payButton);
            Button payBtn = (Button) dialog.getDialogPane().lookupButton(payButton);
            payBtn.setOnAction(e -> {
                // Close the details dialog first to avoid dialog-on-dialog conflicts
                dialog.setResult(null);
                dialog.close();
                
                // Process payment after dialog is closed
                Platform.runLater(() -> {
                    boolean paymentSuccessful = paymentController.showPaymentDialog(sale, owner);
                    if (paymentSuccessful) {
                        refreshSales();
                        if (mainView != null) mainView.refreshTabsForDataChange("sale");
                        
                        // Show the enhanced print dialog after successful payment
                        Sale refreshedSale = salesService.getSaleByIdWithDetails(sale.getId());
                        if (refreshedSale != null) {
                            printReceipt(refreshedSale, owner);
                        }
                    }
                });
            });
        }
        
        dialog.showAndWait();
        refreshSales();
    }
    
    /**
     * Show dialog to add a custom item to a sale
     * @param sale The sale to add custom item to
     * @param owner The owner window
     * @param onSuccess Callback to execute after successful addition (optional)
     */
    private void showAddCustomItemDialog(Sale sale, Stage owner, Runnable onSuccess) {
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
            
            if (updatedSale.isPresent()) {
                // Call the success callback if provided
                if (onSuccess != null) {
                    onSuccess.run();
                }
                showAlert("Success", "Custom item added to sale successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Could not add custom item to sale.", Alert.AlertType.ERROR);
            }
        });
    }
    
    /**
     * Show dialog to add a custom item to a sale (overloaded for backward compatibility)
     * @param sale The sale to add custom item to
     * @param owner The owner window
     */
    private void showAddCustomItemDialog(Sale sale, Stage owner) {
        showAddCustomItemDialog(sale, owner, null);
    }
    
    /**
     * Refresh the items table in the edit dialog
     */
    private void refreshItemsTable(TableView<SaleItem> itemsTable, Sale sale) {
        // Refresh the sale from database to get latest items
        Optional<Sale> freshSale = salesService.getSaleById(sale.getId());
        if (freshSale.isPresent()) {
            Sale updatedSale = freshSale.get();
            // Clear and repopulate to force visual refresh
            itemsTable.getItems().clear();
            itemsTable.getItems().addAll(updatedSale.getItems());
            itemsTable.refresh();
        }
    }
    
    /**
     * Update the sale info label with current totals
     */
    private void updateSaleInfoLabel(Label saleInfoLabel, Sale sale) {
        // Refresh the sale from database to get latest totals
        Optional<Sale> freshSale = salesService.getSaleById(sale.getId());
        if (freshSale.isPresent()) {
            Sale updatedSale = freshSale.get();
            saleInfoLabel.setText("Sale #" + updatedSale.getInvoiceNumber() + " - " + 
                                  (updatedSale.getCustomer() != null ? updatedSale.getCustomer().getFullName() : "No Customer") +
                                  " - Total: $" + updatedSale.getTotal());
        }
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
    
    /**
     * Show dialog to add a new customer
     */
    private void showAddCustomerDialog(Stage owner, List<Customer> customers, ComboBox<Customer> customerComboBox) {
        if (mainView != null) {
            mainView.showAddCustomerDialog();
            // Add a delay to ensure the dialog has time to complete and save
            Platform.runLater(() -> {
                // Wait a bit longer for the database transaction to complete
                try {
                    Thread.sleep(500); // Give time for the save operation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Refresh the customer list
                List<Customer> updatedCustomers = salesService.getAllCustomersWithVehicles();
                customers.clear();
                customers.addAll(updatedCustomers);
                customerComboBox.setItems(FXCollections.observableArrayList(customers));
                
                // Select the most recently added customer (last in the list)
                if (!updatedCustomers.isEmpty()) {
                    Customer lastCustomer = updatedCustomers.get(updatedCustomers.size() - 1);
                    customerComboBox.setValue(lastCustomer);
                    // Trigger the customer selection event to enable vehicle dropdown
                    customerComboBox.fireEvent(new ActionEvent());
                }
            });
        } else {
            showAlert("Error", "Unable to add customer at this time.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Show dialog to add a new vehicle for a customer
     */
    private void showAddVehicleDialog(Stage owner, Customer customer, ComboBox<Vehicle> vehicleComboBox) {
        if (mainView != null) {
            mainView.showAddVehicleDialog(customer);
            // Add a delay to ensure the dialog has time to complete and save
            Platform.runLater(() -> {
                // Wait a bit longer for the database transaction to complete
                try {
                    Thread.sleep(500); // Give time for the save operation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Refresh the vehicle list
                List<Vehicle> updatedVehicles = salesService.getVehiclesForCustomer(customer.getId());
                customer.setVehicles(updatedVehicles);
                vehicleComboBox.setItems(FXCollections.observableArrayList(updatedVehicles));
                
                // Select the most recently added vehicle (last in the list)
                if (!updatedVehicles.isEmpty()) {
                    Vehicle lastVehicle = updatedVehicles.get(updatedVehicles.size() - 1);
                    vehicleComboBox.setValue(lastVehicle);
                }
            });
        } else {
            showAlert("Error", "Unable to add vehicle at this time.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Show dialog to edit customer for a sale
     * @param sale The sale to edit customer for
     * @param owner The owner window
     */
    private void showCustomerEditDialog(Sale sale, Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer for Invoice #" + sale.getInvoiceNumber());
        dialog.setHeaderText("Add or change customer for this sale");
        dialog.initOwner(owner);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType removeButtonType = new ButtonType("Remove Customer", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, removeButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Current customer display
        Label currentCustomerLabel = new Label("Current Customer:");
        currentCustomerLabel.setStyle("-fx-font-weight: bold;");
        
        Label customerInfoLabel = new Label();
        if (sale.getCustomer() != null) {
            customerInfoLabel.setText(sale.getCustomer().getFullName() + " (" + sale.getCustomer().getPhone() + ")");
            if (sale.getCustomer().isTaxExempt()) {
                customerInfoLabel.setText(customerInfoLabel.getText() + " - TAX EXEMPT");
                customerInfoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        } else {
            customerInfoLabel.setText("No customer assigned");
            customerInfoLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
        
        // Customer selection with search filter
        Label selectLabel = new Label("Search & Select Customer:");

        TextField searchField = new TextField();
        searchField.setPromptText("Type to search customers...");
        searchField.setPrefWidth(300);

        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPrefWidth(300);

        // Load all customers into a modifiable list
        final List<Customer> allCustomers = new ArrayList<>(salesService.getAllCustomersWithVehicles());
        ObservableList<Customer> filteredCustomers = FXCollections.observableArrayList(allCustomers);
        customerComboBox.setItems(filteredCustomers);
        if (sale.getCustomer() != null) {
            customerComboBox.setValue(sale.getCustomer());
        }

        // Add search filter functionality - shows dropdown as you type
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String searchTerm = newVal.toLowerCase().trim();
            if (searchTerm.isEmpty()) {
                filteredCustomers.setAll(allCustomers);
                customerComboBox.hide();
            } else {
                List<Customer> filtered = allCustomers.stream()
                    .filter(c -> {
                        String fullName = (c.getFirstName() + " " + c.getLastName()).toLowerCase();
                        String phone = c.getPhone() != null ? c.getPhone().toLowerCase() : "";
                        return fullName.contains(searchTerm) || phone.contains(searchTerm);
                    })
                    .collect(Collectors.toList());
                filteredCustomers.setAll(filtered);

                // Auto-show dropdown with results
                if (!filtered.isEmpty()) {
                    customerComboBox.show();
                    // Auto-select first match
                    customerComboBox.getSelectionModel().selectFirst();
                }
            }
        });

        // Add customer button
        Button addCustomerBtn = new Button("Add New Customer");
        addCustomerBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addCustomerBtn.setOnAction(e -> {
            if (mainView != null) {
                mainView.showAddCustomerDialog();
                // Refresh customer list after adding
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    List<Customer> updatedCustomers = salesService.getAllCustomersWithVehicles();
                    allCustomers.clear();
                    allCustomers.addAll(updatedCustomers);
                    filteredCustomers.setAll(updatedCustomers);
                    searchField.clear();
                });
            }
        });
        
        content.getChildren().addAll(currentCustomerLabel, customerInfoLabel,
                                   new Separator(), selectLabel, searchField, customerComboBox, addCustomerBtn);
        dialog.getDialogPane().setContent(content);
        
        // Handle button actions
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button removeButton = (Button) dialog.getDialogPane().lookupButton(removeButtonType);
        
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Customer selectedCustomer = customerComboBox.getValue();
            if (selectedCustomer != null) {
                sale.setCustomer(selectedCustomer);
                // Recalculate amounts to apply tax exempt status if applicable
                sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                      PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? 
                                      settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
                salesService.updateSale(sale);
                refreshSales();
                showAlert("Customer Updated", "Customer has been assigned to the sale.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("No Selection", "Please select a customer.", Alert.AlertType.WARNING);
                event.consume();
            }
        });
        
        removeButton.addEventFilter(ActionEvent.ACTION, event -> {
            sale.setCustomer(null);
            sale.recalculateAmounts(settingsService.getSalesTaxRate(), 
                                  PaymentType.CREDIT_CARD.equals(sale.getPaymentType()) ? 
                                  settingsService.getCreditCardFeePercentage() : BigDecimal.ZERO);
            salesService.updateSale(sale);
            refreshSales();
            showAlert("Customer Removed", "Customer has been removed from the sale.", Alert.AlertType.INFORMATION);
        });
        
        dialog.showAndWait();
    }

    /**
     * Show dialog to add/edit vehicle for a sale
     * @param sale The sale to edit vehicle for
     * @param owner The owner window
     */
    private void showVehicleEditDialog(Sale sale, Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vehicle for Invoice #" + sale.getInvoiceNumber());
        dialog.setHeaderText("Add or change vehicle for this sale");
        dialog.initOwner(owner);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType removeButtonType = new ButtonType("Remove Vehicle", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, removeButtonType, ButtonType.CANCEL);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Current vehicle display
        Label currentVehicleLabel = new Label("Current Vehicle:");
        currentVehicleLabel.setStyle("-fx-font-weight: bold;");

        Label vehicleInfoLabel = new Label();
        if (sale.getVehicle() != null) {
            Vehicle v = sale.getVehicle();
            vehicleInfoLabel.setText(v.getModelYear() + " " + v.getMake() + " " + v.getModel());
        } else {
            vehicleInfoLabel.setText("No vehicle assigned");
            vehicleInfoLabel.setStyle("-fx-text-fill: #e74c3c;");
        }

        // Customer info
        Label customerLabel = new Label("Customer:");
        customerLabel.setStyle("-fx-font-weight: bold;");

        Label customerNameLabel = new Label();
        if (sale.getCustomer() != null) {
            customerNameLabel.setText(sale.getCustomer().getFullName());
        } else {
            customerNameLabel.setText("No customer - please assign a customer first");
            customerNameLabel.setStyle("-fx-text-fill: #e74c3c;");
        }

        // Vehicle selection
        Label selectLabel = new Label("Select Vehicle:");
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPrefWidth(300);

        // Load customer's vehicles
        if (sale.getCustomer() != null) {
            List<Vehicle> customerVehicles = salesService.getVehiclesForCustomer(sale.getCustomer().getId());
            vehicleComboBox.setItems(FXCollections.observableArrayList(customerVehicles));
            if (sale.getVehicle() != null) {
                // Find and select matching vehicle
                for (Vehicle v : customerVehicles) {
                    if (v.getId().equals(sale.getVehicle().getId())) {
                        vehicleComboBox.setValue(v);
                        break;
                    }
                }
            }
        }

        // Custom display for vehicle combo
        vehicleComboBox.setCellFactory(lv -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                if (empty || vehicle == null) {
                    setText(null);
                } else {
                    setText(vehicle.getModelYear() + " " + vehicle.getMake() + " " + vehicle.getModel());
                }
            }
        });
        vehicleComboBox.setButtonCell(new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                if (empty || vehicle == null) {
                    setText("Select a vehicle...");
                } else {
                    setText(vehicle.getModelYear() + " " + vehicle.getMake() + " " + vehicle.getModel());
                }
            }
        });

        // Add vehicle button
        Button addVehicleBtn = new Button("Add New Vehicle");
        addVehicleBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        addVehicleBtn.setDisable(sale.getCustomer() == null);

        addVehicleBtn.setOnAction(e -> {
            if (sale.getCustomer() != null && mainView != null) {
                mainView.showAddVehicleDialog(sale.getCustomer());
                // Refresh vehicle list after adding
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    List<Vehicle> updatedVehicles = salesService.getVehiclesForCustomer(sale.getCustomer().getId());
                    vehicleComboBox.setItems(FXCollections.observableArrayList(updatedVehicles));
                    // Select the most recently added vehicle
                    if (!updatedVehicles.isEmpty()) {
                        vehicleComboBox.setValue(updatedVehicles.get(updatedVehicles.size() - 1));
                    }
                });
            }
        });

        content.getChildren().addAll(
            currentVehicleLabel, vehicleInfoLabel,
            new Separator(),
            customerLabel, customerNameLabel,
            new Separator(),
            selectLabel, vehicleComboBox, addVehicleBtn
        );
        dialog.getDialogPane().setContent(content);

        // Handle button actions
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button removeButton = (Button) dialog.getDialogPane().lookupButton(removeButtonType);

        // Disable save if no customer
        saveButton.setDisable(sale.getCustomer() == null);

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Vehicle selectedVehicle = vehicleComboBox.getValue();
            if (selectedVehicle != null) {
                sale.setVehicle(selectedVehicle);
                salesService.updateSale(sale);
                refreshSales();
                showAlert("Vehicle Updated", "Vehicle has been assigned to the sale.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("No Selection", "Please select a vehicle.", Alert.AlertType.WARNING);
                event.consume();
            }
        });

        removeButton.addEventFilter(ActionEvent.ACTION, event -> {
            sale.setVehicle(null);
            salesService.updateSale(sale);
            refreshSales();
            showAlert("Vehicle Removed", "Vehicle has been removed from the sale.", Alert.AlertType.INFORMATION);
        });

        dialog.showAndWait();
    }

    /**
     * Process a customer return/void sale
     * Shows confirmation dialog, voids the sale, restores inventory, and auto-prints return receipt
     */
    private void processReturnSale(Sale sale, Stage owner) {
        // Ask for return reason
        TextInputDialog reasonDialog = new TextInputDialog("Customer Return");
        reasonDialog.setTitle("Return/Void Sale");
        reasonDialog.setHeaderText("Void Sale: " + sale.getInvoiceNumber());
        reasonDialog.setContentText("Return Reason:");
        reasonDialog.initOwner(owner);
        
        Optional<String> reasonResult = reasonDialog.showAndWait();
        if (reasonResult.isPresent()) {
            String reason = reasonResult.get();
            
            // Confirm the return
            if (showConfirmationDialog("Confirm Return", 
                    "Are you sure you want to void this sale?\n\n" +
                    "Invoice: " + sale.getInvoiceNumber() + "\n" +
                    "Total: $" + sale.getTotal() + "\n\n" +
                    "All items will be returned to inventory.\n" +
                    "A return receipt will be printed automatically.",
                    owner)) {
                
                // Void the sale
                salesService.voidSale(sale.getId(), reason).ifPresentOrElse(
                    voidedSale -> {
                        // Print return receipt automatically
                        try {
                            printReturnReceipt(voidedSale, owner);
                        } catch (Exception e) {
                            System.err.println("Failed to print return receipt: " + e.getMessage());
                            showAlert("Print Warning", "Sale voided successfully but receipt failed to print: " + e.getMessage(), 
                                    Alert.AlertType.WARNING);
                        }
                        
                        // Refresh all views
                        refreshSales();
                        if (mainView != null) {
                            mainView.refreshTabsForDataChange("all");
                        }
                        
                        showAlert("Sale Voided", 
                                "Sale " + voidedSale.getInvoiceNumber() + " has been voided.\n" +
                                "Return receipt has been printed.\n" +
                                "All items have been returned to inventory.", 
                                Alert.AlertType.INFORMATION);
                    },
                    () -> showAlert("Void Failed", "Could not void the sale. Please check if it's already voided.", Alert.AlertType.ERROR)
                );
            }
        }
    }
    
    /**
     * Print a return/void receipt showing negative amounts
     */
    private void printReturnReceipt(Sale voidedSale, Stage owner) throws Exception {
        if (printerService == null) {
            throw new Exception("Printer service not initialized");
        }
        
        // Show print dialog with options
        Alert printDialog = new Alert(Alert.AlertType.CONFIRMATION);
        printDialog.setTitle("Print Return Receipt");
        printDialog.setHeaderText("Return Receipt for " + voidedSale.getInvoiceNumber());
        printDialog.initOwner(owner);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label infoLabel = new Label("Refund Amount: $" + voidedSale.getTotal());
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        
        Label descLabel = new Label("The return receipt will show NEGATIVE amounts (-$" + 
                                   voidedSale.getTotal() + ") for bookkeeping.");
        descLabel.setWrapText(true);
        
        content.getChildren().addAll(infoLabel, descLabel);
        printDialog.getDialogPane().setContent(content);
        
        ButtonType printButton = new ButtonType("Print Receipt", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipButton = new ButtonType("Skip Printing", ButtonBar.ButtonData.CANCEL_CLOSE);
        printDialog.getButtonTypes().setAll(printButton, skipButton);
        
        Optional<ButtonType> result = printDialog.showAndWait();
        if (result.isPresent() && result.get() == printButton) {
            // Print return receipt with negative amounts
            boolean success = printerService.printReturnReceipt(voidedSale);
            
            if (!success) {
                throw new Exception("Printer service returned false - check printer connection");
            }
        }
    }
    
    /**
     * Process partial return - customer returns some items but not all
     */
    private void processPartialReturn(Sale sale, Stage owner) {
        // Show dialog to select which items to return
        Dialog<List<ReturnItem>> dialog = new Dialog<>();
        dialog.setTitle("Partial Return");
        dialog.setHeaderText("Select Items to Return - Invoice: " + sale.getInvoiceNumber());
        dialog.initOwner(owner);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label instructionLabel = new Label("Check the items being returned and enter quantities:");
        instructionLabel.setStyle("-fx-font-weight: bold;");
        
        // Create table for item selection
        TableView<ReturnItemRow> itemsTable = new TableView<>();
        
        TableColumn<ReturnItemRow, Boolean> selectCol = new TableColumn<>("Return");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(tc -> new CheckBoxTableCell<>());
        selectCol.setPrefWidth(60);
        
        TableColumn<ReturnItemRow, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getItem().getItemName()));
        itemCol.setPrefWidth(250);
        
        TableColumn<ReturnItemRow, Integer> qtyBoughtCol = new TableColumn<>("Qty Bought");
        qtyBoughtCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getItem().getQuantity()));
        qtyBoughtCol.setPrefWidth(100);
        
        TableColumn<ReturnItemRow, Integer> qtyReturnCol = new TableColumn<>("Qty Return");
        qtyReturnCol.setCellValueFactory(cellData -> cellData.getValue().returnQuantityProperty().asObject());
        qtyReturnCol.setCellFactory(tc -> new TableCell<ReturnItemRow, Integer>() {
            private Spinner<Integer> spinner;
            
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (spinner == null) {
                        ReturnItemRow row = getTableRow().getItem();
                        if (row != null) {
                            // Max returnable = units still outstanding (some may already be returned)
                            int maxReturnable = row.getItem().getQuantity() - row.getItem().getQuantityReturned();
                            spinner = new Spinner<>(0, Math.max(maxReturnable, 0), 0);
                            spinner.setEditable(true);
                            spinner.setPrefWidth(80);
                            spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                                if (newVal == null) {
                                    return; // editable spinner cleared - ignore until valid again
                                }
                                row.returnQuantityProperty().set(newVal);
                                // Automatically check/uncheck the checkbox based on quantity
                                if (newVal > 0) {
                                    row.selectedProperty().set(true);
                                } else {
                                    row.selectedProperty().set(false);
                                }
                            });
                        }
                    }
                    setGraphic(spinner);
                }
            }
        });
        qtyReturnCol.setPrefWidth(100);
        
        TableColumn<ReturnItemRow, String> priceCol = new TableColumn<>("Price Each");
        priceCol.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getItem().getUnitPrice()));
        priceCol.setPrefWidth(100);
        
        itemsTable.getColumns().addAll(selectCol, itemCol, qtyBoughtCol, qtyReturnCol, priceCol);
        
        // Populate table with sale items
        ObservableList<ReturnItemRow> returnItems = FXCollections.observableArrayList();
        for (SaleItem item : sale.getItems()) {
            returnItems.add(new ReturnItemRow(item));
        }
        itemsTable.setItems(returnItems);
        itemsTable.setPrefHeight(300);
        
        content.getChildren().addAll(instructionLabel, itemsTable);
        dialog.getDialogPane().setContent(content);
        
        ButtonType processButton = new ButtonType("Process Return", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(processButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(button -> {
            if (button == processButton) {
                List<ReturnItem> returns = new ArrayList<>();
                for (ReturnItemRow row : returnItems) {
                    if (row.isSelected() && row.getReturnQuantity() > 0) {
                        returns.add(new ReturnItem(row.getItem(), row.getReturnQuantity()));
                    }
                }
                return returns.isEmpty() ? null : returns;
            }
            return null;
        });
        
        Optional<List<ReturnItem>> result = dialog.showAndWait();
        if (result.isPresent() && result.get() != null && !result.get().isEmpty()) {
            List<ReturnItem> itemsToReturn = result.get();
            
            // Ask for return reason
            TextInputDialog reasonDialog = new TextInputDialog("Partial Customer Return");
            reasonDialog.setTitle("Return Reason");
            reasonDialog.setHeaderText("Why are these items being returned?");
            reasonDialog.initOwner(owner);
            
            Optional<String> reasonResult = reasonDialog.showAndWait();
            if (reasonResult.isPresent()) {
                String reason = reasonResult.get();

                // Estimate the refund proportionally to the sale's totals so the
                // estimate includes tax (and any discount), not just raw item prices
                BigDecimal itemsSubtotal = itemsToReturn.stream()
                    .map(ri -> ri.item.getUnitPrice().multiply(BigDecimal.valueOf(ri.quantity)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal saleSubtotal = sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO;
                BigDecimal preReturnTotal = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;
                BigDecimal refundEstimate = itemsSubtotal;
                if (saleSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    refundEstimate = itemsSubtotal.multiply(preReturnTotal)
                            .divide(saleSubtotal, 2, java.math.RoundingMode.HALF_UP);
                }

                // Confirm
                if (showConfirmationDialog("Confirm Partial Return",
                        "Returning " + itemsToReturn.size() + " item type(s)\n" +
                        "Estimated Refund: $" + refundEstimate + "\n\n" +
                        "Items will be returned to inventory.\n" +
                        "Return receipt will be printed.",
                        owner)) {

                    // Process partial return
                    salesService.partialReturnSale(sale.getId(), itemsToReturn, reason).ifPresentOrElse(
                        updatedSale -> {
                            refreshSales();
                            if (mainView != null) {
                                mainView.refreshTabsForDataChange("all");
                            }

                            // Exact refund = what the sale total actually dropped by
                            BigDecimal updatedTotal = updatedSale.getTotal() != null ? updatedSale.getTotal() : BigDecimal.ZERO;
                            BigDecimal exactRefund = preReturnTotal.subtract(updatedTotal);

                            showAlert("Partial Return Processed",
                                    "Items returned to inventory.\n" +
                                    "Refund: $" + exactRefund,
                                    Alert.AlertType.INFORMATION);

                            // Show print dialog for partial return receipt
                            printPartialReturnReceipt(updatedSale, itemsToReturn, exactRefund, owner);
                        },
                        () -> showAlert("Return Failed", "Could not process partial return.", Alert.AlertType.ERROR)
                    );
                }
            }
        }
    }
    
    /**
     * Print partial return receipt with print options dialog
     */
    private void printPartialReturnReceipt(Sale sale, List<ReturnItem> itemsToReturn, BigDecimal refundAmount, Stage owner) {
        // Create print options dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Print Partial Return Receipt");
        dialog.setHeaderText("Partial Return Receipt Printing Options");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType printButtonType = new ButtonType("Print", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(printButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Number of copies selection
        Label copiesLabel = new Label("Number of Copies:");
        copiesLabel.setStyle("-fx-font-weight: bold;");
        
        Spinner<Integer> copiesSpinner = new Spinner<>(1, 10, 2); // Default to 2 copies
        copiesSpinner.setEditable(true);
        copiesSpinner.setPrefWidth(80);
        
        HBox copiesBox = new HBox(10);
        copiesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        copiesBox.getChildren().addAll(copiesLabel, copiesSpinner);
        
        // Action selection
        Label actionLabel = new Label("Action:");
        actionLabel.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup actionGroup = new ToggleGroup();
        RadioButton printAction = new RadioButton("Print to Printer");
        RadioButton pdfPreviewAction = new RadioButton("Generate PDF Preview");
        
        printAction.setToggleGroup(actionGroup);
        pdfPreviewAction.setToggleGroup(actionGroup);
        printAction.setSelected(true); // Default to printing
        
        VBox actionOptions = new VBox(8);
        actionOptions.getChildren().addAll(printAction, pdfPreviewAction);
        
        // Printer selection (only show when printing)
        Label printerLabel = new Label("Select Printer:");
        printerLabel.setStyle("-fx-font-weight: bold;");
        
        ComboBox<String> printerComboBox = new ComboBox<>();
        printerComboBox.getItems().add("Default Printer");
        
        // Add available printers
        java.util.List<String> printers = printerService.getAvailablePrinters();
        if (!printers.isEmpty()) {
            printerComboBox.getItems().addAll(printers);
        }
        printerComboBox.setValue("Default Printer");
        
        // Show/hide printer selection and copies based on action
        VBox printerSection = new VBox(8);
        printerSection.getChildren().addAll(printerLabel, printerComboBox);
        
        printAction.setOnAction(e -> {
            printerSection.setVisible(true);
            copiesBox.setVisible(true);
        });
        pdfPreviewAction.setOnAction(e -> {
            printerSection.setVisible(false);
            copiesBox.setVisible(false);
        });
        
        content.getChildren().addAll(
                copiesBox,
                new Separator(),
                actionLabel,
                actionOptions,
                new Separator(),
                printerSection
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> dialogButton == printButtonType);
        
        // Show dialog and process result
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            
            if (pdfPreviewAction.isSelected()) {
                // Generate PDF preview
                String fileName = "partial_return_" + sale.getInvoiceNumber() + "_" + 
                                 java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
                String filePath = System.getProperty("user.home") + "/Desktop/" + fileName;
                
                boolean pdfGenerated = printerService.generatePartialReturnPdf(sale, itemsToReturn, refundAmount, filePath);
                
                if (pdfGenerated) {
                    showAlert("PDF Generated", 
                            "The partial return receipt has been saved to your Desktop as: " + fileName, 
                            Alert.AlertType.INFORMATION);
                    
                    // Try to open the PDF
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Could not open PDF file: " + e.getMessage());
                    }
                } else {
                    showAlert("PDF Error", "Failed to generate partial return receipt PDF.", Alert.AlertType.ERROR);
                }
                
            } else {
                // Print to printer
                String printerNameFromDialog = printerComboBox.getValue();
                if ("Default Printer".equals(printerNameFromDialog)) {
                    printerNameFromDialog = null; // Let PrinterService use its default
                }

                Integer copiesValue = copiesSpinner.getValue();
                int numberOfCopies = copiesValue != null && copiesValue > 0 ? copiesValue : 1;
                boolean allPrinted = true;
                
                // Print multiple copies
                for (int i = 1; i <= numberOfCopies; i++) {
                    boolean printed = printerService.printPartialReturnReceipt(sale, itemsToReturn, refundAmount, printerNameFromDialog);
                    
                    if (!printed) {
                        allPrinted = false;
                        break;
                    }
                }
                
                if (allPrinted) {
                    showAlert("Print Success", 
                            "Printed " + numberOfCopies + " cop" + (numberOfCopies > 1 ? "ies" : "y") + " of partial return receipt.", 
                            Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Print Error", "Failed to print partial return receipt.", Alert.AlertType.ERROR);
                }
            }
        }
    }
    
    /**
     * Helper class for partial return row
     */
    private static class ReturnItemRow {
        private final SaleItem item;
        private final javafx.beans.property.BooleanProperty selected = new javafx.beans.property.SimpleBooleanProperty(false);
        private final javafx.beans.property.IntegerProperty returnQuantity = new javafx.beans.property.SimpleIntegerProperty(0);
        
        public ReturnItemRow(SaleItem item) {
            this.item = item;
        }
        
        public SaleItem getItem() { return item; }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public javafx.beans.property.IntegerProperty returnQuantityProperty() { return returnQuantity; }
        public int getReturnQuantity() { return returnQuantity.get(); }
    }
    
    /**
     * Helper class for return item info
     */
    public static class ReturnItem {
        public final SaleItem item;
        public final int quantity;
        
        public ReturnItem(SaleItem item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }
    }
} 