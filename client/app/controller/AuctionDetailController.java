package app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionDetailController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label currentBidLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Label walletLabel;

    @FXML
    private Label messageLabel;

    private Long auctionId;
    private double currentBid;

    public void setAuction(Long auctionId, String title, double currentBid, String status) {
        this.auctionId = auctionId;
        this.currentBid = currentBid;
        titleLabel.setText(title);
        currentBidLabel.setText(String.valueOf(currentBid));
        statusLabel.setText(status);
    }

    @FXML
    private void onPlaceBid() {
        double bid = parseAmount(bidAmountField.getText());
        if (bid <= currentBid) {
            messageLabel.setText("BID_TOO_LOW");
            return;
        }
        if (bid <= 0) {
            messageLabel.setText("INVALID_AMOUNT");
            return;
        }
        currentBid = bid;
        currentBidLabel.setText(String.valueOf(currentBid));
        messageLabel.setText("BID_RESULT: OK");
    }

    @FXML
    private void onDeposit() {
        messageLabel.setText("DEPOSIT_RESULT: OK");
        refreshWallet();
    }

    @FXML
    private void onWithdraw() {
        messageLabel.setText("WITHDRAW_RESULT: OK");
        refreshWallet();
    }

    private void refreshWallet() {
        walletLabel.setText("Balance: 0.0 | Reserved: 0.0 | Available: 0.0");
    }

    private void renderBidHistory() {
        // Placeholder for Sprint 3 bid history list rendering.
    }

    private double parseAmount(String value) {
        try {
            return Double.parseDouble(value == null ? "" : value.trim());
        } catch (Exception ex) {
            return -1;
        }
    }
}
