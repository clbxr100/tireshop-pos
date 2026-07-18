package com.tireshop.view;

import com.tireshop.model.User;
import com.tireshop.model.TimeEntry;
import com.tireshop.service.UserService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import java.time.format.DateTimeFormatter;

public class TimeClockDialog extends Dialog<TimeEntry> {
    private final UserService userService;
    private final User user;
    private final TextArea notesArea;
    private final boolean isClockInAction;

    public TimeClockDialog(UserService userService, User user, boolean isClockInAction) {
        this.userService = userService;
        this.user = user;
        this.isClockInAction = isClockInAction;
        
        this.setTitle(isClockInAction ? "Clock In" : "Clock Out");
        this.initModality(Modality.APPLICATION_MODAL);
        this.getDialogPane().setMinWidth(350);

        // Create the dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Show current user info
        Label userLabel = new Label("User: " + user.getUsername() + " (" + user.getRole() + ")");
        userLabel.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(userLabel);

        // If clocking out, show clock in time
        if (!isClockInAction) {
            TimeEntry currentEntry = userService.getCurrentTimeEntry(user.getId());
            if (currentEntry != null) {
                Label clockInTimeLabel = new Label("Clocked In At: " + 
                    currentEntry.getClockIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a")));
                content.getChildren().add(clockInTimeLabel);
                if (currentEntry.getNotes() != null && !currentEntry.getNotes().isEmpty()){
                    Label existingNotesLabel = new Label("Clock-In Notes: " + currentEntry.getNotes());
                    existingNotesLabel.setWrapText(true);
                    content.getChildren().add(existingNotesLabel);
                }
            }
        }

        // Notes field
        Label notesLabel = new Label(isClockInAction ? "Notes for Clock-In (optional):" : "Notes for Clock-Out (optional):");
        notesArea = new TextArea();
        notesArea.setPromptText("Enter any relevant notes here...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        content.getChildren().addAll(notesLabel, notesArea);

        this.getDialogPane().setContent(content);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert the result
        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String notes = notesArea.getText();
                System.out.println("[TimeClockDialog] OK button pressed. Action: " + (isClockInAction ? "Clock In" : "Clock Out") + ", Notes: '" + notes + "'");
                if (isClockInAction) {
                    return userService.clockIn(user.getId(), notes);
                } else {
                    return userService.clockOut(user.getId(), notes);
                }
            }
            System.out.println("[TimeClockDialog] Dialog cancelled.");
            return null;
        });
    }

    public static TimeEntry showClockInDialog(UserService userService, User user) {
        System.out.println("[TimeClockDialog] Showing Clock-In dialog for user: " + user.getUsername());
        TimeClockDialog dialog = new TimeClockDialog(userService, user, true);
        return dialog.showAndWait().orElse(null);
    }

    public static TimeEntry showClockOutDialog(UserService userService, User user) {
        System.out.println("[TimeClockDialog] Showing Clock-Out dialog for user: " + user.getUsername());
        TimeClockDialog dialog = new TimeClockDialog(userService, user, false);
        return dialog.showAndWait().orElse(null);
    }
} 