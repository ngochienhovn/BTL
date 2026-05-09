package app.controller;

import app.net.SocketClient;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Map;

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

    @FXML
    private Button bidButton;

    private Long auctionId;
    private double currentBid;

    private final SocketClient socketClient = SocketClient.getInstance();

    @FXML
    public void initialize() {
        socketClient.setMessageListener(this::handleServerMessage);
    }

    public void setAuction(Long auctionId, String title, double currentBid, String status) {
        this.auctionId = auctionId;
        this.currentBid = currentBid;

        titleLabel.setText(title);
        currentBidLabel.setText(formatMoney(currentBid));
        statusLabel.setText(status);
    }

    @FXML
    private void onPlaceBid() {
        double bid = parseAmount(bidAmountField.getText());

        if (auctionId == null) {
            messageLabel.setText("Chưa chọn phiên đấu giá.");
            return;
        }

        if (bid <= 0) {
            messageLabel.setText("INVALID_AMOUNT");
            return;
        }

        if (bid <= currentBid) {
            messageLabel.setText("BID_TOO_LOW");
            return;
        }

        // Sprint 5: debounce, khóa nút ngay khi bấm
        setBidButtonDisabled(true);
        messageLabel.setText("Đang gửi bid...");

        socketClient.sendBid(auctionId, bid);

        // Sau 1.5 giây mở lại nút, tránh spam click
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> setBidButtonDisabled(false));
        }).start();
    }

    private void handleServerMessage(ServerToClientMessage msg) {
        if (msg == null || msg.type == null) {
            return;
        }

        if (msg.type == MessageType.BID_RESULT) {
            handleBidResult(msg);
            return;
        }

        if (msg.type == MessageType.AUCTION_UPDATE) {
            handleAuctionUpdate(msg);
        }
    }

    private void handleBidResult(ServerToClientMessage msg) {
        if (msg.success) {
            messageLabel.setText("BID_RESULT: OK");
        } else {
            messageLabel.setText("BID_RESULT: " + safeText(msg.error, msg.code, "FAILED"));

            if (msg.requiredTopUp != null) {
                messageLabel.setText(messageLabel.getText() + " | Cần nạp thêm: " + formatMoney(msg.requiredTopUp));
            }
        }
    }

    private void handleAuctionUpdate(ServerToClientMessage msg) {
        if (msg.auction == null) {
            return;
        }

        Map<String, Object> auction = msg.auction;

        Long updatedAuctionId = getLong(auction.get("auctionId"));
        if (updatedAuctionId == null) {
            updatedAuctionId = getLong(auction.get("id"));
        }

        // Nếu update không phải phiên đang mở thì bỏ qua
        if (auctionId != null && updatedAuctionId != null && !auctionId.equals(updatedAuctionId)) {
            return;
        }

        Double newPrice = getDouble(auction.get("currentBid"));
        if (newPrice != null) {
            currentBid = newPrice;
            currentBidLabel.setText(formatMoney(currentBid));
        }

        Object bidder = auction.get("bidder");
        if (bidder != null) {
            messageLabel.setText("Giá mới từ: " + bidder);
        }
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

    private void setBidButtonDisabled(boolean disabled) {
        if (bidButton != null) {
            bidButton.setDisable(disabled);
        }
    }

    private double parseAmount(String value) {
        try {
            return Double.parseDouble(value == null ? "" : value.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    private String safeText(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private Double getDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return value == null ? null : Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return value == null ? null : Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}