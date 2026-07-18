package com.tireshop.view;

import com.tireshop.model.User;
import com.tireshop.service.UserService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

public class ClockInOutCredentialDialog extends Dialog<User> {
    private final UserService userService;
    private final TextField usernameField;
    private final PasswordField passwordField;

    public ClockInOutCredentialDialog(UserService userService) {
        this.userService = userService;
        this.setTitle("Clock In/Out - Enter Credentials");
        this.initModality(Modality.APPLICATION_MODAL);

        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        this.getDialogPane().setContent(grid);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Request focus on the username field by default.
        usernameField.requestFocus();

        // Convert the result to a User object upon OK button click
        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String username = usernameField.getText();
                String password = passwordField.getText();
                if (username.isEmpty() || password.isEmpty()) {
                    // Simple validation, could be enhanced with an alert
                    System.err.println("Username or password cannot be empty."); 
                    // How to show an alert from here or prevent dialog closing needs consideration
                    // For now, returning null will indicate failure.
                    return null; 
                }
                return userService.authenticateUser(username, password);
            }
            return null;
        });
    }
} 