package com.tireshop.controller;

import com.tireshop.model.PaymentType;
import com.tireshop.model.Sale;
import com.tireshop.service.SalesService;
import com.tireshop.util.SettingsService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        
        // Create dialog
        Dialog<PaymentType> paymentTypeDialog = new Dialog<>();
        paymentTypeDialog.setTitle("Process Payment");
        paymentTypeDialog.setHeaderText("Select Payment Method");
        paymentTypeDialog.initOwner(owner);
        paymentTypeDialog.initModality(Modality.APPLICATION_MODAL);
        
        // Set buttons
        ButtonType confirmButtonType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        paymentTypeDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 150, 10, 10));
        
        Label totalLabel = new Label();
        updateTotalDisplay(totalLabel, sale, null);
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        ComboBox<PaymentType> paymentTypeComboBox = new ComboBox<>(
                FXCollections.observableArrayList(PaymentType.values()));
        paymentTypeComboBox.setPromptText("Select Payment Method");
        
        paymentTypeComboBox.valueProperty().addListener((obs, oldType, newType) -> {
            updateTotalDisplay(totalLabel, sale, newType);
        });
        
        content.getChildren().addAll(
                totalLabel,
                new Label("Payment Method:"),
                paymentTypeComboBox
        );
        
        paymentTypeDialog.getDialogPane().setContent(content);
        
        // Convert result
        paymentTypeDialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return paymentTypeComboBox.getValue();
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<PaymentType> paymentTypeResult = paymentTypeDialog.showAndWait();
        
        if (paymentTypeResult.isPresent()) {
            PaymentType selectedType = paymentTypeResult.get();
            
            // Process different payment types
            switch (selectedType) {
                case CASH:
                    return processCashPayment(sale, owner);
                case CREDIT_CARD:
                case DEBIT_CARD:
                    return processCardPayment(sale, owner, selectedType);
                case CHECK:
                    return processCheckPayment(sale, owner);
                case FINANCING:
                    return processFinancingPayment(sale, owner);
                case GIFT_CARD:
                    return processGiftCardPayment(sale, owner);
                case STORE_CREDIT:
                    return processStoreCreditPayment(sale, owner);
                default:
                    return false;
            }
        }
        
        return false;
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
        
        // Calculate change dynamically
        cashAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                BigDecimal cashAmount = new BigDecimal(newValue);
                BigDecimal change = cashAmount.subtract(sale.getTotal());
                if (change.compareTo(BigDecimal.ZERO) >= 0) {
                    changeLabel.setText("Change: $" + formatCurrency(change));
                } else {
                    changeLabel.setText("Insufficient amount");
                }
            } catch (NumberFormatException e) {
                changeLabel.setText("Enter a valid amount");
            }
        });
        
        grid.add(new Label("Total:"), 0, 0);
        grid.add(totalLabel, 1, 0);
        grid.add(new Label("Cash Amount:"), 0, 1);
        grid.add(cashAmountField, 1, 1);
        grid.add(new Label("Change:"), 0, 2);
        grid.add(changeLabel, 1, 2);
        
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
    
    private boolean processStoreCreditPayment(Sale sale, Stage owner) {
        // Here you would check if the customer has enough store credit
        // For now, we'll just approve it
        Optional<Sale> completedSale = salesService.completeSale(sale.getId(), PaymentType.STORE_CREDIT);
        return completedSale.isPresent();
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