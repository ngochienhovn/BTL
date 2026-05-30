package app.controller;

import app.MainApp;
import javafx.application.Platform;
import app.model.Auction;
import app.model.AuctionStatus;
import app.model.User;
import app.model.UserRole;
import app.service.NetworkAuctionService;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.ScaleTransition;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public class AuctionCardController {
    private static final PseudoClass LIKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("liked");

    @FXML
    private VBox root;

    @FXML
    private StackPane imageWrap;

    @FXML
    private ImageView imageView;

    @FXML
    private Label badgeLabel;

    @FXML
    private Button likeButton;

    @FXML
    private SVGPath likeIcon;

    @FXML
    private Label titleLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label bidLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Label bidCountLabel;
    
    @FXML
    private HBox statusBadgeContainer;
    
    @FXML
    private Region progressFill;

    @FXML
    private Button actionButton;

    private Auction auction;
    private Timeline timerTimeline;

    @FXML
    public void initialize() {
        // Square corners for the image wrap (as requested)
        Rectangle clip = new Rectangle(230, 150);
        clip.setArcWidth(0);
        clip.setArcHeight(0);
        imageWrap.setClip(clip);
        
        // Smooth hover transition for the card (CSS translate-y is abrupt)
        javafx.animation.TranslateTransition hoverTranslate = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), root);
        root.setOnMouseEntered(e -> {
            hoverTranslate.stop();
            hoverTranslate.setToY(-6);
            hoverTranslate.play();
        });
        root.setOnMouseExited(e -> {
            hoverTranslate.stop();
            hoverTranslate.setToY(0);
            hoverTranslate.play();
        });
    }

    public void setAuction(Auction auction) {
        if (auction == null) return;
        
        // Check for price update to trigger pulse
        if (this.auction != null && this.auction.getId().equals(auction.getId())) {
            if (this.auction.getCurrentBid() < auction.getCurrentBid()) {
                triggerPulseAnimation();
            }
        }
        
        this.auction = auction;
        updateUI();
        startTimer();
    }

    private ScaleTransition pulseTransition;

    private void triggerPulseAnimation() {
        Platform.runLater(() -> {
            if (pulseTransition != null) {
                pulseTransition.stop();
            }
            pulseTransition = new ScaleTransition(javafx.util.Duration.millis(200), root);
            pulseTransition.setFromX(1.0);
            pulseTransition.setFromY(1.0);
            pulseTransition.setToX(1.02);
            pulseTransition.setToY(1.02);
            pulseTransition.setAutoReverse(true);
            pulseTransition.setCycleCount(2);
            pulseTransition.play();
        });
    }

    private void updateUI() {
        titleLabel.setText(auction.getTitle());
        descriptionLabel.setText(auction.getDescription());
        badgeLabel.setText(auction.getCategory());
        
        double oldBid = 0;
        try {
            // Safer parsing of current text
            String bidText = bidLabel.getText().replaceAll("[^\\d.]", "");
            if (!bidText.isEmpty()) oldBid = Double.parseDouble(bidText);
        } catch (Exception ignored) {}
        
        bidLabel.setText(formatCurrency(auction.getCurrentBid()));
        bidCountLabel.setText(auction.getBids().size() + " bids");
        
        // Visual feedback for bid increases
        if (oldBid > 0 && auction.getCurrentBid() > oldBid) {
            flashBidLabel();
            triggerPulseAnimation();
        }

        // Safe Image Loading with Fallback
        app.util.ImageLoader.loadImage(imageView, auction.getImage(), 284, 190, false);

        updateLikeButton();
        updateActionButton();
        updateStatusBadges();
        updateTimerLabel();
    }
    
    private void flashBidLabel() {
        Platform.runLater(() -> {
            bidLabel.setStyle("-fx-background-color: -color-success-subtle; -fx-text-fill: -color-success-fg; -fx-padding: 2 6; -fx-background-radius: 4;");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
            pause.setOnFinished(e -> bidLabel.setStyle("")); // reset to default
            pause.play();
        });
    }

    private void updateLikeButton() {
        User currentUser = MainApp.getCurrentUser();
        boolean isBidder = currentUser == null || currentUser.getRole() == UserRole.BIDDER;
        
        likeButton.setVisible(isBidder);
        likeButton.setManaged(isBidder);
        
        boolean isLiked = MainApp.isLiked(auction.getId());
        if (likeIcon != null) {
            // Background is now always white in CSS, so use RED when liked, SLATE when not.
            likeIcon.setFill(isLiked ? Color.valueOf("#ef4444") : Color.valueOf("#64748b"));
        }
        likeButton.pseudoClassStateChanged(LIKED_PSEUDO_CLASS, isLiked);
    }

    private void updateActionButton() {
        User currentUser = MainApp.getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        boolean isSeller = currentUser != null && currentUser.getRole() == UserRole.SELLER;

        if (isAdmin) {
            actionButton.setText("View Details");
        } else if (isSeller) {
            actionButton.setText("View Auction");
        } else {
            actionButton.setText("Place Bid");
            if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.CANCELED) {
                actionButton.setDisable(true);
            } else {
                actionButton.setDisable(false);
            }
        }
    }

    private void updateTimerLabel() {
        // Use synchronized server time
        long nowMillis = NetworkAuctionService.getInstance().getAdjustedNowMillis();
        LocalDateTime now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault());
        
        Duration totalDuration = Duration.between(auction.getStartTime(), auction.getEndTime());
        Duration remainingDuration = Duration.between(now, auction.getEndTime());
        
        updateProgressBar(totalDuration, remainingDuration);
        
        Duration duration = remainingDuration;
        
        if (duration.isNegative() || auction.getStatus() == AuctionStatus.FINISHED) {
            timerLabel.setText("Auction Ended");
            timerLabel.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-weight: 700;");
            if (timerTimeline != null) timerTimeline.stop();
        } else {
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.toSeconds() % 60;
 
            if (days > 0) {
                timerLabel.setText(String.format("⏳ %dd %dh %dm", days, hours, minutes));
                timerLabel.setStyle("-fx-text-fill: -color-fg-muted;");
            } else if (hours > 0) {
                timerLabel.setText(String.format("⏳ %dh %dm %ds", hours, minutes, seconds));
                timerLabel.setStyle("-fx-text-fill: -color-fg-muted;");
            } else {
                timerLabel.setText(String.format("⏳ %dm %ds", minutes, seconds));
                if (minutes < 5) {
                    timerLabel.setStyle("-fx-text-fill: -color-danger-emphasis; -fx-font-weight: 800;");
                    triggerTimerPulse();
                } else {
                    timerLabel.setStyle("-fx-text-fill: -color-attention-emphasis; -fx-font-weight: 700;");
                    stopTimerPulse();
                }
            }
        }
    }

    private ScaleTransition timerPulseTransition;

    private void triggerTimerPulse() {
        if (timerPulseTransition == null) {
            timerPulseTransition = new ScaleTransition(javafx.util.Duration.millis(500), timerLabel);
            timerPulseTransition.setFromX(1.0);
            timerPulseTransition.setFromY(1.0);
            timerPulseTransition.setToX(1.15);
            timerPulseTransition.setToY(1.15);
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
            timerLabel.setScaleX(1.0);
            timerLabel.setScaleY(1.0);
        }
    }

    private void updateProgressBar(Duration total, Duration remaining) {
        if (total.isZero() || total.isNegative()) {
            progressFill.setMaxWidth(0);
            return;
        }
        double percentage = (double) remaining.toMillis() / total.toMillis();
        percentage = Math.max(0, Math.min(1, percentage));
        
        progressFill.setMaxWidth(284 * percentage);
        
        // Color based on urgency
        progressFill.getStyleClass().removeAll("card-progress-fill-warning", "card-progress-fill-danger");
        if (percentage < 0.1) {
            progressFill.getStyleClass().add("card-progress-fill-danger");
        } else if (percentage < 0.25) {
            progressFill.getStyleClass().add("card-progress-fill-warning");
        }
    }

    private void updateStatusBadges() {
        statusBadgeContainer.getChildren().clear();
        
        // Ending Soon badge
        long nowMillis = NetworkAuctionService.getInstance().getAdjustedNowMillis();
        LocalDateTime now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault());
        Duration remaining = Duration.between(now, auction.getEndTime());
        
        if (!remaining.isNegative() && remaining.toHours() < 1 && 
            (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING)) {
            Label badge = new Label("Ending Soon");
            badge.getStyleClass().addAll("card-badge", "badge-ending");
            statusBadgeContainer.getChildren().add(badge);
        }
        
        // Hot badge (many bids)
        if (auction.getBids().size() >= 10) {
            Label badge = new Label("Hot \uD83D\uDD25");
            badge.getStyleClass().addAll("card-badge", "badge-hot");
            statusBadgeContainer.getChildren().add(badge);
        }
        
        // New badge
        Duration age = Duration.between(auction.getStartTime(), now);
        if (age.toHours() < 24 && !age.isNegative()) {
            Label badge = new Label("New");
            badge.getStyleClass().addAll("card-badge", "badge-new");
            statusBadgeContainer.getChildren().add(badge);
        }
    }

    private void startTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        timerTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateTimerLabel()));
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }

    @FXML
    private void onLikeClicked() {
        MainApp.toggleLiked(auction.getId());
        updateLikeButton();
        // Notify home page to update badge (if possible, or handle via global observer)
    }

    @FXML
    private void onActionClicked() {
        openAuctionDetail();
    }

    @FXML
    private void onCardClicked() {
        openAuctionDetail();
    }

    private void openAuctionDetail() {
        if (!MainApp.isLoggedIn() && MainApp.getCurrentUser() != null && MainApp.getCurrentUser().getRole() == UserRole.BIDDER) {
            // Should be handled by Detail page anyway
        }
        MainApp.showAuctionDetail(auction);
    }

    private String formatCurrency(double amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return numberFormat.format(amount);
    }
    
    public void cleanup() {
        if (timerTimeline != null) {
            timerTimeline.stop();
            timerTimeline = null;
        }
    }
}
