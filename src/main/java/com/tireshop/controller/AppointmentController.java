package com.tireshop.controller;

import com.tireshop.dao.AppointmentDao;
import com.tireshop.dao.CustomerDao;
import com.tireshop.dao.TechnicianDao;
import com.tireshop.dao.VehicleDao;
import com.tireshop.model.*;
import com.tireshop.service.EmailService;
import com.tireshop.util.SettingsService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for appointment operations
 */
public class AppointmentController {
    
    private final AppointmentDao appointmentDao;
    private final CustomerDao customerDao;
    private final VehicleDao vehicleDao;
    private final TechnicianDao technicianDao;
    private final EmailService emailService;
    
    private TableView<Appointment> appointmentsTable;
    private ObservableList<Appointment> appointmentsList;

    // List view date filter - remembered so auto-refresh doesn't wipe an active search
    private DatePicker listFromDatePicker;
    private DatePicker listToDatePicker;
    private boolean listDateFilterActive = false;

    // Calendar view references - kept so refreshAllCalendarViews() can actually
    // rebuild the day/week/month grids after any appointment change
    private DatePicker dayViewDatePicker;
    private ComboBox<Technician> dayViewTechnicianFilter;
    private ComboBox<String> dayViewStatusFilter;
    private VBox dayViewContainer;
    private DatePicker weekViewPicker;
    private GridPane weekViewGrid;
    private DatePicker monthViewPicker;
    private GridPane monthViewGrid;
    
    public AppointmentController(AppointmentDao appointmentDao, 
                              CustomerDao customerDao,
                              VehicleDao vehicleDao,
                              TechnicianDao technicianDao) {
        System.out.println("[CONSTRUCTOR] AppointmentController instance created.");
        this.appointmentDao = appointmentDao;
        this.customerDao = customerDao;
        this.vehicleDao = vehicleDao;
        this.technicianDao = technicianDao;
        
        // Initialize EmailService
        SettingsService settingsService = SettingsService.getInstance();
        this.emailService = new EmailService(settingsService);
    }
    
    /**
     * Initialize the appointments view
     * @param appointmentsPane The main appointments pane
     * @param stage The application stage
     */
    public void initialize(BorderPane appointmentsPane, Stage stage) {
        System.out.println("[INITIALIZE] AppointmentController.initialize called.");
        
        // Modern background styling
        appointmentsPane.setStyle("-fx-background-color: #f8f9fa;");
        appointmentsPane.setPadding(new Insets(15));
        
        // Enhanced header section
        VBox headerSection = new VBox(15);
        headerSection.setPadding(new Insets(20));
        headerSection.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Header title
        Label headerTitle = new Label("📅 Appointment Management");
        headerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        headerSection.getChildren().add(headerTitle);
        appointmentsPane.setTop(headerSection);
        
        // Create TabPane for appointment views with modern styling
        TabPane appointmentsTabPane = new TabPane();
        appointmentsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        appointmentsTabPane.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Create tabs
        Tab dayView = createDayViewTab(stage);
        dayView.setText("📅 Calendar View");
        Tab weekView = createWeekViewTab(stage);
        weekView.setText("📊 Week View");
        Tab monthView = createMonthViewTab(stage);
        monthView.setText("🗓️ Month View");
        Tab listView = createListViewTab(stage);
        listView.setText("📋 List View");
        
        // Add tabs to the TabPane
        appointmentsTabPane.getTabs().addAll(dayView, weekView, monthView, listView);
        
        // Wrapper for main content with spacing
        VBox mainContentWrapper = new VBox(15);
        mainContentWrapper.setPadding(new Insets(15, 0, 0, 0));
        mainContentWrapper.getChildren().add(appointmentsTabPane);
        appointmentsPane.setCenter(mainContentWrapper);
    }
    
    /**
     * Create the day view tab with enhanced calendar interface
     * @param stage The application stage
     * @return Tab containing day view
     */
    private Tab createDayViewTab(Stage stage) {
        Tab tab = new Tab("Calendar View");
        
        BorderPane content = new BorderPane();
        content.setStyle("-fx-background-color: white;");
        
        // Enhanced top controls with modern styling
        VBox topPanel = new VBox(15);
        topPanel.setPadding(new Insets(20));
        
        // Header row with title and primary actions
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("📅 Today's Schedule");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button newAppointmentBtn = new Button("➕ New Appointment");
        newAppointmentBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        headerRow.getChildren().addAll(titleLabel, spacer, newAppointmentBtn, refreshBtn);
        
        // Navigation controls row with modern styling
        HBox navRow = new HBox(15);
        navRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        navRow.setPadding(new Insets(10, 0, 0, 0));
        
        Button prevBtn = new Button("◀ Previous");
        prevBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button nextBtn = new Button("Next ▶");
        nextBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        Button todayBtn = new Button("📅 Today");
        todayBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(160);
        datePicker.setEditable(false); // Prevent clearing to null which would NPE the refresh handlers
        datePicker.setStyle("-fx-padding: 6 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px;");
        
        // View type toggle with modern styling
        ToggleGroup viewGroup = new ToggleGroup();
        ToggleButton dayViewBtn = new ToggleButton("📅 Day");
        ToggleButton weekViewBtn = new ToggleButton("📊 Week");
        ToggleButton monthViewBtn = new ToggleButton("🗓️ Month");
        dayViewBtn.setToggleGroup(viewGroup);
        weekViewBtn.setToggleGroup(viewGroup);
        monthViewBtn.setToggleGroup(viewGroup);
        dayViewBtn.setSelected(true);
        String toggleStyle = "-fx-padding: 6 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px; -fx-cursor: hand;";
        dayViewBtn.setStyle(toggleStyle + " -fx-background-color: #e9ecef;");
        weekViewBtn.setStyle(toggleStyle + " -fx-background-color: #e9ecef;");
        monthViewBtn.setStyle(toggleStyle + " -fx-background-color: #e9ecef;");
        
        // Technician filter with modern styling
        ComboBox<Technician> technicianFilter = new ComboBox<>();
        technicianFilter.setPromptText("🔧 All Technicians");
        technicianFilter.setPrefWidth(160);
        technicianFilter.setStyle("-fx-padding: 6 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px;");
        List<Technician> technicians = technicianDao.findAll();
        technicianFilter.setItems(FXCollections.observableArrayList(technicians));
        technicianFilter.setConverter(new javafx.util.StringConverter<Technician>() {
            @Override
            public String toString(Technician technician) {
                return technician != null ? technician.getFirstName() + " " + technician.getLastName() : "";
            }
            @Override
            public Technician fromString(String string) { return null; }
        });
        
        // Status filter with modern styling
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setPromptText("📋 All Status");
        statusFilter.setPrefWidth(140);
        statusFilter.setStyle("-fx-padding: 6 12; -fx-background-radius: 6px; -fx-border-color: #ced4da; -fx-border-radius: 6px;");
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Status", "Scheduled", "Confirmed", "In Progress", "Completed", "Cancelled"
        ));
        
        navRow.getChildren().addAll(
            prevBtn, todayBtn, nextBtn, datePicker,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            dayViewBtn, weekViewBtn, monthViewBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            technicianFilter, statusFilter
        );
        
        topPanel.getChildren().addAll(headerRow, navRow);
        content.setTop(topPanel);
        
        // Calendar stats bar
        HBox statsBar = createAppointmentStatsBar(LocalDate.now());
        
        // Main calendar view
        ScrollPane scrollView = new ScrollPane();
        scrollView.setFitToWidth(true);
        
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(10));
        mainContent.getChildren().addAll(statsBar, createEnhancedDayView(LocalDate.now(), null, null));
        
        scrollView.setContent(mainContent);
        content.setCenter(scrollView);

        // Keep references so refreshAllCalendarViews() can rebuild this view later
        this.dayViewDatePicker = datePicker;
        this.dayViewTechnicianFilter = technicianFilter;
        this.dayViewStatusFilter = statusFilter;
        this.dayViewContainer = mainContent;

        // Event handlers
        newAppointmentBtn.setOnAction(e -> {
            Optional<Appointment> newAppointment = showNewAppointmentDialog(stage);
            if (newAppointment.isPresent()) {
                if (appointmentsTable != null) {
                    refreshAppointments();
                }
                refreshAllCalendarViews();
            }
        });

        refreshBtn.setOnAction(e -> {
            LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            refreshCalendarView(date, technicianFilter.getValue(),
                statusFilter.getValue(), mainContent);
        });

        // Navigation handlers
        prevBtn.setOnAction(e -> {
            LocalDate currentDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            datePicker.setValue(currentDate.minusDays(1));
        });

        nextBtn.setOnAction(e -> {
            LocalDate currentDate = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
            datePicker.setValue(currentDate.plusDays(1));
        });
        
        todayBtn.setOnAction(e -> datePicker.setValue(LocalDate.now()));
        
        // Date picker change handler
        datePicker.setOnAction(e -> {
            refreshCalendarView(datePicker.getValue(), technicianFilter.getValue(), 
                statusFilter.getValue(), mainContent);
        });
        
        // Filter change handlers
        technicianFilter.setOnAction(e -> {
            refreshCalendarView(datePicker.getValue(), technicianFilter.getValue(), 
                statusFilter.getValue(), mainContent);
        });
        
        statusFilter.setOnAction(e -> {
            refreshCalendarView(datePicker.getValue(), technicianFilter.getValue(), 
                statusFilter.getValue(), mainContent);
        });
        
        tab.setContent(content);
        return tab;
    }
    
    private HBox createAppointmentStatsBar(LocalDate date) {
        HBox statsBar = new HBox(20);
        statsBar.setPadding(new Insets(15));
        statsBar.setAlignment(javafx.geometry.Pos.CENTER);
        statsBar.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 1);");
        
        List<Appointment> appointments = appointmentDao.findByDate(date);
        
        int scheduled = 0, confirmed = 0, completed = 0, cancelled = 0;
        for (Appointment apt : appointments) {
            switch (apt.getStatus()) {
                case SCHEDULED: scheduled++; break;
                case CONFIRMED: confirmed++; break;
                case COMPLETED: completed++; break;
                case CANCELLED: cancelled++; break;
            }
        }
        
        VBox totalBox = createStatBox("📊 Total", String.valueOf(appointments.size()), "#2196F3");
        VBox scheduledBox = createStatBox("⏰ Scheduled", String.valueOf(scheduled), "#FF9800");
        VBox confirmedBox = createStatBox("✅ Confirmed", String.valueOf(confirmed), "#4CAF50");
        VBox completedBox = createStatBox("✔️ Completed", String.valueOf(completed), "#9E9E9E");
        VBox cancelledBox = createStatBox("❌ Cancelled", String.valueOf(cancelled), "#F44336");
        
        statsBar.getChildren().addAll(totalBox, scheduledBox, confirmedBox, completedBox, cancelledBox);
        return statsBar;
    }
    
    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPrefWidth(140);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: " + color + "; -fx-border-width: 2px; -fx-border-radius: 8px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #495057; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(130);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        box.getChildren().addAll(valueLabel, nameLabel);
        return box;
    }
    
    private VBox createEnhancedDayView(LocalDate date, Technician technicianFilter, String statusFilter) {
        VBox dayView = new VBox(15);
        dayView.setPadding(new Insets(20));
        dayView.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");
        
        // Header with date
        Label dateHeader = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        dayView.getChildren().add(dateHeader);
        
        // Get appointments with filters
        List<Appointment> appointments = appointmentDao.findByDate(date);
        
        // Apply filters (compare technicians by ID - each DAO call returns different
        // Java instances for the same row, so object identity always fails)
        if (technicianFilter != null) {
            appointments = appointments.stream()
                .filter(a -> a.getTechnician() != null && a.getTechnician().getId() != null
                        && a.getTechnician().getId().equals(technicianFilter.getId()))
                .collect(Collectors.toList());
        }
        
        if (statusFilter != null && !"All Status".equals(statusFilter)) {
            appointments = appointments.stream()
                .filter(a -> a.getStatus().toString().equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());
        }
        
        // Create time grid
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(10);
        timeGrid.setVgap(5);
        timeGrid.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        
        // Time column constraints
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(80);
        timeCol.setMinWidth(80);
        
        ColumnConstraints appointmentCol = new ColumnConstraints();
        appointmentCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        
        timeGrid.getColumnConstraints().addAll(timeCol, appointmentCol);
        
        int row = 0;
        // Create time slots (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue;
                
                String timeStr = String.format("%02d:%02d %s", 
                    hour > 12 ? hour - 12 : hour == 0 ? 12 : hour,
                    minute, 
                    hour >= 12 ? "PM" : "AM");
                
                Label timeLabel = new Label(timeStr);
                timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057; -fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5px;");
                timeLabel.setAlignment(javafx.geometry.Pos.CENTER);
                timeLabel.setMinWidth(80);
                
                LocalTime slotTime = LocalTime.of(hour, minute);
                List<Appointment> appointmentsAtTime = findAppointmentsAtTime(appointments, slotTime);
                
                HBox slotContent = new HBox(15);
                slotContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                slotContent.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                slotContent.setPadding(new Insets(5));
                slotContent.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0;");
                
                if (!appointmentsAtTime.isEmpty()) {
                    for (Appointment apt : appointmentsAtTime) {
                        VBox appointmentCard = createAppointmentCard(apt);
                        slotContent.getChildren().add(appointmentCard);
                    }
                } else {
                    Label emptyLabel = new Label("⭕ Available");
                    emptyLabel.setStyle("-fx-text-fill: #adb5bd; -fx-font-style: italic; -fx-font-size: 13px;");
                    slotContent.getChildren().add(emptyLabel);
                }
                
                timeGrid.add(timeLabel, 0, row);
                timeGrid.add(slotContent, 1, row);
                row++;
            }
        }
        
        ScrollPane gridScroll = new ScrollPane(timeGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setPrefViewportHeight(600);
        
        dayView.getChildren().add(gridScroll);
        return dayView;
    }
    
    private VBox createAppointmentCard(Appointment appointment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 6, 0, 0, 2); -fx-cursor: hand;");
        card.setMaxWidth(350);
        
        // Apply status-specific styling with modern colors
        switch (appointment.getStatus()) {
            case CONFIRMED:
                card.setStyle(card.getStyle() + " -fx-border-color: #4CAF50; -fx-border-width: 0 0 0 4; -fx-border-radius: 8px;");
                break;
            case COMPLETED:
                card.setStyle(card.getStyle() + " -fx-border-color: #9E9E9E; -fx-border-width: 0 0 0 4; -fx-border-radius: 8px;");
                break;
            case CANCELLED:
                card.setStyle(card.getStyle() + " -fx-border-color: #F44336; -fx-border-width: 0 0 0 4; -fx-border-radius: 8px;");
                break;
            default:
                card.setStyle(card.getStyle() + " -fx-border-color: #FF9800; -fx-border-width: 0 0 0 4; -fx-border-radius: 8px;");
        }
        
        // Title with duration
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(appointment.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        
        long duration = java.time.Duration.between(
            appointment.getStartTime(), appointment.getEndTime()).toMinutes();
        Label durationLabel = new Label("(" + duration + " min)");
        durationLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px; -fx-font-style: italic;");
        
        titleRow.getChildren().addAll(titleLabel, durationLabel);
        
        // Customer info
        if (appointment.getCustomer() != null) {
            Label customerLabel = new Label("👤 " + 
                appointment.getCustomer().getFirstName() + " " + 
                appointment.getCustomer().getLastName());
            customerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-padding: 2 0;");
            card.getChildren().add(customerLabel);
        }
        
        // Vehicle info
        if (appointment.getVehicle() != null) {
            Vehicle v = appointment.getVehicle();
            Label vehicleLabel = new Label("🚗 " + v.getModelYear() + " " + 
                v.getMake() + " " + v.getModel());
            vehicleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-padding: 2 0;");
            card.getChildren().add(vehicleLabel);
        }
        
        // Technician info
        if (appointment.getTechnician() != null) {
            Label techLabel = new Label("🔧 " + 
                appointment.getTechnician().getFirstName() + " " + 
                appointment.getTechnician().getLastName());
            techLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-padding: 2 0;");
            card.getChildren().add(techLabel);
        }
        
        // Status badge with modern styling
        Label statusLabel = new Label(appointment.getStatus().toString());
        String statusColor = getStatusColor(appointment.getStatus());
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: " + statusColor + "; -fx-background-radius: 12px; -fx-padding: 4 8;");
        
        card.getChildren().addAll(titleRow, statusLabel);
        
        // Click handler - SINGLE click to open (easier for users)
        card.setOnMouseClicked(e -> {
            showAppointmentDetailsDialog(appointment);
            e.consume();
        });
        
        return card;
    }
    
    private String getStatusColor(AppointmentStatus status) {
        switch (status) {
            case CONFIRMED: return "#4CAF50";
            case COMPLETED: return "#9E9E9E";
            case CANCELLED: return "#F44336";
            case IN_PROGRESS: return "#2196F3";
            default: return "#FF9800";
        }
    }
    
    private void refreshCalendarView(LocalDate date, Technician technicianFilter, 
                                   String statusFilter, VBox container) {
        container.getChildren().clear();
        HBox statsBar = createAppointmentStatsBar(date);
        VBox dayView = createEnhancedDayView(date, technicianFilter, statusFilter);
        container.getChildren().addAll(statsBar, dayView);
    }
    
    private void showAppointmentDetailsDialog(Appointment appointment) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Appointment Details");
        dialog.setHeaderText(appointment.getTitle());
        
        // Add buttons
        ButtonType editButtonType = new ButtonType("Edit", ButtonBar.ButtonData.LEFT);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(editButtonType, deleteButtonType, ButtonType.CLOSE);
        
        // Style the delete button
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.setStyle("-fx-text-fill: red;");
        
        StringBuilder details = new StringBuilder();
        details.append("Date: ").append(appointment.getStartTime().format(
            DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("\n");
        details.append("Time: ").append(appointment.getStartTime().format(
            DateTimeFormatter.ofPattern("hh:mm a"))).append(" - ");
        details.append(appointment.getEndTime().format(
            DateTimeFormatter.ofPattern("hh:mm a"))).append("\n");
        details.append("Status: ").append(appointment.getStatus()).append("\n");
        
        if (appointment.getCustomer() != null) {
            Customer c = appointment.getCustomer();
            details.append("Customer: ").append(c.getFirstName()).append(" ")
                  .append(c.getLastName()).append("\n");
        }
        
        if (appointment.getVehicle() != null) {
            Vehicle v = appointment.getVehicle();
            details.append("Vehicle: ").append(v.getModelYear()).append(" ")
                  .append(v.getMake()).append(" ").append(v.getModel()).append("\n");
        }
        
        if (appointment.getTechnician() != null) {
            Technician t = appointment.getTechnician();
            details.append("Technician: ").append(t.getFirstName()).append(" ")
                  .append(t.getLastName()).append("\n");
        }
        
        if (appointment.getDescription() != null && !appointment.getDescription().isEmpty()) {
            details.append("\nDescription:\n").append(appointment.getDescription());
        }
        
        dialog.setContentText(details.toString());
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == editButtonType) {
                // Use null as owner to make edit dialog independent
                Optional<Appointment> editedAppointment = showEditAppointmentDialog(null, appointment);
                if (editedAppointment.isPresent()) {
                    // Refresh the views
                    refreshAppointments();
                    refreshAllCalendarViews();
                }
            } else if (result.get() == deleteButtonType) {
                handleDeleteAppointment(appointment);
            }
        }
    }
    
    /**
     * Create the week view tab
     * @param stage The application stage
     * @return Tab containing week view
     */
    private Tab createWeekViewTab(Stage stage) {
        Tab tab = new Tab("Week View");
        
        BorderPane content = new BorderPane();
        
        // Top controls panel
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
        topPanel.getStyleClass().add("search-panel");
        
        // Header row
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("Week View");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button newAppointmentBtn = new Button("New Appointment");
        newAppointmentBtn.getStyleClass().add("primary");
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");
        
        headerRow.getChildren().addAll(titleLabel, spacer, newAppointmentBtn, refreshBtn);
        
        // Navigation controls
        HBox navRow = new HBox(15);
        navRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button prevWeekBtn = new Button("◀ Previous Week");
        Button nextWeekBtn = new Button("Next Week ▶");
        Button currentWeekBtn = new Button("Current Week");
        currentWeekBtn.getStyleClass().add("secondary");
        
        DatePicker weekPicker = new DatePicker(LocalDate.now());
        weekPicker.setPrefWidth(150);
        weekPicker.setEditable(false); // Prevent clearing to null which would NPE the handlers
        
        Label weekRangeLabel = new Label();
        updateWeekRangeLabel(weekRangeLabel, weekPicker.getValue());
        
        navRow.getChildren().addAll(prevWeekBtn, currentWeekBtn, nextWeekBtn, 
            new Separator(javafx.geometry.Orientation.VERTICAL), weekPicker, weekRangeLabel);
        
        topPanel.getChildren().addAll(headerRow, navRow);
        content.setTop(topPanel);
        
        // Main week grid
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        GridPane weekGrid = createWeekGrid(weekPicker.getValue());
        scrollPane.setContent(weekGrid);
        content.setCenter(scrollPane);

        // Keep references so refreshAllCalendarViews() can rebuild this view later
        this.weekViewPicker = weekPicker;
        this.weekViewGrid = weekGrid;

        // Event handlers
        newAppointmentBtn.setOnAction(e -> {
            Optional<Appointment> newAppointment = showNewAppointmentDialog(stage);
            if (newAppointment.isPresent()) {
                if (appointmentsTable != null) {
                    refreshAppointments();
                }
                refreshAllCalendarViews();
            }
        });
        
        refreshBtn.setOnAction(e -> refreshWeekView(weekPicker.getValue(), weekGrid));
        
        prevWeekBtn.setOnAction(e -> {
            LocalDate newDate = weekPicker.getValue().minusWeeks(1);
            weekPicker.setValue(newDate);
        });
        
        nextWeekBtn.setOnAction(e -> {
            LocalDate newDate = weekPicker.getValue().plusWeeks(1);
            weekPicker.setValue(newDate);
        });
        
        currentWeekBtn.setOnAction(e -> weekPicker.setValue(LocalDate.now()));
        
        weekPicker.setOnAction(e -> {
            updateWeekRangeLabel(weekRangeLabel, weekPicker.getValue());
            refreshWeekView(weekPicker.getValue(), weekGrid);
        });
        
        tab.setContent(content);
        return tab;
    }
    
    private void updateWeekRangeLabel(Label label, LocalDate date) {
        LocalDate startOfWeek = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        label.setText(startOfWeek.format(formatter) + " - " + endOfWeek.format(formatter) + ", " + date.getYear());
    }
    
    private GridPane createWeekGrid(LocalDate date) {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color: #e0e0e0;");
        grid.setPadding(new Insets(10));
        
        LocalDate startOfWeek = date.with(java.time.DayOfWeek.MONDAY);
        
        // Create day headers
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = startOfWeek.plusDays(i);
            VBox dayHeader = new VBox(5);
            dayHeader.setAlignment(javafx.geometry.Pos.CENTER);
            dayHeader.setPadding(new Insets(10));
            dayHeader.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            
            Label dayNameLabel = new Label(dayNames[i]);
            dayNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            
            Label dateLabel = new Label(dayDate.format(DateTimeFormatter.ofPattern("MMM dd")));
            dateLabel.setStyle("-fx-text-fill: white;");
            
            dayHeader.getChildren().addAll(dayNameLabel, dateLabel);
            grid.add(dayHeader, i, 0);
        }
        
        // Create time slots for each day
        for (int hour = 8; hour <= 18; hour++) {
            for (int col = 0; col < 7; col++) {
                LocalDate dayDate = startOfWeek.plusDays(col);
                VBox timeSlot = createWeekTimeSlot(dayDate, LocalTime.of(hour, 0));
                grid.add(timeSlot, col, hour - 7);
            }
        }
        
        // Set column constraints for equal width
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7);
            col.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }
        
        return grid;
    }
    
    private VBox createWeekTimeSlot(LocalDate date, LocalTime time) {
        VBox slot = new VBox(2);
        slot.setMinHeight(80);
        slot.setPadding(new Insets(5));
        slot.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        
        // Time label for first column only
        if (date.getDayOfWeek() == java.time.DayOfWeek.MONDAY) {
            Label timeLabel = new Label(time.format(DateTimeFormatter.ofPattern("h:mm a")));
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
            slot.getChildren().add(timeLabel);
        }
        
        // Get appointments for this time slot
        List<Appointment> appointments = appointmentDao.findByDate(date);
        List<Appointment> slotAppointments = appointments.stream()
            .filter(a -> {
                LocalTime aptTime = a.getStartTime().toLocalTime();
                return aptTime.getHour() == time.getHour();
            })
            .collect(Collectors.toList());
        
        // Display appointments
        for (Appointment apt : slotAppointments) {
            VBox aptCard = createCompactAppointmentCard(apt);
            slot.getChildren().add(aptCard);
        }
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            slot.setStyle(slot.getStyle() + "; -fx-background-color: #f5f5f5;");
        }
        
        // Click handler to add appointment
        slot.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                // Double-click to add appointment at this time
                showNewAppointmentDialogForDateTime(date, time);
            }
        });
        
        return slot;
    }
    
    private VBox createCompactAppointmentCard(Appointment appointment) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(3));
        card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 3px; " +
                     "-fx-border-color: #2196F3; -fx-border-radius: 3px;");
        
        // Apply status-specific colors
        switch (appointment.getStatus()) {
            case CONFIRMED:
                card.setStyle(card.getStyle().replace("#e3f2fd", "#c8e6c9").replace("#2196F3", "#4CAF50"));
                break;
            case COMPLETED:
                card.setStyle(card.getStyle().replace("#e3f2fd", "#e0e0e0").replace("#2196F3", "#9e9e9e"));
                break;
            case CANCELLED:
                card.setStyle(card.getStyle().replace("#e3f2fd", "#ffcdd2").replace("#2196F3", "#f44336"));
                break;
        }
        
        Label timeLabel = new Label(appointment.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        timeLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;");
        
        Label titleLabel = new Label(appointment.getTitle());
        titleLabel.setStyle("-fx-font-size: 10px;");
        titleLabel.setWrapText(true);
        
        if (appointment.getCustomer() != null) {
            Label customerLabel = new Label(appointment.getCustomer().getLastName());
            customerLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666;");
            card.getChildren().addAll(timeLabel, titleLabel, customerLabel);
        } else {
            card.getChildren().addAll(timeLabel, titleLabel);
        }
        
        // Click handler
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showAppointmentDetailsDialog(appointment);
            }
            e.consume();
        });
        
        return card;
    }
    
    private void refreshWeekView(LocalDate date, GridPane weekGrid) {
        weekGrid.getChildren().clear();
        GridPane newGrid = createWeekGrid(date);
        weekGrid.getChildren().addAll(newGrid.getChildren());
        weekGrid.getColumnConstraints().clear();
        weekGrid.getColumnConstraints().addAll(newGrid.getColumnConstraints());
    }
    
    private void showNewAppointmentDialogForDateTime(LocalDate date, LocalTime time) {
        // Pre-fill the dialog with the date/time slot the user clicked on
        Optional<Appointment> newAppointment = showNewAppointmentDialog(null, date, time);
        if (newAppointment.isPresent()) {
            if (appointmentsTable != null) {
                refreshAppointments();
            }
            refreshAllCalendarViews();
        }
    }
    
    /**
     * Create the month view tab
     * @param stage The application stage
     * @return Tab containing month view
     */
    private Tab createMonthViewTab(Stage stage) {
        Tab tab = new Tab("Month View");
        
        BorderPane content = new BorderPane();
        
        // Top controls panel
        VBox topPanel = new VBox(10);
        topPanel.setPadding(new Insets(15));
        topPanel.getStyleClass().add("search-panel");
        
        // Header row
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("Month View");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button newAppointmentBtn = new Button("New Appointment");
        newAppointmentBtn.getStyleClass().add("primary");
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");
        
        headerRow.getChildren().addAll(titleLabel, spacer, newAppointmentBtn, refreshBtn);
        
        // Navigation controls
        HBox navRow = new HBox(15);
        navRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button prevMonthBtn = new Button("◀ Previous Month");
        Button nextMonthBtn = new Button("Next Month ▶");
        Button currentMonthBtn = new Button("Current Month");
        currentMonthBtn.getStyleClass().add("secondary");
        
        DatePicker monthPicker = new DatePicker(LocalDate.now());
        monthPicker.setPrefWidth(150);
        monthPicker.setEditable(false); // Prevent clearing to null which would NPE the handlers
        
        Label monthYearLabel = new Label();
        updateMonthYearLabel(monthYearLabel, monthPicker.getValue());
        
        navRow.getChildren().addAll(prevMonthBtn, currentMonthBtn, nextMonthBtn, 
            new Separator(javafx.geometry.Orientation.VERTICAL), monthPicker, monthYearLabel);
        
        topPanel.getChildren().addAll(headerRow, navRow);
        content.setTop(topPanel);
        
        // Main month grid
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        GridPane monthGrid = createMonthGrid(monthPicker.getValue());
        scrollPane.setContent(monthGrid);
        content.setCenter(scrollPane);

        // Keep references so refreshAllCalendarViews() can rebuild this view later
        this.monthViewPicker = monthPicker;
        this.monthViewGrid = monthGrid;

        // Event handlers
        newAppointmentBtn.setOnAction(e -> {
            Optional<Appointment> newAppointment = showNewAppointmentDialog(stage);
            if (newAppointment.isPresent()) {
                if (appointmentsTable != null) {
                    refreshAppointments();
                }
                refreshAllCalendarViews();
            }
        });
        
        refreshBtn.setOnAction(e -> refreshMonthView(monthPicker.getValue(), monthGrid));
        
        prevMonthBtn.setOnAction(e -> {
            LocalDate newDate = monthPicker.getValue().minusMonths(1);
            monthPicker.setValue(newDate);
        });
        
        nextMonthBtn.setOnAction(e -> {
            LocalDate newDate = monthPicker.getValue().plusMonths(1);
            monthPicker.setValue(newDate);
        });
        
        currentMonthBtn.setOnAction(e -> monthPicker.setValue(LocalDate.now()));
        
        monthPicker.setOnAction(e -> {
            updateMonthYearLabel(monthYearLabel, monthPicker.getValue());
            refreshMonthView(monthPicker.getValue(), monthGrid);
        });
        
        tab.setContent(content);
        return tab;
    }
    
    private void updateMonthYearLabel(Label label, LocalDate date) {
        label.setText(date.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    }
    
    private GridPane createMonthGrid(LocalDate date) {
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color: #e0e0e0;");
        grid.setPadding(new Insets(10));
        
        // Day of week headers
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setAlignment(javafx.geometry.Pos.CENTER);
            dayLabel.setPrefWidth(100);
            dayLabel.setPadding(new Insets(10));
            dayLabel.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
            grid.add(dayLabel, i, 0);
        }
        
        // Get first day of month and determine starting position
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        
        // Get number of days in month
        int daysInMonth = date.lengthOfMonth();
        
        // Create calendar days
        int row = 1;
        int col = firstDayOfWeek;
        
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate dayDate = LocalDate.of(date.getYear(), date.getMonth(), day);
            VBox dayCell = createMonthDayCell(dayDate);
            grid.add(dayCell, col, row);
            
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
        
        // Fill in empty cells at start of month
        for (int i = 0; i < firstDayOfWeek; i++) {
            VBox emptyCell = createEmptyMonthCell();
            grid.add(emptyCell, i, 1);
        }
        
        // Fill in empty cells at end of month
        while (col <= 6 && col > 0) {
            VBox emptyCell = createEmptyMonthCell();
            grid.add(emptyCell, col, row);
            col++;
        }
        
        // Set column constraints for equal width
        for (int i = 0; i < 7; i++) {
            ColumnConstraints colConstraint = new ColumnConstraints();
            colConstraint.setPercentWidth(100.0 / 7);
            colConstraint.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            grid.getColumnConstraints().add(colConstraint);
        }
        
        return grid;
    }
    
    private VBox createMonthDayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setMinHeight(100);
        cell.setPadding(new Insets(5));
        cell.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
        
        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold;");
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle(dayLabel.getStyle() + "; -fx-text-fill: white; -fx-background-color: #2196F3; " +
                            "-fx-background-radius: 15px; -fx-padding: 3px 8px;");
        }
        
        cell.getChildren().add(dayLabel);
        
        // Get appointments for this day
        List<Appointment> appointments = appointmentDao.findByDate(date);
        
        // Show appointment count and first few appointments
        if (!appointments.isEmpty()) {
            Label countLabel = new Label(appointments.size() + " appointment" + (appointments.size() > 1 ? "s" : ""));
            countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2196F3; -fx-font-weight: bold;");
            cell.getChildren().add(countLabel);
            
            // Show up to 3 appointments
            int shown = 0;
            for (Appointment apt : appointments) {
                if (shown >= 3) {
                    Label moreLabel = new Label("+" + (appointments.size() - 3) + " more");
                    moreLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #666; -fx-font-style: italic;");
                    cell.getChildren().add(moreLabel);
                    break;
                }
                
                HBox aptBox = new HBox(3);
                aptBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                // Status indicator
                Circle statusDot = new Circle(3);
                switch (apt.getStatus()) {
                    case SCHEDULED:
                        statusDot.setFill(javafx.scene.paint.Color.ORANGE);
                        break;
                    case CONFIRMED:
                        statusDot.setFill(javafx.scene.paint.Color.GREEN);
                        break;
                    case COMPLETED:
                        statusDot.setFill(javafx.scene.paint.Color.GRAY);
                        break;
                    case CANCELLED:
                        statusDot.setFill(javafx.scene.paint.Color.RED);
                        break;
                }
                
                Label timeLabel = new Label(apt.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")));
                timeLabel.setStyle("-fx-font-size: 9px;");
                
                Label titleLabel = new Label(apt.getTitle());
                titleLabel.setStyle("-fx-font-size: 9px;");
                titleLabel.setMaxWidth(80);
                titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                
                aptBox.getChildren().addAll(statusDot, timeLabel, titleLabel);
                cell.getChildren().add(aptBox);
                shown++;
            }
        }
        
        // Click handler
        cell.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                // Double-click to view day details or add appointment
                showDayDetailsDialog(date, appointments);
            }
        });
        
        // Hover effect
        cell.setOnMouseEntered(e -> {
            if (!date.equals(LocalDate.now())) {
                cell.setStyle(cell.getStyle() + "; -fx-background-color: #f5f5f5;");
            }
        });
        
        cell.setOnMouseExited(e -> {
            if (!date.equals(LocalDate.now())) {
                cell.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0;");
            }
        });
        
        return cell;
    }
    
    private VBox createEmptyMonthCell() {
        VBox cell = new VBox();
        cell.setMinHeight(100);
        cell.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0;");
        return cell;
    }
    
    private void refreshMonthView(LocalDate date, GridPane monthGrid) {
        monthGrid.getChildren().clear();
        GridPane newGrid = createMonthGrid(date);
        monthGrid.getChildren().addAll(newGrid.getChildren());
        monthGrid.getColumnConstraints().clear();
        monthGrid.getColumnConstraints().addAll(newGrid.getColumnConstraints());
    }
    
    private void showDayDetailsDialog(LocalDate date, List<Appointment> appointments) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Day Details");
        dialog.setHeaderText(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        
        if (appointments.isEmpty()) {
            Label noApptsLabel = new Label("No appointments scheduled for this day.");
            noApptsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
            content.getChildren().add(noApptsLabel);
        } else {
            Label countLabel = new Label(appointments.size() + " appointment" + 
                (appointments.size() > 1 ? "s" : "") + " scheduled");
            countLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            content.getChildren().add(countLabel);
            
            // Sort appointments by time
            appointments.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
            
            for (Appointment apt : appointments) {
                VBox aptBox = new VBox(5);
                aptBox.setPadding(new Insets(10));
                aptBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5px;");
                
                HBox timeRow = new HBox(10);
                Label timeLabel = new Label(apt.getStartTime().format(DateTimeFormatter.ofPattern("h:mm a")) + 
                    " - " + apt.getEndTime().format(DateTimeFormatter.ofPattern("h:mm a")));
                timeLabel.setStyle("-fx-font-weight: bold;");
                
                Label statusLabel = new Label(apt.getStatus().toString());
                statusLabel.setStyle("-fx-font-size: 11px; -fx-background-color: #e0e0e0; " +
                    "-fx-background-radius: 3px; -fx-padding: 2px 5px;");
                
                timeRow.getChildren().addAll(timeLabel, statusLabel);
                
                Label titleLabel = new Label(apt.getTitle());
                titleLabel.setStyle("-fx-font-size: 14px;");
                
                if (apt.getCustomer() != null) {
                    Label customerLabel = new Label("Customer: " + apt.getCustomer().getFullName());
                    customerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                    aptBox.getChildren().add(customerLabel);
                }
                
                if (apt.getTechnician() != null) {
                    Label techLabel = new Label("Technician: " + 
                        apt.getTechnician().getFirstName() + " " + apt.getTechnician().getLastName());
                    techLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                    aptBox.getChildren().add(techLabel);
                }
                
                aptBox.getChildren().addAll(timeRow, titleLabel);
                content.getChildren().add(aptBox);
            }
        }
        
        Button addAppointmentBtn = new Button("Add Appointment");
        addAppointmentBtn.setOnAction(e -> {
            dialog.close();
            showNewAppointmentDialogForDate(date);
        });
        content.getChildren().add(addAppointmentBtn);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }
    
    private void showNewAppointmentDialogForDate(LocalDate date) {
        // This would be similar to showNewAppointmentDialog but with pre-filled date
        // For now, just show the regular dialog
        showNewAppointmentDialog(null);
    }
    
    /**
     * Create the list view tab
     * @param stage The application stage
     * @return Tab containing list view
     */
    private Tab createListViewTab(Stage stage) {
        System.out.println("[TAB_SETUP] createListViewTab called.");
        Tab tab = new Tab("List View");
        
        BorderPane content = new BorderPane();
        
        // Top controls
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        
        DatePicker fromDatePicker = new DatePicker(LocalDate.now());
        DatePicker toDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        this.listFromDatePicker = fromDatePicker;
        this.listToDatePicker = toDatePicker;
        Button searchBtn = new Button("Search");
        Button showAllBtn = new Button("Show All");
        Button newAppointmentBtn = new Button("New Appointment");
        Button editAppointmentBtn = new Button("Edit");
        Button deleteAppointmentBtn = new Button("Delete");

        controls.getChildren().addAll(
                new Label("From:"), fromDatePicker,
                new Label("To:"), toDatePicker,
                searchBtn, showAllBtn, newAppointmentBtn, editAppointmentBtn, deleteAppointmentBtn);
                
        content.setTop(controls);
        
        // Appointments table
        appointmentsTable = createAppointmentsTable(stage);
        content.setCenter(appointmentsTable);
        
        // Load initial data
        refreshAppointments();
        
        // Button actions
        newAppointmentBtn.setOnAction(e -> {
            Optional<Appointment> newAppointmentOptional = showNewAppointmentDialog(stage);
            if (newAppointmentOptional.isPresent()) {
                refreshAppointments(); // Refresh the list view table
                refreshAllCalendarViews(); // And the day/week/month grids
            }
        });

        editAppointmentBtn.setOnAction(e -> {
            Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
            if (selectedAppointment != null) {
                Optional<Appointment> editedAppointment = showEditAppointmentDialog(stage, selectedAppointment);
                if (editedAppointment.isPresent()) {
                    refreshAppointments();
                    refreshAllCalendarViews();
                }
            } else {
                showAlert("No Selection", "Please select an appointment to edit.", Alert.AlertType.WARNING);
            }
        });

        deleteAppointmentBtn.setOnAction(e -> {
            Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
            if (selectedAppointment != null) {
                handleDeleteAppointment(selectedAppointment);
            } else {
                showAlert("No Selection", "Please select an appointment to delete.", Alert.AlertType.WARNING);
            }
        });
        
        searchBtn.setOnAction(e -> {
            LocalDate fromDate = fromDatePicker.getValue();
            LocalDate toDate = toDatePicker.getValue();

            if (fromDate != null && toDate != null) {
                listDateFilterActive = true;
                searchAppointments(fromDate, toDate);
            }
        });

        showAllBtn.setOnAction(e -> {
            listDateFilterActive = false;
            refreshAppointments();
        });
        
        tab.setContent(content);
        return tab;
    }
    
    /**
     * Create time slots for day view
     * @param date The date to display
     * @return VBox containing time slots
     */
    private VBox createDayTimeSlots(LocalDate date) {
        VBox timeSlotsBox = new VBox(5);
        timeSlotsBox.setPadding(new Insets(10));
        
        // Header with date
        Label dateHeader = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        timeSlotsBox.getChildren().add(dateHeader);
        
        // Get appointments for this date
        List<Appointment> appointments = appointmentDao.findByDate(date);
        
        // Debug print the number of appointments found
        System.out.println("Found " + appointments.size() + " appointments for date: " + date);
        for (Appointment app : appointments) {
            System.out.println("  - " + app.getTitle() + " at " + app.getStartTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
        }
        
        // Create time slots (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue; // Skip after 6:00 PM
                
                String timeStr = String.format("%02d:%02d %s", 
                        hour > 12 ? hour - 12 : hour, 
                        minute, 
                        hour >= 12 ? "PM" : "AM");
                
                LocalTime slotTime = LocalTime.of(hour, minute);
                
                // Find appointments at this time
                List<Appointment> appointmentsAtTime = findAppointmentsAtTime(appointments, slotTime);
                
                HBox timeSlot = new HBox(10);
                timeSlot.setPadding(new Insets(5));
                timeSlot.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 0 1 0;");
                
                // Create time label with fixed width
                Label timeLabel = new Label(timeStr);
                timeLabel.setPrefWidth(80);
                timeLabel.setStyle("-fx-font-weight: bold;");
                
                // If there are appointments, show them
                if (!appointmentsAtTime.isEmpty()) {
                    VBox appointmentsBox = new VBox(5);
                    appointmentsBox.setPrefWidth(500);
                    
                    for (Appointment appointment : appointmentsAtTime) {
                        // Container for this appointment
                        VBox appointmentBox = new VBox(2);
                        appointmentBox.setPadding(new Insets(5));
                        appointmentBox.setStyle("-fx-background-color: #d1e7f0; -fx-background-radius: 5px; -fx-border-color: #7fc7e8; -fx-border-radius: 5px;");
                        
                        // Title with status indicator
                        HBox titleBox = new HBox(5);
                        Circle statusCircle = new Circle(5);
                        if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
                            statusCircle.setFill(javafx.scene.paint.Color.ORANGE);
                        } else if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                            statusCircle.setFill(javafx.scene.paint.Color.GREEN);
                        } else if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                            statusCircle.setFill(javafx.scene.paint.Color.BLUE);
                        } else if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                            statusCircle.setFill(javafx.scene.paint.Color.RED);
                        }
                        
                        Label titleLabel = new Label(appointment.getTitle());
                        titleLabel.setStyle("-fx-font-weight: bold;");
                        titleBox.getChildren().addAll(statusCircle, titleLabel);
                        
                        // Customer info
                        Label customerLabel = new Label();
                        if (appointment.getCustomer() != null) {
                            Customer customer = appointment.getCustomer();
                            customerLabel.setText("Customer: " + customer.getFirstName() + " " + customer.getLastName());
                        } else {
                            customerLabel.setText("No customer assigned");
                        }
                        
                        // Technician info
                        Label technicianLabel = new Label();
                        if (appointment.getTechnician() != null) {
                            Technician technician = appointment.getTechnician();
                            technicianLabel.setText("Technician: " + technician.getFirstName() + " " + technician.getLastName());
                        } else {
                            technicianLabel.setText("No technician assigned");
                        }
                        
                        // Vehicle info
                        Label vehicleLabel = new Label();
                        if (appointment.getVehicle() != null) {
                            Vehicle vehicle = appointment.getVehicle();
                            vehicleLabel.setText("Vehicle: " + vehicle.getMake() + " " + vehicle.getModel());
                        }
                        
                        // Add all components to appointment box
                        appointmentBox.getChildren().addAll(titleBox, customerLabel, technicianLabel);
                        if (appointment.getVehicle() != null) {
                            appointmentBox.getChildren().add(vehicleLabel);
                        }
                        
                        appointmentsBox.getChildren().add(appointmentBox);
                    }
                    
                    timeSlot.getChildren().addAll(timeLabel, appointmentsBox);
                } else {
                    // No appointments
                    Label emptyLabel = new Label("No appointments");
                    emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    timeSlot.getChildren().addAll(timeLabel, emptyLabel);
                }
                
                timeSlotsBox.getChildren().add(timeSlot);
            }
        }
        
        return timeSlotsBox;
    }
    
    /**
     * Find appointments at a specific time
     * @param appointments List of appointments for the day
     * @param time The time to check
     * @return List of appointments at the specified time
     */
    private List<Appointment> findAppointmentsAtTime(List<Appointment> appointments, LocalTime time) {
        return appointments.stream()
                .filter(a -> {
                    LocalTime appointmentTime = a.getStartTime().toLocalTime();
                    return appointmentTime.equals(time);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Create the appointments table
     * @param stage The application stage for dialog ownership
     * @return TableView for appointments
     */
    private TableView<Appointment> createAppointmentsTable(Stage stage) {
        TableView<Appointment> table = new TableView<>();
        
        // Date Column
        TableColumn<Appointment, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new SimpleStringProperty(
                    cellData.getValue().getStartTime().format(formatter));
        });
        dateColumn.setPrefWidth(100);
        
        // Time Column
        TableColumn<Appointment, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(cellData -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            return new SimpleStringProperty(
                    cellData.getValue().getStartTime().format(formatter) + " - " +
                    cellData.getValue().getEndTime().format(formatter));
        });
        timeColumn.setPrefWidth(150);
        
        // Title Column
        TableColumn<Appointment, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setPrefWidth(200);
        
        // Customer Column
        TableColumn<Appointment, String> customerColumn = new TableColumn<>("Customer");
        customerColumn.setCellValueFactory(cellData -> {
            Customer customer = cellData.getValue().getCustomer();
            return new SimpleStringProperty(customer != null ? 
                    customer.getFirstName() + " " + customer.getLastName() : "N/A");
        });
        customerColumn.setPrefWidth(150);
        
        // Vehicle Column
        TableColumn<Appointment, String> vehicleColumn = new TableColumn<>("Vehicle");
        vehicleColumn.setCellValueFactory(cellData -> {
            Vehicle vehicle = cellData.getValue().getVehicle();
            return new SimpleStringProperty(vehicle != null ? vehicle.toString() : "N/A");
        });
        vehicleColumn.setPrefWidth(150);
        
        // Technician Column
        TableColumn<Appointment, String> technicianColumn = new TableColumn<>("Technician");
        technicianColumn.setCellValueFactory(cellData -> {
            Technician technician = cellData.getValue().getTechnician();
            return new SimpleStringProperty(technician != null ? 
                    technician.getFirstName() + " " + technician.getLastName() : "N/A");
        });
        technicianColumn.setPrefWidth(150);
        
        // Status Column with color coding
        TableColumn<Appointment, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusColumn.setPrefWidth(120);
        statusColumn.setCellFactory(column -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Color code by status
                    switch (item) {
                        case "Scheduled":
                            setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                            break;
                        case "Confirmed":
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            break;
                        case "In Progress":
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                            break;
                        case "Completed":
                            setStyle("-fx-text-fill: #8BC34A; -fx-font-weight: bold;");
                            break;
                        case "Cancelled":
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                            break;
                        case "No Show":
                            setStyle("-fx-text-fill: #9E9E9E; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });
        
        // Quick Actions Column
        TableColumn<Appointment, Void> actionsColumn = new TableColumn<>("Quick Actions");
        actionsColumn.setCellFactory(column -> new TableCell<Appointment, Void>() {
            private final Button completeBtn = new Button("✓");
            private final Button cancelBtn = new Button("✗");
            private final HBox actionBox = new HBox(5);
            
            {
                completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 8; -fx-cursor: hand;");
                completeBtn.setTooltip(new Tooltip("Mark as Completed"));
                
                cancelBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 8; -fx-cursor: hand;");
                cancelBtn.setTooltip(new Tooltip("Mark as Cancelled"));
                
                actionBox.setAlignment(javafx.geometry.Pos.CENTER);
                actionBox.getChildren().addAll(completeBtn, cancelBtn);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Appointment appointment = getTableRow().getItem();
                    if (appointment != null) {
                        // Only show buttons for non-completed/non-cancelled appointments
                        if (appointment.getStatus() != AppointmentStatus.COMPLETED && 
                            appointment.getStatus() != AppointmentStatus.CANCELLED) {
                            
                            completeBtn.setOnAction(e -> {
                                updateAppointmentStatus(appointment, AppointmentStatus.COMPLETED);
                            });
                            
                            cancelBtn.setOnAction(e -> {
                                updateAppointmentStatus(appointment, AppointmentStatus.CANCELLED);
                            });
                            
                            setGraphic(actionBox);
                        } else {
                            setGraphic(null);
                        }
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        actionsColumn.setPrefWidth(120);
        
        table.getColumns().addAll(dateColumn, timeColumn, titleColumn, customerColumn, 
                vehicleColumn, technicianColumn, statusColumn, actionsColumn);
        
        // Add double-click handler to edit appointments
        table.setRowFactory(tv -> {
            TableRow<Appointment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Appointment appointment = row.getItem();
                    System.out.println("[AppointmentController] Double-clicked on appointment: " + appointment.getTitle());
                    Optional<Appointment> editedAppointment = showEditAppointmentDialog(stage, appointment);
                    if (editedAppointment.isPresent()) {
                        System.out.println("[AppointmentController] Appointment edited successfully via double-click");
                        refreshAppointments();
                    } else {
                        System.out.println("[AppointmentController] Edit dialog cancelled via double-click");
                    }
                }
            });
            return row;
        });
        
        return table;
    }
    
    /**
     * Update appointment status with database persistence
     */
    private void updateAppointmentStatus(Appointment appointment, AppointmentStatus newStatus) {
        System.out.println("[AppointmentController] Updating appointment status - ID: " + appointment.getId() + ", New Status: " + newStatus);
        
        // Load fresh appointment from database
        Optional<Appointment> freshAppointment = appointmentDao.findById(appointment.getId());
        if (freshAppointment.isPresent()) {
            Appointment appt = freshAppointment.get();
            appt.setStatus(newStatus);
            
            Appointment updatedAppointment = appointmentDao.update(appt);
            System.out.println("[AppointmentController] Appointment status updated successfully");
            
            // Refresh the list view table
            refreshAppointments();
            
            // ALSO refresh calendar views if they exist
            refreshAllCalendarViews();
            
            showAlert("Status Updated", 
                     "Appointment marked as " + newStatus.getDisplayName(), 
                     Alert.AlertType.INFORMATION);
        } else {
            showAlert("Error", "Could not update appointment status.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Refresh all calendar views (day/week/month) after appointment changes
     * so no view keeps showing stale (or deleted) appointments.
     */
    private void refreshAllCalendarViews() {
        try {
            if (dayViewContainer != null && dayViewDatePicker != null) {
                LocalDate date = dayViewDatePicker.getValue() != null
                        ? dayViewDatePicker.getValue() : LocalDate.now();
                refreshCalendarView(date,
                        dayViewTechnicianFilter != null ? dayViewTechnicianFilter.getValue() : null,
                        dayViewStatusFilter != null ? dayViewStatusFilter.getValue() : null,
                        dayViewContainer);
            }
            if (weekViewGrid != null && weekViewPicker != null) {
                LocalDate date = weekViewPicker.getValue() != null
                        ? weekViewPicker.getValue() : LocalDate.now();
                refreshWeekView(date, weekViewGrid);
            }
            if (monthViewGrid != null && monthViewPicker != null) {
                LocalDate date = monthViewPicker.getValue() != null
                        ? monthViewPicker.getValue() : LocalDate.now();
                refreshMonthView(date, monthViewGrid);
            }
        } catch (Exception e) {
            System.err.println("[AppointmentController] Error refreshing calendar views: " + e.getMessage());
        }
    }
    
    /**
     * Refresh the appointments list
     */
    public void refreshAppointments() {
        // Remember the current selection so auto-refresh doesn't lose it
        Appointment selected = appointmentsTable != null
                ? appointmentsTable.getSelectionModel().getSelectedItem() : null;
        Long selectedId = selected != null ? selected.getId() : null;

        List<Appointment> appointments;
        // Re-apply the date filter if the user ran a search - don't wipe their results
        if (listDateFilterActive && listFromDatePicker != null && listToDatePicker != null
                && listFromDatePicker.getValue() != null && listToDatePicker.getValue() != null) {
            appointments = appointmentDao.findBetweenDates(listFromDatePicker.getValue(), listToDatePicker.getValue());
        } else {
            appointments = appointmentDao.findAll();
        }
        appointmentsList = FXCollections.observableArrayList(appointments);
        if (appointmentsTable != null) {
            appointmentsTable.setItems(appointmentsList);
            // Restore selection if the appointment still exists in the results
            if (selectedId != null) {
                for (Appointment a : appointmentsList) {
                    if (a.getId() != null && a.getId().equals(selectedId)) {
                        appointmentsTable.getSelectionModel().select(a);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Search appointments between dates
     * @param fromDate Start date
     * @param toDate End date
     */
    private void searchAppointments(LocalDate fromDate, LocalDate toDate) {
        List<Appointment> appointments = appointmentDao.findBetweenDates(fromDate, toDate);
        appointmentsList = FXCollections.observableArrayList(appointments);
        appointmentsTable.setItems(appointmentsList);
    }
    
    /**
     * Get today's appointments for dashboard
     * @return List of today's appointments
     */
    public List<Appointment> getTodaysAppointments() {
        return appointmentDao.findByDate(LocalDate.now());
    }
    
    /**
     * Show dialog to create a new appointment
     * @param owner The owner window
     * @return Optional containing the created appointment, or empty if cancelled.
     */
    public Optional<Appointment> showNewAppointmentDialog(Stage owner) {
        return showNewAppointmentDialog(owner, null, null);
    }

    /**
     * Show dialog to create a new appointment, optionally pre-filled with the
     * date/time the user clicked on in a calendar view.
     * @param owner The owner window
     * @param prefilledDate Date to pre-select (null = today)
     * @param prefilledStartTime Start time to pre-select (null = 9:00 AM)
     * @return Optional containing the created appointment, or empty if cancelled.
     */
    public Optional<Appointment> showNewAppointmentDialog(Stage owner, LocalDate prefilledDate, LocalTime prefilledStartTime) {
        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle("New Appointment");
        dialog.setHeaderText("Create New Appointment");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Form fields
        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Select Customer (Optional)");
        
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPromptText("Select Vehicle (Optional)");
        vehicleComboBox.setDisable(true);
        
        ComboBox<Technician> technicianComboBox = new ComboBox<>();
        technicianComboBox.setPromptText("Select Technician");
        
        DatePicker datePicker = new DatePicker(prefilledDate != null ? prefilledDate : LocalDate.now());
        datePicker.setEditable(false); // Prevent clearing to null which would NPE on save

        ComboBox<String> startTimeComboBox = new ComboBox<>();
        ComboBox<String> endTimeComboBox = new ComboBox<>();
        
        // Populate time dropdowns (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue; // Skip after 6:00 PM
                
                String time = String.format("%02d:%02d %s", 
                        hour > 12 ? hour - 12 : hour, 
                        minute, 
                        hour >= 12 ? "PM" : "AM");
                
                startTimeComboBox.getItems().add(time);
                endTimeComboBox.getItems().add(time);
            }
        }
        
        if (prefilledStartTime != null) {
            // Round to the nearest 30-minute slot offered by the dropdowns
            LocalTime rounded = prefilledStartTime.withMinute(prefilledStartTime.getMinute() < 30 ? 0 : 30)
                    .withSecond(0).withNano(0);
            if (rounded.isBefore(LocalTime.of(8, 0))) rounded = LocalTime.of(8, 0);
            if (rounded.isAfter(LocalTime.of(17, 30))) rounded = LocalTime.of(17, 30);
            startTimeComboBox.setValue(formatTimeForComboBox(rounded));
            endTimeComboBox.setValue(formatTimeForComboBox(rounded.plusHours(1)));
        } else {
            startTimeComboBox.setValue("09:00 AM");
            endTimeComboBox.setValue("10:00 AM");
        }
        
        ComboBox<AppointmentStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(AppointmentStatus.values());
        statusComboBox.setValue(AppointmentStatus.SCHEDULED);
        
        // Debug info about the database
        System.out.println("Loading customers from database...");
        
        // Load customers from database
        List<Customer> customers = customerDao.findAll();
        System.out.println("Found " + customers.size() + " customers");
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
        
        // Debug info about technicians
        System.out.println("Loading technicians from database...");
        
        // Load technicians from database
        List<Technician> technicians = technicianDao.findAll();
        System.out.println("Found " + technicians.size() + " technicians");
        technicianComboBox.getItems().addAll(technicians);
        
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
        
        // When customer is selected, load their vehicles
        customerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            vehicleComboBox.getItems().clear();
            
            if (newVal != null) {
                vehicleComboBox.setDisable(false);
                
                // Load vehicles for selected customer
                List<Vehicle> vehicles = vehicleDao.findByCustomerId(newVal.getId());
                System.out.println("Found " + vehicles.size() + " vehicles for customer " + newVal.getFirstName() + " " + newVal.getLastName());
                vehicleComboBox.getItems().addAll(vehicles);
            } else {
                vehicleComboBox.setDisable(true);
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
        
        // Add form fields to grid
        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);
        
        grid.add(new Label("Customer:"), 0, row);
        grid.add(customerComboBox, 1, row++);
        
        grid.add(new Label("Vehicle:"), 0, row);
        grid.add(vehicleComboBox, 1, row++);
        
        grid.add(new Label("Technician:"), 0, row);
        grid.add(technicianComboBox, 1, row++);
        
        grid.add(new Label("Date:"), 0, row);
        grid.add(datePicker, 1, row++);
        
        grid.add(new Label("Start Time:"), 0, row);
        grid.add(startTimeComboBox, 1, row++);
        
        grid.add(new Label("End Time:"), 0, row);
        grid.add(endTimeComboBox, 1, row++);
        
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        grid.add(statusLabel, 0, row);
        grid.add(statusComboBox, 1, row++);
        
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(new ScrollPane(content));

        // Validate BEFORE the dialog closes - on failure consume the event so the
        // dialog stays open and the user's input isn't wiped
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
                showAlert("Missing Information", "Please enter a title for the appointment.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (technicianComboBox.getValue() == null) {
                showAlert("Missing Information", "Please select a technician.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (datePicker.getValue() == null) {
                showAlert("Missing Information", "Please pick a date for the appointment.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            LocalTime startTime = parseTime(startTimeComboBox.getValue());
            LocalTime endTime = parseTime(endTimeComboBox.getValue());
            if (!endTime.isAfter(startTime)) {
                showAlert("Invalid Time", "End time must be after start time.", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        // Convert result (input is guaranteed valid - the filter above blocked bad input)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                // Parse times
                LocalTime startTime = parseTime(startTimeComboBox.getValue());
                LocalTime endTime = parseTime(endTimeComboBox.getValue());

                // Create appointment
                Appointment appointment = new Appointment();
                appointment.setTitle(titleField.getText());
                appointment.setDescription(descriptionArea.getText());
                appointment.setCustomer(customerComboBox.getValue());
                appointment.setVehicle(vehicleComboBox.getValue());
                appointment.setTechnician(technicianComboBox.getValue());
                
                LocalDate date = datePicker.getValue();
                appointment.setStartTime(LocalDateTime.of(date, startTime));
                appointment.setEndTime(LocalDateTime.of(date, endTime));
                
                appointment.setStatus(statusComboBox.getValue());
                
                // Save appointment to database
                System.out.println("Saving appointment: " + appointment.getTitle() + " at " + appointment.getStartTime());
                Appointment savedAppointment = appointmentDao.save(appointment);
                System.out.println("Appointment saved with ID: " + savedAppointment.getId());
                
                // Send confirmation email
                if (savedAppointment.getCustomer() != null && savedAppointment.getCustomer().getEmail() != null) {
                    new Thread(() -> {
                        boolean emailSent = emailService.sendAppointmentConfirmation(savedAppointment);
                        if (emailSent) {
                            System.out.println("Confirmation email sent to: " + savedAppointment.getCustomer().getEmail());
                        } else {
                            System.out.println("Failed to send confirmation email");
                        }
                    }).start();
                }
                
                return savedAppointment;
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Appointment> result = dialog.showAndWait();
        if (result.isPresent()) {
            // showAlert is fine here, but the refresh should be handled by the caller
            showAlert("Appointment Created", "The appointment has been created successfully.", Alert.AlertType.INFORMATION);
            return result; // Return the Optional<Appointment>
        }
        return Optional.empty(); // Return empty if no appointment was created or dialog cancelled
    }
    
    /**
     * Show dialog to edit an existing appointment
     * @param owner The owner window
     * @param appointment The appointment to edit
     * @return Optional containing the edited appointment, or empty if cancelled
     */
    public Optional<Appointment> showEditAppointmentDialog(Stage owner, Appointment appointment) {
        System.out.println("[AppointmentController] showEditAppointmentDialog called with appointment: " + appointment.getTitle());
        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle("Edit Appointment");
        dialog.setHeaderText("Edit Appointment");
        if (owner != null) {
            dialog.initOwner(owner);
        }
        System.out.println("[AppointmentController] Dialog created and owner set");
        
        // Set buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Form fields - pre-populate with existing values
        TextField titleField = new TextField(appointment.getTitle());
        TextArea descriptionArea = new TextArea(appointment.getDescription());
        descriptionArea.setPrefRowCount(3);
        
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setPromptText("Select Customer (Optional)");
        
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.setPromptText("Select Vehicle (Optional)");
        vehicleComboBox.setDisable(appointment.getCustomer() == null);
        
        ComboBox<Technician> technicianComboBox = new ComboBox<>();
        technicianComboBox.setPromptText("Select Technician");
        
        DatePicker datePicker = new DatePicker(appointment.getStartTime().toLocalDate());
        datePicker.setEditable(false); // Prevent clearing to null which would NPE on save

        ComboBox<String> startTimeComboBox = new ComboBox<>();
        ComboBox<String> endTimeComboBox = new ComboBox<>();
        
        // Populate time dropdowns (8:00 AM to 6:00 PM in 30-min increments)
        for (int hour = 8; hour <= 18; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour == 18 && minute > 0) continue; // Skip after 6:00 PM
                
                String time = String.format("%02d:%02d %s", 
                        hour > 12 ? hour - 12 : hour == 0 ? 12 : hour, 
                        minute, 
                        hour >= 12 ? "PM" : "AM");
                
                startTimeComboBox.getItems().add(time);
                endTimeComboBox.getItems().add(time);
            }
        }
        
        // Set current times
        LocalTime startTime = appointment.getStartTime().toLocalTime();
        LocalTime endTime = appointment.getEndTime().toLocalTime();
        startTimeComboBox.setValue(formatTimeForComboBox(startTime));
        endTimeComboBox.setValue(formatTimeForComboBox(endTime));
        
        ComboBox<AppointmentStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(AppointmentStatus.values());
        statusComboBox.setValue(appointment.getStatus());
        statusComboBox.setPrefWidth(200);
        // Make status dropdown more visible
        statusComboBox.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        
        // Load customers from database
        List<Customer> customers = customerDao.findAll();
        customerComboBox.getItems().addAll(customers);
        // Select the matching instance BY ID - appointment.getCustomer() is a different
        // object instance than the freshly loaded ones, so identity comparison would fail
        if (appointment.getCustomer() != null) {
            for (Customer c : customers) {
                if (c.getId() != null && c.getId().equals(appointment.getCustomer().getId())) {
                    customerComboBox.setValue(c);
                    break;
                }
            }
        }
        
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
        
        // Load technicians from database
        List<Technician> technicians = technicianDao.findAll();
        technicianComboBox.getItems().addAll(technicians);
        // Select the matching instance BY ID (same detached-instance issue as customer)
        if (appointment.getTechnician() != null) {
            for (Technician t : technicians) {
                if (t.getId() != null && t.getId().equals(appointment.getTechnician().getId())) {
                    technicianComboBox.setValue(t);
                    break;
                }
            }
        }
        
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
        
        // Load vehicles for current customer if exists
        if (appointment.getCustomer() != null) {
            List<Vehicle> vehicles = vehicleDao.findByCustomerId(appointment.getCustomer().getId());
            vehicleComboBox.getItems().addAll(vehicles);
            // Select the matching instance BY ID - appointment.getVehicle() is a
            // different object instance than the freshly loaded ones
            if (appointment.getVehicle() != null) {
                for (Vehicle v : vehicles) {
                    if (v.getId() != null && v.getId().equals(appointment.getVehicle().getId())) {
                        vehicleComboBox.setValue(v);
                        break;
                    }
                }
            }
        }

        // When customer is selected, load their vehicles
        customerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            vehicleComboBox.getItems().clear();

            if (newVal != null) {
                vehicleComboBox.setDisable(false);

                // Load vehicles for selected customer
                List<Vehicle> vehicles = vehicleDao.findByCustomerId(newVal.getId());
                vehicleComboBox.getItems().addAll(vehicles);

                // If the same customer is re-selected, restore the original vehicle
                // (compare BY ID - equals() is identity here so it never matched)
                if (oldVal != null && newVal.getId() != null && newVal.getId().equals(oldVal.getId())
                        && appointment.getVehicle() != null) {
                    for (Vehicle v : vehicles) {
                        if (v.getId() != null && v.getId().equals(appointment.getVehicle().getId())) {
                            vehicleComboBox.setValue(v);
                            break;
                        }
                    }
                }
            } else {
                vehicleComboBox.setDisable(true);
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
        
        // Add form fields to grid
        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);
        
        grid.add(new Label("Customer:"), 0, row);
        grid.add(customerComboBox, 1, row++);
        
        grid.add(new Label("Vehicle:"), 0, row);
        grid.add(vehicleComboBox, 1, row++);
        
        grid.add(new Label("Technician:"), 0, row);
        grid.add(technicianComboBox, 1, row++);
        
        grid.add(new Label("Date:"), 0, row);
        grid.add(datePicker, 1, row++);
        
        grid.add(new Label("Start Time:"), 0, row);
        grid.add(startTimeComboBox, 1, row++);
        
        grid.add(new Label("End Time:"), 0, row);
        grid.add(endTimeComboBox, 1, row++);
        
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        grid.add(statusLabel, 0, row);
        grid.add(statusComboBox, 1, row++);
        
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(new ScrollPane(content));

        // Validate BEFORE the dialog closes - on failure consume the event so the
        // dialog stays open and the user's edits aren't wiped
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
                showAlert("Missing Information", "Please enter a title for the appointment.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (technicianComboBox.getValue() == null) {
                showAlert("Missing Information", "Please select a technician.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            if (datePicker.getValue() == null) {
                showAlert("Missing Information", "Please pick a date for the appointment.", Alert.AlertType.ERROR);
                event.consume();
                return;
            }
            LocalTime newStartTime = parseTime(startTimeComboBox.getValue());
            LocalTime newEndTime = parseTime(endTimeComboBox.getValue());
            if (!newEndTime.isAfter(newStartTime)) {
                showAlert("Invalid Time", "End time must be after start time.", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        // Convert result (input is guaranteed valid - the filter above blocked bad input)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Parse times
                LocalTime newStartTime = parseTime(startTimeComboBox.getValue());
                LocalTime newEndTime = parseTime(endTimeComboBox.getValue());

                // Update appointment
                appointment.setTitle(titleField.getText());
                appointment.setDescription(descriptionArea.getText());
                appointment.setCustomer(customerComboBox.getValue());
                appointment.setVehicle(vehicleComboBox.getValue());
                appointment.setTechnician(technicianComboBox.getValue());
                
                LocalDate date = datePicker.getValue();
                appointment.setStartTime(LocalDateTime.of(date, newStartTime));
                appointment.setEndTime(LocalDateTime.of(date, newEndTime));
                
                appointment.setStatus(statusComboBox.getValue());
                
                // Save updated appointment to database
                System.out.println("Updating appointment: " + appointment.getTitle());
                Appointment updatedAppointment = appointmentDao.update(appointment);
                System.out.println("Appointment updated with ID: " + updatedAppointment.getId());
                
                // Send update email if status changed to confirmed
                if (updatedAppointment.getCustomer() != null && updatedAppointment.getCustomer().getEmail() != null) {
                    if (statusComboBox.getValue() == AppointmentStatus.CONFIRMED) {
                        new Thread(() -> {
                            boolean emailSent = emailService.sendAppointmentConfirmation(updatedAppointment);
                            if (emailSent) {
                                System.out.println("Confirmation email sent to: " + updatedAppointment.getCustomer().getEmail());
                            }
                        }).start();
                    }
                }
                
                return updatedAppointment;
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Appointment> result = dialog.showAndWait();
        if (result.isPresent()) {
            showAlert("Appointment Updated", "The appointment has been updated successfully.", Alert.AlertType.INFORMATION);
            return result;
        }
        return Optional.empty();
    }
    
    /**
     * Format LocalTime to string for combo box (e.g. "09:00 AM")
     * @param time The time to format
     * @return Formatted time string
     */
    private String formatTimeForComboBox(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        String period = hour >= 12 ? "PM" : "AM";
        
        if (hour > 12) {
            hour -= 12;
        } else if (hour == 0) {
            hour = 12;
        }
        
        return String.format("%02d:%02d %s", hour, minute, period);
    }
    
    /**
     * Parse time string (e.g. "09:00 AM") to LocalTime
     * @param timeString Time string
     * @return LocalTime
     */
    private LocalTime parseTime(String timeString) {
        // Extract hour, minute, and AM/PM
        String[] parts = timeString.split(" ");
        String[] timeParts = parts[0].split(":");
        
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Convert to 24-hour format
        if (parts[1].equals("PM") && hour < 12) {
            hour += 12;
        } else if (parts[1].equals("AM") && hour == 12) {
            hour = 0;
        }
        
        return LocalTime.of(hour, minute);
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

    private void handleDeleteAppointment(Appointment appointment) {
        if (appointment == null) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Appointment");
        confirmDialog.setHeaderText("Confirm Deletion");
        confirmDialog.setContentText("Are you sure you want to delete the appointment: \n" + 
                                   appointment.getTitle() + " on " + 
                                   appointment.getStartTime().toLocalDate().toString() + " at " +
                                   appointment.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a")) + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                appointmentDao.delete(appointment);
                showAlert("Appointment Deleted", "The appointment has been deleted successfully.", Alert.AlertType.INFORMATION);
                refreshAppointments(); // Refresh the list view
                refreshAllCalendarViews(); // Refresh the day/week/month grids too

            } catch (Exception e) {
                showAlert("Error", "Could not delete the appointment: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }
} 