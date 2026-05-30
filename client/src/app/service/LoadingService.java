package app.service;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoadingService {
    private static final LoadingService INSTANCE = new LoadingService();
    private Stage mainStage;
    private Popup popup;
    private StackPane root;
    private int activeCount = 0;
    private javafx.animation.PauseTransition watchdog;

    private LoadingService() {
        Platform.runLater(this::initOverlay);
    }

    public static LoadingService getInstance() {
        return INSTANCE;
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    private void initOverlay() {
        root = new StackPane();
        root.setPrefSize(2000, 2000); // Large enough to cover screen
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        
        // Custom Spinner using Arc
        Arc spinner = new Arc();
        spinner.setCenterX(50.0f);
        spinner.setCenterY(50.0f);
        spinner.setRadiusX(25.0f);
        spinner.setRadiusY(25.0f);
        spinner.setStartAngle(45.0f);
        spinner.setLength(270.0f);
        spinner.setType(ArcType.OPEN);
        spinner.setFill(null);
        spinner.setStroke(Color.WHITE);
        spinner.setStrokeWidth(5);
        
        RotateTransition rotate = new RotateTransition(Duration.seconds(1), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rotate.play();

        Label text = new Label("Processing...");
        text.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        content.getChildren().addAll(spinner, text);
        root.getChildren().add(content);
        
        popup = new Popup();
        popup.getContent().add(root);
        popup.setHideOnEscape(false);

        // Setup watchdog to prevent permanent UI lock
        watchdog = new javafx.animation.PauseTransition(Duration.seconds(15));
        watchdog.setOnFinished(e -> {
            if (activeCount > 0) {
                activeCount = 0;
                hide();
                System.err.println("[LoadingService] Watchdog triggered: Force hiding overlay due to timeout.");
            }
        });
    }

    public void show() {
        Platform.runLater(() -> {
            activeCount++;
            if (activeCount == 1) {
                if (mainStage != null && mainStage.isShowing()) {
                    root.setPrefWidth(mainStage.getWidth());
                    root.setPrefHeight(mainStage.getHeight());
                    popup.show(mainStage, mainStage.getX(), mainStage.getY());
                    
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                }
            }
            watchdog.playFromStart();
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            activeCount--;
            if (activeCount <= 0) {
                activeCount = 0;
                watchdog.stop();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> popup.hide());
                fadeOut.play();
            }
        });
    }
}
