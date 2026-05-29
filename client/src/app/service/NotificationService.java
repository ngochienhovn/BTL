package app.service;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationService {
    private static final NotificationService INSTANCE = new NotificationService();
    private Stage mainStage;
    private Popup globalPopup;
    private VBox toastContainer;

    private NotificationService() {
        globalPopup = new Popup();
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPadding(new Insets(15));
        globalPopup.getContent().add(toastContainer);
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public void showNotification(String title, String message, NotificationType type) {
        Platform.runLater(() -> {
            VBox toastCard = new VBox();
            toastCard.getStylesheets().add(getClass().getResource("/resources/css/home-page.css").toExternalForm());
            toastCard.getStyleClass().add("notification-toast");
            toastCard.getStyleClass().add("notification-" + type.name().toLowerCase());
            toastCard.setMinWidth(300);
            toastCard.setMaxWidth(350);

            HBox contentBox = new HBox(15);
            contentBox.setPadding(new Insets(15, 20, 10, 20));
            contentBox.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label(type.getEmoji());
            icon.setStyle("-fx-font-size: 24px;");

            VBox textFlow = new VBox(2);
            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: -color-fg-default;");
            Label msgLbl = new Label(message);
            msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-fg-muted;");
            msgLbl.setWrapText(true);

            textFlow.getChildren().addAll(titleLbl, msgLbl);
            contentBox.getChildren().addAll(icon, textFlow);
            HBox.setHgrow(textFlow, Priority.ALWAYS);

            // Progress bar at the bottom
            Region progressBar = new Region();
            progressBar.setPrefHeight(4);
            progressBar.setMaxHeight(4);
            progressBar.setStyle("-fx-background-color: " + type.getColor() + "; -fx-background-radius: 0 0 4 4;");
            // Set initial width to match the min width
            progressBar.setPrefWidth(350);

            toastCard.getChildren().addAll(contentBox, progressBar);

            // Add to container at the top so it pushes others down
            toastContainer.getChildren().add(0, toastCard);

            if (mainStage != null && mainStage.isShowing()) {
                if (!globalPopup.isShowing()) {
                    globalPopup.show(mainStage, 
                        mainStage.getX() + mainStage.getWidth() - 380, 
                        mainStage.getY() + 80);
                } else {
                    // update position in case window moved
                    globalPopup.setX(mainStage.getX() + mainStage.getWidth() - 380);
                    globalPopup.setY(mainStage.getY() + 80);
                }
            }

            // Animate In
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastCard);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Progress Bar Animation
            Timeline progressAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.prefWidthProperty(), 350)),
                new KeyFrame(Duration.seconds(4), new KeyValue(progressBar.prefWidthProperty(), 0))
            );
            
            // Animate Out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toastCard);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                toastContainer.getChildren().remove(toastCard);
                if (toastContainer.getChildren().isEmpty()) {
                    globalPopup.hide();
                }
            });

            progressAnim.setOnFinished(e -> fadeOut.play());
            progressAnim.play();
        });
    }

    public enum NotificationType {
        INFO("\u2139\ufe0f", "#3b82f6"), 
        SUCCESS("\u2705", "#10b981"), 
        WARNING("\u26a0\ufe0f", "#f59e0b"), 
        ERROR("\u274c", "#ef4444");
        
        private final String emoji;
        private final String color;
        NotificationType(String emoji, String color) { 
            this.emoji = emoji; 
            this.color = color;
        }
        public String getEmoji() { return emoji; }
        public String getColor() { return color; }
    }
}
