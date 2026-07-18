package com.tireshop.view;

import com.tireshop.model.User;
import com.tireshop.model.TimeEntry;
import com.tireshop.service.UserService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialog extends Dialog<User> {
    private final UserService userService;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private User loggedInUser;

    public LoginDialog(UserService userService) {
        this.userService = userService;
        this.setTitle("Login");
        this.initModality(Modality.APPLICATION_MODAL);

        // Create the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        usernameField = new TextField();
        passwordField = new PasswordField();

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        this.getDialogPane().setContent(grid);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert the result to a User object
        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String username = usernameField.getText();
                String password = passwordField.getText();
                return userService.authenticateUser(username, password);
            }
            return null;
        });
    }

    public boolean showAndWaitForLogin() {
        this.showAndWait().ifPresent(user -> {
            if (user != null) {
                loggedInUser = user;
            }
        });
        return loggedInUser != null;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
} 