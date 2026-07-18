package com.tireshop.controller;

import com.tireshop.dao.ServiceDao;
import com.tireshop.dao.TechnicianDao;
import com.tireshop.model.Service;
import com.tireshop.model.Technician; // Assuming we might manage technicians here too
import com.tireshop.util.DatabaseManager; // For session factory if creating DAOs internally, or pass them
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.application.Platform;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ServiceController {

    private ServiceDao serviceDao;
    private TechnicianDao technicianDao; // Keep for now, might use later
    private Stage stage;

    private TableView<Service> servicesTable;
    private ObservableList<Service> servicesList;

    private TableView<Technician> techniciansTable;
    private ObservableList<Technician> techniciansList;

    // Fields for Add/Edit Service Dialog
    private TextField nameField;
    private TextArea descriptionArea;
    private TextField priceField;
    private TextField categoryField;
    private TextField durationField; // Estimated duration in minutes
    private CheckBox taxableCheckBox;

    public ServiceController(ServiceDao serviceDao, TechnicianDao technicianDao) {
        System.out.println("[CONSTRUCTOR] ServiceController instance created.");
        this.serviceDao = serviceDao;
        this.technicianDao = technicianDao;
    }

    public void initialize(BorderPane parentPane, Stage stage) {
        System.out.println("[INITIALIZE] ServiceController.initialize called.");
        this.stage = stage;

        // Main content pane for this controller will be a SplitPane
        SplitPane splitPane = new SplitPane();

        // --- Services Panel (Left side of SplitPane) ---
        BorderPane servicesPane = new BorderPane();
        servicesPane.setPadding(new Insets(10));

        VBox servicesContainer = new VBox(10);
        Label servicesTitle = new Label("Manage Services");
        servicesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        HBox serviceControls = new HBox(10);
        serviceControls.setPadding(new Insets(5,0,5,0));
        Button addServiceBtn = new Button("Add Service");
        Button editServiceBtn = new Button("Edit Service");
        Button deleteServiceBtn = new Button("Delete Service");
        Button refreshServicesBtn = new Button("Refresh Services");

        addServiceBtn.setOnAction(e -> showAddServiceDialog());
        editServiceBtn.setOnAction(e -> {
            Service selectedService = servicesTable.getSelectionModel().getSelectedItem();
            if (selectedService != null) {
                showEditServiceDialog(selectedService);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a service to edit.");
            }
        });
        deleteServiceBtn.setOnAction(e -> {
            Service selectedService = servicesTable.getSelectionModel().getSelectedItem();
            if (selectedService != null) {
                handleDeleteService(selectedService);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a service to delete.");
            }
        });
        refreshServicesBtn.setOnAction(e -> refreshServicesTable());
        serviceControls.getChildren().addAll(addServiceBtn, editServiceBtn, deleteServiceBtn, refreshServicesBtn);

        servicesTable = new TableView<>();
        setupServicesTable();
        servicesContainer.getChildren().addAll(servicesTitle, serviceControls, servicesTable);
        VBox.setVgrow(servicesTable, javafx.scene.layout.Priority.ALWAYS);
        servicesPane.setCenter(servicesContainer);

        // --- Technicians Panel (Right side of SplitPane) ---
        BorderPane techniciansPane = new BorderPane();
        techniciansPane.setPadding(new Insets(10));
        
        VBox techniciansContainer = new VBox(10);
        Label techniciansTitle = new Label("Manage Technicians");
        techniciansTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox technicianControls = new HBox(10);
        technicianControls.setPadding(new Insets(5,0,5,0));
        Button addTechnicianBtn = new Button("Add Technician");
        Button editTechnicianBtn = new Button("Edit Technician");
        Button deleteTechnicianBtn = new Button("Delete Technician");
        Button refreshTechniciansBtn = new Button("Refresh Technicians");

        // TODO: Implement technician button actions
        addTechnicianBtn.setOnAction(e -> showAddTechnicianDialog());
        editTechnicianBtn.setOnAction(e -> {
            Technician selectedTechnician = techniciansTable.getSelectionModel().getSelectedItem();
            if (selectedTechnician != null) {
                showEditTechnicianDialog(selectedTechnician);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a technician to edit.");
            }
        });
        deleteTechnicianBtn.setOnAction(e -> {
            Technician selectedTechnician = techniciansTable.getSelectionModel().getSelectedItem();
            if (selectedTechnician != null) {
                handleDeleteTechnician(selectedTechnician);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a technician to delete.");
            }
        });
        refreshTechniciansBtn.setOnAction(e -> refreshTechniciansTable());

        technicianControls.getChildren().addAll(addTechnicianBtn, editTechnicianBtn, deleteTechnicianBtn, refreshTechniciansBtn);

        techniciansTable = new TableView<>();
        setupTechniciansTable(); // We will create this method
        techniciansContainer.getChildren().addAll(techniciansTitle, technicianControls, techniciansTable);
        VBox.setVgrow(techniciansTable, javafx.scene.layout.Priority.ALWAYS);
        techniciansPane.setCenter(techniciansContainer);

        // Add services and technicians panes to the split pane
        splitPane.getItems().addAll(servicesPane, techniciansPane);
        splitPane.setDividerPositions(0.5); // Equal split initially

        // Initial data load
        refreshServicesTable();
        refreshTechniciansTable(); // We will create this method

        parentPane.setCenter(splitPane);
        System.out.println("[INITIALIZE] ServiceController UI setup complete with SplitPane.");
    }

    private void setupServicesTable() {
        TableColumn<Service, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Service, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        TableColumn<Service, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(300);

        TableColumn<Service, BigDecimal> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        // Custom cell factory for BigDecimal formatting if needed
        priceColumn.setCellFactory(column -> new TableCell<Service, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                }
            }
        });


        TableColumn<Service, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setPrefWidth(150);

        TableColumn<Service, Integer> durationColumn = new TableColumn<>("Duration (min)");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedDurationMinutes"));

        TableColumn<Service, Boolean> taxableColumn = new TableColumn<>("Taxable");
        taxableColumn.setCellValueFactory(new PropertyValueFactory<>("taxable"));
        taxableColumn.setCellFactory(column -> new TableCell<Service, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Yes" : "No");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        servicesTable.getColumns().addAll(idColumn, nameColumn, descriptionColumn, priceColumn, categoryColumn, durationColumn, taxableColumn);
        servicesList = FXCollections.observableArrayList();
        servicesTable.setItems(servicesList);
    }

    public void refreshServicesTable() {
        System.out.println("[ServiceController] Refreshing services table...");
        try {
            List<Service> currentServices = serviceDao.findAll();
            servicesList.setAll(currentServices);
            System.out.println("[ServiceController] Found " + currentServices.size() + " services.");
        } catch (Exception e) {
            System.err.println("[ServiceController] Error refreshing services table: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load services from the database.");
        }
    }

    private void showAddServiceDialog() {
        Dialog<Service> dialog = new Dialog<>();
        dialog.setTitle("Add New Service");
        dialog.setHeaderText("Enter details for the new service.");
        dialog.initOwner(stage);

        setupServiceDialogPane(dialog, null); // null for new service

        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(service -> {
            try {
                serviceDao.save(service);
                refreshServicesTable();
                showAlert(Alert.AlertType.INFORMATION, "Service Added", "New service added successfully.");
            } catch (Exception e) {
                System.err.println("[ServiceController] Error saving new service: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save the new service.");
            }
        });
    }

    private void showEditServiceDialog(Service serviceToEdit) {
        Dialog<Service> dialog = new Dialog<>();
        dialog.setTitle("Edit Service");
        dialog.setHeaderText("Update details for the service: " + serviceToEdit.getName());
        dialog.initOwner(stage);

        setupServiceDialogPane(dialog, serviceToEdit);

        Optional<Service> result = dialog.showAndWait();
        result.ifPresent(editedService -> {
            try {
                serviceDao.update(editedService);
                refreshServicesTable();
                showAlert(Alert.AlertType.INFORMATION, "Service Updated", "Service updated successfully.");
            } catch (Exception e) {
                System.err.println("[ServiceController] Error updating service: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Update Error", "Could not update the service.");
            }
        });
    }

    private void setupServiceDialogPane(Dialog<Service> dialog, Service existingService) {
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        nameField = new TextField();
        nameField.setPromptText("Service Name");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(3);
        priceField = new TextField();
        priceField.setPromptText("0.00");
        categoryField = new TextField();
        categoryField.setPromptText("Service Category");
        durationField = new TextField();
        durationField.setPromptText("e.g., 30");
        taxableCheckBox = new CheckBox("Taxable");

        if (existingService != null) {
            nameField.setText(existingService.getName());
            descriptionArea.setText(existingService.getDescription());
            priceField.setText(existingService.getPrice() != null ? existingService.getPrice().toPlainString() : "");
            categoryField.setText(existingService.getCategory());
            durationField.setText(String.valueOf(existingService.getEstimatedDurationMinutes()));
            taxableCheckBox.setSelected(existingService.isTaxable());
        } else {
            // Default to taxable for new services
            taxableCheckBox.setSelected(true);
        }

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryField, 1, 3);
        grid.add(new Label("Duration (min):"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(new Label("Tax Status:"), 0, 5);
        grid.add(taxableCheckBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> nameField.requestFocus());

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Service name cannot be empty.");
                event.consume();
                return;
            }
            try {
                new BigDecimal(priceField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid price format. Please enter a number.");
                event.consume();
                return;
            }
            try {
                Integer.parseInt(durationField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid duration format. Please enter a whole number for minutes.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Service service = (existingService != null) ? existingService : new Service();
                service.setName(nameField.getText().trim());
                service.setDescription(descriptionArea.getText().trim());
                try {
                    service.setPrice(new BigDecimal(priceField.getText().trim()));
                } catch (NumberFormatException e) { /* already handled by filter, but good practice */ return null;}
                service.setCategory(categoryField.getText().trim());
                try {
                     service.setEstimatedDurationMinutes(Integer.parseInt(durationField.getText().trim()));
                } catch (NumberFormatException e) { /* already handled by filter */ return null;}
                service.setTaxable(taxableCheckBox.isSelected());
                return service;
            }
            return null;
        });
    }

    private void handleDeleteService(Service service) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Service");
        confirmAlert.setHeaderText("Confirm Deletion");
        confirmAlert.setContentText("Are you sure you want to delete the service: " + service.getName() + "?");
        confirmAlert.initOwner(stage);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceDao.delete(service);
                refreshServicesTable();
                showAlert(Alert.AlertType.INFORMATION, "Service Deleted", "Service deleted successfully.");
            } catch (Exception e) {
                 // Consider foreign key constraints, etc.
                System.err.println("[ServiceController] Error deleting service: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Delete Error", "Could not delete the service. It might be in use.");
            }
        }
    }

    private void setupTechniciansTable() {
        TableColumn<Technician, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Technician, String> firstNameColumn = new TableColumn<>("First Name");
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        firstNameColumn.setPrefWidth(120);

        TableColumn<Technician, String> lastNameColumn = new TableColumn<>("Last Name");
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameColumn.setPrefWidth(120);

        TableColumn<Technician, String> employeeIdColumn = new TableColumn<>("Employee ID");
        employeeIdColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        employeeIdColumn.setPrefWidth(100);

        TableColumn<Technician, String> specializationColumn = new TableColumn<>("Specialization");
        specializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        specializationColumn.setPrefWidth(150);

        TableColumn<Technician, Boolean> isActiveColumn = new TableColumn<>("Active");
        isActiveColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        isActiveColumn.setCellFactory(column -> new TableCell<Technician, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Yes" : "No");
                }
            }
        });

        techniciansTable.getColumns().addAll(idColumn, firstNameColumn, lastNameColumn, employeeIdColumn, specializationColumn, isActiveColumn);
        techniciansList = FXCollections.observableArrayList();
        techniciansTable.setItems(techniciansList);
    }

    private void refreshTechniciansTable() {
        System.out.println("[ServiceController] Refreshing technicians table...");
        try {
            List<Technician> currentTechnicians = technicianDao.findAll();
            techniciansList.setAll(currentTechnicians);
            System.out.println("[ServiceController] Found " + currentTechnicians.size() + " technicians.");
        } catch (Exception e) {
            System.err.println("[ServiceController] Error refreshing technicians table: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load technicians from the database.");
        }
    }

    private void showAddTechnicianDialog() {
        Dialog<Technician> dialog = new Dialog<>();
        dialog.setTitle("Add New Technician");
        dialog.setHeaderText("Enter details for the new technician.");
        dialog.initOwner(stage);
        setupTechnicianDialogPane(dialog, null);
        Optional<Technician> result = dialog.showAndWait();
        result.ifPresent(technician -> {
            try {
                technicianDao.save(technician);
                refreshTechniciansTable();
                showAlert(Alert.AlertType.INFORMATION, "Technician Added", "New technician added successfully.");
            } catch (Exception e) {
                System.err.println("[ServiceController] Error saving new technician: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save the new technician.");
            }
        });
    }

    private void showEditTechnicianDialog(Technician technicianToEdit) {
        Dialog<Technician> dialog = new Dialog<>();
        dialog.setTitle("Edit Technician");
        dialog.setHeaderText("Update details for technician: " + technicianToEdit.getFirstName() + " " + technicianToEdit.getLastName());
        dialog.initOwner(stage);
        setupTechnicianDialogPane(dialog, technicianToEdit);
        Optional<Technician> result = dialog.showAndWait();
        result.ifPresent(editedTechnician -> {
            try {
                technicianDao.update(editedTechnician);
                refreshTechniciansTable();
                showAlert(Alert.AlertType.INFORMATION, "Technician Updated", "Technician updated successfully.");
            } catch (Exception e) {
                System.err.println("[ServiceController] Error updating technician: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Update Error", "Could not update the technician.");
            }
        });
    }

    // Fields for Add/Edit Technician Dialog (declare if not already class members)
    private TextField techFirstNameField, techLastNameField, techEmployeeIdField, techSpecializationField;
    private CheckBox techIsActiveCheckBox;

    private void setupTechnicianDialogPane(Dialog<Technician> dialog, Technician existingTechnician) {
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        techFirstNameField = new TextField();
        techFirstNameField.setPromptText("First Name");
        techLastNameField = new TextField();
        techLastNameField.setPromptText("Last Name");
        techEmployeeIdField = new TextField();
        techEmployeeIdField.setPromptText("Employee ID");
        techSpecializationField = new TextField();
        techSpecializationField.setPromptText("Specialization");
        techIsActiveCheckBox = new CheckBox("Is Active");
        techIsActiveCheckBox.setSelected(true); // Default to active

        if (existingTechnician != null) {
            techFirstNameField.setText(existingTechnician.getFirstName());
            techLastNameField.setText(existingTechnician.getLastName());
            techEmployeeIdField.setText(existingTechnician.getEmployeeId());
            techSpecializationField.setText(existingTechnician.getSpecialization());
            techIsActiveCheckBox.setSelected(existingTechnician.isActive());
        }

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(techFirstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(techLastNameField, 1, 1);
        grid.add(new Label("Employee ID:"), 0, 2);
        grid.add(techEmployeeIdField, 1, 2);
        grid.add(new Label("Specialization:"), 0, 3);
        grid.add(techSpecializationField, 1, 3);
        grid.add(techIsActiveCheckBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> techFirstNameField.requestFocus());

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (techFirstNameField.getText().trim().isEmpty() || techLastNameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "First and Last name cannot be empty.");
                event.consume();
            }
            // Add more validation as needed (e.g., employee ID format/uniqueness if required)
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Technician technician = (existingTechnician != null) ? existingTechnician : new Technician();
                technician.setFirstName(techFirstNameField.getText().trim());
                technician.setLastName(techLastNameField.getText().trim());
                technician.setEmployeeId(techEmployeeIdField.getText().trim());
                technician.setSpecialization(techSpecializationField.getText().trim());
                technician.setActive(techIsActiveCheckBox.isSelected());
                return technician;
            }
            return null;
        });
    }

    private void handleDeleteTechnician(Technician technician) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Technician");
        confirmAlert.setHeaderText("Confirm Deletion");
        confirmAlert.setContentText("Are you sure you want to delete technician: " + technician.getFirstName() + " " + technician.getLastName() + "?");
        confirmAlert.initOwner(stage);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                technicianDao.delete(technician);
                refreshTechniciansTable();
                showAlert(Alert.AlertType.INFORMATION, "Technician Deleted", "Technician deleted successfully.");
            } catch (Exception e) {
                System.err.println("[ServiceController] Error deleting technician: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Delete Error", "Could not delete technician. They might be assigned to appointments or services.");
            }
        }
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