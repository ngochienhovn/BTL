package app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class AuctionDetailController {

    // ===== UI =====
    @FXML private Label titleLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label statusLabel;

    @FXML private TextField bidField;
    @FXML private ListView<String> bidHistoryList;

    @FXML private Label balanceLabel;
    @FXML private Label reservedLabel;
    @FXML private Label availableLabel;
    @FXML private TextField amountField;

    // ===== Fake Data =====
    private double balance = 1000;
    private double reserved = 200;
    private double currentBid = 300;

    private List<String> bidHistory = new ArrayList<>();

    // ===== SET DATA =====
    public void setAuction(String title, double currentBid, String status) {
        titleLabel.setText(title);
        currentBidLabel.setText("Current Bid: " + currentBid);
        statusLabel.setText("Status: " + status);
    }

    // ===== PLACE BID =====
    @FXML
    private void onPlaceBid() {
        try {
            double bidAmount = Double.parseDouble(bidField.getText());

            double available = balance - reserved;

            if (bidAmount > available) {
                double required = bidAmount - available;

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText("Insufficient funds");
                alert.setContentText("Need top-up: " + required + "\nDeposit now?");

                alert.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.OK) {
                        amountField.setText(String.valueOf(required));
                    }
                });

                return;
            }

            // Fake bid success
            currentBid = bidAmount;
            reserved += bidAmount;

            bidHistory.add("You bid: " + bidAmount);

            currentBidLabel.setText("Current Bid: " + currentBid);

            renderBidHistory();
            refreshWallet();

        } catch (Exception e) {
            showError("Invalid bid");
        }
    }

    // ===== DEPOSIT =====
    @FXML
    private void onDeposit() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            balance += amount;

            refreshWallet();
        } catch (Exception e) {
            showError("Invalid amount");
        }
    }

    // ===== WITHDRAW =====
    @FXML
    private void onWithdraw() {
        try {
            double amount = Double.parseDouble(amountField.getText());

            double available = balance - reserved;

            if (amount > available) {
                showError("Not enough available balance");
                return;
            }

            balance -= amount;
            refreshWallet();

        } catch (Exception e) {
            showError("Invalid amount");
        }
    }

    // ===== WALLET =====
    private void refreshWallet() {
        balanceLabel.setText("Balance: " + balance);
        reservedLabel.setText("Reserved: " + reserved);
        availableLabel.setText("Available: " + (balance - reserved));
    }

    // ===== HISTORY =====
    private void renderBidHistory() {
        bidHistoryList.getItems().setAll(bidHistory);
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}