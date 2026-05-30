package app.controller;

import app.MainApp;
import app.ThemeManager;
import app.model.Auction;
import app.model.AuctionStatus;
import app.model.Bid;
import app.model.User;
import app.service.AuctionObserver;
import app.service.BidResult;
import app.service.IAuctionService;
import app.service.NetworkAuctionService;
import app.service.NotificationService;
import com.ltnc.auction.shared.dto.WalletDto;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;

public class AuctionDetailController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label itemNameLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label currentBidLabel;

    @FXML
    private Label timeLeftLabel;

    @FXML
    private Label startingBidLabel;

    @FXML
    private Label sellerLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label auctionIdLabel;

    @FXML
    private Label categoryDetailLabel;

    @FXML
    private Label startingBidDetailLabel;

    @FXML
    private Label currentPriceDetailLabel;

    @FXML
    private Label autoBidFeedbackLabel;

    @FXML
    private Label totalBidsDetailLabel;

    @FXML
    private Label startTimeDetailLabel;

    @FXML
    private Label endTimeDetailLabel;

    @FXML
    private HBox highestBidderRow;

    @FXML
    private HBox winnerRow;

    @FXML
    private Label winnerValueLabel;

    @FXML
    private Label winnerLabel;

    @FXML
    private Label minimumBidLabel;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Label bidMessageLabel;

    @FXML
    private Label extendedBadgeLabel;

    @FXML
    private ListView<Bid> bidHistoryList;

    @FXML
    private LineChart<String, Number> bidChart;

    @FXML
    private ImageView itemImageView;

    @FXML
    private Label daysValueLabel;

    @FXML
    private Label hoursValueLabel;

    @FXML
    private Label minsValueLabel;

    @FXML
    private Label secsValueLabel;

    @FXML
    private Label statusBadge;

    @FXML
    private Button placeBidButton;

    @FXML
    private TextField autoBidMaxField;

    @FXML
    private TextField autoBidIncrementField;



    @FXML
    private Label highestBidderTitleLabel;

    @FXML
    private Label highestBidderValueLabel;

    @FXML
    private VBox biddingContainer;

    @FXML
    private VBox autoBidContainer;

    @FXML
    private VBox finalResultContainer;

    @FXML
    private Label finalPriceLabel;

    @FXML
    private Label finalWinnerLabel;

    @FXML
    private HBox finalWinnerRow;

    @FXML
    private Label finalStatusLabel;

    @FXML
    private Button enableAutoBidButton;

    private Auction auction;
    private Timeline timeline;
    private final IAuctionService auctionService = MainApp.getAuctionService();
    private final NetworkAuctionService netAuction = NetworkAuctionService.getInstance();
    private AuctionObserver auctionObserver;
    private final javafx.collections.ObservableList<Bid> bidHistoryItems = FXCollections.observableArrayList();
    private javafx.animation.PauseTransition uiUpdateDebouncer;
    private final java.util.Set<String> animatedBidIds = new java.util.HashSet<>();
    private boolean isAutoBidActive = false;

    @FXML
    private void initialize() {
        bidHistoryList.setCellFactory(listView -> new ListCell<Bid>() {
            @Override
            protected void updateItem(Bid bid, boolean empty) {
                super.updateItem(bid, empty);
                if (empty || bid == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox container = new HBox();
                    container.setSpacing(12);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setStyle("-fx-padding: 8 0;");

                    StackPane avatarStack = new StackPane();
                    avatarStack.setPrefSize(32, 32);
                    avatarStack.setMinSize(32, 32);
                    avatarStack.setMaxSize(32, 32);
                    
                    String name = bid.getBidder() != null ? bid.getBidder() : "?";
                    String firstLetter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                    int hash = name.hashCode();
                    String[] colors = {"#a855f7", "#ec4899", "#3b82f6", "#10b981", "#f59e0b", "#6366f1", "#14b8a6"};
                    String color = colors[Math.abs(hash) % colors.length];
                    
                    avatarStack.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 16;");
                    
                    Label letterLabel = new Label(firstLetter);
                    letterLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13px;");
                    avatarStack.getChildren().add(letterLabel);

                    VBox textContainer = new VBox();
                    textContainer.setSpacing(2);
                    
                    Label bidderLabel = new Label(name);
                    bidderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-fg-default; -fx-font-size: 13px;");
                    
                    var formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm");
                    Label timeLabel = new Label(bid.getTimestamp().format(formatter));
                    timeLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 11px;");
                    
                    textContainer.getChildren().addAll(bidderLabel, timeLabel);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label amountLabel = new Label(formatCurrency(bid.getAmount()));
                    amountLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: -color-success-fg; -fx-font-size: 13px; -fx-background-color: -color-success-subtle; -fx-padding: 4 10; -fx-background-radius: 12;");

                    container.getChildren().addAll(avatarStack, textContainer, spacer, amountLabel);
                    setGraphic(container);
                    setText(null);

                    if (getIndex() == 0 && bid.getId() != null && !animatedBidIds.contains(bid.getId())) {
                        animatedBidIds.add(bid.getId());
                        javafx.animation.FadeTransition flash = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), container);
                        flash.setFromValue(0.4);
                        flash.setToValue(1.0);
                        flash.setCycleCount(4);
                        flash.setAutoReverse(true);
                        flash.play();
                    }
                }
            }
        });

        bidHistoryList.setItems(bidHistoryItems);

        uiUpdateDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        uiUpdateDebouncer.setOnFinished(e -> {
            if (auction == null) {
                return;
            }
            Auction updated = netAuction.findCachedAuction(auction.getId());
            if (updated != null) {
                auction = updated;
            }
            refreshDetailLabels();
            renderBidHistory();
            updateExtendedBadge();
            updateStatusBadge();
        });

        auctionObserver = () -> {
            Platform.runLater(() -> {
                if (uiUpdateDebouncer != null) {
                    uiUpdateDebouncer.playFromStart();
                }
            });
        };
        netAuction.registerObserver(auctionObserver);
    }

    /** Cleanup method to be called by MainApp when switching views. */
    public void cleanup() {
        if (uiUpdateDebouncer != null) {
            uiUpdateDebouncer.stop();
        }
        if (auctionObserver != null) {
            netAuction.unregisterObserver(auctionObserver);
        }
        if (timeline != null) {
            timeline.stop();
        }
    }

    public void setAuction(Auction auction) {
        if (auction == null) {
            return;
        }

        boolean isNewAuction = this.auction == null || !this.auction.getId().equals(auction.getId());
        double oldBid = this.auction != null ? this.auction.getCurrentBid() : 0;
        this.auction = auction;
        if (isNewAuction) {
            bidHistoryItems.clear();
            animatedBidIds.clear();
            isAutoBidActive = false;
            if (autoBidContainer != null) {
                autoBidContainer.getStyleClass().remove("autobid-container-active");
            }
            if (enableAutoBidButton != null) {
                enableAutoBidButton.setText(ThemeManager.get("detail.autoBid.enable"));
                enableAutoBidButton.getStyleClass().remove("autobid-btn-active");
            }
            if (autoBidMaxField != null) autoBidMaxField.clear();
            if (autoBidIncrementField != null) autoBidIncrementField.clear();
        }

        titleLabel.setText(ThemeManager.get("detail.header"));
        itemNameLabel.setText(auction.getTitle());
        descriptionLabel.setText(auction.getDescription());
        // Safe Image Loading
        app.util.ImageLoader.loadImage(itemImageView, auction.getImage(), 520, 520, true);
        refreshDetailLabels();
        renderBidHistory();

        updateExtendedBadge();
        updateStatusBadge();

        startCountdown();
        updateBiddingVisibility();
        
        if (oldBid > 0 && auction.getCurrentBid() > oldBid) {
            flashLabel(currentBidLabel);
            if (currentPriceDetailLabel != null) flashLabel(currentPriceDetailLabel);
        }
    }
    
    private void flashLabel(Label label) {
        if (label == null) return;
        label.setStyle("-fx-background-color: -color-success-subtle; -fx-text-fill: -color-success-fg; -fx-padding: 4 8; -fx-background-radius: 4;");
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        pause.setOnFinished(e -> label.setStyle("")); // reset to default
        pause.play();
    }

    private void updateBiddingVisibility() {
        if (auction == null) return;

        User currentUser = MainApp.getCurrentUser();
        // Bidding is only for BIDDERS. Admins and Sellers shouldn't see bidding actions.
        boolean isBidder = currentUser == null || currentUser.getRole() == app.model.UserRole.BIDDER;

        boolean isClosed = auction.getStatus() == AuctionStatus.FINISHED 
                || auction.getStatus() == AuctionStatus.PAID 
                || auction.getStatus() == AuctionStatus.CANCELED;

        boolean showBidding = isBidder && !isClosed;

        if (biddingContainer != null) {
            biddingContainer.setVisible(showBidding);
            biddingContainer.setManaged(showBidding);
        }
        if (autoBidContainer != null) {
            autoBidContainer.setVisible(showBidding);
            autoBidContainer.setManaged(showBidding);
        }
        if (finalResultContainer != null) {
            finalResultContainer.setVisible(isClosed);
            finalResultContainer.setManaged(isClosed);
        }
    }

    private void updateStatusBadge() {
        if (auction == null || statusBadge == null) return;
        
        String status = auction.getStatus().name();
        statusBadge.setText(status);
        
        // Dynamic styling based on status
        if ("ACTIVE".equals(status)) {
            statusBadge.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis;");
        } else if ("FINISHED".equals(status)) {
            statusBadge.setStyle("-fx-background-color: -color-neutral-emphasis; -fx-text-fill: -color-fg-emphasis;");
        } else {
            statusBadge.setStyle("-fx-background-color: -color-warning-emphasis; -fx-text-fill: -color-fg-emphasis;");
        }
    }

    private String getHighestBidder(Auction auction) {
        if (auction.getBids() == null || auction.getBids().isEmpty()) {
            return "-";
        }
        return auction.getBids().stream()
                .max(Comparator.comparingDouble(Bid::getAmount))
                .map(Bid::getBidder)
                .orElse("-");
    }

    private void updateBidButtonState() {
        if (placeBidButton == null) return;
        placeBidButton.setDisable(false);
        String currentText = bidMessageLabel.getText();
        String insufficientText = ThemeManager.get("bid.insufficient.balance");
        if (insufficientText != null && insufficientText.equals(currentText)) {
            bidMessageLabel.setText("");
        }
    }

    private void showBidMessage(String message, boolean isError) {
        if (bidMessageLabel != null) {
            bidMessageLabel.setText(message);
            if (isError) {
                bidMessageLabel.setStyle("-fx-text-fill: -color-danger-emphasis; -fx-font-size: 13px; -fx-font-weight: bold;");
            } else {
                bidMessageLabel.setStyle("-fx-text-fill: -color-success-emphasis; -fx-font-size: 13px; -fx-font-weight: bold;");
            }
        }
        String title = isError ? ThemeManager.get("notification.error") : ThemeManager.get("notification.success");
        NotificationService.NotificationType type = isError ? 
            NotificationService.NotificationType.ERROR : 
            NotificationService.NotificationType.SUCCESS;
        NotificationService.getInstance().showNotification(title, message, type);
    }

    private void refreshDetailLabels() {
        if (auction == null) {
            return;
        }
        currentBidLabel.setText(formatCurrency(auction.getCurrentBid()));
        startingBidLabel.setText(formatCurrency(auction.getStartingBid()));
        timeLeftLabel.setText(formatTimeLeft(auction));
        sellerLabel.setText(auction.getSeller());
        minimumBidLabel.setText(MessageFormat.format(
                ThemeManager.get("detail.bid.enterMin"), formatCurrency(auction.getCurrentBid() + 10)));
        statusLabel.setText(auction.getStatus().name());
        
        // Detailed Labels Populating
        if (auctionIdLabel != null) auctionIdLabel.setText(auction.getId());
        if (categoryDetailLabel != null) categoryDetailLabel.setText(auction.getCategory());
        if (startingBidDetailLabel != null) startingBidDetailLabel.setText(formatCurrency(auction.getStartingBid()));
        if (currentPriceDetailLabel != null) currentPriceDetailLabel.setText(formatCurrency(auction.getCurrentBid()));
        if (totalBidsDetailLabel != null) totalBidsDetailLabel.setText(String.valueOf(auction.getBids().size()));
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (startTimeDetailLabel != null && auction.getStartTime() != null) {
            startTimeDetailLabel.setText(auction.getStartTime().format(dtf));
        }
        if (endTimeDetailLabel != null && auction.getEndTime() != null) {
            endTimeDetailLabel.setText(auction.getEndTime().format(dtf));
        }

        boolean isClosed = auction.getStatus() == AuctionStatus.FINISHED 
                || auction.getStatus() == AuctionStatus.PAID 
                || auction.getStatus() == AuctionStatus.CANCELED;

        if (isClosed) {
            if (highestBidderRow != null) {
                highestBidderRow.setVisible(false);
                highestBidderRow.setManaged(false);
            }
            if (winnerRow != null) {
                winnerRow.setVisible(true);
                winnerRow.setManaged(true);
            }
            
            String winner = auction.getWinnerBidder();
            boolean hasWinner = winner != null && !winner.isBlank() && !winner.equals("-");
            
            if (winnerValueLabel != null) {
                if (!hasWinner) {
                    winnerValueLabel.setText("No winner");
                    winnerValueLabel.setStyle("-fx-text-fill: -color-danger-emphasis; -fx-font-weight: bold;");
                } else {
                    winnerValueLabel.setText(winner);
                    winnerValueLabel.setStyle("-fx-text-fill: -color-success-emphasis; -fx-font-weight: bold;");
                }
            }

            // Populate Final Result Container
            if (finalPriceLabel != null) {
                finalPriceLabel.setText(formatCurrency(auction.getCurrentBid()));
            }
            if (finalWinnerLabel != null) {
                finalWinnerLabel.setText(hasWinner ? winner : "No Winner");
                finalWinnerLabel.setStyle(hasWinner ? "-fx-text-fill: -color-fg-default; -fx-font-weight: bold;" : "-fx-text-fill: -color-danger-emphasis; -fx-font-weight: bold;");
            }
            if (finalStatusLabel != null) {
                finalStatusLabel.setText(auction.getStatus().name());
                if (auction.getStatus() == AuctionStatus.PAID) {
                    finalStatusLabel.setStyle("-fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-emphasis;");
                } else if (auction.getStatus() == AuctionStatus.CANCELED) {
                    finalStatusLabel.setStyle("-fx-background-color: -color-danger-emphasis; -fx-text-fill: -color-fg-emphasis;");
                } else {
                    finalStatusLabel.setStyle("-fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-emphasis;");
                }
            }
        } else {
            if (highestBidderRow != null) {
                highestBidderRow.setVisible(true);
                highestBidderRow.setManaged(true);
            }
            if (winnerRow != null) {
                winnerRow.setVisible(false);
                winnerRow.setManaged(false);
            }
            if (highestBidderValueLabel != null) {
                highestBidderValueLabel.setText(getHighestBidder(auction));
            }
        }
        
        updateExtendedBadge();
        updateBidButtonState();
    }

    private void updateExtendedBadge() {
        if (auction == null) {
            return;
        }
        if (auction.isExtendedByAntiSniping()) {
            extendedBadgeLabel.setText(ThemeManager.get("detail.antisniping"));
        } else {
            extendedBadgeLabel.setText("");
        }
    }

    private LocalDateTime nowAdjusted() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(netAuction.getAdjustedNowMillis()),
                ZoneId.systemDefault());
    }

    @FXML
    private void onPlaceBid() {
        if (!MainApp.isLoggedIn()) {
            MainApp.setPendingAuction(auction != null ? auction.getId() : null);
            MainApp.showSignInPage();
            return;
        }

        if (auction == null || bidAmountField.getText() == null || bidAmountField.getText().isBlank()) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(bidAmountField.getText());
        } catch (Exception e) {
            showBidMessage(ThemeManager.get("wallet.invalidAmount"), true);
            return;
        }

        double minRequired = (auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingBid()) + 10.0;
        if (amount < minRequired) {
            showBidMessage(ThemeManager.get("bid.tooLow"), true);
            return;
        }

        app.service.LoadingService.getInstance().show();
        new Thread(() -> {
            BidResult result = auctionService.placeBid(MainApp.getCurrentUser(), auction.getId(), amount);
            Platform.runLater(() -> {
                app.service.LoadingService.getInstance().hide();
                if (result == BidResult.OK) {
                    Auction updated = netAuction.findCachedAuction(auction.getId());
                    if (updated != null) {
                        auction = updated;
                    }
                    bidAmountField.clear();
                    showBidMessage(ThemeManager.get("bid.success"), false);
                    refreshDetailLabels();
                    renderBidHistory();
                    return;
                }
                if (result == BidResult.INSUFFICIENT_FUNDS) {
                    Double need = netAuction.consumeLastBidRequiredTopUp();
                    TextInputDialog dialog = new TextInputDialog(need != null ? String.format(Locale.US, "%.2f", need) : "");
                    dialog.setTitle(ThemeManager.get("bid.insufficient.title"));
                    String header = ThemeManager.get("bid.insufficient.headerBase")
                            + (need != null ? MessageFormat.format(ThemeManager.get("bid.insufficient.hint"), formatCurrency(need)) : "");
                    dialog.setHeaderText(header);
                    dialog.setContentText(ThemeManager.get("bid.insufficient.prompt"));
                    dialog.showAndWait().ifPresent(s -> {
                        try {
                            double topUp = Double.parseDouble(s.trim());
                            app.service.LoadingService.getInstance().show();
                            new Thread(() -> {
                                boolean success = netAuction.deposit(MainApp.getCurrentUser(), topUp);
                                Platform.runLater(() -> {
                                    app.service.LoadingService.getInstance().hide();
                                    if (topUp > 0 && success) {
                                        showBidMessage(ThemeManager.get("bid.retryAfterTopUp"), false);
                                        onPlaceBid();
                                    } else {
                                        showBidMessage(ThemeManager.get("bid.topUpFail"), true);
                                    }
                                });
                            }).start();
                        } catch (Exception ex) {
                            showBidMessage(ThemeManager.get("bid.topUpInvalid"), true);
                        }
                    });
                    return;
                }
                if (result == BidResult.AUCTION_CLOSED) {
                    showBidMessage(ThemeManager.get("bid.auctionClosed"), true);
                } else if (result == BidResult.BID_TOO_LOW) {
                    showBidMessage(ThemeManager.get("bid.tooLow"), true);
                } else if (result == BidResult.INVALID_AMOUNT) {
                    showBidMessage(ThemeManager.get("wallet.invalidAmount"), true);
                } else {
                    showBidMessage(ThemeManager.get("bid.error.generic"), true);
                }
            });
        }).start();
    }

    @FXML
    private void onEnableAutoBid() {
        if (auction == null || !MainApp.isLoggedIn()) {
            showBidMessage(ThemeManager.get("detail.login.required"), true);
            return;
        }

        if (isAutoBidActive) {
            isAutoBidActive = false;
            if (autoBidContainer != null) {
                autoBidContainer.getStyleClass().remove("autobid-container-active");
            }
            if (enableAutoBidButton != null) {
                enableAutoBidButton.setText(ThemeManager.get("detail.autoBid.enable"));
                enableAutoBidButton.getStyleClass().remove("autobid-btn-active");
            }
            showBidMessage(ThemeManager.get("autobid.disabled"), false);
            return;
        }

        try {
            double maxBid = Double.parseDouble(autoBidMaxField.getText());
            double increment = Double.parseDouble(autoBidIncrementField.getText());

            double currentPrice = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingBid();
            if (maxBid <= currentPrice) {
                showBidMessage(ThemeManager.get("autobid.error.maxBidTooLow"), true);
                return;
            }
            if (increment <= 0) {
                showBidMessage(ThemeManager.get("autobid.error.stepTooLow"), true);
                return;
            }

            User user = MainApp.getCurrentUser();
            app.service.LoadingService.getInstance().show();
            new Thread(() -> {
                boolean success = auctionService.registerAutoBid(auction.getId(), user.getEmail(), maxBid, increment);
                Platform.runLater(() -> {
                    app.service.LoadingService.getInstance().hide();
                    if (success) {
                        isAutoBidActive = true;
                        if (autoBidContainer != null && !autoBidContainer.getStyleClass().contains("autobid-container-active")) {
                            autoBidContainer.getStyleClass().add("autobid-container-active");
                        }
                        if (enableAutoBidButton != null) {
                            enableAutoBidButton.setText(ThemeManager.get("autobid.disable"));
                            if (!enableAutoBidButton.getStyleClass().contains("autobid-btn-active")) {
                                enableAutoBidButton.getStyleClass().add("autobid-btn-active");
                            }
                        }
                        showBidMessage(ThemeManager.get("autobid.enabled"), false);
                    } else {
                        showBidMessage(ThemeManager.get("autobid.error.failed"), true);
                    }
                });
            }).start();
        } catch (NumberFormatException exception) {
            showBidMessage(ThemeManager.get("autobid.invalid"), true);
        } catch (Exception exception) {
            showBidMessage(ThemeManager.get("autobid.error.failed"), true);
        }
    }

    private void renderBidHistory() {
        if (auction == null) {
            return;
        }
        var bids = auction.getBids().stream()
                .sorted(Comparator.comparing(Bid::getTimestamp).reversed())
                .toList();
        if (bidHistoryItems.isEmpty()) {
            bidHistoryItems.setAll(bids);
        } else {
            java.util.Set<String> existingIds = new java.util.HashSet<>();
            for (Bid b : bidHistoryItems) {
                if (b != null && b.getId() != null) {
                    existingIds.add(b.getId());
                }
            }
            for (int i = bids.size() - 1; i >= 0; i--) {
                Bid b = bids.get(i);
                if (b != null && b.getId() != null && !existingIds.contains(b.getId())) {
                    bidHistoryItems.add(0, b);
                }
            }
        }
        renderBidChart();
    }

    private void renderBidChart() {
        XYChart.Series<String, Number> series;
        if (bidChart.getData().isEmpty()) {
            series = new XYChart.Series<>();
            bidChart.getData().add(series);
        } else {
            series = bidChart.getData().get(0);
        }

        series.getData().clear();

        var sorted = auction.getBids().stream()
                .sorted(Comparator.comparing(Bid::getTimestamp))
                .toList();
        var formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (Bid bid : sorted) {
            String label = bid.getBidder() + " (" + bid.getTimestamp().format(formatter) + ")";
            XYChart.Data<String, Number> data = new XYChart.Data<>(label, bid.getAmount());
            series.getData().add(data);

            java.util.function.Consumer<javafx.scene.Node> configureNode = (node) -> {
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    ThemeManager.get("detail.bidder") + ": " + bid.getBidder() + "\n" +
                    ThemeManager.get("detail.time") + ": " + bid.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                    ThemeManager.get("detail.amount") + ": " + formatCurrency(bid.getAmount())
                );
                tooltip.setShowDelay(javafx.util.Duration.ZERO);
                tooltip.setHideDelay(javafx.util.Duration.ZERO);
                tooltip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: -color-bg-overlay; -fx-text-fill: -color-fg-default; -fx-border-color: -color-border-default; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");
                javafx.scene.control.Tooltip.install(node, tooltip);

                node.setOnMouseEntered(e -> {
                    node.setScaleX(1.5);
                    node.setScaleY(1.5);
                    node.setCursor(javafx.scene.Cursor.HAND);
                });
                node.setOnMouseExited(e -> {
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                    node.setCursor(javafx.scene.Cursor.DEFAULT);
                });
            };

            if (data.getNode() != null) {
                configureNode.accept(data.getNode());
            }
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    configureNode.accept(newNode);
                }
            });
        }
    }

    @FXML
    private void onBackToHome() {
        MainApp.showHomePage();
    }


    private String formatCurrency(double amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return numberFormat.format(amount);
    }

    private String formatTimeLeft(Auction value) {
        Duration duration = Duration.between(nowAdjusted(), value.getEndTime());
        if (duration.isNegative()) {
            return ThemeManager.get("detail.time.ended");
        }
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return hours + "h " + minutes + "m";
    }

    private void startCountdown() {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateCountdown()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        updateCountdown();
    }

    private void updateCountdown() {
        if (auction == null) {
            return;
        }
        java.time.Duration d = java.time.Duration.between(nowAdjusted(), auction.getEndTime());
        if (d.isNegative()) {
            daysValueLabel.setText("0");
            hoursValueLabel.setText("0");
            minsValueLabel.setText("0");
            secsValueLabel.setText("0");
            
            if (auction.getStatus() == AuctionStatus.RUNNING || auction.getStatus() == AuctionStatus.OPEN) {
                statusLabel.setText("CLOSING...");
                if (placeBidButton != null) placeBidButton.setDisable(true);
            } else {
                statusLabel.setText(auction.getStatus().name());
            }
            return;
        }
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long mins = d.toMinutes() % 60;
        long secs = d.toSeconds() % 60;
        daysValueLabel.setText(String.valueOf(days));
        hoursValueLabel.setText(String.valueOf(hours));
        minsValueLabel.setText(String.valueOf(mins));
        secsValueLabel.setText(String.valueOf(secs));
        statusLabel.setText(auction.getStatus().name());
        
        long totalMins = d.toMinutes();
        if (totalMins < 5) {
            daysValueLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
            hoursValueLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
            minsValueLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
            secsValueLabel.setStyle("-fx-text-fill: -color-danger-emphasis;");
            triggerTimerPulse();
        } else {
            daysValueLabel.setStyle("");
            hoursValueLabel.setStyle("");
            minsValueLabel.setStyle("");
            secsValueLabel.setStyle("");
            stopTimerPulse();
        }
        
        updateExtendedBadge();
    }

    private javafx.animation.ScaleTransition timerPulseTransition;

    private void triggerTimerPulse() {
        if (timerPulseTransition == null) {
            timerPulseTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), secsValueLabel.getParent());
            timerPulseTransition.setFromX(1.0);
            timerPulseTransition.setFromY(1.0);
            timerPulseTransition.setToX(1.1);
            timerPulseTransition.setToY(1.1);
            timerPulseTransition.setAutoReverse(true);
            timerPulseTransition.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timerPulseTransition.play();
        } else if (timerPulseTransition.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            timerPulseTransition.play();
        }
    }

    private void stopTimerPulse() {
        if (timerPulseTransition != null) {
            timerPulseTransition.stop();
            secsValueLabel.getParent().setScaleX(1.0);
            secsValueLabel.getParent().setScaleY(1.0);
        }
    }
}
