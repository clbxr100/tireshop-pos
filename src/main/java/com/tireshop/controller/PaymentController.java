package com.tireshop.controller;

import com.tireshop.model.PaymentType;
import com.tireshop.model.Sale;
import com.tireshop.service.SalesService;
import com.tireshop.util.SettingsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Optional;

/**
 * Controller for payment processing
 */
public class PaymentController {
    
    private final SalesService salesService;
    private final SettingsService settingsService;
    
    public PaymentController(SalesService salesService, SettingsService settingsService) {
        this.salesService = salesService;
        this.settingsService = settingsService;
    }
    
    /**
     * Show payment dialog for a sale
     * @param sale The sale to process payment for
     * @param owner The owner window
     * @return true if payment was successful
     */
    public boolean showPaymentDialog(Sale sale, Stage owner) {
        if (sale == null) return false;
        
        System.out.println("=== PAYMENT DIALOG STARTING ===");
        
        // Create a custom dialog with total display
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Process Payment");
        dialog.setHeaderText("Payment for Invoice: " + sale.getInvoiceNumber());
        dialog.initOwner(owner);
        
        // Create content with total display
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Display total amount prominently
        Label totalLabel = new Label("Total Amount Due:");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        BigDecimal totalAmount = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;
        Label amountLabel = new Label("$" + formatCurrency(totalAmount));
        amountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Show tax status if applicable
        Label taxLabel = new Label();
        if (sale.getCustomer() != null && sale.getCustomer().isTaxExempt()) {
            taxLabel.setText("Tax Exempt Customer - No Tax Applied");
            taxLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (sale.getTax() != null && sale.getTax().compareTo(BigDecimal.ZERO) > 0) {
            taxLabel.setText("Tax: $" + formatCurrency(sale.getTax()));
            taxLabel.setStyle("-fx-text-fill: #7f8c8d;");
        }
        
        Label instructionLabel = new Label("Choose a payment method:");
        instructionLabel.setStyle("-fx-font-size: 14px;");
        
        content.getChildren().addAll(totalLabel, amountLabel, taxLabel, instructionLabel);
        dialog.getDialogPane().setContent(content);
        
        // Create custom buttons for each payment type
        ButtonType cashBtn = new ButtonType("Cash");
        ButtonType creditCardBtn = new ButtonType("Credit Card");
        ButtonType debitCardBtn = new ButtonType("Debit Card");
        ButtonType checkBtn = new ButtonType("Check");
        ButtonType giftCardBtn = new ButtonType("Gift Card");
        ButtonType financingBtn = new ButtonType("Financing");
        ButtonType storeChargeBtn = new ButtonType("Store Charge");
        ButtonType splitPaymentBtn = new ButtonType("Split Payment");

        dialog.getDialogPane().getButtonTypes().setAll(cashBtn, creditCardBtn, debitCardBtn, checkBtn,
                                     giftCardBtn, financingBtn, storeChargeBtn, splitPaymentBtn, ButtonType.CANCEL);
        
        System.out.println("Payment dialog created with total display");
        
        // Set result converter to track which button was pressed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cashBtn) {
                return processCashPayment(sale, owner);
            } else if (dialogButton == creditCardBtn) {
                return processCardPayment(sale, owner, PaymentType.CREDIT_CARD);
            } else if (dialogButton == debitCardBtn) {
                return processCardPayment(sale, owner, PaymentType.DEBIT_CARD);
            } else if (dialogButton == checkBtn) {
                return processCheckPayment(sale, owner);
            } else if (dialogButton == giftCardBtn) {
                return processGiftCardPayment(sale, owner);
            } else if (dialogButton == financingBtn) {
                return processFinancingPayment(sale, owner);
            } else if (dialogButton == storeChargeBtn) {
                return processStoreChargePayment(sale, owner);
            } else if (dialogButton == splitPaymentBtn) {
                return processSplitPayment(sale, owner);
            }
            return false;
        });
        
        // Show dialog and process result
        Optional<Boolean> result = dialog.showAndWait();
        System.out.println("Dialog result: " + (result.isPresent() ? result.get() : false));
        
        return result.orElse(false);
    }
    
    private void updateTotalDisplay(Label totalAmountLabel, Sale sale, PaymentType selectedPaymentType) {
        BigDecimal currentSubtotal = sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO;
        BigDecimal currentTax = sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO;
        BigDecimal baseAmountForDisplay = currentSubtotal.add(currentTax);
        String labelText;

        if (settingsService == null) { 
            totalAmountLabel.setText("Total Amount Due: $" + formatCurrency(baseAmountForDisplay) + " (Error: Settings not loaded)");
            return;
        }

        if (PaymentType.CREDIT_CARD.equals(selectedPaymentType) || PaymentType.DEBIT_CARD.equals(selectedPaymentType)) {
            BigDecimal ccFeePercentage = settingsService.getCreditCardFeePercentage();
            if (ccFeePercentage != null && ccFeePercentage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal feeAmount = baseAmountForDisplay.multiply(ccFeePercentage);
                BigDecimal totalWithFee = baseAmountForDisplay.add(feeAmount);
                labelText = String.format("Total: $%.2f (includes $%.2f CC fee)", totalWithFee, feeAmount);
            } else {
                 labelText = "Total Amount Due: $" + formatCurrency(baseAmountForDisplay) + " (CC Fee: $0.00)";
            }
        } else {
             labelText = "Total Amount Due: $" + formatCurrency(baseAmountForDisplay);
        }
        totalAmountLabel.setText(labelText);
    }
    
    private boolean processCashPayment(Sale sale, Stage owner) {
        // Create dialog
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Cash Payment");
        dialog.setHeaderText("Enter Cash Amount");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType confirmButtonType = new ButtonType("Complete Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
        
        Label totalLabel = new Label("Total Amount: $" + formatCurrency(sale.getTotal()));
        TextField cashAmountField = new TextField();
        cashAmountField.setPromptText("Enter cash amount");
        Label changeLabel = new Label("Change: $0.00");
        Label remainingLabel = new Label("Remaining: $" + formatCurrency(sale.getTotal()));
        remainingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        
        // Calculate change and remaining amount dynamically
        cashAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                BigDecimal cashAmount = new BigDecimal(newValue);
                BigDecimal change = cashAmount.subtract(sale.getTotal());
                BigDecimal remaining = sale.getTotal().subtract(cashAmount);
                
                if (change.compareTo(BigDecimal.ZERO) >= 0) {
                    changeLabel.setText("Change: $" + formatCurrency(change));
                    remainingLabel.setText("Remaining: $0.00");
                    remainingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                } else {
                    changeLabel.setText("Insufficient amount");
                    remainingLabel.setText("Remaining: $" + formatCurrency(remaining));
                    remainingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                }
            } catch (NumberFormatException e) {
                changeLabel.setText("Enter a valid amount");
                remainingLabel.setText("Remaining: $" + formatCurrency(sale.getTotal()));
                remainingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }
        });
        
        grid.add(new Label("Total:"), 0, 0);
        grid.add(totalLabel, 1, 0);
        grid.add(new Label("Cash Amount:"), 0, 1);
        grid.add(cashAmountField, 1, 1);
        grid.add(new Label("Remaining:"), 0, 2);
        grid.add(remainingLabel, 1, 2);
        grid.add(new Label("Change:"), 0, 3);
        grid.add(changeLabel, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    return new BigDecimal(cashAmountField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<BigDecimal> result = dialog.showAndWait();
        if (result.isPresent()) {
            BigDecimal cashAmount = result.get();
            if (cashAmount.compareTo(sale.getTotal()) >= 0) {
                // Process cash payment
                Optional<Sale> completedSale = salesService.completeCashPayment(sale.getId());
                return completedSale.isPresent();
            } else {
                showAlert("Insufficient Cash", "The cash amount is less than the total due.", Alert.AlertType.ERROR);
                return false;
            }
        }
        
        return false;
    }
    
    private boolean processCardPayment(Sale sale, Stage owner, PaymentType cardType) {
        // Create dialog
        Dialog<CardPaymentData> dialog = new Dialog<>();
        dialog.setTitle(cardType.getDisplayName() + " Payment");
        dialog.setHeaderText("Enter Card Information");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType confirmButtonType = new ButtonType("Process Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<String> cardTypeField = new ComboBox<>(FXCollections.observableArrayList(
                "Visa", "MasterCard", "American Express", "Discover"));
        cardTypeField.setPromptText("Select Card Type");
        
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Card Number");
        
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV");
        
        grid.add(new Label("Card Type:"), 0, 0);
        grid.add(cardTypeField, 1, 0);
        grid.add(new Label("Card Number:"), 0, 1);
        grid.add(cardNumberField, 1, 1);
        grid.add(new Label("Expiry Date:"), 0, 2);
        grid.add(expiryField, 1, 2);
        grid.add(new Label("CVV:"), 0, 3);
        grid.add(cvvField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // Card details are now optional. User might process via external terminal.
                // We might still want to capture card type if selected.
                String type = cardTypeField.getValue();
                String number = cardNumberField.getText();
                String expiry = expiryField.getText();
                String cvvText = cvvField.getText();

                // If some details are entered, we might infer they are relevant.
                // If all are blank, it's clearly external. 
                // For now, pass them as is.
                
                return new CardPaymentData(type, number, expiry, cvvText);
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<CardPaymentData> result = dialog.showAndWait();
        if (result.isPresent()) {
            CardPaymentData cardData = result.get();
            
            // In real world, this would connect to a payment processor
            // For now, we'll simulate a successful authorization
            String authCode = "AUTH_EXT"; // Default for externally processed
            String lastFour = "XXXX";   // Default for externally processed

            if (cardData.cardNumber != null && !cardData.cardNumber.isEmpty()) {
                lastFour = cardData.cardNumber.substring(Math.max(0, cardData.cardNumber.length() - 4));
                // Simulate auth code if card number was entered, otherwise keep default
                authCode = "AUTH" + (int)(Math.random() * 1000000);
            }
            
            Optional<Sale> completedSale = salesService.completeCardPayment(
                    sale.getId(), cardData.cardType, lastFour, authCode);
            
            if (completedSale.isPresent()) {
                showAlert("Payment Processed", 
                        "Card payment approved.\nAuthorization Code: " + authCode, 
                        Alert.AlertType.INFORMATION);
                return true;
            } else {
                showAlert("Payment Failed", "Could not process card payment.", Alert.AlertType.ERROR);
                return false;
            }
        }
        
        return false;
    }
    
    private boolean processCheckPayment(Sale sale, Stage owner) {
        // Create dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Check Payment");
        dialog.setHeaderText("Enter Check Information");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType confirmButtonType = new ButtonType("Complete Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField checkNumberField = new TextField();
        checkNumberField.setPromptText("Check Number");
        
        grid.add(new Label("Total Amount:"), 0, 0);
        grid.add(new Label("$" + formatCurrency(sale.getTotal())), 1, 0);
        grid.add(new Label("Check Number:"), 0, 1);
        grid.add(checkNumberField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                if (checkNumberField.getText().isEmpty()) {
                    showAlert("Missing Information", "Please enter the check number.", Alert.AlertType.ERROR);
                    return null;
                }
                return checkNumberField.getText();
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String checkNumber = result.get();
            Optional<Sale> completedSale = salesService.completeCheckPayment(sale.getId(), checkNumber);
            return completedSale.isPresent();
        }
        
        return false;
    }
    
    private boolean processFinancingPayment(Sale sale, Stage owner) {
        // In a real system, this would integrate with financing providers
        // For now, we'll just set the payment type and show approval
        showAlert("Financing Approved", 
                "Customer has been approved for financing.\nTotal Amount: $" + formatCurrency(sale.getTotal()), 
                Alert.AlertType.INFORMATION);
        
        Optional<Sale> completedSale = salesService.completeSale(sale.getId(), PaymentType.FINANCING);
        return completedSale.isPresent();
    }
    
    private boolean processGiftCardPayment(Sale sale, Stage owner) {
        // Create dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Gift Card Payment");
        dialog.setHeaderText("Enter Gift Card Information");
        dialog.initOwner(owner);
        
        // Set buttons
        ButtonType confirmButtonType = new ButtonType("Process Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField giftCardNumberField = new TextField();
        giftCardNumberField.setPromptText("Gift Card Number");
        
        grid.add(new Label("Total Amount:"), 0, 0);
        grid.add(new Label("$" + formatCurrency(sale.getTotal())), 1, 0);
        grid.add(new Label("Gift Card Number:"), 0, 1);
        grid.add(giftCardNumberField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                if (giftCardNumberField.getText().isEmpty()) {
                    showAlert("Missing Information", "Please enter the gift card number.", Alert.AlertType.ERROR);
                    return null;
                }
                return giftCardNumberField.getText();
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            // In a real system, validate the gift card and check balance
            // For now, we'll just approve it
            Optional<Sale> completedSale = salesService.completeSale(sale.getId(), PaymentType.GIFT_CARD);
            return completedSale.isPresent();
        }
        
        return false;
    }
    
    private boolean processStoreChargePayment(Sale sale, Stage owner) {
        // Store charge requires a customer - the balance is tracked on their account
        if (sale.getCustomer() == null) {
            showAlert("Customer Required",
                    "Store Charge requires a customer assigned to the sale.\n"
                    + "Edit the sale and select a customer first.",
                    Alert.AlertType.WARNING);
            return false;
        }

        BigDecimal total = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;
        BigDecimal currentBalance = sale.getCustomer().getChargeBalance();
        BigDecimal newBalance = currentBalance.add(total);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Store Charge");
        confirm.setHeaderText("Charge to " + sale.getCustomer().getFullName() + "'s account?");
        confirm.setContentText("Sale Total: $" + formatCurrency(total)
                + "\nCurrent Balance: $" + formatCurrency(currentBalance)
                + "\nNew Balance: $" + formatCurrency(newBalance));
        confirm.initOwner(owner);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Optional<Sale> completedSale = salesService.completeStoreCharge(sale.getId());
            if (completedSale.isPresent()) {
                showAlert("Store Charge Complete",
                        "Charged $" + formatCurrency(total) + " to "
                                + sale.getCustomer().getFullName() + "'s account.\n"
                                + "Account balance: $" + formatCurrency(newBalance),
                        Alert.AlertType.INFORMATION);
                return true;
            }
            showAlert("Error", "Could not complete the store charge.", Alert.AlertType.ERROR);
        }
        return false;
    }

    private boolean processSplitPayment(Sale sale, Stage owner) {
        // Create split payment dialog
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Split Payment");
        dialog.setHeaderText("Split Payment for Invoice: " + sale.getInvoiceNumber());
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        BigDecimal totalAmount = sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO;

        // Track payments added
        java.util.List<SplitPaymentEntry> payments = new java.util.ArrayList<>();

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        content.setPrefHeight(430);

        // Total display
        Label totalLabel = new Label("Total Amount: $" + formatCurrency(totalAmount));
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Remaining balance
        Label remainingLabel = new Label("Remaining: $" + formatCurrency(totalAmount));
        remainingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        // Change due (when cash overpays)
        Label changeLabel = new Label("Change Due: $0.00");
        changeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");

        // Payments list with scroll pane
        VBox paymentsListBox = new VBox(5);
        paymentsListBox.setStyle("-fx-padding: 10;");
        Label paymentsHeader = new Label("Payments Added:");
        paymentsHeader.setStyle("-fx-font-weight: bold;");
        paymentsListBox.getChildren().add(paymentsHeader);

        // Wrap payments list in a scroll pane with fixed height
        ScrollPane paymentsScrollPane = new ScrollPane(paymentsListBox);
        paymentsScrollPane.setFitToWidth(true);
        paymentsScrollPane.setPrefHeight(150);
        paymentsScrollPane.setMaxHeight(150);
        paymentsScrollPane.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");

        // Payment entry section
        HBox paymentEntryBox = new HBox(10);
        ComboBox<PaymentType> paymentTypeCombo = new ComboBox<>();
        paymentTypeCombo.getItems().addAll(PaymentType.CASH, PaymentType.CREDIT_CARD,
            PaymentType.DEBIT_CARD, PaymentType.CHECK, PaymentType.GIFT_CARD);
        // Partial store charge: pay part now, rest goes on the customer's account
        if (sale.getCustomer() != null) {
            paymentTypeCombo.getItems().add(PaymentType.STORE_CHARGE);
        }
        paymentTypeCombo.setPromptText("Payment Type");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(100);

        Button addPaymentBtn = new Button("Add Payment");
        addPaymentBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");

        paymentEntryBox.getChildren().addAll(paymentTypeCombo, amountField, addPaymentBtn);

        // Assemble the dialog content (without this the dialog shows up empty)
        content.getChildren().addAll(totalLabel, remainingLabel, changeLabel,
                paymentsScrollPane, paymentEntryBox);
        dialog.getDialogPane().setContent(content);

        // Buttons
        ButtonType completeBtn = new ButtonType("Complete Sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(completeBtn, ButtonType.CANCEL);

        // Get reference to complete button
        javafx.scene.Node completeBtnNode = dialog.getDialogPane().lookupButton(completeBtn);
        completeBtnNode.setDisable(true);

        // Holder so the refresh runnable can reference itself from remove buttons
        final Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            BigDecimal paid = payments.stream()
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = totalAmount.subtract(paid);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                remainingLabel.setText("Remaining: $" + formatCurrency(remaining));
                remainingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                changeLabel.setText("Change Due: $0.00");
            } else {
                remainingLabel.setText("Remaining: $0.00");
                remainingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                changeLabel.setText("Change Due: $" + formatCurrency(remaining.negate()));
            }

            // Enable complete only when the total is fully covered
            completeBtnNode.setDisable(remaining.compareTo(BigDecimal.ZERO) > 0);

            // Rebuild the payments list with remove buttons
            paymentsListBox.getChildren().clear();
            paymentsListBox.getChildren().add(paymentsHeader);
            for (SplitPaymentEntry entry : payments) {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label paymentLabel = new Label(entry.type.getDisplayName() + ": $" + formatCurrency(entry.amount));
                paymentLabel.setPrefWidth(300);
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
                removeBtn.setOnAction(ev -> {
                    payments.remove(entry);
                    refreshHolder[0].run();
                });
                row.getChildren().addAll(paymentLabel, removeBtn);
                paymentsListBox.getChildren().add(row);
            }
        };

        // Add payment button handler
        addPaymentBtn.setOnAction(e -> {
            PaymentType selectedType = paymentTypeCombo.getValue();
            String amountText = amountField.getText().trim();

            if (selectedType == null) {
                showAlert("Missing Information", "Please select a payment type.", Alert.AlertType.WARNING);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException ex) {
                showAlert("Invalid Amount", "Please enter a valid number.", Alert.AlertType.WARNING);
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Invalid Amount", "Please enter a positive amount.", Alert.AlertType.WARNING);
                return;
            }

            // Non-cash payments cannot exceed the remaining balance - you can't put
            // more on a card/check than is owed. Cash may exceed it (change is given).
            BigDecimal paidSoFar = payments.stream()
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainingBefore = totalAmount.subtract(paidSoFar);
            if (selectedType != PaymentType.CASH && amount.compareTo(remainingBefore) > 0) {
                showAlert("Amount Too High",
                        selectedType.getDisplayName() + " cannot be more than the remaining balance of $"
                                + formatCurrency(remainingBefore) + ".",
                        Alert.AlertType.WARNING);
                return;
            }

            payments.add(new SplitPaymentEntry(selectedType, amount));

            // Clear inputs
            paymentTypeCombo.setValue(null);
            amountField.clear();

            refreshHolder[0].run();
        });

        dialog.setResultConverter(dialogButton -> dialogButton == completeBtn);

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get() && !payments.isEmpty()) {
            // Change can only come from cash overpayment
            BigDecimal totalPaid = payments.stream()
                .map(p -> p.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal change = totalPaid.subtract(totalAmount);
            if (change.compareTo(BigDecimal.ZERO) < 0) {
                change = BigDecimal.ZERO;
            }

            // Deduct the change from cash entries (last-to-first) so the stored
            // records are NET amounts and reports stay accurate
            BigDecimal changeLeft = change;
            for (int i = payments.size() - 1; i >= 0 && changeLeft.compareTo(BigDecimal.ZERO) > 0; i--) {
                SplitPaymentEntry entry = payments.get(i);
                if (entry.type == PaymentType.CASH) {
                    BigDecimal deduction = entry.amount.min(changeLeft);
                    entry.amount = entry.amount.subtract(deduction);
                    changeLeft = changeLeft.subtract(deduction);
                }
            }

            // Build real payment records, skipping any entries zeroed by change
            java.util.List<com.tireshop.model.SalePayment> salePayments = new java.util.ArrayList<>();
            for (SplitPaymentEntry entry : payments) {
                if (entry.amount.compareTo(BigDecimal.ZERO) > 0) {
                    salePayments.add(new com.tireshop.model.SalePayment(null, entry.type, entry.amount));
                }
            }

            try {
                Optional<Sale> completedSale = salesService.completeSplitSale(sale.getId(), salePayments, change);
                if (completedSale.isPresent()) {
                    StringBuilder summary = new StringBuilder("Split Payment Completed!\n\n");
                    for (com.tireshop.model.SalePayment p : salePayments) {
                        summary.append(p.getPaymentType().getDisplayName()).append(": $")
                               .append(formatCurrency(p.getAmount()));
                        if (p.getPaymentType() == PaymentType.STORE_CHARGE) {
                            summary.append(" (added to ").append(sale.getCustomer().getFullName())
                                   .append("'s account)");
                        }
                        summary.append("\n");
                    }
                    if (change.compareTo(BigDecimal.ZERO) > 0) {
                        summary.append("\nChange Due: $").append(formatCurrency(change));
                    }
                    showAlert("Payment Successful", summary.toString(), Alert.AlertType.INFORMATION);
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            showAlert("Payment Failed", "Could not complete the split payment. Please try again.", Alert.AlertType.ERROR);
        }

        return false;
    }

    /**
     * Helper class for split payment entries
     */
    private static class SplitPaymentEntry {
        PaymentType type;
        BigDecimal amount;

        SplitPaymentEntry(PaymentType type, BigDecimal amount) {
            this.type = type;
            this.amount = amount;
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }
    
    /**
     * Helper class for card payment data
     */
    private static class CardPaymentData {
        String cardType;
        String cardNumber;
        String expiryDate;
        String cvv;
        
        public CardPaymentData(String cardType, String cardNumber, String expiryDate, String cvv) {
            this.cardType = cardType;
            this.cardNumber = cardNumber;
            this.expiryDate = expiryDate;
            this.cvv = cvv;
        }
    }
} 