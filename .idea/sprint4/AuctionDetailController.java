package client.app.controller;

import client.app.network.ServerMessageCallback;
import client.app.network.ServerMessageListener;
import client.app.model.ServerToClientMessage;
import client.app.model.BidTransaction;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.animation.*;
import javafx.util.Duration;

import java.net.URL;
import java.time.*;
import java.util.ResourceBundle;

public class AuctionDetailController implements Initializable, ServerMessageCallback {

    private ServerMessageListener messageListener;
    private Timeline countdownTimeline;
    private LocalDateTime serverEndTime;

    // ===== FXML BINDINGS =====
    @FXML private Label auctionTitleLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label endTimeLabel;

    @FXML private Label walletBalanceLabel;
    @FXML private Label walletReservedLabel;
    @FXML private Label walletAvailableLabel;

    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> bidderColumn;
    @FXML private TableColumn<BidTransaction, Number> amountColumn;
    @FXML private TableColumn<BidTransaction, String> timeColumn;

    @FXML private TextField bidAmountTextField;
    @FXML private Button placeBidButton;

    // ===== INIT =====
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Setup table columns
        bidderColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getBidder()));

        amountColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleDoubleProperty(data.getValue().getAmount()));

        timeColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTime().toString()));

        // IMPORTANT: messageListener must be injected/set before initialize is called
        if (messageListener != null) {
            messageListener.registerCallback(this);
        }
    }

    // ===== SET LISTENER (CALL FROM OUTSIDE) =====
    public void setMessageListener(ServerMessageListener listener) {
        this.messageListener = listener;
        if (listener != null) {
            listener.registerCallback(this);
        }
    }

    // ===== RECEIVE MESSAGES =====
    @Override
    public void onMessageReceived(ServerToClientMessage message) {

        switch (message.eventType) {
            case "AUCTION_UPDATE":
                handleAuctionUpdate(message);
                break;

            case "WALLET_UPDATE":
                handleWalletUpdate(message);
                break;

            case "STATE_CHANGE":
                handleStateChange(message);
                break;
        }
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            placeBidButton.setDisable(true);
            endTimeLabel.setText("DISCONNECTED");
        });
    }

    // ===== HANDLERS =====

    private void handleAuctionUpdate(ServerToClientMessage msg) {

        Platform.runLater(() -> {
            currentBidLabel.setText(String.valueOf(msg.currentBid));

            if (msg.bidHistory != null) {
                bidHistoryTable.getItems().setAll(msg.bidHistory);
            }

            // Start countdown if first time
            if (msg.endTime != null) {
                serverEndTime = msg.endTime;
                startCountdownTimer(serverEndTime, msg.serverTimeMs);
            }
        });
    }

    private void handleWalletUpdate(ServerToClientMessage msg) {

        Platform.runLater(() -> {
            walletBalanceLabel.setText(String.valueOf(msg.balance));
            walletReservedLabel.setText(String.valueOf(msg.reserved));
            walletAvailableLabel.setText(String.valueOf(msg.available));
        });
    }

    private void handleStateChange(ServerToClientMessage msg) {

        Platform.runLater(() -> {
            boolean isRunning = "RUNNING".equals(msg.state);
            placeBidButton.setDisable(!isRunning);

            if ("FINISHED".equals(msg.state)) {
                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                }
                endTimeLabel.setText("FINISHED");
            }
        });
    }

    // ===== COUNTDOWN TIMER =====

    private void startCountdownTimer(LocalDateTime endTime, long serverCurrentMs) {

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        long offset = System.currentTimeMillis() - serverCurrentMs;

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {

                    long now = System.currentTimeMillis() - offset;

                    LocalDateTime nowTime = Instant.ofEpochMilli(now)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    long remaining = java.time.Duration.between(nowTime, endTime).toMillis();

                    if (remaining <= 0) {
                        endTimeLabel.setText("FINISHED");
                        countdownTimeline.stop();
                        return;
                    }

                    long seconds = remaining / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;

                    String formatted = String.format("%02d:%02d:%02d",
                            hours,
                            minutes % 60,
                            seconds % 60);

                    endTimeLabel.setText(formatted);
                })
        );

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // ===== CLEANUP =====
    public void cleanup() {

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (messageListener != null) {
            messageListener.unregisterCallback(this);
        }
    }
}